"""Data-only bridges for modern Twilight Forest, Special Mobs and local Thaumic Tinkerer."""
import re,json
from pathlib import Path

def generate(out):
    mod=out.MOD
    source=Path('references/ForbiddenMagic-GTNH/src/main/java/fox/spiteful/forbidden/compat/Compat.java').read_text(encoding='utf8')
    aspects={'BEAST':'bestia','CLOTH':'pannus','METAL':'metallum','DEATH':'mortuus','UNDEAD':'exanimis','FLIGHT':'volatus','GREED':'lucrum','MIND':'cognitio','PLANT':'herba','FIRE':'ignis','SLIME':'limus','MAGIC':'praecantatio','COLD':'gelum','POISON':'venenum','ELDRITCH':'alienis','FLESH':'corpus','GLUTTONY':mod+':gula','LUST':mod+':luxuria','PRIDE':mod+':superbia','NETHER':mod+':infernus'}
    names={'Wild Boar':'boar','Bighorn Sheep':'bighorn_sheep','Wild Deer':'deer','Twilight Wraith':'wraith','Forest Squirrel':'squirrel','Forest Bunny':'dwarf_rabbit','Forest Raven':'raven','Twilight Kobold':'kobold','Mini Ghast':'carminite_ghastling','Tower Ghast':'carminite_ghastguard','Tower Golem':'carminite_golem','Tower Termite':'towerwood_borer','WinterWolf':'winter_wolf'}
    tf={}
    for old,aspect in re.findall(r'spawnerMobs.put\("TwilightForest.([^"]+)", (?:DarkAspects|Aspect).(\w+)\)',source):
        name=names.get(old,old.lower().replace(' ','_'))
        # Keep the lust entry; its noLust fallback is selected at runtime.
        if name not in tf:tf[name]=aspects[aspect]
    modern_names=Path('references/modern-mobs/TFEntityNames.java').read_text()
    for name in tf:
        if '"'+name+'"' not in modern_names:raise ValueError('Unknown Twilight entity '+name)
    out.write(f'data/{mod}/forbidden_magic/cage_mobs/twilightforest.json',{'twilightforest:'+k:v for k,v in tf.items()})
    families=Path('references/modern-mobs/MobFamily.java').read_text()
    available=set()
    for match in re.finditer(r'new MobFamily<>\(.*?"(\w+)",\s*"[^"]+",.*?new EntityType\[\].*?},(.*?)\);',families,re.S):
        family,variants=match.groups();available.add(family.lower());available.update((v+family).lower() for v in re.findall(r'"(\w+)"',variants))
    sm={};removed=[]
    for old,aspect in re.findall(r'spawnerMobs.put\("SpecialMobs.([^"]+)", (?:(?:DarkAspects|Aspect).(\w+)|silver)\)',source):
        name=old.removeprefix('Special').replace('Brutish','Brute').replace('PigZombie','ZombifiedPiglin').lower()
        if name not in available:removed.append(old);continue
        sm['specialmobs:'+name]=aspects[aspect] if aspect else 'lucrum'
    out.write(f'data/{mod}/forbidden_magic/cage_mobs/specialmobs.json',sm)
    out.write('optional_packs/thaumic_tinkerer/pack.mcmeta',{'pack':{'pack_format':15,'description':'Forbidden Magic Thaumic Tinkerer integration'}})
    enchantments={
      'wrath':{'perditio':6,'ignis':6},'cluster':{'ignis':5,'terra':5,'ordo':5},'greedy':{'perditio':50,'terra':30,'ordo':20},
      'educational':{'aer':10,'ignis':10,'aqua':10,'terra':10,'ordo':15,'perditio':15},'consuming':{'perditio':10},
      'corrupting':{'perditio':20,'ignis':15,'terra':10},'voidtouched':{'perditio':70},'impact':{'perditio':30,'terra':30}}
    entries=[]
    for name,cost in enchantments.items():
        icon='lucrative' if name=='cluster' else name
        out.texture('misc',icon,'misc/'+icon)
        entries.append({'enchantment':mod+':'+name,'research':mod+':'+name.upper(),'icon':mod+':textures/misc/'+icon+'.png','aspects':cost})
    out.write(f'optional_packs/thaumic_tinkerer/data/{mod}/osmotic_enchantments/forbidden.json',{'entries':entries})
    out.write(f'optional_packs/thaumic_tinkerer/data/thaumic_tinkerer/item_aspects/zz_forbidden_magic_shards.json',{'format':1,'entries':[
      {'items':['thaumic_tinkerer:nether_shard'],'aspects':[{'aspect':mod+':infernus','amount':2},{'aspect':'praecantatio','amount':1},{'aspect':'vitreus','amount':1}]},
      {'items':['thaumic_tinkerer:ender_shard'],'aspects':[{'aspect':'alienis','amount':2},{'aspect':'praecantatio','amount':1},{'aspect':'vitreus','amount':1}]}]})
    out.write(f'data/{mod}/thaumcraft/original_resources.json',out.manifest)
    Path('references/modern-mobs/migration.json').write_text(json.dumps({'twilight_count':len(tf),'special_count':len(sm),'special_removed_no_matching_modern_species':removed},indent=2)+'\n',encoding='utf8')
    print('Wrote',len(tf),'Twilight and',len(sm),'Special Mobs cage entries; 8 osmotic enchantments.')
