"""Original EMTBlocks.addAspects registrations for objects still supplied by modern IC2."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources'
rows={
 'iron_fence':{'potentia':1,'metallum':2,'tempestas':1},
 'reinforced_glass':{'metallum':2,'gelum':2},
 'generator':{'metallum':2,'potentia':2,'machina':1},
 'water_generator':{'metallum':2,'potentia':1,'aqua':2,'machina':1},
 'solar_generator':{'metallum':1,'potentia':1,'lux':2,'machina':1},
 'wind_generator':{'potentia':1,'tempestas':3,'machina':1},
 'batbox':{'metallum':1,'potentia':3,'machina':1},
 'cesu':{'metallum':2,'potentia':4,'machina':1},
 'mfe':{'metallum':2,'potentia':5,'machina':1},
 'mfsu':{'metallum':2,'potentia':7,'machina':1},
 'iron_furnace':{'metallum':2,'ignis':2,'machina':1},
 'electric_furnace':{'metallum':2,'potentia':1,'ignis':3,'machina':1},
 'macerator':{'metallum':1,'potentia':2,'perfodio':2,'machina':1},
 'extractor':{'metallum':2,'potentia':1,'fabrico':2,'machina':1},
 'compressor':{'metallum':1,'potentia':2,'vacuos':3,'machina':1},
}
# Original rubberTrampoline/scaffold/ironScaffold no longer exist in IC2 Refactored.
# Do not assign their tags to unrelated blocks or invent new blocks solely for scanning.
path=root/'data/emt/item_aspects/ic2.json';path.parent.mkdir(parents=True,exist_ok=True)
path.write_text(json.dumps({'format':1,'entries':[{'items':['ic2:'+name],'aspects':[{'aspect':a,'amount':n} for a,n in values.items()]} for name,values in rows.items()]},indent=2)+'\n',encoding='utf8')
