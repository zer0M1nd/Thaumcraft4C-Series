"""Install the 0.2.6 review corrections after the immutable 0920_02 handoff."""
from pathlib import Path
import hashlib
import json
import shutil

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'art/resource-review-0.2.6'
RES=ROOT/'src/main/resources'

def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

def apply(approved):
    manifest=json.loads((ART/'manifest.json').read_text(encoding='utf8'))
    for path,digest in manifest['files'].items():
        source=ART/'runtime'/path
        if hashlib.sha256(source.read_bytes()).hexdigest()!=digest:
            raise ValueError('Correction source differs from its manifest: '+path)
        target=RES/path;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
        if path.endswith('.png'):approved[path]=digest
    # Preserve the actual 1.1.3.0 GUI via the user's legacy JAR, without bundling its pixels.
    legacy_path=RES/'data/thaumicenergistics/thaumcraft/original_resources.json'
    legacy=json.loads(legacy_path.read_text(encoding='utf8'))
    for name in ['arcane.crafting','states']:
        path=f'assets/thaumicenergistics/textures/gui/{name}.png'
        legacy['mapping'][path]=path
    write(legacy_path,legacy)
    for locale,label in [('en_us','Swap armor'),('zh_cn','交换护甲')]:
        path=RES/f'assets/thaumicenergistics/lang/{locale}.json'
        data=json.loads(path.read_text(encoding='utf8'));data['gui.thaumicenergistics.swap_armor']=label
        write(path,data)

def prepare():
    """Prepare the layout and native AE2 model inputs for Blockbench's Java codec."""
    runtime=ART/'runtime'
    write(runtime/'assets/ae2/screens/terminals/thaumicenergistics_arcane.json',{
        'includes':['terminal.json'],
        'slots':{
            'CRAFTING_GRID':{'left':44,'bottom':153,'grid':'BREAK_AFTER_3COLS'},
            'CRAFTING_RESULT':{'left':116,'bottom':117},
            'thaumicenergistics_wand':{'left':116,'bottom':153},
            'thaumicenergistics_armor':{'left':8,'bottom':162,'grid':'VERTICAL'},
            'PLAYER_INVENTORY':{'left':8,'bottom':81,'grid':'BREAK_AFTER_9COLS'},
            'PLAYER_HOTBAR':{'left':8,'bottom':23,'grid':'HORIZONTAL'}},
        'widgets':{'clearCraftingGrid':{'left':98,'bottom':154},'swapArmor':{'left':26,'bottom':131}},
        'text':{'player_inventory_title':{'text':{'text':''},'position':{'left':8,'bottom':93}}},
        'terminalStyle':{'bottom':{'texture':'thaumicenergistics:textures/gui/arcane.crafting.png',
            'textureWidth':256,'textureHeight':256,'srcRect':[0,71,195,172]}}})
    native=ROOT/'references/AE2/src/main/resources/assets/ae2/models/item/display_base.json'
    model=json.loads(native.read_text(encoding='utf8'))
    model['textures']={'sides':'ae2:part/monitor_sides','back':'ae2:part/monitor_back',
                       'front':'ae2:part/monitor_front','grid':'thaumicenergistics:modern/part/terminal_green',
                       'particle':'#sides'}
    model['elements']=[e for e in model['elements'] if e['name'] in ['Front','Back']]
    # Coplanar transparent overlays are native AE2's display construction, with unchanged UV density.
    model['elements'].append({'name':'Green crafting grid','from':[2,2,8],'to':[14,14,9],
        'faces':{'north':{'texture':'#grid'}}})
    write(ART/'blockbench-input/arcane_terminal_item.json',model)

def seal():
    files={p.relative_to(ART/'runtime').as_posix():hashlib.sha256(p.read_bytes()).hexdigest()
           for p in sorted((ART/'runtime').rglob('*')) if p.is_file()}
    write(ART/'manifest.json',{'version':'0.2.6','files':files})

if __name__=='__main__':
    import sys
    {'prepare':prepare,'seal':seal}[sys.argv[1]]()
