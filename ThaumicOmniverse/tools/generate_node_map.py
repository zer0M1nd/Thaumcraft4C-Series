"""JourneyMap node tracking guide; the server does not require the client-only map mod."""
import json
from pathlib import Path

root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

key='thaumicomniverse:SCANNED_NODE_MAP'
write(root/'data/thaumicomniverse/research_catalog/node_map.json',{'format':1,'categories':[{
    'key':'THAUMICOMNIVERSE','icon':'thaumicomniverse:textures/gui/research_category.png',
    'background':'thaumcraft:textures/gui/gui_researchback.png','research':[{
        'key':key,'category':'THAUMICOMNIVERSE','displayColumn':0,'displayRow':22,
        'complexity':1,'isSecondary':True,'parents':['NODES'],'pageCount':1,
        'aspects':{'cognitio':2,'auram':2},'icon_item':{'item':'thaumcraft:thaumometer'}}]}]})
write(root/'data/thaumicomniverse/research_pages/node_map.json',{'format':1,'entries':[{
    'key':key,'pages':[{'type':'TEXT','text':'omniverse.page.scanned_node_map'}]}]})
texts={
    'tc.research_name.'+key:('节点测绘','Aura Node Mapping'),
    'tc.research_text.'+key:('记录已经发现的灵气','Remember discovered aura'),
    'omniverse.page.scanned_node_map':(
        '客户端安装旅行地图后，扫描过的灵气节点会显示在小地图和全屏地图中。在按键设置中为“切换已扫描节点显示”分配按键，或修改神秘万象客户端配置以开关显示。记录由服务端按玩家保存，不会公开其他玩家发现的节点。已加载节点显示最近确认的要素数量；未加载节点标记 ~，根据上次稳定器和永久节点状态预估恢复。由于本体在重新加载时随机选择恢复的要素，地图显示可能范围，不把预估当成确定值。地图不会为了更新节点强制加载区块。',
        'With JourneyMap installed on the client, personally scanned aura nodes appear on the minimap and fullscreen map. Assign the Toggle scanned aura nodes key or use the Omniverse client configuration to toggle them. The server saves discoveries per player and does not reveal another player\'s nodes. Loaded nodes show their latest confirmed amounts. Unloaded markers use ~ and forecast recovery from the last stabilizer and permanent node state. The core randomly selects recovered aspects on reload, so the map shows possible ranges rather than inventing exact values. Tracking never force-loads chunks.')}
for index,lang in enumerate(['zh_cn','en_us']):
    path=root/'assets/thaumicomniverse/lang'/(lang+'.json')
    data=json.loads(path.read_text(encoding='utf-8'));data.update({k:v[index] for k,v in texts.items()});write(path,data)
print('Generated scanned-node mapping guide')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
