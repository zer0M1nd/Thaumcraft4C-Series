"""Export already-built, verified artifacts. Does not build dependencies or run games."""
from pathlib import Path
import hashlib, json, re, shutil, zipfile
root=Path(__file__).resolve().parents[1]
version=re.search(r"version='([^']+)'",(root/'build.gradle').read_text()).group(1)
metadata=(root/'src/main/resources/META-INF/mods.toml').read_text()
if f'version="{version}"' not in metadata:raise SystemExit('Version mismatch')
base=f'Thaumic-Energistics-Modern-Port-{version}'
out=root/'exports';out.mkdir(exist_ok=True)
approved=json.loads((root/'src/main/resources/META-INF/thaumicenergistics-modern-assets.json').read_text(encoding='utf8'))['textures']
for suffix in ['.jar','-sources.jar']:
    source=root/'build/libs'/f'{base}{suffix}'
    if not source.is_file():raise SystemExit(f'Build first: {source}')
    with zipfile.ZipFile(source) as z:
        bad=[n for n in z.namelist() if (n.endswith(('.png','.ogg','.obj','.lang')) and n not in approved) or n.startswith(('tc4revival/','thaumcraft/api/','appeng/'))]
        if bad:raise SystemExit(f'External content: {bad}')
        for name,digest in approved.items():
            if name not in z.namelist() or hashlib.sha256(z.read(name)).hexdigest()!=digest:
                raise SystemExit(f'Missing or changed approved texture: {name}')
        if suffix=='.jar':
            if any(n.startswith('temodern/test/') for n in z.namelist()):raise SystemExit('Test classes in release jar')
            if f'version="{version}"' not in z.read('META-INF/mods.toml').decode():raise SystemExit('Stale build')
    shutil.copy2(source,out/source.name)
files=[]
for folder in ['src','scripts','docs','gradle']:
    files.extend(p for p in (root/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts and not p.name.endswith('.pyc'))
files.extend(root/name for name in ['build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','README.md','LICENSE','.gitignore'])
# Include editable approved art and regeneration inputs, never the abandoned drafts.
files.extend(p for p in (root/'art/0920_02-handoff').rglob('*')
             if p.is_file() and '__pycache__' not in p.parts and 'references' not in p.relative_to(root/'art/0920_02-handoff').parts)
files.extend(p for p in (root/'art/resource-review-0.2.6').rglob('*') if p.is_file())
files.extend(p for p in (root/'art/report0921_1').rglob('*') if p.is_file() and 'references' not in p.relative_to(root/'art/report0921_1').parts)
with zipfile.ZipFile(out/f'{base}-project-source.zip','w',zipfile.ZIP_DEFLATED) as z:
    for file in sorted(files):z.write(file,file.relative_to(root).as_posix())
artifacts=sorted(out.glob(base+'*'))
checksums=''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in artifacts)
(out/f'SHA256SUMS-{version}.txt').write_text(checksums,encoding='utf8')
(out/'SHA256SUMS.txt').write_text(checksums,encoding='utf8')
print(checksums)
print('Project source files:',len(files))
