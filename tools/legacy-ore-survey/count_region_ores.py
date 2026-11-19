"""Count real 1.7.10 Anvil Sections/Blocks/Add/Data with nbtlib + NumPy.

Usage: python count_region_ores.py SERVER_DIRECTORY [--nbtlib-path DIRECTORY]
Requires a stopped server, survey-ready.txt, and helper-recorded numeric ore ID.
No region data is modified. Written outputs are separate JSON/CSV reports.
"""
from pathlib import Path
from collections import Counter
import argparse,csv,gzip,io,json,struct,sys,zlib,hashlib

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('server',type=Path)
parser.add_argument('--nbtlib-path',type=Path)
args=parser.parse_args()
if args.nbtlib_path:sys.path.insert(0,str(args.nbtlib_path))
import nbtlib
import numpy as np

root=args.server.resolve();world=root/'world'
assert (root/'survey-ready.txt').exists(),'Population check must finish before counting'
environment=json.loads((root/'survey-environment.json').read_text())
level=nbtlib.load(world/'level.dat')
assert int(level['Data']['RandomSeed'])==environment['seed']==9202026
ore_id=environment['ore_numeric_id']
# Forge stores remapping information in the saved world; verify the ID instead
# of relying on a hard-coded block number across installations.
registry=level['FML']['ItemData']
registered=[r for r in registry if str(r['K']) in ('\u0001Thaumcraft:blockCustomOre','Thaumcraft:blockCustomOre')]
assert registered and any(int(r['V'])==ore_id for r in registered),registered

def read_chunk(x,z):
    p=world/'region'/f'r.{x//32}.{z//32}.mca'
    with p.open('rb') as f:
        f.seek(((x%32)+(z%32)*32)*4);location=int.from_bytes(f.read(4),'big')
        assert location,(x,z,'missing chunk')
        f.seek((location>>8)*4096);length=int.from_bytes(f.read(4),'big');compression=f.read(1)[0];data=f.read(length-1)
    raw=gzip.decompress(data) if compression==1 else zlib.decompress(data) if compression==2 else data
    assert compression in (1,2,3)
    return nbtlib.File.parse(io.BytesIO(raw))['Level']

def nibbles(tag):
    packed=np.asarray(tag).view(np.uint8)
    expanded=np.empty(packed.size*2,dtype=np.uint8)
    expanded[::2]=packed&15;expanded[1::2]=packed>>4
    return expanded

names=['wind','fire','water','earth','order','entropy']
counts=np.zeros(16,dtype=np.int64);stone=0;zeros=[];rows=[];biomes=Counter();height=Counter()
for z in range(1000,1064):
    for x in range(1000,1064):
        c=read_chunk(x,z)
        assert int(c['xPos'])==x and int(c['zPos'])==z
        assert int(c['TerrainPopulated'])==1,(x,z,'unpopulated')
        per_chunk=np.zeros(16,dtype=np.int64);chunkstone=0
        for section in c['Sections']:
            block_ids=np.asarray(section['Blocks']).view(np.uint8).astype(np.uint16)
            assert block_ids.size==4096
            if 'Add' in section:block_ids|=nibbles(section['Add']).astype(np.uint16)<<8
            chunkstone+=int(np.count_nonzero(block_ids==1))
            hits=block_ids==ore_id
            if np.any(hits):
                metadata=nibbles(section['Data'])
                per_chunk+=np.bincount(metadata[hits],minlength=16)
                positions=np.flatnonzero(hits & (metadata>=1) & (metadata<=6))
                for y,number in zip(*np.unique(int(section['Y'])*16+positions//256,return_counts=True)):
                    height[int(y)]+=int(number)
        counts+=per_chunk;stone+=chunkstone
        total=int(per_chunk[1:7].sum());biome=int(np.asarray(c['Biomes']).view(np.uint8)[8+8*16]);biomes[biome]+=1
        if total==0:zeros.append([x,z,biome])
        rows.append([x,z,biome,*map(int,per_chunk[1:7]),total,chunkstone])

total=int(counts[1:7].sum())
modern=json.loads((root.parent/'ore-survey-20261008-054/comparison.json').read_text())['current']
result={'minecraft':'1.7.10','thaumcraft':'4.2.3.5','forge':'10.13.4.1614','seed':9202026,
        'area_blocks':[1024,1024],'start_blocks':[16000,16000],'counted_chunks':len(rows),
        'all_chunks_terrain_populated':True,'ore_numeric_id':ore_id,'ore_id_verified_from_level_dat':True,
        'ore_names':names,'counts':list(map(int,counts[1:7])),'total':total,'per_chunk':total/len(rows),
        'stone':stone,'ore_fraction_of_stone_plus_ore':total/(stone+total),
        'cinnabar':int(counts[0]),'amber':int(counts[7]),'zero_ore_chunks':zeros,
        'min_per_chunk':min(r[9] for r in rows),'max_per_chunk':max(r[9] for r in rows),
        'biome_samples':dict(biomes),'ores_per_y':dict(sorted(height.items())),
        'modern_054_total':modern['total'],'modern_relative_change_percent':(modern['total']/total-1)*100,
        'parser':{'name':'nbtlib','version':nbtlib.__version__,'numpy':np.__version__}}
(root/'ore-counts.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
with (root/'ore-counts-per-chunk.csv').open('w',newline='',encoding='utf8') as f:
    w=csv.writer(f);w.writerow(['chunk_x','chunk_z','biome_at_centre',*names,'total_infused','stone']);w.writerows(rows)
hashes={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (world/'region').glob('*.mca')}
(root/'region-hashes.json').write_text(json.dumps(hashes,indent=2)+'\n')
print(json.dumps({k:v for k,v in result.items() if k not in ('ores_per_y','zero_ore_chunks','biome_samples')},ensure_ascii=False,indent=2))
