"""Read-only verification of the seed-815491 natural village fixtures and resident UUIDs."""
import argparse,gzip,json,struct,zlib
from pathlib import Path
from verify_natural_biome_save import nbt,chunk,indices

def entities(world,x,z):
    path=world/'entities'/f'r.{x//32}.{z//32}.mca'
    if not path.exists():return []
    with path.open('rb') as f:
        f.seek(4*((x%32)+(z%32)*32));address=int.from_bytes(f.read(4),'big')
        if not address:return []
        f.seek((address>>8)*4096);length=int.from_bytes(f.read(4),'big');kind=f.read(1)[0];raw=f.read(length-1)
    assert kind in (1,2,3),kind
    data=nbt(gzip.decompress(raw) if kind==1 else zlib.decompress(raw) if kind==2 else raw)
    return data.get('Entities',[])

def signed(value):return value-(1<<32) if value&(1<<31) else value

def verify(world):
    result={}
    for role,(x,y,z) in {'wizard':(22569,63,17619),'banker':(22634,74,17993)}.items():
        saved=chunk(world,x//16,z//16);matches=[]
        for key,refs in saved['structures']['References'].items():
            if not key.startswith('minecraft:village_'):continue
            for ref in refs:
                start=chunk(world,signed(ref&0xffffffff),signed((ref>>32)&0xffffffff))['structures']['starts'][key]
                matches.extend(p for p in start['Children'] if p['id']=='thaumcraft:legacy_village_house' and p['Role']==role.upper() and p['Origin']=={'X':x,'Y':y,'Z':z})
        assert len(matches)==1,(role,'piece count',len(matches))
        piece=matches[0];assert piece['Resident'],(role,'resident flag')
        nearby=[]
        for cx in range(x//16-2,x//16+3):
            for cz in range(z//16-2,z//16+3):
                nearby.extend(e for e in entities(world,cx,cz) if e.get('id')=='minecraft:villager' and e.get('VillagerData',{}).get('profession')=='thaumcraft:'+role and abs(e['Pos'][0]-x)<=40 and abs(e['Pos'][2]-z)<=40)
        assert len(nearby)==1,(role,'resident count',len(nearby))
        if role=='wizard':
            chest=next(e for e in saved['block_entities'] if (e.get('x'),e.get('y'),e.get('z'))==(22567,69,17617))
            assert chest['Items'] and piece['Chest']
        result[role]={'origin':[x,y,z],'uuid':nearby[0]['UUID'],'profession':nearby[0]['VillagerData']['profession'],'resident_saved':bool(piece['Resident'])}
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--world',type=Path,default=Path('.tooling/natural-biome-server/natural-biome-validation'));parser.add_argument('--compare',type=Path)
    args=parser.parse_args();result=verify(args.world)
    if args.compare:
        previous=json.loads(args.compare.read_text(encoding='utf-8-sig'))
        assert result==previous,('restart changed village records',previous,result)
    print(json.dumps(result,indent=2))
