#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLCHAINS = ROOT / "compatibility" / "toolchains.json"
PROPS = ROOT / "gradle.properties"
NETWORKING = ROOT / "compatibility" / "networking-probe.json"


def load_properties(path: Path) -> tuple[list[str], dict[str, str]]:
    lines = path.read_text(encoding="utf-8").splitlines()
    values: dict[str, str] = {}
    for line in lines:
        if line.strip() and not line.lstrip().startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return lines, values


def write_properties(path: Path, original: list[str], values: dict[str, str]) -> None:
    seen: set[str] = set()
    out: list[str] = []
    for line in original:
        if line.strip() and not line.lstrip().startswith("#") and "=" in line:
            key = line.split("=", 1)[0].strip()
            if key in values:
                out.append(f"{key}={values[key]}")
                seen.add(key)
                continue
        out.append(line)
    for key, value in values.items():
        if key not in seen:
            out.append(f"{key}={value}")
    path.write_text("\n".join(out) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Retarget the disposable CI checkout for a MineIPTV source probe.")
    parser.add_argument("--minecraft", required=True)
    parser.add_argument("--loader", required=True, choices=("fabric", "neoforge", "forge"))
    args = parser.parse_args()

    matrix = json.loads(TOOLCHAINS.read_text(encoding="utf-8"))["versions"]
    if args.minecraft not in matrix:
        raise SystemExit(f"Unknown target: {args.minecraft}")
    target = matrix[args.minecraft]
    if args.loader == "forge" and not target["forge"].get("available", False):
        raise SystemExit(f"Forge is not published for Minecraft {args.minecraft}")

    network = json.loads(NETWORKING.read_text(encoding="utf-8"))["versions"].get(args.minecraft)
    if not network:
        raise SystemExit(f"No source-probe Common Networking artifact pinned for {args.minecraft}")

    original, values = load_properties(PROPS)
    values.update({
        "minecraft_version": args.minecraft,
        "minecraft_version_range": f"[{args.minecraft}]",
        "java_version": str(target["java"]),
        "fabric_loader_version": target["fabric"]["loader"],
        "fabric_api_version": target["fabric"]["fabric_api"],
        "neoforge_version": target["neoforge"]["version"],
        "common_network_version": network,
        "mod_version": f"0.2.0-dev+{args.minecraft}",
    })
    if target["forge"].get("available", False):
        forge = target["forge"]
        values["forge_version"] = forge["version"]
        values["forge_artifact_version"] = f"{args.minecraft}-{forge['version']}"
        major = forge["version"].split(".", 1)[0]
        values["forge_version_range"] = f"[{major},)"

    write_properties(PROPS, original, values)
    print(f"Prepared {args.loader} source probe for Minecraft {args.minecraft} / Java {target['java']} / networking {network}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
