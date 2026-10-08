#!/usr/bin/env python3
import json
import struct
import sys
import tomllib
from pathlib import Path
from zipfile import ZipFile

jar = Path(sys.argv[1])
assert jar.is_file(), f"missing jar: {jar}"

TV_IDS = ("tv_1x1", "tv_2x1", "tv_2x2", "tv_3x2", "tv_4x3")

with ZipFile(jar) as z:
    assert z.testzip() is None, "corrupt ZIP entry in JAR"
    names = set(z.namelist())

    required = {
        "META-INF/MANIFEST.MF",
        "META-INF/neoforge.mods.toml",
        "me/riyo/mineiptv/neoforge/MineIptvNeoForge.class",
        "me/riyo/mineiptv/neoforge/MineIptvNeoForgeClient.class",
        "mineiptv.png",
        "assets/mineiptv/blockstates/television.json",
        "assets/mineiptv/blockstates/tv_panel.json",
        "assets/mineiptv/models/block/television.json",
        "assets/mineiptv/models/block/tv_panel.json",
        "assets/mineiptv/lang/en_us.json",
        "assets/mineiptv/lang/ja_jp.json",
        "data/mineiptv/loot_table/blocks/television.json",
        "data/mineiptv/loot_table/blocks/tv_panel.json",
    }
    for tv_id in TV_IDS:
        required.add(f"assets/mineiptv/models/item/{tv_id}.json")
        required.add(f"data/mineiptv/recipe/{tv_id}.json")

    missing = sorted(required - names)
    assert not missing, f"missing required entries: {missing}"

    # Loader/resource-generation separation checks for the exact 1.21.1 NeoForge target.
    forbidden_exact = {
        "fabric.mod.json",
        "META-INF/mods.toml",  # legacy Forge metadata, not NeoForge metadata
    }
    leaked = sorted(forbidden_exact & names)
    assert not leaked, f"foreign/legacy loader metadata leaked into NeoForge jar: {leaked}"
    assert not any(n.startswith("assets/mineiptv/items/") for n in names), \
        "post-1.21.1 client-item definitions leaked into jar"
    assert not any(n.startswith("data/mineiptv/recipes/") for n in names), \
        "pre-1.21 plural recipe path leaked into jar"
    assert not any(n.startswith("data/mineiptv/loot_tables/") for n in names), \
        "pre-1.21 plural loot-table path leaked into jar"

    metadata_text = z.read("META-INF/neoforge.mods.toml").decode("utf-8")
    assert "${" not in metadata_text, "unexpanded Gradle placeholder in neoforge.mods.toml"
    metadata = tomllib.loads(metadata_text)
    assert metadata["modLoader"] == "javafml"
    assert metadata["loaderVersion"] == "[1,)"
    assert metadata["license"] == "MIT"
    mods = metadata.get("mods", [])
    assert len(mods) == 1 and mods[0]["modId"] == "mineiptv"
    assert mods[0]["version"] == "0.2.0-dev+1.21.1"
    assert mods[0]["logoFile"] == "mineiptv.png"
    deps = metadata["dependencies"]["mineiptv"]
    dep_by_id = {d["modId"]: d for d in deps}
    assert set(dep_by_id) == {"minecraft", "neoforge"}, f"unexpected required dependencies: {set(dep_by_id)}"
    assert dep_by_id["minecraft"]["versionRange"] == "[1.21.1]"
    assert dep_by_id["neoforge"]["versionRange"] == "[21.1.256,)"
    assert all(d["side"] == "BOTH" for d in deps)
    assert "commonnetwork" not in metadata_text.lower()

    manifest = z.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
    assert "Fabric-" not in manifest, "NeoLoom/Fabric manifest attributes leaked into NeoForge jar"

    # Parse every JSON shipped by the mod, not only a sample.
    json_docs = {}
    for name in sorted(names):
        if name.endswith(".json"):
            json_docs[name] = json.loads(z.read(name).decode("utf-8"))

    # Validate all local model references resolve to files that actually exist in the JAR.
    def iter_model_refs(value):
        if isinstance(value, dict):
            for key, child in value.items():
                if key in {"model", "parent"} and isinstance(child, str) and child.startswith("mineiptv:"):
                    yield child
                yield from iter_model_refs(child)
        elif isinstance(value, list):
            for child in value:
                yield from iter_model_refs(child)

    for source_name, doc in json_docs.items():
        for ref in iter_model_refs(doc):
            model_path = f"assets/mineiptv/models/{ref.split(':', 1)[1]}.json"
            assert model_path in names, f"{source_name} references missing model {ref} ({model_path})"

    # 1.21.1 item models use assets/<ns>/models/item/<id>.json.
    for tv_id in TV_IDS:
        item_model = json_docs[f"assets/mineiptv/models/item/{tv_id}.json"]
        assert item_model.get("parent") == "minecraft:item/generated", f"unexpected item model parent for {tv_id}"
        assert "layer0" in item_model.get("textures", {}), f"missing layer0 texture for {tv_id}"

    # Every size must be craftable and must produce its own registered item.
    custom_item_ids = {f"mineiptv:{tv_id}" for tv_id in TV_IDS}
    for tv_id in TV_IDS:
        recipe_name = f"data/mineiptv/recipe/{tv_id}.json"
        recipe = json_docs[recipe_name]
        assert recipe.get("type") == "minecraft:crafting_shaped", f"{recipe_name}: wrong recipe type"
        assert recipe.get("result", {}).get("id") == f"mineiptv:{tv_id}", f"{recipe_name}: wrong result"
        assert recipe.get("result", {}).get("count", 1) == 1, f"{recipe_name}: wrong result count"
        pattern = recipe.get("pattern")
        assert isinstance(pattern, list) and 1 <= len(pattern) <= 3 and all(1 <= len(row) <= 3 for row in pattern), \
            f"{recipe_name}: invalid crafting-grid pattern"
        for ingredient in recipe.get("key", {}).values():
            if isinstance(ingredient, dict):
                item = ingredient.get("item")
                if isinstance(item, str) and item.startswith("mineiptv:"):
                    assert item in custom_item_ids, f"{recipe_name}: unknown MineIPTV ingredient {item}"

    # Empty block loot tables are intentional because the multi-block teardown code emits exactly one TV item.
    for block in ("television", "tv_panel"):
        loot = json_docs[f"data/mineiptv/loot_table/blocks/{block}.json"]
        assert loot.get("type") == "minecraft:block"
        assert loot.get("pools") == []

    # Ensure all visible item names and the key mapping are localised in both bundled languages.
    for lang_name in ("en_us", "ja_jp"):
        lang = json_docs[f"assets/mineiptv/lang/{lang_name}.json"]
        assert "key.mineiptv.open" in lang
        assert "key.categories.mineiptv" in lang
        for tv_id in TV_IDS:
            assert f"item.mineiptv.{tv_id}" in lang, f"missing {tv_id} translation in {lang_name}"

    # The whole mod must be Java 21 bytecode, not just its entrypoint classes.
    class_files = sorted(n for n in names if n.startswith("me/riyo/mineiptv/") and n.endswith(".class"))
    assert class_files, "no MineIPTV class files found"
    for cls in class_files:
        data = z.read(cls)
        assert data[:4] == b"\xca\xfe\xba\xbe", f"invalid class file: {cls}"
        major = struct.unpack(">H", data[6:8])[0]
        assert major == 65, f"{cls} is class version {major}, expected Java 21 (65)"

print(f"validated {len(class_files)} classes and {len(json_docs)} JSON resources: {jar}")
