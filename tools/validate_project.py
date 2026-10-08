from pathlib import Path
import json, re, sys, tomllib
ROOT = Path(__file__).resolve().parents[1]
errors=[]

# Gradle properties used to validate expanded TOML metadata.
props={}
for raw in (ROOT/'gradle.properties').read_text(encoding='utf-8').splitlines():
    raw=raw.strip()
    if not raw or raw.startswith('#') or '=' not in raw:
        continue
    k,v=raw.split('=',1); props[k.strip()]=v.strip()

def expand(text):
    return re.sub(r'\$\{([^}]+)\}', lambda m: props.get(m.group(1), m.group(0)), text)

# JSON syntax (source placeholders are quoted and therefore still valid JSON).
for p in ROOT.rglob('*.json'):
    try: json.loads(p.read_text(encoding='utf-8'))
    except Exception as e: errors.append(f'JSON {p.relative_to(ROOT)}: {e}')

# Expanded Forge/NeoForge metadata must be valid TOML.
for rel in ['neoforge/src/main/resources/META-INF/neoforge.mods.toml','forge/src/main/resources/META-INF/mods.toml']:
    p=ROOT/rel
    try: tomllib.loads(expand(p.read_text(encoding='utf-8')))
    except Exception as e: errors.append(f'TOML {rel}: {e}')

# Common module must not import loader APIs.
for p in (ROOT/'common/src/main/java').rglob('*.java'):
    text=p.read_text(encoding='utf-8')
    if re.search(r'^import net\.(fabricmc|neoforged|minecraftforge)\.', text, re.M):
        errors.append(f'loader import in common: {p.relative_to(ROOT)}')

# Only explicitly client-side common classes may import Minecraft client APIs.
client_allowed={
    'FfmpegPlayer.java','IptvScreen.java','MineIptvClientCore.java','TelevisionRenderer.java','TvPlaybackManager.java'
}
for p in (ROOT/'common/src/main/java').rglob('*.java'):
    text=p.read_text(encoding='utf-8')
    if 'net.minecraft.client' in text and p.name not in client_allowed:
        errors.append(f'unexpected client import in common server-safe class: {p.relative_to(ROOT)}')

required=[
 'settings.gradle','gradle.properties','common/build.gradle','fabric/build.gradle','neoforge/build.gradle','forge/build.gradle',
 'fabric/src/main/resources/fabric.mod.json','neoforge/src/main/resources/META-INF/neoforge.mods.toml','forge/src/main/resources/META-INF/mods.toml',
 'common/src/main/java/me/riyo/mineiptv/network/TvControlPayload.java',
 'common/src/main/java/me/riyo/mineiptv/tv/TelevisionBlockEntity.java']
for rel in required:
    if not (ROOT/rel).is_file(): errors.append('missing '+rel)

# Recipe upgrade chain.
chain=['tv_1x1','tv_2x1','tv_2x2','tv_3x2','tv_4x3']
for i,name in enumerate(chain):
    p=ROOT/f'common/src/main/resources/data/mineiptv/recipe/{name}.json'
    if not p.is_file(): errors.append('missing recipe '+name); continue
    data=json.loads(p.read_text())
    if data.get('result',{}).get('id') != f'mineiptv:{name}': errors.append('bad result '+name)
    if i and f'mineiptv:{chain[i-1]}' not in p.read_text(): errors.append('upgrade does not consume previous TV: '+name)

# Multiplayer invariants.
be=(ROOT/'common/src/main/java/me/riyo/mineiptv/tv/TelevisionBlockEntity.java').read_text()
for needle in ['StreamUrl','Playing','Revision','Owner','getUpdatePacket','getUpdateTag','canEdit']:
    if needle not in be: errors.append('BE sync/ownership missing '+needle)
net=(ROOT/'common/src/main/java/me/riyo/mineiptv/network/MineIptvNetwork.java').read_text()
for needle in ['distanceToSqr','StreamUrlPolicy.allowed','TelevisionBlockEntity','canEdit']:
    if needle not in net: errors.append('network validation missing '+needle)

# Known stale symbols from the earlier Fabric-only branch must not survive.
for p in ROOT.rglob('*.java'):
    text=p.read_text(encoding='utf-8')
    if 'MineIptvClient.id(' in text or 'MineIptvMod' in text:
        errors.append(f'stale symbol in {p.relative_to(ROOT)}')

# 26.3 split Forge artifact/mod version and NeoForge mod-bus renderer lifecycle.
if props.get('forge_artifact_version') != f"{props.get('minecraft_version')}-{props.get('forge_version')}":
    errors.append('forge_artifact_version must be minecraft_version-forge_version')
neo_client=(ROOT/'neoforge/src/main/java/me/riyo/mineiptv/neoforge/MineIptvNeoForgeClient.java').read_text()
if 'EventBusSubscriber.Bus.MOD' not in neo_client:
    errors.append('NeoForge client lifecycle/render registration is not on MOD bus')

if errors:
    print('\n'.join('ERROR: '+x for x in errors)); sys.exit(1)
print('MineIPTV static validation OK')
print('loaders: fabric, neoforge, forge')
print('minecraft baseline: 26.3')
print('multiplayer state: server-authoritative + owner-protected block entity sync')
