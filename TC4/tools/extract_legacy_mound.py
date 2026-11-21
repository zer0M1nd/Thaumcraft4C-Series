"""Extract ordered literal writes from the read-only TC4 mound source."""
import re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
s=(root/'references/deobfuscated_src/thaumcraft/common/lib/world/WorldGenMound.java').read_text()
s=s[s.index('    public boolean generate('):s.index('        ChestGenHooks info3')]
pattern=r'world.setBlock\(i \+ (\d+), j \+ (\d+), k \+ (\d+), (?:\(Block\))?Blocks\.(\w+)(?:, (\d+), (\d+))?\);'
rows=re.findall(pattern,s)
assert len(rows)==s.count('world.setBlock(')
output=root/'src/main/resources/data/thaumcraft/legacy_structures/mound.csv'
output.parent.mkdir(parents=True,exist_ok=True)
output.write_text(''.join(','.join((x,y,z,block,meta or '0',flags or '3'))+'\n' for x,y,z,block,meta,flags in rows),encoding='utf-8')
print('Ordered mound writes:',len(rows))
