"""Export the pre-split aspect definitions as historical comparison fixtures."""
import argparse,pathlib,json,zipfile,hashlib
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('output',type=pathlib.Path,help='directory for the historical fixture')
ROOT=parser.parse_args().output.resolve();BASE=pathlib.Path(__file__).resolve().parents[4]/'References';NS='thaumicomniverse'
defs=[
 ('terminus',0xB90000,['lucrum','alienis'],771,['avaritia'],'Avaritia-1.7.10-1.31.jar','assets/avaritia/textures/misc/terminus.png','Finality, Ending'),
 ('strontio',15647411,['cognitio','perditio'],1,['gtceu'],'gregtech-5.09.31.jar','assets/gregtech/textures/aspects/STRONTIO.png','Stupidness, Incompetence'),
 ('nebrisum',0xEEEE7E,['perfodio','lucrum'],1,['gtceu'],'gregtech-5.09.31.jar','assets/gregtech/textures/aspects/NEBRISUM.png','Cheatyness, Raiding'),
 ('electrum',0xC0EEEE,['potentia','machina'],1,['gtceu'],'gregtech-5.09.31.jar','assets/gregtech/textures/aspects/ELECTRUM.png','Electricity, Lightning'),
 ('magneto',0xC0C0C0,['metallum','iter'],1,['gtceu'],'gregtech-5.09.31.jar','assets/gregtech/textures/aspects/MAGNETO.png','Magnetism, Attraction'),
 ('radio',0xC0FFC0,['lux','potentia'],1,['gtceu'],'gregtech-5.09.31.jar','assets/gregtech/textures/aspects/RADIO.png','Radiation'),
 ('tofu',0xFFFFF0,['aqua','messis'],1,['tofucraft'],'Thaumcraft-1.7.10-4.2.3.5.jar','assets/thaumcraft/textures/aspects/terra.png','Tofu'),
 ('tempus',11963647,['vacuos','ordo'],1,['mobees'],'magicbees-1.7.10-2.4.4.jar','assets/magicbees/textures/aspects/tempus.png','Time'),
]
rows=[];provenance=[];lang={};zh=['终焉','愚昧','作弊','电力','磁性','辐射','豆腐','时间']
for i,(tag,color,components,blend,mods,jar,path,description) in enumerate(defs):
 src=BASE/('Projects/TC4' if tag=='tofu' else 'Collaboration/jars')/jar
 with zipfile.ZipFile(src) as z:
  actual=next(n for n in z.namelist() if n.lower()==path.lower());data=z.read(actual)
 dest=ROOT/f'src/main/resources/assets/{NS}/textures/aspects/{tag}.png';dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data)
 rows.append({'tag':NS+':'+tag,'color':color,'components':components,'chatColor':'','blendFactor':blend,'texture':NS+':textures/aspects/'+tag+'.png','required_mods':mods})
 lang['tc.aspect.'+NS+':'+tag]=description
 provenance.append({'tag':tag,'source_jar':jar,'source_entry':actual,'jar_sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'texture_sha256':hashlib.sha256(data).hexdigest(),'definition_source':'Collaboration/decompiled/'+('magicbees-1.7.10-2.4.4/magicbees/main/utils/compat/ThaumcraftHelper.java:110' if tag=='tempus' else 'TofuFactoryR-1.3.0-MC1.7.10/tsuteto/tofufactory/integration/plugins/PluginTC.java:76' if tag=='tofu' else 'gregtech-5.09.31/gregtech/common/GT_ThaumcraftCompat.java:101-105' if mods==['gtceu'] else 'Avaritia-1.7.10-1.31/fox/spiteful/avaritia/compat/thaumcraft/Lucrum.java:59')})
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
write(ROOT/f'src/main/resources/data/{NS}/thaumcraft/aspects.json',rows)
write(ROOT/f'src/main/resources/assets/{NS}/lang/en_us.json',lang)
write(ROOT/f'src/main/resources/assets/{NS}/lang/zh_cn.json',{'tc.aspect.'+NS+':'+r[0]:zh[i] for i,r in enumerate(defs)})
write(ROOT/'evidence/aspect-provenance.json',provenance)
