"""Extract selected modern API classes with CFR for local compatibility research only."""
from pathlib import Path
import re
import subprocess
import sys
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
workspace = root.parents[1]
module, pattern = sys.argv[1:3]
legacy = module.startswith('legacy:')
module = module.removeprefix('legacy:')
target = root / ('evidence/collab-legacy' if legacy else 'evidence/collab-api') / module
target.mkdir(parents=True, exist_ok=True)
subset = target / 'selected.jar'
jar = workspace / 'Collaboration/jars' / (module + '.jar') if legacy else root / 'evidence/collab-jars' / (module + '-local.jar')
with ZipFile(jar) as source, ZipFile(subset, 'w') as output:
    for name in source.namelist():
        if name.endswith('.class') and re.search(pattern, name):
            output.writestr(name, source.read(name))
java = workspace / 'TC4/.tooling/jdk-17/bin/java.exe'
if not java.exists():
    java = Path(r'D:\Program Files\Java\jdk-17.0.5\bin\java.exe')
subprocess.run([str(java), '-jar', str(workspace / 'Collaboration/tools/cfr-0.152.jar'),
                str(subset), '--outputdir', str(target), '--silent', 'true'], check=True,
               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
print(module + ': ' + str(len(list(target.rglob('*.java')))) + ' classes read')
