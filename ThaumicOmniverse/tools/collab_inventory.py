"""Pin optional API JARs from the local pack; keep third-party bytes out of our artifact."""
import hashlib
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = Path(r'E:\MC\All the mods 9 TC\.minecraft\versions\All the Mods 9\mods')
NAMES = {
    'botania': 'Botania-', 'immersiveengineering': 'ImmersiveEngineering-',
    'farmersdelight': 'FarmersDelight-', 'mekanismtools': 'MekanismTools-',
    'mekanism': 'Mekanism-', 'jade': 'Jade-', 'create': 'create-1.',
    'apotheosis': 'Apotheosis-', 'sophisticatedbackpacks': 'sophisticatedbackpacks-',
    'sophisticatedcore': 'sophisticatedcore-', 'mysticalagriculture': 'MysticalAgriculture-',
    'productivebees': 'productivebees-', 'evilcraft': 'EvilCraft-',
    'cyclopscore': 'CyclopsCore-', 'journeymap': 'journeymap-',
    'allthemodium': 'allthemodium-', 'allthetweaks': 'allthetweaks-', 'kubejs': 'kubejs-',
}
dest = ROOT / 'evidence/collab-jars'
dest.mkdir(exist_ok=True)
# Retain pins acquired separately (nested Create libraries, optional legacy counterparts).
manifest = dest / 'manifest.json'
rows = json.loads(manifest.read_text(encoding='utf-8')) if manifest.exists() else []
for name, pattern in {**NAMES, 'tconstruct': 'TConstruct-', 'mantle': 'Mantle-'}.items():
    folder = PACK if name in NAMES else ROOT / 'evidence/modern-jars'
    candidates = list(folder.glob(pattern + '*.jar'))
    if len(candidates) != 1:
        raise ValueError((name, candidates))
    source = candidates[0]
    target = dest / (name + '-local.jar')
    shutil.copy2(source, target)
    rows = [row for row in rows if row['module'] != name]
    rows.append({'module': name, 'source': str(source), 'file': target.name,
                 'sha256': hashlib.sha256(target.read_bytes()).hexdigest()})
(dest / 'manifest.json').write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'Pinned {len(rows)} API JARs')
