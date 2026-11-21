"""Legacy 19-node progression. Native AE2 multi-resource replaces dedicated essentia buses."""
import json

def expand(root, mod, previous, recipe_groups, write):
    # key, old chapter (text only), icon, position, parents, research aspects, complexity, flags
    rows = [
      ('START','START','diffusion_core',(0,0),[],{},0,['AutoUnlock','Round']),
      ('CORES','START','diffusion_core',(0,-2),['START','DISTILESSENTIA'],{'limus':3,'praecantatio':5,'machina':5,'permutatio':5},2,[]),
      ('STORAGE','STORAGE','essentia_cell_1k',(0,2),['START','DISTILESSENTIA','WARDEDARCANA'],{'vacuos':5,'potentia':5,'vitreus':3,'metallum':3},2,[]),
      ('IO','AUTOMATION','coalescence_core',(-2,-2),['CORES','TUBEFILTER'],{'machina':5,'metallum':3,'vitreus':3,'aer':3},2,['Concealed']),
      ('ARCANE_TERMINAL','ARCANE','arcane_terminal',(2,-1),['START'],{'fabrico':5,'potentia':3,'machina':3,'vacuos':3},1,[]),
      ('ESSENTIA_PROVIDER','AUTOMATION','essentia_provider',(-2,-4),['IO','INFUSION','TUBEFILTER'],{'machina':3,'praecantatio':5,'ordo':3,'sensus':7},3,['Concealed']),
      ('INFUSION_PROVIDER','AUTOMATION','infusion_provider',(-5,-2),['IO','MIRROR','INFUSION'],{'machina':3,'praecantatio':3,'permutatio':7,'motus':7,'sensus':5},3,['Concealed','Special']),
      ('ADVANCED_INFUSION_PROVIDER','AUTOMATION','advanced_infusion_provider',(-7,-2),['INFUSION_PROVIDER'],{'machina':1,'praecantatio':1,'permutatio':1,'motus':1,'instrumentum':1,'cognitio':1,'sensus':1},3,['Concealed','Special']),
      ('IRON_GEARBOX','VIS','iron_gearbox',(4,-5),[],{'machina':6,'metallum':4,'permutatio':4},1,['Secondary']),
      ('THAUMIUM_GEARBOX','VIS','thaumium_gearbox',(4,-6),['IRON_GEARBOX','COREUSE'],{'machina':10,'praecantatio':8,'metallum':5},1,['Secondary']),
      ('QUARTZ','QUARTZ','storage_component_1k',(-5,-5),['ALCHEMICALDUPLICATION'],{'vitreus':5},1,['Secondary','Concealed']),
      ('VIS','VIS','vis_interface',(4,-1),['ARCANE_TERMINAL','VISPOWER'],{'auram':5,'potentia':4,'vacuos':3,'machina':2},2,[]),
      ('ARCANE_ASSEMBLER','ARCANE','arcane_assembler',(6,-1),['VIS','SCEPTRE','START'],{'fabrico':5,'machina':3,'cognitio':3,'permutatio':3,'auram':3,'lucrum':3},3,['Concealed']),
      ('KNOWLEDGE_INSCRIBER','ARCANE','knowledge_inscriber',(6,0),['ARCANE_ASSEMBLER'],{'cognitio':5,'machina':3,'fabrico':3,'permutatio':1},1,['Secondary','Concealed']),
      ('TOOLS','TOOLS','wrench_focus',(-3,-7),['FOCUSFIRE'],{'machina':6,'instrumentum':5,'praecantatio':3},1,['Secondary']),
      ('GENERATOR','VIS','essentia_vibration_chamber',(1,-4),['CORES'],{'machina':5,'praecantatio':3,'aqua':1,'motus':3,'potentia':5},2,['Concealed']),
      ('DISTILLATION','AUTOMATION','distillation_encoder',(-4,-4),['IO'],{'fabrico':5,'permutatio':5,'perditio':5,'machina':3,'lucrum':3,'cognitio':1},3,['Concealed']),
      ('GOLEMS','GOLEMS','golem_wireless_backpack',(0,-6),['CORES','COREGATHER'],{'potentia':5,'auram':5,'cognitio':3,'tutamen':3,'motus':1,'permutatio':1},3,['Concealed']),
      ('MICROSCOPE','TOOLS','cell_microscope',(4,-4),['START'],{'vacuos':5,'potentia':5,'vitreus':3,'metallum':3},1,[]),
    ]
    old={r[0]:r for r in previous}
    keys={r[0] for r in rows}
    mapping={'diffusion_core':'CORES','coalescence_core':'CORES','arcane_terminal':'ARCANE_TERMINAL','knowledge_core':'KNOWLEDGE_INSCRIBER','arcane_assembler':'ARCANE_ASSEMBLER','knowledge_inscriber':'KNOWLEDGE_INSCRIBER','iron_gear':'IRON_GEARBOX','iron_gearbox':'IRON_GEARBOX','thaumium_gearbox':'THAUMIUM_GEARBOX','essentia_provider':'ESSENTIA_PROVIDER','infusion_provider':'INFUSION_PROVIDER','advanced_infusion_provider':'ADVANCED_INFUSION_PROVIDER','distillation_encoder':'DISTILLATION','cell_microscope':'MICROSCOPE','essentia_vibration_chamber':'GENERATOR'}
    recipe_groups=dict(recipe_groups,**mapping)
    for name,key in recipe_groups.items():
        p=root/f'data/{mod}/recipes/{name}.json'
        data=json.loads(p.read_text(encoding='utf8'))
        if 'research' in data:
            # Both inscriber recipes are unlocked by the assembler research in the legacy addon.
            required='ARCANE_ASSEMBLER' if name in ['knowledge_core','knowledge_inscriber'] else key
            data['research']=f'{mod}:{required}';write(f'data/{mod}/recipes/{name}.json',data)
    en=json.loads((root/f'assets/{mod}/lang/en_us.json').read_text(encoding='utf8')) if (root/f'assets/{mod}/lang/en_us.json').exists() else {}
    zh=json.loads((root/f'assets/{mod}/lang/zh_cn.json').read_text(encoding='utf8')) if (root/f'assets/{mod}/lang/zh_cn.json').exists() else {}
    chapters=[];metadata={}
    overrides={
      'MICROSCOPE':('Hold use while aiming at a dropped cell, ME drive or ME chest. Stored items are scanned into your Thaumcraft knowledge after 45 ticks.','持续使用并瞄准掉落的存储元件、ME 驱动器或 ME 箱子；45 刻后扫描其中的物品，获得神秘时代扫描知识。'),
      'KNOWLEDGE_INSCRIBER':('Place a knowledge core and set the ghost recipe matrix. Save an unlocked arcane recipe; saving an existing output removes it. Select a stored output to load its matrix. Each core holds 21 recipes.','放入知识核心，在虚拟九宫格中设置配方，保存已解锁的奥术配方。再次保存相同产物会移除配方。点击上方产物可加载配方；每个核心最多记录 21 个配方。'),
      'ARCANE_ASSEMBLER':('Install a core containing recipes from a knowledge inscriber. This machine advertises those recipes directly to ME crafting CPUs; no pattern provider is required. Supply network vis and power. Armor and speed cards modify crafting costs and speed.','放入由知识刻写器记录配方的核心。装配器直接向 ME 合成 CPU 提供这些配方，无需额外样板供应器。提供网络灵气和电力；装备与加速卡影响消耗和速度。'),
      'ARCANE_TERMINAL':('Browse, search, insert and extract network resources using the native ME terminal. Put ingredients and a wand in the crafting area; Craft uses your research and vis. Refill fetches matching ingredients from the network.','使用 ME 原生终端浏览、搜索与存取网络资源。在合成区放入材料与法杖；合成使用你的研究与灵气，补充按钮从网络提取对应材料。'),
      'VIS':('Sneak-use an AE memory card on the input interface, then use it on an output in the same ME network. Each output draws only from its bound input. Multiple outputs may share an input.','手持 AE 存储卡潜行右键输入接口，再右键同一 ME 网络中的输出接口。每个输出仅从绑定的输入获取灵气，多个输出可共享一个输入。'),
      'TOOLS':('Sneak with the wrench focus attached to a wand. Left click rotates; right click dismantles. A successful operation consumes 10 centivis each of air and fire before discounts.','安装扳手核心后潜行操作：左键旋转，右键拆卸。成功操作基础消耗为风与火各 10 厘点灵气，适用法杖减免。'),
    }
    for key,prior,icon,pos,parents,aspects,complexity,flags in rows:
        template=old[prior]
        title_en=en.get(f'block.{mod}.{icon}',en.get(f'item.{mod}.{icon}',template[1]))
        title_zh=zh.get(f'block.{mod}.{icon}',zh.get(f'item.{mod}.{icon}',template[2]))
        if key=='START':title_en,title_zh='Thaumic Energistics','神秘能源'
        if key=='IO':title_en,title_zh='Essentia Network I/O','源质网络输入/输出'
        if key=='QUARTZ':title_en,title_zh='Quartz Duplication','石英复制'
        # Keep existing research titles independent from renamed item labels.
        if key=='STORAGE':title_zh='1k 源质存储元件'
        if key=='MICROSCOPE':title_zh='存储元件显微镜'
        text=overrides.get(key,(template[5],template[6]))
        chapters.append((key,title_en,title_zh,icon,None,*text))
        metadata[key]={'parents':[f'{mod}:{p}' if p in keys else p for p in parents],'displayColumn':pos[0],'displayRow':pos[1],'aspects':aspects,'complexity':complexity,'isAutoUnlock':False,'isSecondary':False,**{f'is{flag}':True for flag in flags}}
        if key=='QUARTZ':metadata[key]['itemTriggers']=[{'item':'ae2:certus_quartz_crystal'}]
    return chapters,recipe_groups,metadata
