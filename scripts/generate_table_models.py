"""Bake legacy ModelTable / ModelResearchTable rigid bodies into chunk models.

The old model transform is translate(8,16,8), rotateX(180 degrees).
Dynamic inkwell, quill and paper remain block-entity rendered.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/thaumcraft/models/block'

def cube(x,y,z,dx,dy,dz,u,v,w,h):
    uv = {
        'west': [u,v+dz,u+dz,v+dz+dy],
        'east': [u+dz+dx,v+dz,u+2*dz+dx,v+dz+dy],
        'south': [u+dz,v+dz,u+dz+dx,v+dz+dy],
        'north': [u+2*dz+dx,v+dz,u+2*dz+2*dx,v+dz+dy],
        'up': [u+dz,v,u+dz+dx,v+dz],
        'down': [u+dz+dx,v+dz,u+dz+2*dx,v],
    }
    return {'from':[8+x,16-y-dy,8-z-dz], 'to':[8+x+dx,16-y,8-z],
            'faces':{face:{'texture':'#body','uv':[a*16/(w if i%2==0 else h) for i,a in enumerate(coords)],
                           **({'rotation':180} if face in ('north','south','east','west') else {})}
                     for face,coords in uv.items()}}

def model(research):
    w,h=(128,64) if research else (64,32)
    boxes = [(-8,0,-8,32,4,16,0,0)] if research else [(-8,0,-8,16,4,16,0,0)]
    if research:
        boxes += [(x,4,z,4,12,4,0,24) for x in (-6,18) for z in (-6,2)]
        boxes += [(-4,10,-2,24,4,4,24,24)]
    else:
        boxes += [(x,4,-2,4,8,4,0,20) for x in (2,-6)]
        boxes += [(-8,12,-4,16,4,8,16,20)]
    return {'ambientocclusion':False,'textures':{'particle':'thaumcraft:blocks/woodplain',
            'body':'thaumcraft:models/'+('restable' if research else 'table')},
            'elements':[cube(*b,w,h) for b in boxes]}

def write():
    OUT.mkdir(parents=True,exist_ok=True)
    for name,data in [('table_single',model(False)),('table_master',model(True))]:
        (OUT/(name+'.json')).write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')

if __name__ == '__main__': write()
