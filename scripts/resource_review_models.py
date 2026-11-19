"""Author the corrected terminal through the configured Blockbench MCP, preserving AE2 geometry."""
import base64
import json
from pathlib import Path
import art_mcp as m
from resource_review import ROOT, ART, write, seal

m.STATE=ROOT/'.tooling/resource-review-0.2.5/mcp-session.txt'
def data(result):
    return json.loads(next(c['text'] for c in result['content'] if c['type']=='text'))
def export(codec):
    result=m.call('export_model',codec_id=codec,result_format='embedded',max_content_length=2000000)
    for entry in result['content']:
        if entry['type']=='resource':
            resource=entry['resource']
            if 'text' in resource:return resource['text']
    payload=data(result)
    if payload.get('truncated') or not payload.get('content'):raise ValueError('Incomplete Blockbench export')
    return payload['content']

model=json.loads((ART/'blockbench-input/arcane_terminal_item.json').read_text(encoding='utf8'))
m.call('create_project',name='TE 0.2.6 - AE2 Arcane Terminal',format='java_block')
m.call('risky_eval',code="Undo.initEdit({project_settings:true}); Project.java_block_version='1.9.0'; Undo.finishEdit('Target Java 1.9 through 1.21.5');")
for key,reference in model['textures'].items():
    if reference.startswith('#'):continue
    namespace,path=reference.split(':')
    source=(ROOT/'references/AE2/src/main/resources/assets/ae2/textures' if namespace=='ae2'
            else ART/'runtime/assets/thaumicenergistics/textures')/(path+'.png')
    m.call('create_texture',name=key,data=str(source),width=16,height=16)
textures=data(m.call('list_textures'))
by_name={t['name'].removesuffix('.png'):t for t in textures}
elements=[{k:e[k] for k in ['name','from','to']} for e in model['elements']]
m.call('place_cube',elements=elements,faces=False)
outline=data(m.call('list_outline'))
ids={e['name']:e['uuid'] for e in outline['roots']}
for element in model['elements']:
    x1,y1,z1=element['from'];x2,y2,z2=element['to']
    uv={'north':[16-x2,16-y2,16-x1,16-y1], 'south':[x1,16-y2,x2,16-y1],
        'east':[16-z2,16-y2,16-z1,16-y1], 'west':[z1,16-y2,z2,16-y1],
        'up':[x1,z1,x2,z2], 'down':[x1,16-z2,x2,16-z1]}
    faces={side:({'texture':by_name[face['texture'][1:]]['uuid'],'uv':uv[side]}
                 if (face:=element['faces'].get(side)) else {'texture':None}) for side in uv}
    m.call('set_cube_uv',id=ids[element['name']],faces=faces,box_uv=False)
write(ART/'blockbench-uv-audit.json',{name:data(m.call('get_cube_uv',id=id)) for name,id in ids.items()})
write(ART/'blockbench-export-formats.json',data(m.call('list_export_formats',only_current_format=False)))
compiled=json.loads(export('java_block'))
compiled.pop('format_version',None)  # Minecraft 1.20.1 block models have no format_version field.
compiled['parent']=model['parent']
compiled['textures']={by_name[name]['id']:ref for name,ref in model['textures'].items() if not ref.startswith('#')}
compiled['textures']['particle']='ae2:part/monitor_sides'
runtime=ART/'runtime/assets/thaumicenergistics/models'
write(runtime/'item/arcane_terminal.json',compiled)
write(runtime/'part/arcane_terminal_base.json',{'parent':'ae2:part/display_base'})
grid=next(e for e in compiled['elements'] if e['name']=='Green crafting grid')
grid['from']=[2,2,0];grid['to']=[14,14,2]
grid['faces']['north']['texture']='#grid'
overlay={'textures':{'grid':'thaumicenergistics:modern/part/terminal_green','particle':'#grid'},'elements':[grid]}
write(runtime/'part/arcane_terminal_off.json',overlay)
grid['faces']['north']['forge_data']={'block_light':15,'sky_light':15}
write(runtime/'part/arcane_terminal.json',overlay)
(ART/'arcane_terminal.bbmodel').write_text(export('project'),encoding='utf8')
m.call('create_offscreen_view',id='te_resource_review',width=640,height=640,antialias=False,copy_view='none')
frame=m.call('set_camera_angle',view='te_resource_review',position=[8,8,-60],target=[8,8,8],projection='orthographic',zoom=3)
for entry in frame['content']:
    if entry['type']=='image':(ART/'terminal-front.png').write_bytes(base64.b64decode(entry['data']))
m.call('delete_offscreen_view',view='te_resource_review')
seal()
print('Blockbench exported the corrected terminal, native gray frame, green grid, and UV audit')
