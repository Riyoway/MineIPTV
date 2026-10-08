#!/usr/bin/env python3
from __future__ import annotations

import io
import json
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[2]
DECLARED = ROOT / "compatibility" / "toolchains.json"
REPORT = ROOT / "compatibility" / "upstream-audit.json"
USER_AGENT = "MineIPTV-upstream-audit/1.0 (+https://github.com/Riyoway/MineIPTV)"


def get_bytes(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=60) as response:
        return response.read()


def get_text(url: str) -> str:
    return get_bytes(url).decode("utf-8")


def properties(text: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def gradle_from_wrapper(text: str) -> str:
    props = properties(text)
    url = props.get("distributionUrl", "")
    match = re.search(r"gradle-([0-9][0-9A-Za-z.\-]*)-(?:bin|all)\.zip", url)
    require(match is not None, f"cannot parse Gradle distributionUrl: {url!r}")
    return match.group(1)


def java_from_build(text: str) -> int:
    patterns = (
        r"JavaLanguageVersion\.of\((\d+)\)",
        r"options\.release\s*=\s*(\d+)",
        r"JavaVersion\.VERSION_(\d+)",
    )
    for pattern in patterns:
        match = re.search(pattern, text)
        if match:
            return int(match.group(1))
    raise AssertionError("cannot find Java target in build script")


def fabric_audit(mc: str, expected: dict, java: int) -> dict:
    ref = quote(mc, safe="")
    base = f"https://raw.githubusercontent.com/FabricMC/fabric-example-mod/{ref}"
    gp = properties(get_text(f"{base}/gradle.properties"))
    build = get_text(f"{base}/build.gradle")
    wrapper = get_text(f"{base}/gradle/wrapper/gradle-wrapper.properties")

    require(gp.get("minecraft_version") == mc,
            f"Fabric {mc}: minecraft_version={gp.get('minecraft_version')!r}")
    require(gp.get("loader_version") == expected["loader"],
            f"Fabric {mc}: loader {gp.get('loader_version')!r} != {expected['loader']!r}")
    require(gp.get("loom_version") == expected["loom"],
            f"Fabric {mc}: Loom {gp.get('loom_version')!r} != {expected['loom']!r}")
    require(gp.get("fabric_api_version") == expected["fabric_api"],
            f"Fabric {mc}: API {gp.get('fabric_api_version')!r} != {expected['fabric_api']!r}")
    require(f"id '{expected['loom_plugin']}'" in build,
            f"Fabric {mc}: expected plugin {expected['loom_plugin']!r} not found")

    observed_java = java_from_build(build)
    observed_gradle = gradle_from_wrapper(wrapper)
    require(observed_java == java,
            f"Fabric {mc}: Java {observed_java} != declared {java}")
    require(observed_gradle == expected["gradle"],
            f"Fabric {mc}: Gradle {observed_gradle} != {expected['gradle']}")

    if mc.startswith("26."):
        require("loom.officialMojangMappings()" not in build,
                f"Fabric {mc}: 26.x example unexpectedly applies old Mojang mapping setup")
    else:
        require("loom.officialMojangMappings()" in build,
                f"Fabric {mc}: pre-26.1 example does not apply Mojang mappings")

    return {
        "source": expected["source"],
        "loader": gp["loader_version"],
        "loom": gp["loom_version"],
        "loom_plugin": expected["loom_plugin"],
        "fabric_api": gp["fabric_api_version"],
        "java": observed_java,
        "gradle": observed_gradle,
    }


def neoforge_audit(mc: str, expected: dict, java: int) -> dict:
    repo = f"MDK-{mc}-ModDevGradle"
    base = f"https://raw.githubusercontent.com/NeoForgeMDKs/{repo}/main"
    gp = properties(get_text(f"{base}/gradle.properties"))
    build = get_text(f"{base}/build.gradle")
    wrapper = get_text(f"{base}/gradle/wrapper/gradle-wrapper.properties")

    require(gp.get("minecraft_version") == mc,
            f"NeoForge {mc}: minecraft_version={gp.get('minecraft_version')!r}")
    require(gp.get("neo_version") == expected["version"],
            f"NeoForge {mc}: version {gp.get('neo_version')!r} != {expected['version']!r}")

    plugin_match = re.search(r"id ['\"]net\.neoforged\.moddev['\"] version ['\"]([^'\"]+)['\"]", build)
    require(plugin_match is not None, f"NeoForge {mc}: ModDevGradle plugin declaration missing")
    observed_moddev = plugin_match.group(1)
    observed_gradle = gradle_from_wrapper(wrapper)
    observed_java = java_from_build(build)

    require(observed_moddev == expected["moddev"],
            f"NeoForge {mc}: ModDevGradle {observed_moddev} != {expected['moddev']}")
    require(observed_gradle == expected["gradle"],
            f"NeoForge {mc}: Gradle {observed_gradle} != {expected['gradle']}")
    require(observed_java == java,
            f"NeoForge {mc}: Java {observed_java} != declared {java}")

    return {
        "source": expected["source"],
        "neoforge": gp["neo_version"],
        "moddev": observed_moddev,
        "java": observed_java,
        "gradle": observed_gradle,
        "loader_version_range": gp.get("loader_version_range"),
        "parchment_minecraft_version": gp.get("parchment_minecraft_version"),
        "parchment_mappings_version": gp.get("parchment_mappings_version"),
    }


def forge_versions_from_maven() -> set[str]:
    url = "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"
    root = ET.fromstring(get_bytes(url))
    return {node.text for node in root.findall("./versioning/versions/version") if node.text}


def forge_audit(mc: str, expected: dict, java: int, published: set[str]) -> dict:
    prefix = f"{mc}-"
    matches = sorted(v for v in published if v.startswith(prefix))

    if not expected["available"]:
        require(not matches,
                f"Forge {mc}: declared unavailable but official Maven now contains {matches[-5:]}")
        return {
            "available": False,
            "source": expected["source"],
            "official_maven_matches": [],
        }

    artifact_version = f"{mc}-{expected['version']}"
    require(artifact_version in published,
            f"Forge {mc}: {artifact_version} is absent from official Maven metadata")

    data = get_bytes(expected["source"])
    with zipfile.ZipFile(io.BytesIO(data)) as zf:
        names = set(zf.namelist())
        require("build.gradle" in names, f"Forge {mc}: MDK missing build.gradle")
        require("gradle/wrapper/gradle-wrapper.properties" in names,
                f"Forge {mc}: MDK missing Gradle wrapper properties")
        build = zf.read("build.gradle").decode("utf-8")
        wrapper = zf.read("gradle/wrapper/gradle-wrapper.properties").decode("utf-8")

    fg_match = re.search(r"id ['\"]net\.minecraftforge\.gradle['\"] version ['\"]([^'\"]+)['\"]", build)
    require(fg_match is not None, f"Forge {mc}: ForgeGradle declaration missing from exact MDK")
    observed_fg = fg_match.group(1)
    observed_gradle = gradle_from_wrapper(wrapper)
    observed_java = java_from_build(build)
    require(observed_java == java,
            f"Forge {mc}: Java {observed_java} != declared {java}")

    declared_fg = expected.get("forgegradle")
    declared_gradle = expected.get("gradle")
    if declared_fg is not None:
        require(observed_fg == declared_fg,
                f"Forge {mc}: ForgeGradle {observed_fg!r} != {declared_fg!r}")
    if declared_gradle is not None:
        require(observed_gradle == declared_gradle,
                f"Forge {mc}: Gradle {observed_gradle!r} != {declared_gradle!r}")

    return {
        "available": True,
        "source": expected["source"],
        "forge": expected["version"],
        "artifact": artifact_version,
        "forgegradle": observed_fg,
        "java": observed_java,
        "gradle": observed_gradle,
    }


def main() -> int:
    declared = json.loads(DECLARED.read_text(encoding="utf-8"))
    published_forge = forge_versions_from_maven()
    report = {
        "schema": 1,
        "note": "Observed directly from official upstream sources by audit_upstream_toolchains.py",
        "versions": {},
    }

    for mc, target in declared["versions"].items():
        java = int(target["java"])
        print(f"::group::{mc}")
        try:
            fabric = fabric_audit(mc, target["fabric"], java)
            print(f"Fabric OK: {fabric}")
            neoforge = neoforge_audit(mc, target["neoforge"], java)
            print(f"NeoForge OK: {neoforge}")
            forge = forge_audit(mc, target["forge"], java, published_forge)
            print(f"Forge OK: {forge}")
            report["versions"][mc] = {
                "java": java,
                "fabric": fabric,
                "neoforge": neoforge,
                "forge": forge,
            }
        finally:
            print("::endgroup::")

    REPORT.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"Wrote {REPORT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"UPSTREAM AUDIT FAILED: {type(exc).__name__}: {exc}", file=sys.stderr)
        raise
