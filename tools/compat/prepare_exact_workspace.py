#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import json
import os
import re
import shutil
import sys
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLCHAINS = ROOT / "compatibility" / "toolchains.json"
NETWORKING = ROOT / "compatibility" / "networking-probe.json"
BASE_PROPERTIES = ROOT / "gradle.properties"
WORK_ROOT = ROOT / ".compat-work"

sys.path.insert(0, str(ROOT / "tools" / "compat"))
from preprocess_sources import preprocess_text  # noqa: E402


def read_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def version_tuple(value: str) -> tuple[int, ...]:
    return tuple(int(part) for part in value.split("."))


def copy_tree(source: Path, target: Path) -> None:
    if source.exists():
        shutil.copytree(source, target, dirs_exist_ok=True)


def preprocess_java(root: Path, minecraft: str) -> None:
    for file in root.rglob("*.java"):
        text = file.read_text(encoding="utf-8")
        file.write_text(preprocess_text(text, minecraft), encoding="utf-8")


def expand_resources(root: Path, values: dict[str, str], loader: str, minecraft: str) -> None:
    text_suffixes = {".json", ".toml", ".mcmeta", ".properties", ".lang", ".txt"}
    for file in root.rglob("*"):
        if not file.is_file() or file.suffix.lower() not in text_suffixes:
            continue
        try:
            text = file.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        for key, value in values.items():
            replacement = value
            if file.suffix.lower() in {".json", ".mcmeta"}:
                replacement = replacement.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")
            text = text.replace("${" + key + "}", replacement)
        file.write_text(text, encoding="utf-8")

    # NeoForge 1.21.x still requires the javafml loader header. NeoForge 26.x
    # removed modLoader/loaderVersion from the official metadata template.
    if loader == "neoforge" and version_tuple(minecraft) < version_tuple("26.1"):
        metadata = root / "META-INF" / "neoforge.mods.toml"
        if metadata.exists():
            text = metadata.read_text(encoding="utf-8")
            if "loaderVersion=" not in text:
                text = 'modLoader="javafml"\nloaderVersion="[1,)"\n' + text
                metadata.write_text(text, encoding="utf-8")


def source_and_resources(loader: str, minecraft: str, workspace: Path, values: dict[str, str]) -> None:
    java_root = workspace / "src" / "main" / "java"
    resources_root = workspace / "src" / "main" / "resources"
    copy_tree(ROOT / "common" / "src" / "main" / "java", java_root)
    copy_tree(ROOT / loader / "src" / "main" / "java", java_root)
    copy_tree(ROOT / "common" / "src" / "main" / "resources", resources_root)
    copy_tree(ROOT / loader / "src" / "main" / "resources", resources_root)
    preprocess_java(java_root, minecraft)
    expand_resources(resources_root, values, loader, minecraft)
    shutil.copy2(ROOT / "LICENSE", workspace / "LICENSE")


def fabric_workspace(workspace: Path, minecraft: str, target: dict, networking: str, values: dict[str, str]) -> tuple[str, str]:
    source_and_resources("fabric", minecraft, workspace, values)
    fabric = target["fabric"]
    plugin = fabric["loom_plugin"]
    loom = fabric["loom"]
    pre_26 = version_tuple(minecraft) < version_tuple("26.1")
    mapping_line = '    mappings loom.officialMojangMappings()\n' if pre_26 else ""
    dep_config = "modImplementation" if pre_26 else "implementation"

    (workspace / "settings.gradle").write_text(
        "pluginManagement {\n"
        "    repositories {\n"
        "        maven { url = 'https://maven.fabricmc.net/' }\n"
        "        gradlePluginPortal()\n"
        "        mavenCentral()\n"
        "    }\n"
        "}\n"
        "rootProject.name = 'MineIPTV-fabric-exact'\n",
        encoding="utf-8",
    )
    (workspace / "build.gradle").write_text(
        f"plugins {{\n    id '{plugin}' version '{loom}'\n    id 'java'\n}}\n\n"
        "group = 'me.riyo.mineiptv'\n"
        f"version = '0.2.0-dev+{minecraft}'\n\n"
        "repositories {\n"
        "    maven { url = 'https://maven.blamejared.com' }\n"
        "    mavenCentral()\n"
        "}\n\n"
        "dependencies {\n"
        f"    minecraft 'com.mojang:minecraft:{minecraft}'\n"
        f"{mapping_line}"
        f"    {dep_config} 'net.fabricmc:fabric-loader:{fabric['loader']}'\n"
        f"    {dep_config} 'net.fabricmc.fabric-api:fabric-api:{fabric['fabric_api']}'\n"
        f"    {dep_config} 'mysticdrew:common-networking-fabric:{networking}'\n"
        "}\n\n"
        f"tasks.withType(JavaCompile).configureEach {{ options.release = {target['java']} }}\n"
        "java {\n"
        f"    sourceCompatibility = JavaVersion.VERSION_{target['java']}\n"
        f"    targetCompatibility = JavaVersion.VERSION_{target['java']}\n"
        "}\n",
        encoding="utf-8",
    )
    # Fabric's own current 1.21.x example CI intentionally runs Loom on JDK 25
    # while compiling the mod to Java 21.
    return fabric["gradle"], "25"


def neoforge_workspace(workspace: Path, minecraft: str, target: dict, networking: str, values: dict[str, str]) -> tuple[str, str]:
    source_and_resources("neoforge", minecraft, workspace, values)
    neo = target["neoforge"]
    (workspace / "settings.gradle").write_text(
        "pluginManagement {\n"
        "    repositories {\n"
        "        maven { url = 'https://maven.neoforged.net/releases/' }\n"
        "        gradlePluginPortal()\n"
        "        mavenCentral()\n"
        "    }\n"
        "}\n"
        "rootProject.name = 'MineIPTV-neoforge-exact'\n",
        encoding="utf-8",
    )
    (workspace / "build.gradle").write_text(
        "plugins {\n"
        "    id 'java-library'\n"
        f"    id 'net.neoforged.moddev' version '{neo['moddev']}'\n"
        "}\n\n"
        "group = 'me.riyo.mineiptv'\n"
        f"version = '0.2.0-dev+{minecraft}'\n\n"
        "repositories {\n"
        "    maven { url = 'https://maven.blamejared.com' }\n"
        "    mavenCentral()\n"
        "}\n\n"
        "neoForge {\n"
        f"    version = '{neo['version']}'\n"
        "    mods {\n"
        "        mineiptv { sourceSet(sourceSets.main) }\n"
        "    }\n"
        "}\n\n"
        "dependencies {\n"
        f"    implementation 'mysticdrew:common-networking-neoforge:{networking}'\n"
        "}\n\n"
        "java {\n"
        f"    toolchain.languageVersion = JavaLanguageVersion.of({target['java']})\n"
        "}\n"
        "tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }\n",
        encoding="utf-8",
    )
    return neo["gradle"], str(target["java"])


def download(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "MineIPTV-exact-workspace/1.0"})
    with urllib.request.urlopen(request, timeout=90) as response:
        return response.read()


def forge_workspace(workspace: Path, minecraft: str, target: dict, networking: str, values: dict[str, str]) -> tuple[str, str]:
    forge = target["forge"]
    if not forge.get("available", False):
        raise SystemExit(f"Forge is not published for Minecraft {minecraft}")

    # Start from the exact MDK published for this exact Forge artifact. This is
    # deliberately stricter than trying to keep one generic ForgeGradle script.
    data = download(forge["source"])
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        archive.extractall(workspace)

    src = workspace / "src"
    if src.exists():
        shutil.rmtree(src)
    source_and_resources("forge", minecraft, workspace, values)

    build = workspace / "build.gradle"
    text = build.read_text(encoding="utf-8")
    text += (
        "\n// MineIPTV exact-probe dependency\n"
        "repositories { maven { url = 'https://maven.blamejared.com' } }\n"
        "dependencies {\n"
        f"    implementation 'mysticdrew:common-networking-forge:{networking}'\n"
        "}\n"
    )
    build.write_text(text, encoding="utf-8")
    return forge["gradle"], str(target["java"])


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate an isolated MineIPTV workspace using the exact official loader toolchain.")
    parser.add_argument("--loader", required=True, choices=("fabric", "neoforge", "forge"))
    parser.add_argument("--minecraft", required=True)
    parser.add_argument("--github-output", type=Path)
    args = parser.parse_args()

    matrix = json.loads(TOOLCHAINS.read_text(encoding="utf-8"))["versions"]
    network_matrix = json.loads(NETWORKING.read_text(encoding="utf-8"))["versions"]
    if args.minecraft not in matrix:
        raise SystemExit(f"Unknown Minecraft target: {args.minecraft}")
    if args.minecraft not in network_matrix:
        raise SystemExit(f"No Common Networking probe artifact pinned for {args.minecraft}")

    target = matrix[args.minecraft]
    networking = network_matrix[args.minecraft]
    base = read_properties(BASE_PROPERTIES)
    values = dict(base)
    values.update({
        "java_version": str(target["java"]),
        "minecraft_version": args.minecraft,
        "minecraft_version_range": f"[{args.minecraft}]",
        "fabric_loader_version": target["fabric"]["loader"],
        "fabric_api_version": target["fabric"]["fabric_api"],
        "neoforge_version": target["neoforge"]["version"],
        "common_network_version": networking,
        "mod_version": f"0.2.0-dev+{args.minecraft}",
    })
    if target["forge"].get("available", False):
        forge_version = target["forge"]["version"]
        values.update({
            "forge_version": forge_version,
            "forge_artifact_version": f"{args.minecraft}-{forge_version}",
            "forge_version_range": f"[{forge_version.split('.', 1)[0]},)",
        })

    workspace = WORK_ROOT / f"{args.loader}-{args.minecraft}"
    if workspace.exists():
        shutil.rmtree(workspace)
    workspace.mkdir(parents=True)

    if args.loader == "fabric":
        gradle, runtime_java = fabric_workspace(workspace, args.minecraft, target, networking, values)
    elif args.loader == "neoforge":
        gradle, runtime_java = neoforge_workspace(workspace, args.minecraft, target, networking, values)
    else:
        gradle, runtime_java = forge_workspace(workspace, args.minecraft, target, networking, values)

    relative = workspace.relative_to(ROOT).as_posix()
    print(f"Prepared exact {args.loader} workspace for Minecraft {args.minecraft}")
    print(f"workspace={relative} gradle={gradle} java={runtime_java}")
    if args.github_output:
        with args.github_output.open("a", encoding="utf-8") as output:
            output.write(f"workspace={relative}\n")
            output.write(f"gradle={gradle}\n")
            output.write(f"java={runtime_java}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
