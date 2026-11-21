"""Read-only checks of natural-biome level.dat and actual saved region palettes."""
import argparse, gzip, io, struct, zlib
from pathlib import Path
from collections import Counter

def nbt(raw):
    f=io.BytesIO(raw)
    def number(fmt): return struct.unpack('>'+fmt,f.read(struct.calcsize('>'+fmt)))[0]
    def string(): return f.read(number('H')).decode('utf-8')
    def value(t):
        if t in range(1,7): return number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
        if t==7: return f.read(number('i'))
        if t==8: return string()
        if t==9:
            child=number('B'); return [value(child) for _ in range(number('i'))]
        if t==10:
            result={}
            while (child:=number('B')):
                name=string(); result[name]=value(child)
            return result
        if t in (11,12): return [number('i' if t==11 else 'q') for _ in range(number('i'))]
        raise ValueError(t)
    root=number('B'); string(); return value(root)

def chunk(root,x,z):
    path=root/'region'/f'r.{x//32}.{z//32}.mca'
    with path.open('rb') as f:
        f.seek(4*((x%32)+(z%32)*32)); location=int.from_bytes(f.read(4),'big')
        assert location, (x,z,'not saved')
        f.seek((location>>8)*4096); length=int.from_bytes(f.read(4),'big'); compression=f.read(1)[0]; raw=f.read(length-1)
    assert compression in (1,2,3),compression
    return nbt(gzip.decompress(raw) if compression==1 else zlib.decompress(raw) if compression==2 else raw)

def indices(storage,size,minimum_bits):
    palette=storage['palette']
    if len(palette)==1: return [0]*size
    bits=max(minimum_bits,(len(palette)-1).bit_length()); per=64//bits; mask=(1<<bits)-1
    return [(storage['data'][i//per]>>((i%per)*bits))&mask for i in range(size)]

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('world',type=Path);args=parser.parse_args();world=args.world
    root=nbt(gzip.decompress((world/'level.dat').read_bytes()))['Data']
    source=root['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']['biome_source']
    assert source['type']=='thaumcraft:legacy_natural',source
    assert source['delegate']['type']=='minecraft:multi_noise'
    assert source['forest_weight']==5 and source['taint_weight']==2 and source['seed']==815491
    print('Saved biome source PASS:',source)
    for x,z,biome in [(32,-32,'thaumcraft:magical_forest'),(16,16,'thaumcraft:tainted_land')]:
        c=chunk(world,x,z); section=next(s for s in c['sections'] if s['Y']==4); b=section['biomes']
        assert b['palette'][indices(b,64,1)[0]]==biome
        counts=Counter()
        for dx in range(-1,2):
            for dz in range(-1,2):
                for s in chunk(world,x+dx,z+dz)['sections']:
                    storage=s.get('block_states')
                    if storage:
                        palette=storage['palette'];counts.update(palette[i]['Name'] for i in indices(storage,4096,4))
        interesting={k:v for k,v in counts.items() if k.startswith('thaumcraft:') or k.endswith('_log') or k.endswith('_leaves')}
        print('Saved terrain PASS:',biome,(x,z),interesting)
        if biome.endswith('tainted_land'): assert counts['thaumcraft:taint_fibres']>0 and counts['thaumcraft:taint_crust']>0
        else: assert sum(v for k,v in counts.items() if k.endswith('_log'))>0
