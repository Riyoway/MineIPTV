#!/usr/bin/env python3
import json
import struct
import sys
import tomllib
from pathlib import Path
from zipfile import ZipFile

jar = Path(sys.argv[1])
assert jar.is_file(), f"missing jar: {jar}"

with ZipFile(jar) as z:
    names = set(z.namelist())
    required = {
        "META-INF/neoforge.mods.toml",
        "me/riyo/mineiptv/neoforge/MineIptvNeoForge.class",
        "me/riyo/mineiptv/neoforge/MineIptvNeoForgeClient.class",
        "mineiptv.png",
        "assets/mineiptv/models/item/tv_1x1.json",
        "data/mineiptv/recipe/tv_1x1.json",
    }
    missing = sorted(required - names)
    assert not missing, f"missing required entries: {missing}"
    assert "fabric.mod.json" not in names, "Fabric metadata leaked into NeoForge jar"
    assert not any(n.startswith("assets/mineiptv/items/") for n in names), "post-1.21.1 client-item definitions leaked into jar"

    metadata_text = z.read("META-INF/neoforge.mods.toml").decode("utf-8")
    assert "${" not in metadata_text, "unexpanded Gradle placeholder in neoforge.mods.toml"
    metadata = tomllib.loads(metadata_text)
    assert metadata["modLoader"] == "javafml"
    assert metadata["loaderVersion"] == "[1,)"
    mods = metadata.get("mods", [])
    assert len(mods) == 1 and mods[0]["modId"] == "mineiptv"
    deps = metadata["dependencies"]["mineiptv"]
    dep_by_id = {d["modId"]: d for d in deps}
    assert dep_by_id["minecraft"]["versionRange"] == "[1.21.1]"
    assert dep_by_id["neoforge"]["versionRange"] == "[21.1.256,)"

    manifest = z.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
    assert "Fabric-" not in manifest, "NeoLoom/Fabric manifest attributes leaked into NeoForge jar"

    for name in names:
        if name.endswith(".json"):
            json.loads(z.read(name).decode("utf-8"))

    for cls in (
        "me/riyo/mineiptv/neoforge/MineIptvNeoForge.class",
        "me/riyo/mineiptv/neoforge/MineIptvNeoForgeClient.class",
    ):
        data = z.read(cls)
        assert data[:4] == b"\xca\xfe\xba\xbe"
        major = struct.unpack(">H", data[6:8])[0]
        assert major == 65, f"{cls} is class version {major}, expected Java 21 (65)"

print(f"validated: {jar}")
