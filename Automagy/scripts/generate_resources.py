"""Generate this port's JSON definitions. Original binary assets are only mapped, never copied."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"

def modern_resource_paths(value):
    if isinstance(value, str) and re.fullmatch(r'automagy:(?:textures|block|item|items|models|gui|misc)/[A-Za-z0-9/._-]+', value):
        return value.lower()
    if isinstance(value, list): return [modern_resource_paths(v) for v in value]
    if isinstance(value, dict): return {k: modern_resource_paths(v) for k, v in value.items()}
    return value

def write(path, data):
    # Complete aspect definitions are maintained directly since TC4 0.4.56.
    if ('/item_aspects/' in path or '/item_bonus_aspects/' in path) and (ROOT / 'data/automagy/item_aspects/explicit.json').exists(): return
    if path.endswith('/original_resources.json'):
        data = {**data, 'mapping': {k.lower(): v for k, v in data['mapping'].items()}}
    else:
        data = modern_resource_paths(data)
    if path.startswith("data/automagy/item_aspects/"):
        data = {"format": 1, **data}
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def item(identifier):
    return {"item": identifier if ":" in identifier else "automagy:" + identifier}

def stack(identifier, count=1):
    return {**item(identifier), "count": count, "metadata": 0, "damageable": False}

def aspects(values):
    return [{"aspect": key, "amount": amount} for key, amount in values.items()]

# Inventarium structure inputs stay extensible without modifying the base mod.
for tag, values in {
    "locus_silverwood": ["thaumcraft:silverwood_planks"],
    "locus_water_clusters": ["thaumcraft:water_crystal_cluster"],
    "locus_aggregators": ["automagy:locus_aggregator"],
    "inventarium_shelves": ["minecraft:bookshelf"],
    "inventarium_amber": ["thaumcraft:amber_block","automagy:glowing_amber","automagy:glowing_amber_bricks"],
    "inventarium_order_crystals": ["thaumcraft:order_crystal_cluster"],
    "inventarium_memory": ["automagy:amber_memory"],
    "inventarium_container_blacklist": [],
}.items():
    write(f"data/automagy/tags/blocks/{tag}.json", {"replace": False, "values": values})

MAPPING = {}
LANG_MAP = {}
EN = {"itemGroup.automagy": "Automagy", "message.automagy.hourglass.seconds": "Timer: %s seconds",
      "tooltip.automagy.link": "Target: %s, %s, %s (%s)", "message.automagy.eye.unlinked": "Bind this eye to a target block first.",
      "message.automagy.redcrystal.dim": "Maximum signal: %s", "message.automagy.redcrystal.dense": "Minimum signal: %s",
      "gui.automagy.hourglass.time": "Duration: %s s    Remaining: %s s", "gui.automagy.hourglass.flip": "Flip",
      "gui.automagy.hourglass.signal": "Output: %s", "gui.automagy.hourglass.sand": "On flip: %s",
      "gui.automagy.hourglass.input": "Input: %s", "gui.automagy.hourglass.repeat": "Repeat: %s"}
ZH = {"itemGroup.automagy": "自动化魔法", "message.automagy.hourglass.seconds": "计时：%s 秒",
      "tooltip.automagy.link": "目标：%s，%s，%s（%s）", "message.automagy.eye.unlinked": "请先将水晶眼绑定到目标方块。",
      "message.automagy.redcrystal.dim": "信号上限：%s", "message.automagy.redcrystal.dense": "信号阈值：%s",
      "gui.automagy.hourglass.time": "时长：%s 秒    剩余：%s 秒", "gui.automagy.hourglass.flip": "翻转",
      "gui.automagy.hourglass.signal": "输出：%s", "gui.automagy.hourglass.sand": "翻转行为：%s",
      "gui.automagy.hourglass.input": "输入：%s", "gui.automagy.hourglass.repeat": "循环：%s"}
for name, english, chinese in [
    ("signal", ["Disabled", "Pulse", "Alternating", "Constant", "Negative pulse"], ["关闭", "脉冲", "交替", "持续", "负脉冲"]),
    ("sand", ["Reverse sand", "Restart", "Start / stop"], ["反转沙量", "重新计时", "启动／停止"]),
    ("input", ["Ignore", "Rising edge", "Run while unpowered", "Run while powered"], ["忽略", "上升沿翻转", "无信号时运行", "有信号时运行"]),
    ("repeat", ["No", "Yes"], ["否", "是"]),
]:
    for i, (en, zh) in enumerate(zip(english, chinese)):
        EN[f"gui.automagy.hourglass.{name}.{i}"] = en
        ZH[f"gui.automagy.hourglass.{name}.{i}"] = zh

def external(destination, source):
    MAPPING["assets/automagy/" + destination] = "assets/automagy/" + source

def lang(modern, legacy):
    LANG_MAP[modern] = legacy

for name in ["hourglass", "hourglass-gold", "hourglass-sand", "hourglass-glass", "hourglass-redstone"]:
    external(f"textures/models/{name}.png", f"textures/models/{name}.png")
external("models/external/hourglass.obj", "textures/models/hourglass.obj")
external("models/external/redcrystal.obj", "textures/models/redcrystal.obj")
external("models/external/comparator.obj", "textures/models/comparator.obj")
for texture in ["comparator", "comparatorReading", "comparatorSignaling"]:
    external(f"textures/models/{texture}.png", f"textures/models/{texture}.png")
external("textures/item/crystal_eye.png", "textures/items/crystalEye.png")
external("textures/gui/research_icon.png", "textures/gui/researchIcon.png")
lang("item.automagy.crystal_eye", "item.crystalEye.name")
lang("block.automagy.remote_comparator", "tile.blockRemoteComparator.name")
write("assets/automagy/models/item/crystal_eye.json", {"parent":"minecraft:item/generated", "textures":{"layer0":"automagy:item/crystal_eye"}})
write("assets/automagy/models/item/remote_comparator.json", {"parent":"builtin/entity", "textures":{"particle":"minecraft:block/stone"}})
write("assets/automagy/models/block/remote_comparator_particle.json", {"textures":{"particle":"minecraft:block/stone"}})
write("assets/automagy/blockstates/remote_comparator.json", {"variants":{"":{"model":"automagy:block/remote_comparator_particle"}}})
write("data/automagy/loot_tables/blocks/remote_comparator.json", {"type":"minecraft:block", "pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:remote_comparator"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
for texture in ["redcrystalStandard", "redcrystalActive"] + ["redcrystal" + variant + active for variant in ["Amp", "Dim", "Dense", "Res"] for active in ["", "Active"]]:
    external(f"textures/models/{texture}.png", f"textures/models/{texture}.png")
external("textures/item/sliver.png", "textures/items/shardSliver.png")
external("textures/block/inversion_torch.png", "textures/blocks/blockTorchInversion.png")
external("textures/block/inversion_torch_off.png", "textures/blocks/blockTorchInversion_off.png")
external("lang/en_us.json", "lang/en_US.lang")
external("lang/zh_cn.json", "lang/zh_CN.lang")
ASPECT_ENTRIES = []
RECIPES = {}
DISPLAYS = []

def infusion_display(value):
    ingredient=value["ingredient"]
    result=stack(ingredient["item"])
    if "nbt" in ingredient: result["nbt"]=ingredient["nbt"]
    if "nbt" in value: result["nbt"]=value["nbt"]
    if ingredient.get("type")=="automagy:enchanted":
        tag="StoredEnchantments" if ingredient["item"]=="minecraft:enchanted_book" else "Enchantments"
        result["nbt"]='{'+tag+':[{id:"'+ingredient["enchantment"]+'",lvl:'+str(ingredient["minimum"])+'s}]}'
    return {"kind":"stack","stack":result}

def recipe(name, definition):
    RECIPES[name] = definition
    write(f"data/automagy/recipes/{name}.json", definition)
    if definition["type"] == "thaumcraft:infusion":
        DISPLAYS.append({"key":"automagy:"+name,"class":"thaumcraft.api.crafting.InfusionRecipe","data":{
            "kind":"infusion","research":definition["research"],"instability":definition["instability"],"aspects":aspects(definition["aspects"]),
            "input":infusion_display(definition["center"]),
            "ingredients":{"kind":"sequence","elements":[infusion_display(c) for c in definition["components"]]},
            "output":{"kind":"stack","stack":stack(definition["result"]["item"],definition["result"].get("count",1))}}})
        return
    if definition["type"] == "thaumcraft:crucible":
        DISPLAYS.append({"key":"automagy:"+name, "class":"thaumcraft.api.crafting.CrucibleRecipe", "data":{
            "kind":"crucible", "research":definition["research"], "aspects":aspects(definition["aspects"]),
            "catalyst":{"kind":"stack","stack":stack(definition["catalyst"]["item"])},
            "output":{"kind":"stack","stack":stack(definition["result"]["item"],definition["result"].get("count",1))}}})
        return
    arcane = definition["type"] == "thaumcraft:arcane"
    shaped = "pattern" in definition
    if definition["type"] not in ["thaumcraft:arcane", "minecraft:crafting_shaped", "minecraft:crafting_shapeless"]:
        return
    def ingredient(value):
        if "item" in value:
            result=stack(value["item"])
            if "nbt" in value:result["nbt"]=value["nbt"]
            return {"kind": "stack", "stack": result}
        examples = {"minecraft:wooden_slabs": [f"minecraft:{wood}_slab" for wood in ["oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry"]],
                    "minecraft:sand": ["minecraft:sand", "minecraft:red_sand"]}
        return {"kind": "ore", "names": [value["tag"]], "elements": [ingredient(item(i)) for i in examples.get(value["tag"],[])]}
    inputs = [ingredient(definition["key"][char]) if char != " " else None
              for row in definition["pattern"] for char in row] if shaped else [ingredient(i) for i in definition["ingredients"]]
    cls = ("thaumcraft.api.crafting.ShapedArcaneRecipe" if shaped else "thaumcraft.api.crafting.ShapelessArcaneRecipe") if arcane else "net.minecraftforge.oredict.ShapedOreRecipe"
    data = {"kind": ("arcane_shaped" if shaped else "arcane_shapeless") if arcane else "crafting_ore",
            "output": {"kind": "stack", "stack": stack(definition["result"]["item"], definition["result"].get("count", 1))},
            "ingredients": {"kind": "sequence", "elements": inputs}, "shaped": shaped,
            "research": definition.get("research", ""), "aspects": aspects(definition.get("vis", {})), "mirrored": True}
    if shaped:
        data.update(width=len(definition["pattern"][0]), height=len(definition["pattern"]))
    DISPLAYS.append({"key": "automagy:" + name, "class": cls, "data": data})

for suffix, old in [("", ""), ("_amp", "Amp"), ("_dim", "Dim"), ("_dense", "Dense"), ("_res", "Res"), ("_merc", "Merc")]:
    name = "redcrystal" + suffix
    lang("block.automagy." + name, "tile.blockRedcrystal" + old + ".name")
    write(f"assets/automagy/blockstates/{name}.json", {"variants": {"": {"model": "automagy:block/redcrystal_particle"}}})
    write(f"assets/automagy/models/item/{name}.json", {"parent":"builtin/entity", "textures":{"particle":"minecraft:block/redstone_block"}})
    write(f"data/automagy/loot_tables/blocks/{name}.json", {"type":"minecraft:block", "pools":[{"rolls":1,
          "entries":[{"type":"minecraft:item","name":"automagy:"+name}], "conditions":[{"condition":"minecraft:survives_explosion"}]}]})
write("assets/automagy/models/block/redcrystal_particle.json", {"textures":{"particle":"minecraft:block/redstone_block"}})
recipe("redcrystal", {"type":"thaumcraft:crucible", "research":"automagy:REDCRYSTAL", "catalyst":item("minecraft:redstone"),
                      "aspects":{"vitreus":1}, "result":item("redcrystal")})
for suffix, primal, aspect in [("amp","fire","ignis"),("dim","entropy","perditio"),("dense","earth","terra")]:
    recipe("redcrystal_"+suffix, {"type":"thaumcraft:arcane", "mode":"shapeless", "research":"automagy:REDCRYSTAL_"+suffix.upper(),
            "vis":{aspect:2}, "ingredients":[item("redcrystal"),item(primal+"_sliver"),item("redcrystal")], "result":{**item("redcrystal_"+suffix),"count":2}})
recipe("redcrystal_res", {"type":"thaumcraft:arcane", "research":"automagy:REDCRYSTAL_RES", "vis":{"aer":8,"ordo":4},
        "pattern":[" R ","QSQ"], "key":{"R":item("redcrystal"),"Q":item("minecraft:quartz"),"S":item("thaumcraft:air_shard")}, "result":item("redcrystal_res")})
recipe("magic_hourglass", {"type":"thaumcraft:arcane", "research":"automagy:MAGICHOURGLASS", "vis":{"ordo":4,"aer":2,"terra":1,"perditio":1},
        "pattern":[" R ","OHA"," P "], "key":{"R":item("redcrystal"),"O":item("order_sliver"),"H":item("hourglass"),"P":item("minecraft:light_weighted_pressure_plate"),"A":item("air_sliver")},
        "result":item("magic_hourglass")})
recipe("remote_comparator", {"type":"thaumcraft:arcane", "research":"automagy:REMOTECOMPARATOR", "vis":{"aer":5,"perditio":5,"ordo":5},
        "pattern":[" R ","RAR","SSS"],"key":{"R":item("redcrystal"),"A":item("thaumcraft:air_shard"),"S":item("thaumcraft:arcane_stone")},"result":item("remote_comparator")})
recipe("crystal_eye", {"type":"thaumcraft:crucible", "research":"automagy:REMOTECOMPARATOR", "aspects":{"vitreus":2,"sensus":4},
        "catalyst":item("minecraft:spider_eye"),"result":item("crystal_eye")})
recipe("crystal_eye_clear", {"type":"minecraft:crafting_shapeless", "ingredients":[item("crystal_eye")], "result":item("crystal_eye")})
for name,values in [("crystal_eye",{"sensus":6,"vitreus":2,"praecantatio":1}),
                    ("remote_comparator",{"sensus":2,"vacuos":2,"praecantatio":1,"potentia":4,"machina":3,"vitreus":4,"ordo":2})]:
    ASPECT_ENTRIES.append({"items":["automagy:"+name],"aspects":aspects(values)})

PRIMALS = [("air", "aer"), ("fire", "ignis"), ("water", "aqua"), ("earth", "terra"), ("order", "ordo"), ("entropy", "perditio")]
for i, (name, aspect) in enumerate(PRIMALS):
    sliver = name + "_sliver"
    lang("item.automagy." + sliver, f"item.shardSliver.{i}.name")
    write(f"assets/automagy/models/item/{sliver}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "automagy:item/sliver"}})
    ASPECT_ENTRIES.append({"items": ["automagy:" + sliver], "aspects": aspects({aspect: 1})})
    recipe(sliver, {"type": "thaumcraft:arcane", "mode": "shapeless", "research": "automagy:SLIVERS",
                   "ingredients": [item("thaumcraft:" + name + "_shard")], "vis": {"perditio": 1}, "result": {**item(sliver), "count": 4}})
    # The order shard uses four order vis: the two equal aspect contributions add together.
    cost = {"ordo": 2}
    cost[aspect] = cost.get(aspect, 0) + 2
    recipe(name + "_shard", {"type": "thaumcraft:arcane", "mode": "shapeless", "research": "automagy:SLIVERS",
                            "ingredients": [item(sliver)] * 4, "vis": cost, "result": item("thaumcraft:" + name + "_shard")})
write("data/automagy/tags/items/slivers.json", {"replace": False, "values": ["automagy:" + name + "_sliver" for name, _ in PRIMALS]})

for name, legacy in [("hourglass", "blockHourglass"), ("magic_hourglass", "blockHourglassMagic"), ("inversion_torch", "blockTorchInversion")]:
    lang("block.automagy." + name, "tile." + legacy + ".name")
    write(f"data/automagy/loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "automagy:" + name}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    if name != "inversion_torch":
        write(f"assets/automagy/blockstates/{name}.json", {"variants": {"": {"model": "automagy:block/hourglass_particle"}}})
        write(f"assets/automagy/models/item/{name}.json", {"parent": "builtin/entity", "textures": {"particle": "minecraft:block/glass"},
            "display": {"gui": {"rotation": [15, 0, 0], "scale": [1, 1, 1]},
                        "ground": {"translation": [0, 2, 0], "scale": [.5, .5, .5]}}})
write("assets/automagy/models/block/hourglass_particle.json", {"textures": {"particle": "minecraft:block/glass"}})
torch_states = {}
for powered in range(16):
    suffix = "" if powered else "_off"
    for face in ["up", "down", "north", "east", "south", "west"]:
        model = "inversion_torch" + ("_wall" if face not in ["up", "down"] else "") + suffix
        data = {"model": "automagy:block/" + model}
        if face not in ["up", "down"]:
            data["y"] = {"east": 0, "south": 90, "west": 180, "north": 270}[face]
        torch_states[f"facing={face},power={powered}"] = data
write("assets/automagy/blockstates/inversion_torch.json", {"variants": torch_states})
for wall in [False, True]:
    for lit in [False, True]:
        suffix = "" if lit else "_off"
        name = "inversion_torch" + ("_wall" if wall else "") + suffix
        write(f"assets/automagy/models/block/{name}.json", {"parent": "minecraft:block/template_torch_wall" if wall else "minecraft:block/template_torch",
              "render_type": "minecraft:cutout", "textures": {"torch": "automagy:block/inversion_torch" + suffix}})
write("assets/automagy/models/item/inversion_torch.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "automagy:block/inversion_torch"}})
recipe("hourglass", {"type": "minecraft:crafting_shaped", "pattern": [" _ ", "|S|", " _R"],
        "key": {"_": {"tag": "minecraft:wooden_slabs"}, "|": item("minecraft:glass_pane"), "S": {"tag": "minecraft:sand"}, "R": item("minecraft:repeater")}, "result": item("hourglass")})
recipe("inversion_torch", {"type": "thaumcraft:arcane", "mode": "shapeless", "research": "automagy:SLIVERS", "vis": {"ordo": 1},
        "ingredients": [item("minecraft:redstone_torch"), item("order_sliver")], "result": item("inversion_torch")})
for name, values in [("hourglass", {"machina":6,"potentia":4,"arbor":1,"terra":1,"perditio":1}),
                     ("magic_hourglass", {"machina":7,"potentia":4,"arbor":1,"terra":1,"perditio":1,"ordo":2,"motus":2,"praecantatio":1})]:
    ASPECT_ENTRIES.append({"items": ["automagy:" + name], "aspects": aspects(values)})

CATEGORY = "AUTOMAGY"
RESEARCH = []
PAGES = []
def research(key, icon, x, y, pages, costs=None, parents=None, auto=False, secondary=False):
    entry = {"key": "automagy:" + key, "category": CATEGORY, "icon_item": stack(icon), "displayColumn": x, "displayRow": y,
             "pageCount": len(pages), "complexity": 0 if auto else 1, "aspects": aspects(costs or {}), "isAutoUnlock": auto,
             "isRound": auto, "isSecondary": secondary, "parents": ["automagy:" + p for p in parents or []]}
    RESEARCH.append(entry)
    PAGES.append({"key": entry["key"], "pages": pages})
    for prefix in ["name", "text"]:
        lang(f"tc.research_{prefix}.automagy:{key}", f"Automagy.research_{prefix}.{key}")

def text_page(key, index):
    modern = f"tc.research_page.automagy:{key}.{index}"
    lang(modern, f"Automagy.research_page.{key}.{index}")
    return {"type": "TEXT", "aspects": [], "text": modern}

def recipe_page(name):
    recipe = RECIPES[name]
    display = next(d for d in DISPLAYS if d["key"] == "automagy:" + name)
    return {"type": {"thaumcraft:arcane":"ARCANE_CRAFTING", "thaumcraft:crucible":"CRUCIBLE_CRAFTING", "thaumcraft:infusion":"INFUSION_CRAFTING"}.get(recipe["type"], "NORMAL_CRAFTING"), "aspects": [],
            "recipe": {"kind": "reference", "class": display["class"], "keys": [display["key"]]},
            "recipeOutput": stack(recipe["result"]["item"], recipe["result"].get("count", 1))}

research("REDSTONETHEORY", "minecraft:redstone", 0, 0,
         [text_page("REDSTONETHEORY", n) for n in [1, 2, 3]] + [recipe_page("inversion_torch"), text_page("REDSTONETHEORY", 4), recipe_page("hourglass"), text_page("REDSTONETHEORY", 5)], auto=True)
research("SLIVERS", "air_sliver", -1, -2, [text_page("SLIVERS", 1)] +
         [recipe_page(name + suffix) for suffix in ["_sliver", "_shard"] for name, _ in PRIMALS], auto=True)
research("REDCRYSTAL", "redcrystal", 0, 2, [text_page("REDCRYSTAL",1),recipe_page("redcrystal"),text_page("REDCRYSTAL",2)],
         {"potentia":2,"vitreus":2,"praecantatio":2}, ["REDSTONETHEORY"])
RESEARCH[-1]["siblings"]=["automagy:REDCRYSTAL_"+suffix for suffix in ["AMP","DIM","DENSE"]]
research("MAGICHOURGLASS", "magic_hourglass", -3,1, [text_page("MAGICHOURGLASS",1),recipe_page("magic_hourglass"),text_page("MAGICHOURGLASS",2),text_page("MAGICHOURGLASS",3)],
         {"machina":2,"potentia":2,"motus":3}, ["REDCRYSTAL"], secondary=True)
for suffix,x,y,aspect in [("AMP",-2,3,"ignis"),("DIM",2,3,"perditio"),("DENSE",0,4,"terra")]:
    key="REDCRYSTAL_"+suffix
    research(key,"redcrystal_"+suffix.lower(),x,y,[text_page(key,1),recipe_page("redcrystal_"+suffix.lower())],
             {"potentia":1,"machina":1,"vitreus":1,aspect:2},["REDCRYSTAL"])
    RESEARCH[-1].update(isConcealed=True,isStub=True,isRound=True)
research("REDCRYSTAL_RES","redcrystal_res",-3,5,[text_page("REDCRYSTAL_RES",1),recipe_page("redcrystal_res"),text_page("REDCRYSTAL_RES",2)],
         {"potentia":4,"machina":3,"vitreus":2,"aer":8})
RESEARCH[-1].update(complexity=2,isConcealed=True,parentsHidden=["automagy:REDCRYSTAL"])
research("REMOTECOMPARATOR","remote_comparator",3,1,[text_page("REMOTECOMPARATOR",1),recipe_page("remote_comparator"),text_page("REMOTECOMPARATOR",2),recipe_page("crystal_eye")],
         {"potentia":2,"machina":2,"sensus":4,"aer":2},["REDCRYSTAL"])

lang("block.automagy.vis_reader", "tile.blockVisReader.name")
for face in ["s","w","n","e","btm","top"]:
    external(f"textures/block/vis_reader_{face}.png",f"textures/blocks/blockVisReader_{face}.png")
write("assets/automagy/models/block/vis_reader.json", {"parent":"minecraft:block/cube",
      "textures":{name:"automagy:block/vis_reader_"+suffix for name,suffix in
                  [("north","n"),("east","e"),("south","s"),("west","w"),("up","top"),("down","btm"),("particle","top")]}})
write("assets/automagy/models/item/vis_reader.json", {"parent":"automagy:block/vis_reader"})
write("assets/automagy/blockstates/vis_reader.json", {"variants":{f"facing={face}":{"model":"automagy:block/vis_reader","y":rotation}
      for face,rotation in [("north",0),("east",90),("south",180),("west",270)]}})
write("data/automagy/loot_tables/blocks/vis_reader.json", {"type":"minecraft:block","pools":[{"rolls":1,
      "entries":[{"type":"minecraft:item","name":"automagy:vis_reader"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
write("data/minecraft/tags/blocks/mineable/pickaxe.json", {"replace":False,"values":["automagy:vis_reader","automagy:remote_comparator"]})
recipe("vis_reader", {"type":"thaumcraft:arcane","research":"automagy:VISREADER","vis":{aspect:3 for _,aspect in PRIMALS},
       "pattern":[" T ","SQS"," SR"],"key":{"T":item("thaumcraft:thaumometer"),"S":item("thaumcraft:arcane_stone"),"Q":item("minecraft:quartz"),"R":item("redcrystal")},"result":item("vis_reader")})
ASPECT_ENTRIES.append({"items":["automagy:vis_reader"],"aspects":aspects({"potentia":1,"machina":1,"vitreus":2,"aer":1,"terra":3,"praecantatio":2,"sensus":2})})
research("VISREADER","vis_reader",3,-1,[text_page("VISREADER",1),recipe_page("vis_reader")]+[text_page("VISREADER",i) for i in [2,3,4,5]],
         {"sensus":5,"praecantatio":5,"auram":3},secondary=True)
RESEARCH[-1].update(parents=["automagy:REMOTECOMPARATOR","DISTILESSENTIA"])
EN.update({"gui.automagy.reader.auto":"Auto","gui.automagy.reader.columns":"Label      Mode          Capacity     Side    Invert",
           "gui.automagy.reader.signals":"Signals: %s / %s / %s / %s"})
ZH.update({"gui.automagy.reader.auto":"自动","gui.automagy.reader.columns":"标签       模式              容量      方向    反转",
           "gui.automagy.reader.signals":"信号：%s / %s / %s / %s"})
for i,(en,zh) in enumerate(zip(["Disabled","Total","Highest","Lowest","Types"],["关闭","总量","最高","最低","种类数"])):
    EN[f"gui.automagy.reader.mode.{i}"]=en;ZH[f"gui.automagy.reader.mode.{i}"]=zh
for i,en,zh in [(6,"All","全部"),(0,"Down","下"),(2,"North","北"),(3,"South","南"),(4,"West","西"),(5,"East","东")]:
    EN[f"gui.automagy.reader.direction.{i}"]=en;ZH[f"gui.automagy.reader.direction.{i}"]=zh

external("textures/item/crystal_brain.png","textures/items/crystalBrain.png")
external("textures/item/crystal_brain_overlay.png","textures/items/crystalBrain-overlay.png")
external("textures/item/entity_eye.png","textures/items/crystalEntityEye.png")
for i,(name,aspect) in enumerate(PRIMALS):
    brain=name+"_crystal_brain"
    lang("item.automagy."+brain,f"item.crystalBrain.{i}.name")
    write(f"assets/automagy/models/item/{brain}.json", {"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/crystal_brain","layer1":"automagy:item/crystal_brain_overlay"}})
    recipe(brain,{"type":"thaumcraft:crucible","research":"automagy:CRYSTALBRAIN","aspects":{"vitreus":10,aspect:8},"catalyst":item("thaumcraft:zombie_brain"),"result":item(brain)})
    ASPECT_ENTRIES.append({"items":["automagy:"+brain],"aspects":aspects({"vitreus":8,aspect:8,"cognitio":4,"exanimis":2})})
research("CRYSTALBRAIN","air_crystal_brain",7,-1,[text_page("CRYSTALBRAIN",1)]+[recipe_page(name+"_crystal_brain") for name,_ in PRIMALS],
         {"cognitio":5,"aqua":1,"aer":1,"ordo":1,"vitreus":5,"ignis":1,"terra":1,"perditio":1},["REMOTECOMPARATOR"])
RESEARCH[-1]["complexity"]=3
lang("item.automagy.entity_eye","item.crystalEntityEye.name")
lang("block.automagy.entity_sensor","tile.blockEntitySignal.name")
write("assets/automagy/models/item/entity_eye.json", {"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/entity_eye"}})
for closed in [False,True]:
    for lit in [False,True]:
        suffix=("2" if closed else "")+("" if lit else "-dark")
        external("textures/block/entity_sensor"+suffix+".png","textures/blocks/blockEntitySignal"+suffix+".png")
        if not closed:
            external("textures/block/entity_sensor"+suffix+".png.mcmeta","textures/blocks/blockEntitySignal"+suffix+".png.mcmeta")
        for face in ["down","up","north","south","west","east"]:
            write(f"assets/automagy/models/block/entity_sensor_{face}{suffix}.json", {"textures":{"particle":"automagy:block/entity_sensor-dark","face":"automagy:block/entity_sensor"+suffix},
                  "elements":[{"from":[0,0,0],"to":[16,16,16],"faces":{face:{"texture":"#face","cullface":face}}}]})
parts=[]
for face,bit in [("down",0),("up",1),("north",2),("south",3),("west",4),("east",5)]:
    for closed in [False,True]:
        for lit in [False,True]:
            suffix=("2" if closed else "")+("" if lit else "-dark")
            parts.append({"when":{"lit":str(lit).lower(),"mask":"|".join(str(i) for i in range(64) if bool(i&(1<<bit))==closed)},
                          "apply":{"model":f"automagy:block/entity_sensor_{face}{suffix}"}})
write("assets/automagy/blockstates/entity_sensor.json", {"multipart":parts})
write("assets/automagy/models/item/entity_sensor.json", {"parent":"minecraft:block/cube_all","textures":{"all":"automagy:block/entity_sensor-dark"}})
write("data/automagy/loot_tables/blocks/entity_sensor.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:entity_sensor"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
recipe("entity_eye", {"type":"thaumcraft:crucible","research":"automagy:ENTITYSIGNAL","aspects":{"bestia":4,"spiritus":2},"catalyst":item("crystal_eye"),"result":item("entity_eye")})
recipe("entity_sensor", {"type":"thaumcraft:infusion","research":"automagy:ENTITYSIGNAL","instability":1,"aspects":{"terra":25,"sensus":15,"victus":15},
       "center":{"ingredient":item("earth_crystal_brain")},"components":[{"ingredient":item(i)} for i in
       ["minecraft:golden_carrot","thaumcraft:arcane_stone","redcrystal","thaumcraft:arcane_stone","minecraft:egg","thaumcraft:arcane_stone","minecraft:leather","thaumcraft:arcane_stone"]],"result":item("entity_sensor")})
research("ENTITYSIGNAL","entity_sensor",9,-4,[text_page("ENTITYSIGNAL",1),recipe_page("entity_sensor"),text_page("ENTITYSIGNAL",2),text_page("ENTITYSIGNAL",3),recipe_page("entity_eye")],
         {"terra":10,"sensus":5,"victus":5,"bestia":3,"spiritus":3},["CRYSTALBRAIN"])
RESEARCH[-1].update(complexity=2,isHidden=True,itemTriggers=[stack("earth_crystal_brain")])
ASPECT_ENTRIES.append({"items":["automagy:entity_sensor"],"aspects":aspects({"terra":10,"sensus":10,"victus":10,"vitreus":9,"metallum":8,"cognitio":4,"bestia":2,"potentia":2,"machina":2,"spiritus":2})})
EN.update({"tooltip.automagy.entity_eye.unbound":"Use on a creature; sneak and hold use to bind yourself.","tooltip.automagy.entity_eye.target":"Attuned to: %s",
           "gui.automagy.sensor.option.0":"Adults: %s","gui.automagy.sensor.option.1":"Children: %s","gui.automagy.sensor.option.2":"This player only: %s","gui.automagy.sensor.count":"Detected: %s"})
ZH.update({"tooltip.automagy.entity_eye.unbound":"对生物使用；潜行并长按使用可绑定自己。","tooltip.automagy.entity_eye.target":"绑定目标：%s",
           "gui.automagy.sensor.option.0":"检测成年：%s","gui.automagy.sensor.option.1":"检测幼体：%s","gui.automagy.sensor.option.2":"仅指定玩家：%s","gui.automagy.sensor.count":"检测数量：%s"})
write("data/minecraft/tags/blocks/mineable/pickaxe.json", {"replace":False,"values":["automagy:vis_reader","automagy:remote_comparator","automagy:entity_sensor","automagy:mob_lure"]})

for index,(name,texture) in enumerate([("enchanted_paper","enchantedPaper"),("white_filter","enchantedPaperWhitelist"),("black_filter","enchantedPaperBlacklist"),("enchanted_tome","blankEnchantedBook")]):
    external(f"textures/item/{name}.png",f"textures/items/{texture}.png")
    lang("item.automagy."+name,f"item.enchantedPaper.{index}.name")
    write(f"assets/automagy/models/item/{name}.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/"+name}})
recipe("enchanted_paper",{"type":"thaumcraft:arcane","research":"automagy:ENCHANTEDPAPER","vis":{aspect:1 for _,aspect in PRIMALS},
       "pattern":["gPg","PgP","gPg"],"key":{"g":item("minecraft:gold_nugget"),"P":item("minecraft:paper")},"result":{**item("enchanted_paper"),"count":4}})
for color,aspect in [("white","ordo"),("black","perditio")]:
    recipe(color+"_filter",{"type":"thaumcraft:arcane","mode":"shapeless","research":"automagy:ENCHANTEDPAPER","vis":{aspect:2},
           "ingredients":[item("enchanted_paper"),{"tag":"forge:dyes/"+color}],"result":item(color+"_filter")})
recipe("enchanted_tome",{"type":"thaumcraft:arcane","research":"automagy:ENCHANTEDPAPER","vis":{aspect:5 for _,aspect in PRIMALS},
       "pattern":["LPP","GPP","LPP"],"key":{"L":item("minecraft:leather"),"G":item("minecraft:gold_ingot"),"P":item("enchanted_paper")},"result":item("enchanted_tome")})
recipe("enchanted_tome_alternate",{"type":"thaumcraft:arcane","research":"automagy:ENCHANTEDPAPER","vis":{aspect:7 for _,aspect in PRIMALS},
       "pattern":[" B ","GSG"," B "],"key":{"B":item("minecraft:book"),"G":item("minecraft:gold_ingot"),"S":{"tag":"forge:slimeballs"}},"result":item("enchanted_tome")})
research("ENCHANTEDPAPER","enchanted_paper",5,7,[text_page("ENCHANTEDPAPER",1),recipe_page("enchanted_paper"),text_page("ENCHANTEDPAPER",2),
         recipe_page("white_filter"),recipe_page("black_filter"),text_page("ENCHANTEDPAPER",3),recipe_page("enchanted_tome"),recipe_page("enchanted_tome_alternate")],
         {"ordo":5,"cognitio":3,"praecantatio":3},["GREEDYCHEST"],secondary=True)
ASPECT_ENTRIES.append({"items":["automagy:enchanted_paper"],"aspects":aspects({"cognitio":1,"ordo":1})})
EN.update({"gui.automagy.filter.name":"Item name (* wildcard)","gui.automagy.filter.option.0":"Count items: %s",
           "gui.automagy.filter.option.1":"Ignore damage: %s","gui.automagy.filter.option.2":"Ignore NBT: %s",
           "gui.automagy.filter.help":"Inventory click: add reference. Empty-hand left/right: +/-1. Shift-click reference: clear."})
ZH.update({"gui.automagy.filter.name":"物品名称（* 通配符）","gui.automagy.filter.option.0":"比较数量：%s",
           "gui.automagy.filter.option.1":"忽略损伤：%s","gui.automagy.filter.option.2":"忽略 NBT：%s",
           "gui.automagy.filter.help":"点击背包物品添加引用；空手左右点击增减数量；Shift 点击引用清除。"})

for texture in ["blockTally","blockTallyRedOff","blockTallyRedOn","blockTallyRead","blockTallyLensWorld","blockTallyLensDrops"]:
    external(f"textures/block/{texture}.png",f"textures/blocks/{texture}.png")
opposites={"down":"up","up":"down","north":"south","south":"north","west":"east","east":"west"}
for kind,legacy in [("inventory","blockTally"),("world","blockTallyLensWorld"),("drops","blockTallyLensDrops")]:
    name="tally_"+kind
    lang("block.automagy."+name,"tile."+legacy+".name")
    variants={}
    for face in opposites:
        for lit in [False,True]:
            model=name+"_"+face+("_on" if lit else "_off")
            textures={side:"automagy:block/blockTally" for side in opposites}
            textures[face]="automagy:block/blockTallyRed"+("On" if lit else "Off")
            textures[opposites[face]]="automagy:block/"+("blockTallyRead" if kind=="inventory" else legacy)
            textures["particle"]="automagy:block/blockTally"
            write("assets/automagy/models/block/"+model+".json",{"parent":"minecraft:block/cube","textures":textures})
            variants[f"facing={face},lit={str(lit).lower()}"]={"model":"automagy:block/"+model}
    write("assets/automagy/blockstates/"+name+".json",{"variants":variants})
    write("assets/automagy/models/item/"+name+".json",{"parent":"automagy:block/"+name+"_down_on"})
    normal=[{"type":"minecraft:item","name":"automagy:tally_inventory"}]
    if kind!="inventory":normal.append({"type":"minecraft:item","name":"automagy:tally_lens_"+kind})
    silk={"condition":"minecraft:match_tool","predicate":{"enchantments":[{"enchantment":"minecraft:silk_touch","levels":{"min":1}}]}}
    survival={"condition":"minecraft:survives_explosion"}
    pools=[{"rolls":1,"entries":[normal[0]],"conditions":[survival]}]
    if kind!="inventory":
        pools=[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:"+name}],"conditions":[survival,silk]}]
        pools += [{"rolls":1,"entries":[entry],"conditions":[survival,{"condition":"minecraft:inverted","term":silk}]} for entry in normal]
    write("data/automagy/loot_tables/blocks/"+name+".json",{"type":"minecraft:block","pools":pools})
for index,(kind,suffix) in enumerate([("blank",""),("world","World"),("drops","Drops")]):
    name="tally_lens_"+kind
    lang("item.automagy."+name,f"item.tallyLens.{index}.name")
    external("textures/item/"+name+".png","textures/items/tallyLens"+suffix+".png")
    write("assets/automagy/models/item/"+name+".json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/"+name}})
recipe("tally_inventory",{"type":"thaumcraft:arcane","research":"automagy:TALLYBLOCK","vis":{"aer":2,"terra":2,"ordo":3},
       "pattern":[" G ","WQW"," WR"],"key":{"G":{"tag":"forge:glass"},"W":item("thaumcraft:greatwood_planks"),"Q":item("minecraft:quartz"),"R":item("redcrystal")},"result":item("tally_inventory")})
recipe("tally_lens_blank",{"type":"thaumcraft:arcane","research":"automagy:TALLYBLOCK_LENS","vis":{"aer":2,"terra":1},
       "pattern":[" g ","gPg"," gR"],"key":{"g":item("minecraft:gold_nugget"),"P":item("minecraft:glass_pane"),"R":item("minecraft:redstone")},"result":item("tally_lens_blank")})
for kind,aspect in [("world","vacuos"),("drops","perfodio")]:
    recipe("tally_lens_"+kind,{"type":"thaumcraft:crucible","research":"automagy:TALLYBLOCK_LENS","aspects":{"sensus":4,aspect:4},"catalyst":item("tally_lens_blank"),"result":item("tally_lens_"+kind)})
ASPECT_ENTRIES.append({"items":["automagy:tally_lens_blank"],"aspects":aspects({"aer":1,"terra":1,"vitreus":1})})
research("TALLYBLOCK","tally_inventory",5,2,[text_page("TALLYBLOCK",1),recipe_page("tally_inventory"),text_page("TALLYBLOCK",2)],
         {"sensus":3,"potentia":3,"ordo":3},["REMOTECOMPARATOR","ENCHANTEDPAPER"],secondary=True)
research("TALLYBLOCK_LENS","tally_lens_world",6,1,[text_page("TALLYBLOCK_LENS",1)]+[recipe_page("tally_lens_"+kind) for kind in ["blank","world","drops"]],
         {"sensus":3,"vacuos":2,"perfodio":2},["TALLYBLOCK"],secondary=True)
write("data/minecraft/tags/blocks/mineable/axe.json",{"replace":False,"values":["automagy:tally_"+kind for kind in ["inventory","world","drops"]]})
EN.update({"gui.automagy.tally.all":"Require all: %s","gui.automagy.tally.status":"Target: %s   Signal: %s"})
ZH.update({"gui.automagy.tally.all":"匹配所有条件：%s","gui.automagy.tally.status":"有效目标：%s   信号：%s"})

lang("tc.research_category.AUTOMAGY", "tc.research_category.AUTOMAGY")
external("textures/models/tenacious_chest.png","textures/models/tenaciousChest.png")
lang("block.automagy.tenacious_chest","tile.blockTenaciousChest.name")
write("assets/automagy/models/block/tenacious_chest.json",{"textures":{"particle":"minecraft:block/oak_planks"}})
write("assets/automagy/blockstates/tenacious_chest.json",{"variants":{"":{"model":"automagy:block/tenacious_chest"}}})
write("assets/automagy/models/item/tenacious_chest.json",{"parent":"minecraft:builtin/entity","textures":{"particle":"minecraft:block/oak_planks"},
      "display":{"gui":{"rotation":[30,45,0],"scale":[.625,.625,.625]},"ground":{"translation":[0,3,0],"scale":[.25,.25,.25]},
      "fixed":{"scale":[.5,.5,.5]},"thirdperson_righthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.375,.375,.375]},
      "firstperson_righthand":{"rotation":[0,45,0],"scale":[.4,.4,.4]}}})
write("data/automagy/loot_tables/blocks/tenacious_chest.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:tenacious_chest"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
write("data/minecraft/tags/blocks/mineable/axe.json",{"replace":False,"values":["automagy:tally_"+kind for kind in ["inventory","world","drops"]]+["automagy:tenacious_chest"]})
recipe("tenacious_chest",{"type":"thaumcraft:arcane","research":"automagy:TENACIOUSCHEST","vis":{"aer":5,"terra":2,"aqua":2},
       "pattern":["W_W","WSW","WWW"],"key":{"W":{"tag":"minecraft:planks"},"_":{"tag":"minecraft:wooden_trapdoors"},"S":{"tag":"forge:slimeballs"}},"result":item("tenacious_chest")})
research("TENACIOUSCHEST","tenacious_chest",6,11,[text_page("TENACIOUSCHEST",1),recipe_page("tenacious_chest"),text_page("TENACIOUSCHEST",2)],
         {"lucrum":3,"vacuos":3},["AVARICIOUS"],secondary=True)
write("data/automagy/item_aspects/content.json", {"entries": ASPECT_ENTRIES})
external("textures/models/greedy_chest.png","textures/models/greedychest.png")
external("textures/models/greedy_chest_lit.png","textures/models/greedychest-litrunes.png")
external("textures/item/avarice_pearl.png","textures/items/avaricePearl.png")
lang("block.automagy.greedy_chest","tile.blockGreedyChest.name")
lang("item.automagy.avarice_pearl","item.avaricePearl.0.name")
write("assets/automagy/models/item/avarice_pearl.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/avarice_pearl"}})
for folder in ["models/block","models/item","blockstates"]:
    text=(ROOT/f"assets/automagy/{folder}/tenacious_chest.json").read_text(encoding="utf-8").replace("tenacious_chest","greedy_chest")
    write(f"assets/automagy/{folder}/greedy_chest.json",json.loads(text))
write("data/automagy/loot_tables/blocks/greedy_chest.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:greedy_chest"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
write("data/minecraft/tags/blocks/mineable/axe.json",{"replace":False,"values":["automagy:tally_"+kind for kind in ["inventory","world","drops"]]+["automagy:tenacious_chest","automagy:greedy_chest"]})
recipe("avarice_pearl",{"type":"thaumcraft:crucible","research":"automagy:GREEDYCHEST","aspects":{"lucrum":5},"catalyst":item("minecraft:ender_pearl"),"result":item("avarice_pearl")})
recipe("greedy_chest",{"type":"thaumcraft:arcane","research":"automagy:GREEDYCHEST","vis":{"aer":7,"ordo":2,"perditio":2},
       "pattern":["W_W","WAW","WWW"],"key":{"W":item("thaumcraft:greatwood_planks"),"_":{"tag":"minecraft:wooden_trapdoors"},"A":item("avarice_pearl")},"result":item("greedy_chest")})
research("GREEDYCHEST","greedy_chest",3,8,[text_page("GREEDYCHEST",1),recipe_page("avarice_pearl"),text_page("GREEDYCHEST",2),recipe_page("greedy_chest"),text_page("GREEDYCHEST",3)],
         {"lucrum":5,"vacuos":3,"permutatio":3})
RESEARCH[-1]["parents"]=["HUNGRYCHEST"]
ASPECT_ENTRIES.extend([{"items":["automagy:greedy_chest"],"aspects":aspects({"arbor":9,"lucrum":5,"alienis":4,"permutatio":4,"iter":3,"praecantatio":2,"aer":2})},
    {"items":["automagy:avarice_pearl"],"aspects":aspects({"lucrum":5,"alienis":4,"iter":4,"praecantatio":3})}])
write("data/automagy/item_aspects/content.json", {"entries": ASPECT_ENTRIES})
EN.update({"gui.automagy.greedy.side":"%s %s","gui.automagy.greedy.up":"Up %s"})
ZH.update({"gui.automagy.greedy.side":"%s %s","gui.automagy.greedy.up":"上 %s"})
for index,(en,zh) in enumerate(zip(["Share new arrivals","Maintain proportion","Leave at source","Maintain stock"],["分配新增物品","保持比例","源端保留数量","补足本箱数量"])):
    EN[f"gui.automagy.greedy.mode.{index}"]=en;ZH[f"gui.automagy.greedy.mode.{index}"]=zh
external("textures/models/eager_chest.png","textures/models/eagerchest.png")
external("textures/item/mercatus_amalgam.png","textures/items/tradeEssence.png")
external("textures/item/shattered_avarice_pearl.png","textures/items/avaricePearlShattered.png")
lang("block.automagy.eager_chest","tile.blockEagerChest.name")
lang("item.automagy.mercatus_amalgam","item.edible.0.name")
EN.update({"item.automagy.shattered_avarice_pearl":"Shattered Avarice Pearl","gui.automagy.eager.enabled":"Exchange with me: %s","gui.automagy.eager.give":"Give","gui.automagy.eager.take":"Take"})
ZH.update({"item.automagy.shattered_avarice_pearl":"破碎的贪婪珍珠","gui.automagy.eager.enabled":"与我交换物品：%s","gui.automagy.eager.give":"给出","gui.automagy.eager.take":"收取"})
for name in ["mercatus_amalgam","shattered_avarice_pearl"]:
    write(f"assets/automagy/models/item/{name}.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/"+name}})
for folder in ["models/block","models/item","blockstates"]:
    text=(ROOT/f"assets/automagy/{folder}/tenacious_chest.json").read_text(encoding="utf-8").replace("tenacious_chest","eager_chest")
    write(f"assets/automagy/{folder}/eager_chest.json",json.loads(text))
write("data/automagy/loot_tables/blocks/eager_chest.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:eager_chest"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
write("data/minecraft/tags/blocks/mineable/axe.json",{"replace":False,"values":["automagy:tally_"+kind for kind in ["inventory","world","drops"]]+["automagy:tenacious_chest","automagy:greedy_chest","automagy:eager_chest"]})
recipe("shattered_avarice_pearl",{"type":"thaumcraft:arcane","mode":"shapeless","research":"automagy:AVARICIOUS","vis":{"perditio":2},
       "ingredients":[item("avarice_pearl"),item("entropy_sliver")],"result":{**item("shattered_avarice_pearl"),"count":2}})
# The legacy horizontal renderer was attached to dropped shattered pearls only.
write("assets/automagy/models/item/shattered_avarice_pearl.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/shattered_avarice_pearl"},"display":{"ground":{"rotation":[90,0,0],"translation":[0,2,0],"scale":[0.6,0.6,0.6]}}})
recipe("mercatus_amalgam",{"type":"thaumcraft:arcane","research":"automagy:EAGERCHEST","vis":{"ordo":2,"perditio":2},"pattern":["P","Q","B"],
       "key":{"P":item("shattered_avarice_pearl"),"Q":item("thaumcraft:quicksilver"),"B":item("minecraft:glass_bottle")},"result":item("mercatus_amalgam")})
recipe("eager_chest",{"type":"thaumcraft:arcane","research":"automagy:EAGERCHEST","vis":{"ordo":5,"perditio":5},"pattern":["w_w","WAW","WWW"],
       "key":{"W":item("thaumcraft:greatwood_planks"),"w":{"tag":"minecraft:planks"},"_":{"tag":"minecraft:wooden_trapdoors"},"A":item("mercatus_amalgam")},"result":item("eager_chest")})
research("EAGERCHEST","eager_chest",6,9,[text_page("EAGERCHEST",1),recipe_page("mercatus_amalgam"),text_page("EAGERCHEST",2),recipe_page("eager_chest")],
         {"permutatio":5,"vacuos":3,"humanus":3},["AVARICIOUS","ENCHANTEDPAPER"])
ASPECT_ENTRIES.append({"items":["automagy:eager_chest"],"aspects":aspects({"arbor":9,"lucrum":3,"alienis":1,"permutatio":5,"praecantatio":1,"ordo":3})})
write("data/automagy/item_aspects/content.json", {"entries": ASPECT_ENTRIES})
external("textures/item/misers_pearl.png","textures/items/misersPearl.png")
lang("item.automagy.misers_pearl","item.avaricePearl.1.name")
lang("message.automagy.avaricious","Automagy.chat.triggerAvariciousResearch")
write("assets/automagy/models/item/misers_pearl.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/misers_pearl"}})
recipe("misers_pearl",{"type":"thaumcraft:arcane","research":"automagy:AVARICIOUS","vis":{"ordo":5,"aer":5},"pattern":["GDG","ASO"," P "],
       "key":{"G":item("minecraft:glowstone_dust"),"D":item("minecraft:diamond"),"A":item("thaumcraft:air_shard"),"O":item("thaumcraft:order_shard"),"S":item("thaumcraft:salis_mundus"),"P":item("avarice_pearl")},"result":item("misers_pearl")})
research("AVARICIOUS","misers_pearl",4,10,[text_page("AVARICIOUS",1),recipe_page("shattered_avarice_pearl"),text_page("AVARICIOUS",2),text_page("AVARICIOUS",3),recipe_page("misers_pearl")],
         {"lucrum":2,"ordo":1},["GREEDYCHEST"],secondary=True)
RESEARCH[-1]["isHidden"]=True
ASPECT_ENTRIES.append({"items":["automagy:misers_pearl"],"aspects":aspects({"lucrum":13,"alienis":4,"iter":4,"praecantatio":5,"vitreus":4,"aer":5,"ordo":3,"auram":3,"permutatio":2,"sensus":2})})
write("data/automagy/item_aspects/content.json", {"entries": ASPECT_ENTRIES})
for name,legacy in [("hungry_maw","hungryMaw"),("finical_maw","finicalMaw")]:
    external(f"textures/block/{name}.png",f"textures/models/{legacy}.png")
    lang("block.automagy."+name,"tile.block"+("HungryMaw" if name=="hungry_maw" else "FinicalMaw")+".name")
    elements=[]
    for x,z,dx,dz,u,v in [(11,5,1,6,0,0),(4,5,1,6,15,0),(5,11,6,1,30,0),(5,4,6,1,45,0),(10,10,1,1,0,8),(5,10,1,1,0,11),(10,5,1,1,0,14),(5,5,1,1,0,17)]:
        uv={"down":[u+dz+dx,v,u+dz+dx+dx,v+dz],"up":[u+dz,v,u+dz+dx,v+dz],
            "west":[u,v+dz,u+dz,v+dz+1],"north":[u+dz,v+dz,u+dz+dx,v+dz+1],
            "east":[u+dz+dx,v+dz,u+dz+dx+dz,v+dz+1],"south":[u+dz+dx+dz,v+dz,u+dz+dx+dz+dx,v+dz+1]}
        elements.append({"from":[8+(x-8)*.75,-.19,8+(z-8)*.75],"to":[8+(x+dx-8)*.75,.56,8+(z+dz-8)*.75],"faces":{face:{"texture":"#frame","uv":[a/(4 if i%2==0 else 2) for i,a in enumerate(coords)]} for face,coords in uv.items()}})
    # The moving .29-wide portal is drawn by MawRenderer, not a vanilla portal bitmap.
    write(f"assets/automagy/models/block/{name}.json",{"render_type":"minecraft:cutout","textures":{"particle":"automagy:block/"+name,"frame":"automagy:block/"+name},"elements":elements})
    write(f"assets/automagy/blockstates/{name}.json",{"variants":{"facing="+face:{"model":"automagy:block/"+name,**rotation} for face,rotation in
          [("up",{}),("down",{"x":180}),("north",{"x":90}),("south",{"x":90,"y":180}),("west",{"x":90,"y":270}),("east",{"x":90,"y":90})]}})
    external(f"textures/item/{name}.png",f"textures/blocks/block{legacy[0].upper()+legacy[1:]}.png")
    write(f"assets/automagy/models/item/{name}.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/"+name}})
    write(f"data/automagy/loot_tables/blocks/{name}.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:"+name}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
recipe("finical_maw",{"type":"thaumcraft:arcane","research":"automagy:FINICALMAW","vis":{"ordo":10,"aer":4,"perditio":4,"terra":4},"pattern":[" Io","IMI","oI "],
       "key":{"I":item("minecraft:iron_ingot"),"M":item("hungry_maw"),"o":item("order_sliver")},"result":item("finical_maw")})
EN["guide.automagy.maw.crafting"]="Place obsidian directly above a hungry chest. Use a wand fitted with the portable hole focus on that obsidian after completing this research. The chest is consumed. The process checks above, north, south, west and east, consuming up to four obsidian blocks and producing one maw per block. Keep the area loaded until it finishes."
ZH["guide.automagy.maw.crafting"]="在饕餮箱正上方放置黑曜石，完成本研究后，用安装便携洞核心的法杖对该黑曜石使用。饕餮箱会被消耗；制作过程依次检查上、北、南、西、东五个位置，最多消耗四块黑曜石，每块产出一个贪食之口。请保持周围区块加载，直到制作结束。"
EN["guide.automagy.maw.filter"]="Use a populated runic filter on the finical maw to insert it. Use an empty hand to retrieve the filter. Configure the paper before inserting it. Whitelist quantities limit the destination inventory; blacklist quantities leave that many items in the dropped stack."
ZH["guide.automagy.maw.filter"]="对挑食之口使用已配置的符文筛选纸即可放入；空手使用可取回。请先编辑筛选纸再放入。白名单数量限制目的容器内的库存，黑名单数量保留当前掉落物堆叠中的指定数量。"
research("HUNGRYMAW","hungry_maw",1,6,[text_page("HUNGRYMAW",1),{"type":"TEXT","aspects":[],"text":"guide.automagy.maw.crafting"},text_page("HUNGRYMAW",2)],{"fames":5,"vacuos":3,"tenebrae":3,"permutatio":3})
RESEARCH[-1]["parents"]=["HUNGRYCHEST","FOCUSPORTABLEHOLE"]
research("FINICALMAW","finical_maw",3,6,[text_page("FINICALMAW",1),recipe_page("finical_maw"),{"type":"TEXT","aspects":[],"text":"guide.automagy.maw.filter"}],{"fames":8,"ordo":8,"permutatio":5},["HUNGRYMAW","ENCHANTEDPAPER"],secondary=True)
ASPECT_ENTRIES.append({"items":["automagy:hungry_maw"],"aspects":aspects({"terra":2,"ignis":2,"tenebrae":1,"permutatio":1,"fames":1})})
write("data/automagy/item_aspects/content.json", {"entries": ASPECT_ENTRIES})
# Unseen Scribe table geometry and original UV atlas. Animated book/quill are added by its renderer.
external("textures/models/scribe_table.png", "textures/models/scribeTable.png")
lang("block.automagy.unseen_scribe", "tile.blockUnseenScribe.name")
EN.update({"message.automagy.scribe.linked":"Connected to an Inventarium.","message.automagy.scribe.no_core":"No available Inventarium within range."})
ZH.update({"message.automagy.scribe.linked":"已连接至存储核心。","message.automagy.scribe.no_core":"范围内没有可连接的存储核心。"})
def scribe_part(x,y,z,w,h,d,u,v):
    atlas={"west":[u,v+d,u+d,v+d+h],"north":[u+d,v+d,u+d+w,v+d+h],
           "east":[u+d+w,v+d,u+2*d+w,v+d+h],"south":[u+2*d+w,v+d,u+2*d+2*w,v+d+h],
           "up":[u+d,v,u+d+w,v+d],"down":[u+d+w,v,u+d+2*w,v+d]}
    return {"from":[x,y,z],"to":[x+w,y+h,z+d],"faces":{face:{"texture":"#table","uv":[a/4 if i%2==0 else a/2 for i,a in enumerate(uv)]} for face,uv in atlas.items()}}
scribe_parts=[scribe_part(x,0,z,4,12,4,0,16) for x in [1,11] for z in [1,11]]
scribe_parts += [scribe_part(0,12,0,16,4,16,0,0),scribe_part(12,3,5,2,2,6,18,24),
                 scribe_part(2,3,5,2,2,6,18,24),scribe_part(5,3,12,6,2,2,35,23)]
# The translucent inkwell uses the legacy ModelRenderer transform in the BER, like the book/quill.
write("assets/automagy/models/block/unseen_scribe.json", {"textures":{"table":"automagy:models/scribe_table","particle":"automagy:models/scribe_table"},"elements":scribe_parts})
write("assets/automagy/models/item/unseen_scribe.json", {"parent":"builtin/entity","gui_light":"side","display":{
    "gui":{"rotation":[30,225,0],"scale":[.625,.625,.625]},
    "ground":{"translation":[0,3,0],"scale":[.25,.25,.25]},
    "fixed":{"scale":[.5,.5,.5]},
    "thirdperson_righthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.375,.375,.375]},
    "thirdperson_lefthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.375,.375,.375]},
    "firstperson_righthand":{"rotation":[0,45,0],"scale":[.4,.4,.4]},
    "firstperson_lefthand":{"rotation":[0,225,0],"scale":[.4,.4,.4]}}})
write("assets/automagy/blockstates/unseen_scribe.json", {"variants":{f"facing={f}":{"model":"automagy:block/unseen_scribe","y":r} for f,r in [("north",0),("east",90),("south",180),("west",270)]}})
write("data/automagy/loot_tables/blocks/unseen_scribe.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:unseen_scribe"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
external("textures/models/scribe_pointer.png", "textures/models/scribePointer.png")
lang("block.automagy.scribe_pointer", "tile.blockScribePointer.name")
pointer_parts=[]
for x,y,z,w,h,d,u,v in [(-3,14,-3,6,8,6,0,14),(-3,15,-4,6,6,8,30,0),(-4,15,-3,8,6,6,0,0),(-2.5,22,-2.5,5,1,5,30,18),(-3,23,-3,6,1,6,30,25)]:
    part=scribe_part(x,24-y-h,z,w,h,d,u,v)
    for key in ["from","to"]:
        part[key]=[8+part[key][0]/2,part[key][1]/2,8+part[key][2]/2]
    pointer_parts.append(part)
write("assets/automagy/models/block/scribe_pointer.json", {"textures":{"table":"automagy:models/scribe_pointer","particle":"automagy:models/scribe_pointer"},"elements":pointer_parts})
write("assets/automagy/models/item/scribe_pointer.json", {"parent":"automagy:block/scribe_pointer"})
write("assets/automagy/blockstates/scribe_pointer.json", {"variants":{"":{"model":"automagy:block/scribe_pointer"}}})
write("data/automagy/loot_tables/blocks/scribe_pointer.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:scribe_pointer"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
recipe("scribe_pointer", {"type":"thaumcraft:arcane","research":"automagy:INVENTARIUM_POINTER","vis":{"terra":5,"aer":30,"ordo":5},
    "pattern":["QQQ","QEQ","GOG"],"key":{"Q":item("minecraft:quartz"),"E":item("minecraft:ender_eye"),"G":item("minecraft:gold_ingot"),"O":item("thaumcraft:order_crystal_cluster")},"result":item("scribe_pointer")})
EN.update({"gui.automagy.inventarium.title":"Inventarium", "gui.automagy.inventarium.sort":"Change sorting", "gui.automagy.inventarium.count":"Recorded: %s"})
ZH.update({"gui.automagy.inventarium.title":"存储记录", "gui.automagy.inventarium.sort":"切换排序", "gui.automagy.inventarium.count":"记录数量：%s"})
EN.update({"gui.automagy.inventarium.by_count":"By quantity", "gui.automagy.inventarium.by_id":"By item ID", "gui.automagy.inventarium.omitted":"Shelf capacity is insufficient; some slots are not recorded."})
ZH.update({"gui.automagy.inventarium.by_count":"按数量", "gui.automagy.inventarium.by_id":"按物品 ID", "gui.automagy.inventarium.omitted":"书架容量不足，部分槽位未被记录。"})
EN["gui.automagy.inventarium.search"]="Item ID / server item name"
ZH["gui.automagy.inventarium.search"]="物品 ID／服务端物品名称"
EN["gui.automagy.requisition.title"]="Requisition Tome"
ZH["gui.automagy.requisition.title"]="请求之书"
EN["gui.automagy.requisition.request"]="Left: +1; right: -1; Shift: stack; Ctrl: 10; Alt: allow unavailable"
ZH["gui.automagy.requisition.request"]="左键+1，右键-1；Shift：一组；Ctrl：10个；Alt：允许超出库存"
lang("block.automagy.requisition_tome","tile.blockReqTome.name")
EN.update({"gui.automagy.requisition.show_requests":"Show this terminal's requests","gui.automagy.requisition.show_inventory":"Show inventory","gui.automagy.requisition.pending":"Still requested: %s","gui.automagy.requisition.cancel":"Left: +1; right: -1; Shift: stack; Ctrl: 10; Alt: allow unavailable"})
ZH.update({"gui.automagy.requisition.show_requests":"查看本终端请求","gui.automagy.requisition.show_inventory":"查看库存","gui.automagy.requisition.pending":"尚未交付：%s","gui.automagy.requisition.cancel":"左键+1，右键-1；Shift：一组；Ctrl：10个；Alt：允许超出库存"})
EN["guide.automagy.requisition.delivery"]="Requests are delivered by linked Fill golems to their home inventory, using the home's configured side. The Requisition Tome is a request terminal, not an item container. Bind a golem to a suitable receiving container with the golem bell, then use the Automagy linker to connect that golem to the taskmaster. The taskmaster must be connected to the Inventarium and have memory capacity. Mirror-based routing is a separate system."
ZH["guide.automagy.requisition.delivery"]="请求由已链接的填充傀儡送往其家位置的容器，并遵守该容器绑定面的插入规则。请求之书是请求终端，不是物品容器。先用傀儡铃将傀儡的家设为收货容器，再使用自动化魔法链接工具把傀儡连接至主控。主控需要连接存储核心并占用记忆名额。魔镜分发属于另一套独立路由机制。"
# Inventarium core. Research entry is completed together with the scribe system.
external("textures/models/brain_order.png", "textures/models/brainOrder.png")
external("textures/models/brain_entropy.png", "textures/models/brainEntropy.png")
lang("block.automagy.inventarium", "tile.blockInventarium.name")
write("assets/automagy/blockstates/inventarium.json", {"variants":{"":{"model":"automagy:block/inventarium"}}})
write("assets/automagy/models/block/inventarium.json", {"textures":{"particle":"automagy:models/brain_order"},"elements":[]})
write("assets/automagy/models/item/inventarium.json", {"parent":"builtin/entity","gui_light":"front"})
write("data/automagy/loot_tables/blocks/inventarium.json", {"type":"minecraft:block","pools":[
    {"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:inventarium"}],"conditions":[{"condition":"minecraft:survives_explosion"}]},
    {"rolls":1,"entries":[{"type":"minecraft:item","name":"thaumcraft:order_shard","functions":[{"function":"minecraft:set_count","count":6}]}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
recipe("inventarium", {"type":"thaumcraft:infusion","research":"automagy:INVENTARIUM","instability":4,
    "aspects":{"ordo":40,"cognitio":24,"sensus":8,"machina":8},"center":{"ingredient":item("order_crystal_brain")},
    "components":[{"ingredient":item(i)} for i in ["enchanted_paper","minecraft:item_frame","minecraft:chest","remote_comparator"]],"result":item("inventarium")})
recipe("unseen_scribe", {"type":"thaumcraft:arcane","mode":"shapeless","research":"automagy:INVENTARIUM","vis":{"aer":15,"ordo":30,"terra":5},
    "ingredients":[item(i) for i in ["thaumcraft:scribing_tools","enchanted_tome","thaumcraft:nitor","thaumcraft:order_shard","thaumcraft:table","air_sliver"]],"result":item("unseen_scribe")})
EN["guide.automagy.inventarium.structure"]="Place the core above an Order Crystal Cluster, with amber two blocks below the core. On the cluster's level, arrange eight bookshelves at offsets (±2,±1) and (±1,±2). Keep the eight neighboring blocks around both core and cluster empty. Placing the core consumes the cluster. Nearby shelves each permit reading 81 slots. Order clusters within three blocks add one connection and eight blocks of range. The base limit is one connection at a distance of ten."
ZH["guide.automagy.inventarium.structure"]="在秩序晶簇上方放置核心，核心下方第二格放琥珀块。晶簇同层的水平偏移（±2，±1）和（±1，±2）处共放八座书架。核心与晶簇各自周围的八格须保持空气。放置核心会消耗晶簇。核心周围三格内每座普通书架允许读取81个槽位，每个秩序晶簇增加一个连接和八格连接距离。基础上限为一个连接、十格距离。"
EN["guide.automagy.inventarium.view"]="Use a wand on an Unseen Scribe to connect it to the nearest available core. Supply Order vis from above. Without a pointer, its facing selects the first container within three blocks; the scribe records that container type in a cube of radius three. Right-click without a wand to view recorded contents. Sorting and page controls only change the view; displayed items cannot be taken. An asterisk means shelf capacity omitted some slots."
ZH["guide.automagy.inventarium.view"]="用法杖对记录装置使用可连接最近的可用核心，并从上方供应秩序灵力。未安装指针时，装置沿朝向三格选取第一个容器，在半径三格立方体内记录同类型容器。使用非法杖物品或空手右键可查看记录。排序和翻页只改变显示，不能直接取走展示物品。页码旁星号表示书架容量不足，部分槽位未记录。"
research("INVENTARIUM","unseen_scribe",8,3,[text_page("INVENTARIUM",1),recipe_page("inventarium"),text_page("INVENTARIUM",2),
    {"type":"TEXT","aspects":[],"text":"guide.automagy.inventarium.structure"},text_page("INVENTARIUM",3),recipe_page("unseen_scribe"),
    text_page("INVENTARIUM",4),text_page("INVENTARIUM",5),{"type":"TEXT","aspects":[],"text":"guide.automagy.inventarium.view"}],
    {"sensus":5,"cognitio":5,"ordo":5},["CRYSTALBRAIN","TALLYBLOCK"])
RESEARCH[-1].update(isHidden=True,parentsHidden=["VISPOWER"],itemTriggers=[stack("order_crystal_brain")])
research("INVENTARIUM_POINTER","scribe_pointer",7,4,[text_page("INVENTARIUM_POINTER",1),recipe_page("scribe_pointer"),text_page("INVENTARIUM_POINTER",2)],
    {"sensus":5,"vitreus":3,"vacuos":3},["INVENTARIUM"],secondary=True)
RESEARCH[-1]["isConcealed"]=True
for identifier,values in {
    "inventarium":{"cognitio":10,"machina":10,"ordo":10,"potentia":4,"praecantatio":4,"sensus":10,"vacuos":6,"vitreus":10},
    "scribe_pointer":{"ordo":4,"vitreus":5,"vacuos":2,"metallum":3,"lucrum":2,"praecantatio":3,"sensus":5},
}.items(): ASPECT_ENTRIES.append({"items":["automagy:"+identifier],"aspects":aspects(values)})
write("data/automagy/item_aspects/content.json", {"entries": ASPECT_ENTRIES})
for wood,suffix in [("greatwood",""),("silverwood","Silverwood")]:
    name=wood+"_bookshelf"
    external(f"textures/block/{name}.png",f"textures/blocks/blockBookshelfEnchanted{suffix}.png")
    EN["block.automagy."+name]="Enchanted "+wood.title()+" Bookshelf"
    ZH["block.automagy."+name]="附魔"+("宏伟之木" if wood=="greatwood" else "银树")+"书架"
    write(f"assets/automagy/models/block/{name}.json", {"parent":"minecraft:block/cube_column","textures":{"end":f"thaumcraft:block/planks_{wood}","side":f"automagy:block/{name}"}})
    write(f"assets/automagy/models/item/{name}.json", {"parent":f"automagy:block/{name}"})
    write(f"assets/automagy/blockstates/{name}.json", {"variants":{"":{"model":f"automagy:block/{name}"}}})
    write(f"data/automagy/loot_tables/blocks/{name}.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:"+name}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
recipe("greatwood_bookshelf", {"type":"thaumcraft:arcane","research":"automagy:ENCHANTEDPAPER","vis":{a:5 for a in ["aer","terra","ignis","aqua","ordo","perditio"]},
    "pattern":["WWW","BBB","WWW"],"key":{"W":item("thaumcraft:greatwood_planks"),"B":item("enchanted_tome")},"result":item("greatwood_bookshelf")})
recipe("silverwood_bookshelf", {"type":"thaumcraft:arcane","research":"automagy:INVENTARIUM_EXTRA","vis":{"terra":14,"ignis":14,"ordo":14},
    "pattern":["WWW","SBS","WWW"],"key":{"W":item("thaumcraft:silverwood_planks"),"S":item("greatwood_bookshelf"),"B":item("minecraft:enchanted_book")},"result":item("silverwood_bookshelf")})
write("data/minecraft/tags/blocks/enchantment_power_provider.json", {"replace":False,"values":["automagy:greatwood_bookshelf","automagy:silverwood_bookshelf"]})
for page in PAGES:
    if page["key"]=="automagy:ENCHANTEDPAPER":
        page["pages"].append(recipe_page("greatwood_bookshelf"))
        next(r for r in RESEARCH if r["key"]==page["key"])["pageCount"]=len(page["pages"])
research("INVENTARIUM_EXTRA","silverwood_bookshelf",10,3,[text_page("INVENTARIUM_EXTRA",1),recipe_page("silverwood_bookshelf")],{"cognitio":5},["INVENTARIUM"])
RESEARCH[-1].update(isConcealed=True,isRound=True)
next(r for r in RESEARCH if r["key"]=="automagy:INVENTARIUM")["siblings"]=["automagy:INVENTARIUM_EXTRA"]
for wood,primal,greed,magic in [("greatwood",3,4,1),("silverwood",6,5,5)]:
    values={a:primal for a in ["aer","terra","ignis","aqua","ordo","perditio"]}
    values.update(cognitio=10,arbor=3,lucrum=greed,praecantatio=magic)
    ASPECT_ENTRIES.append({"items":[f"automagy:{wood}_bookshelf"],"aspects":aspects(values)})

write("data/automagy/item_aspects/content.json", {"entries":ASPECT_ENTRIES})
external("textures/block/amber_memory.png","textures/blocks/amberMemory.png")
external("textures/block/amber_memory_glow.png","textures/blocks/amberMemoryGlow.png")
EN["block.automagy.amber_memory"]="Amber Memory Bank"
ZH["block.automagy.amber_memory"]="琥珀记忆库"
write("assets/automagy/models/block/amber_memory.json", {"parent":"minecraft:block/cube_all","textures":{"all":"automagy:block/amber_memory"}})
write("assets/automagy/models/block/amber_memory_active.json", {"parent":"minecraft:block/block","render_type":"minecraft:cutout","textures":{"base":"automagy:block/amber_memory","glow":"automagy:block/amber_memory_glow","particle":"automagy:block/amber_memory"},"elements":[
    {"from":[0,0,0],"to":[16,16,16],"faces":{f:{"texture":"#base"} for f in ["up","down","north","south","east","west"]}},
    {"from":[-.001,-.001,-.001],"to":[16.001,16.001,16.001],"faces":{f:{"texture":"#glow"} for f in ["up","down","north","south","east","west"]}}]})
write("assets/automagy/models/item/amber_memory.json", {"parent":"automagy:block/amber_memory"})
write("assets/automagy/blockstates/amber_memory.json", {"variants":{"active=false":{"model":"automagy:block/amber_memory"},"active=true":{"model":"automagy:block/amber_memory_active"}}})
write("data/automagy/loot_tables/blocks/amber_memory.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:amber_memory"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
for name,index,pattern in [("glowing_amber",0,[" A ","ANA"," A "]),("glowing_amber_bricks",1,["A A"," N ","A A"])]:
    lang("block.automagy."+name,f"tile.blockTranslucent.{index}.name")
    write(f"assets/automagy/models/block/{name}.json", {"parent":"thaumcraft:block/amber_block","render_type":"minecraft:translucent"})
    write(f"assets/automagy/models/item/{name}.json", {"parent":f"automagy:block/{name}"})
    write(f"assets/automagy/blockstates/{name}.json", {"variants":{"":{"model":f"automagy:block/{name}"}}})
    write(f"data/automagy/loot_tables/blocks/{name}.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:"+name}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
    recipe(name,{"type":"thaumcraft:arcane","research":"automagy:NITORLIGHT","vis":{"terra":2,"aqua":2,"ignis":4},"pattern":pattern,
        "key":{"A":item("thaumcraft:amber"),"N":item("thaumcraft:nitor")},"result":item(name)})
    ASPECT_ENTRIES.append({"items":["automagy:"+name],"aspects":aspects({"vinculum":5,"vitreus":5,"lux":5,"potentia":3,"ignis":3,"sensus":1})})
write("data/automagy/tags/items/glowing_amber.json", {"replace":False,"values":["automagy:glowing_amber","automagy:glowing_amber_bricks"]})
research("NITORLIGHT","glowing_amber",-5,0,[text_page("NITORLIGHT",1),recipe_page("glowing_amber"),recipe_page("glowing_amber_bricks")],{"lux":2},secondary=True)
RESEARCH[-1]["parentsHidden"]=["NITOR"]
recipe("amber_memory",{"type":"thaumcraft:arcane","research":"automagy:INVENTARIUM_DELIVERY","vis":{"aqua":10,"ignis":10,"ordo":10},"pattern":["IOI","DBD","IGI"],
    "key":{"I":item("minecraft:iron_ingot"),"O":item("thaumcraft:order_shard"),"D":item("minecraft:diamond"),"B":item("thaumcraft:zombie_brain"),"G":{"tag":"automagy:glowing_amber"}},"result":item("amber_memory")})

write("data/automagy/item_aspects/content.json", {"entries":ASPECT_ENTRIES})
external("textures/models/requisition_book.png","textures/models/reqTome.png")
external("textures/models/requisition_stand.png","textures/models/reqTomeStand.png")
# Full legacy pedestal and book are rendered by RequisitionTomeRenderer.
write("assets/automagy/models/block/requisition_tome.json", {"textures":{"particle":"automagy:models/requisition_stand"},"elements":[]})
DEVICE_DISPLAY={"gui":{"rotation":[25,45,0],"scale":[.65,.65,.65]},"ground":{"translation":[0,3,0],"scale":[.3,.3,.3]},"fixed":{"rotation":[0,180,0],"scale":[.6,.6,.6]},"firstperson_righthand":{"rotation":[0,45,0],"scale":[.5,.5,.5]},"thirdperson_righthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.375,.375,.375]}}
write("assets/automagy/models/item/requisition_tome.json", {"parent":"builtin/entity","display":DEVICE_DISPLAY})
write("assets/automagy/blockstates/requisition_tome.json", {"variants":{f"facing={f},powered={p}":{"model":"automagy:block/requisition_tome","y":r} for f,r in [("north",0),("east",90),("south",180),("west",270)] for p in ["false","true"]}})
write("data/automagy/loot_tables/blocks/requisition_tome.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:requisition_tome"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
recipe("requisition_tome", {"type":"thaumcraft:arcane","research":"automagy:INVENTARIUM_DELIVERY","vis":{"terra":10,"aer":10,"ordo":54},"pattern":[" C ","WBA","EPE"],
    "key":{"C":item("thaumcraft:primal_charm"),"W":item("white_filter"),"B":item("enchanted_tome"),"A":item("avarice_pearl"),"E":item("minecraft:emerald"),"P":item("thaumcraft:arcane_pedestal")},"result":item("requisition_tome")})
external("textures/item/golem_linker.png","textures/items/golemLinker.png")
lang("item.automagy.golem_linker","item.golemLinker.name")
write("assets/automagy/models/item/golem_linker.json", {"parent":"minecraft:item/handheld","textures":{"layer0":"automagy:item/golem_linker"}})
for name,legacy in [("golem_taskmaster","golemTaskmaster"),("golem_inhibitor","golemInhibitor")]:
    external(f"textures/models/{name}.png",f"textures/models/{legacy}.png")
    EN["block.automagy."+name]="Golem Taskmaster" if name=="golem_taskmaster" else "Golem Inhibitor"
    ZH["block.automagy."+name]="傀儡主控" if name=="golem_taskmaster" else "傀儡抑制器"
    parts=[scribe_part(x,y,z,w,h,d,u,v) for x,y,z,w,h,d,u,v in [(4,0,4,8,2,8,0,0),(6.5,2,6.5,3,6,3,0,11),(6,8,6,4,1,4,33,0),(7.5,9,7.5,1,1,1,34,6)]]
    write(f"assets/automagy/models/block/{name}.json", {"textures":{"table":f"automagy:models/{name}","particle":f"automagy:models/{name}"},"elements":parts})
    write(f"assets/automagy/models/item/{name}.json", {"parent":"builtin/entity","display":DEVICE_DISPLAY})
    write(f"assets/automagy/blockstates/{name}.json", {"variants":{"":{"model":f"automagy:block/{name}"}}})
    write(f"data/automagy/loot_tables/blocks/{name}.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:"+name}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
EN.update({"message.automagy.inhibitor.exempt":"Linked golems are exempt.","message.automagy.inhibitor.only_linked":"Only linked golems are inhibited."})
ZH.update({"message.automagy.inhibitor.exempt":"豁免已链接傀儡。","message.automagy.inhibitor.only_linked":"仅抑制已链接傀儡。"})
recipe("golem_inhibitor", {"type":"thaumcraft:arcane","research":"automagy:GOLEMINHIBITOR","vis":{"perditio":15,"ordo":5,"aer":5,"ignis":5},"pattern":["ABQ","TIT","IRI"],
    "key":{"A":item("thaumcraft:air_shard"),"B":item("entropy_crystal_brain"),"Q":item("minecraft:quartz"),"T":item("thaumcraft:thaumium_ingot"),"I":item("minecraft:iron_ingot"),"R":item("redcrystal_amp")},"result":item("golem_inhibitor")})
recipe("golem_linker", {"type":"thaumcraft:infusion","research":"automagy:GOLEMLINKER","instability":3,"aspects":{"ordo":16,"spiritus":4,"humanus":4,"instrumentum":8},
    "center":{"ingredient":item("thaumcraft:silverwood_log")},"components":[{"ingredient":item(i)} for i in ["minecraft:diamond","minecraft:quartz_block","thaumcraft:golem_upgrade_order"]],"result":item("golem_linker")})
recipe("golem_taskmaster", {"type":"thaumcraft:infusion","research":"automagy:INVENTARIUM_DELIVERY","instability":4,"aspects":{"ordo":30,"cognitio":20,"humanus":20,"spiritus":20,"lucrum":8,"machina":8},
    "center":{"ingredient":item("order_crystal_brain")},"components":[{"ingredient":item(i)} for i in ["minecraft:ender_eye","thaumcraft:arcane_stone","thaumcraft:amber","thaumcraft:silverwood_planks","thaumcraft:balanced_shard","thaumcraft:arcane_stone","white_filter","thaumcraft:silverwood_planks"]],"result":item("golem_taskmaster")})
research("GOLEMINHIBITOR","golem_inhibitor",6,-5,[text_page("GOLEMINHIBITOR",1),recipe_page("golem_inhibitor")],{"cognitio":5,"vinculum":8,"machina":4,"spiritus":2,"humanus":2},["CRYSTALBRAIN"])
RESEARCH[-1].update(complexity=2,isHidden=True,parentsHidden=["GOLEMFETTER","THAUMIUM"],itemTriggers=[stack("entropy_crystal_brain")])
research("GOLEMLINKER","golem_linker",7,-5,[text_page("GOLEMLINKER",1),recipe_page("golem_linker"),text_page("GOLEMLINKER",2)],{"ordo":8,"humanus":4,"spiritus":4,"instrumentum":4},["GOLEMINHIBITOR"],secondary=True)
RESEARCH[-1].update(isConcealed=True,parentsHidden=["INFUSION","UPGRADEORDER","GOLEMBELL"])
research("INVENTARIUM_DELIVERY","requisition_tome",10,5,[text_page("INVENTARIUM_DELIVERY",1),recipe_page("golem_taskmaster"),text_page("INVENTARIUM_DELIVERY",2),recipe_page("amber_memory"),
    text_page("INVENTARIUM_DELIVERY",3),text_page("INVENTARIUM_DELIVERY",4),text_page("INVENTARIUM_DELIVERY",5),recipe_page("requisition_tome"),text_page("INVENTARIUM_DELIVERY",6),{"type":"TEXT","aspects":[],"text":"guide.automagy.requisition.delivery"}],
    {"lucrum":5,"humanus":3,"cognitio":5,"spiritus":3,"ordo":3},["INVENTARIUM"])
RESEARCH[-1].update(complexity=2,isConcealed=True,parentsHidden=["automagy:GOLEMLINKER","automagy:GOLEMINHIBITOR","automagy:NITORLIGHT"])
ASPECT_ENTRIES.append({"items":["automagy:golem_inhibitor"],"aspects":aspects({"aer":4,"cognitio":4,"ignis":2,"metallum":10,"perditio":8,"potentia":3,"praecantatio":6,"vitreus":10,"vinculum":4})})

write("data/automagy/item_aspects/content.json", {"entries":ASPECT_ENTRIES})
external("textures/item/auto_hand_mirror.png","textures/items/autoHandMirror.png")
external("textures/gui/auto_hand_mirror.png","textures/gui/autoHandMirror.png")
lang("item.automagy.auto_hand_mirror","item.autoHandMirror.name")
write("assets/automagy/models/item/auto_hand_mirror.json", {"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/auto_hand_mirror"}})
recipe("auto_hand_mirror", {"type":"thaumcraft:arcane","research":"automagy:AUTOHANDMIRROR","vis":{"ordo":54,"aer":25,"perditio":25},"pattern":[" F ","tSt"," H "],
    "key":{"F":item("finical_maw"),"t":item("thaumcraft:thaumium_nugget"),"S":item("thaumcraft:salis_mundus"),"H":item("thaumcraft:hand_mirror")},"result":item("auto_hand_mirror")})
research("AUTOHANDMIRROR","auto_hand_mirror",4,4,[text_page("AUTOHANDMIRROR",1),recipe_page("auto_hand_mirror"),text_page("AUTOHANDMIRROR",2)],{"fames":8,"ordo":8,"permutatio":5},["FINICALMAW"],secondary=True)
RESEARCH[-1].update(isConcealed=True,parentsHidden=["MIRRORHAND"])
EN["gui.automagy.hand_mirror.help"]="Filters control pickup. The center slot sends manually."
ZH["gui.automagy.hand_mirror.help"]="两侧筛选拾取物，中间手动传送。"
external("models/external/merccrystal.obj","textures/models/merccrystal.obj")
for texture in ["merccrystal","merccrystalActive"]:external(f"textures/models/{texture}.png",f"textures/models/{texture}.png")
write("data/automagy/loot_tables/blocks/redcrystal_merc.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:redcrystal_merc","functions":[{"function":"minecraft:copy_nbt","source":"block_entity","ops":[{"source":"Mirror","target":"Mirror","op":"replace"}]}]}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
recipe("redcrystal_merc", {"type":"thaumcraft:arcane","research":"automagy:REDCRYSTAL_MIRRORBOUND","vis":{"ignis":5,"ordo":10,"perditio":5,"aer":10},"pattern":[" O ","GRQ","SSS"],
    "key":{"O":item("thaumcraft:order_shard"),"G":item("thaumcraft:mirrored_glass"),"R":item("redcrystal_res"),"Q":item("thaumcraft:quicksilver"),"S":item("thaumcraft:arcane_stone")},"result":item("redcrystal_merc")})
research("REDCRYSTAL_MIRRORBOUND","redcrystal_merc",-5,4,[text_page("REDCRYSTAL_MIRRORBOUND",1),recipe_page("redcrystal_merc"),text_page("REDCRYSTAL_MIRRORBOUND",2)],{"iter":8,"potentia":5,"tenebrae":3,"praecantatio":5},["REDCRYSTAL_RES"])
RESEARCH[-1].update(complexity=2,isConcealed=True,parentsHidden=["MIRROR"])
external("textures/models/brain_water.png","textures/models/brainWater.png")
lang("block.automagy.essentia_locus","tile.blockEssentiaLocus.name")
write("assets/automagy/blockstates/essentia_locus.json", {"variants":{"":{"model":"automagy:block/essentia_locus"}}})
write("assets/automagy/models/block/essentia_locus.json", {"textures":{"particle":"automagy:models/brain_water"},"elements":[]})
write("assets/automagy/models/item/essentia_locus.json", {"parent":"builtin/entity","gui_light":"front"})
write("data/automagy/loot_tables/blocks/essentia_locus.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":name,"functions":[{"function":"minecraft:set_count","count":count}]}],"conditions":[{"condition":"minecraft:survives_explosion"}]} for name,count in [("automagy:essentia_locus",1),("thaumcraft:water_shard",6)]]})
recipe("essentia_locus", {"type":"thaumcraft:infusion","research":"automagy:ESSENTIALOCUS","instability":4,"aspects":{"aqua":32,"praecantatio":24,"sensus":16,"cognitio":8,"ordo":8},"center":{"ingredient":item("water_crystal_brain")},"components":[{"ingredient":item(i)} for i in ["minecraft:water_bucket","thaumcraft:vis_filter","thaumcraft:goggles","thaumcraft:essentia_resonator"]],"result":item("essentia_locus")})
research("ESSENTIALOCUS","essentia_locus",4,-3,[text_page("ESSENTIALOCUS",1),recipe_page("essentia_locus"),text_page("ESSENTIALOCUS",2),{"type":"TEXT","aspects":[],"text":"guide.automagy.locus.structure"},text_page("ESSENTIALOCUS",3)],{"cognitio":5,"aqua":10,"praecantatio":8},["CRYSTALBRAIN","VISREADER"])
RESEARCH[-1].update(isHidden=True,parentsHidden=["TUBES","GOGGLES"],itemTriggers=[stack("water_crystal_brain")])
EN["guide.automagy.locus.structure"]="Build a 3x3 pool of source water with a lapis block at its center, edged by twelve silverwood planks (no corners). Put a water crystal cluster on the lapis and place the locus above it. Keep the eight surrounding spaces clear on both levels above the pool. The cluster is consumed; breaking the locus returns six water shards. Insert linked crystal eyes to discover connected containers. Empty-hand use retrieves the last eye. Water crystals touching the rim increase the limits."
ZH["guide.automagy.locus.structure"]="建造3×3水源池，中心换成青金石块，外沿用十二块银树木板围边（四角空缺）。青金石上放水晶簇，再在其上放定位器；池上两层各八格周围空间保持空气。水晶簇会被消耗，拆除定位器返还六枚水碎片。插入已绑定水晶眼可扫描相连容器，空手取回最后一只眼睛。紧邻木板的水晶簇增加数量和距离上限。"
for suffix in ["top","side","top_overlay","side_overlay"]:external(f"textures/block/locus_aggregator_{suffix}.png",f"textures/blocks/locus_aggregator_{suffix}.png")
lang("block.automagy.locus_aggregator","tile.blockEssentiaLocusAggregator.name")
faces=["up","down","north","south","east","west"]
for upper in [False,True]:
    lo,hi=(8,16) if upper else (0,8)
    name="locus_aggregator"+("_top" if upper else "")
    write(f"assets/automagy/models/block/{name}.json", {"parent":"minecraft:block/block","render_type":"minecraft:cutout","textures":{"top":"automagy:block/locus_aggregator_top","side":"automagy:block/locus_aggregator_side","bottom":"minecraft:block/lapis_block","glow_top":"automagy:block/locus_aggregator_top_overlay","glow_side":"automagy:block/locus_aggregator_side_overlay","particle":"automagy:block/locus_aggregator_side"},"elements":[
        {"from":[0,lo,0],"to":[16,hi,16],"faces":{f:{"texture":"#"+({"up":"top","down":"bottom"}.get(f,"side"))} for f in faces}},
        {"from":[-.001,lo,-.001],"to":[16.001,hi+.001,16.001],"shade":False,"forge_data":{"block_light":15,"sky_light":15,"ambient_occlusion":False},"faces":{f:{"texture":"#glow_top" if f=="up" else "#glow_side"} for f in faces if f!="down"}}]})
write("assets/automagy/models/item/locus_aggregator.json", {"parent":"automagy:block/locus_aggregator"})
write("assets/automagy/blockstates/locus_aggregator.json", {"variants":{"half=bottom":{"model":"automagy:block/locus_aggregator"},"half=top":{"model":"automagy:block/locus_aggregator_top"}}})
path="data/minecraft/tags/blocks/mineable/pickaxe.json"
tag=json.loads((ROOT/path).read_text(encoding="utf-8"));tag["values"]=list(dict.fromkeys(tag["values"]+["automagy:locus_aggregator"]));write(path,tag)
write("data/automagy/loot_tables/blocks/locus_aggregator.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:locus_aggregator"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
recipe("locus_aggregator", {"type":"thaumcraft:infusion","research":"automagy:ESSENTIALOCUSAGGREGATOR","instability":4,"aspects":{"aqua":32,"praecantatio":24,"limus":16,"iter":8,"permutatio":8,"tenebrae":16},"center":{"ingredient":item("thaumcraft:essentia_mirror")},"components":[{"ingredient":item(i)} for i in ["minecraft:map","thaumcraft:void_jar","thaumcraft:balanced_shard","minecraft:lapis_block","thaumcraft:quicksilver","thaumcraft:balanced_shard"]],"result":item("locus_aggregator")})
research("ESSENTIALOCUSAGGREGATOR","locus_aggregator",2,-4,[text_page("ESSENTIALOCUSAGGREGATOR",1),recipe_page("locus_aggregator"),text_page("ESSENTIALOCUSAGGREGATOR",2)],{"praecantatio":5,"limus":5,"iter":5,"tenebrae":5},["ESSENTIALOCUS"],secondary=True)
RESEARCH[-1].update(isConcealed=True,parentsHidden=["MIRRORESSENTIA","JARVOID"])
ASPECT_ENTRIES.append({"items":["automagy:locus_aggregator"],"aspects":aspects({"iter":10,"permutatio":10,"aqua":8,"praecantatio":8,"sensus":8,"tenebrae":5,"limus":5,"alienis":4})})

external("textures/item/vishroom_stew.png","textures/items/vishroomStew.png")
lang("item.automagy.vishroom_stew","item.foodstuff.0.name")
lang("tooltip.automagy.vishroom_stew","Automagy.tip.foodstuff.0")
write("assets/automagy/models/item/vishroom_stew.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/vishroom_stew"}})
for name,ingredients in [("bucket",["vishroom_bucket","minecraft:bowl"]),("red",["thaumcraft:vishroom","minecraft:red_mushroom","minecraft:bowl"]),("brown",["thaumcraft:vishroom","minecraft:brown_mushroom","minecraft:bowl"])]:
    recipe("vishroom_stew_"+name,{"type":"minecraft:crafting_shapeless","ingredients":[item(i) for i in ingredients],"result":item("vishroom_stew")})

external("textures/models/thirsty_tank.png","textures/models/thirstyTank.png")
lang("block.automagy.thirsty_tank","tile.blockThirstyTank.name")
EN["message.automagy.tank.capacity"]="Drain some liquid before removing this capacity glyph."
ZH["message.automagy.tank.capacity"]="请先排出部分液体，再移除此扩容符文。"
tank_parts=[]
for x,y,z,w,h,d,u,v in [(0,0,0,16,2,16,0,0)]+[(x,2,z,4,14,2,0,32) for x in [0,12] for z in [0,14]]+[(x,2,z,2,14,3,13,32) for x in [0,14] for z in [2,11]]+[(2,2,2,12,14,12,28,32)]:
    part=scribe_part(x,y,z,w,h,d,u,v)
    for face in part["faces"].values():face["uv"]=[a/2 for a in face["uv"]]
    tank_parts.append(part)
write("assets/automagy/models/block/thirsty_tank.json",{"parent":"minecraft:block/block","render_type":"minecraft:translucent","textures":{"table":"automagy:models/thirsty_tank","particle":"automagy:models/thirsty_tank"},"elements":tank_parts})
write("assets/automagy/models/item/thirsty_tank.json",{"parent":"builtin/entity","gui_light":"front","display":{"gui":{"rotation":[30,225,0],"translation":[0,0,0],"scale":[.625,.625,.625]},"ground":{"translation":[0,3,0],"scale":[.25,.25,.25]},"fixed":{"scale":[.5,.5,.5]}}})
write("assets/automagy/blockstates/thirsty_tank.json",{"variants":{"":{"model":"automagy:block/thirsty_tank"}}})
write("data/automagy/loot_tables/blocks/thirsty_tank.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:thirsty_tank","functions":[{"function":"minecraft:copy_nbt","source":"block_entity","ops":[{"source":key,"target":"BlockEntityTag."+key,"op":"replace"} for key in ["Tank","Glyphs"]]}]}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
for i,name in enumerate(["void","consume","siphon","envy","temperance","preserve","guzzler","reservoir","bovine"],1):
    external(f"textures/item/tank_glyph_{i}.png",f"textures/items/glyph-{name}.png")
    external(f"textures/block/tank_glyph_{i}.png",f"textures/items/glyphover-{name}.png")
    lang(f"item.automagy.tank_glyph_{i}",f"item.glyph.{i}.name")
    write(f"assets/automagy/models/item/tank_glyph_{i}.json",{"parent":"minecraft:item/generated","textures":{"layer0":f"automagy:item/tank_glyph_{i}"}})
recipe("thirsty_tank",{"type":"thaumcraft:arcane","research":"automagy:THIRSTYTANK","vis":{"aqua":16,"ordo":4,"perditio":4},"pattern":["SGS","GAG","SWS"],"key":{"S":item("thaumcraft:arcane_stone"),"G":{"tag":"forge:glass"},"A":item("avarice_pearl"),"W":item("thaumcraft:water_shard")},"result":item("thirsty_tank")})
research("THIRSTYTANK","thirsty_tank",0,9,[text_page("THIRSTYTANK",1),recipe_page("thirsty_tank"),text_page("THIRSTYTANK",2)],{"fames":8,"aqua":5,"vacuos":3,"permutatio":3},["GREEDYCHEST"])
RESEARCH[-1].update(complexity=2)

external("textures/item/aqua_ink.png","textures/items/glyph-aquaink.png")
lang("item.automagy.aqua_ink","item.glyph.0.name")
write("assets/automagy/models/item/aqua_ink.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/aqua_ink"}})
recipe("aqua_ink",{"type":"thaumcraft:arcane","mode":"shapeless","research":"automagy:THIRSTYTANK_GLYPH","vis":{"aqua":1},"ingredients":[item("water_sliver"),item("water_sliver"),{"tag":"forge:slimeballs"},{"tag":"forge:dyes/black"},item("minecraft:clay_ball")],"result":{**item("aqua_ink"),"count":2}})
glyph_specs=[
 (1,["Oi ","B i","si "],{"perditio":10},{"B":"minecraft:blaze_powder","O":"minecraft:obsidian"}),
 (2,[" i ","iEi","siB"],{"perditio":15},{"E":"thaumcraft:entropy_shard","B":"minecraft:blaze_powder"}),
 (3,[" ii","iH ","si "],{"ordo":2,"perditio":2},{"H":"minecraft:hopper"}),
 (4,[" i ","iAi","sQ "],{"ignis":5},{"A":"avarice_pearl","Q":"thaumcraft:quicksilver"}),
 (5,["ir ","rir","sri"],{"ordo":10},{"r":"minecraft:redstone"}),
 (6,["iii"," i ","sSO"],{"ordo":5},{"S":"minecraft:slime_ball","O":"thaumcraft:order_shard"}),
 (7,[" ii","iSP","siM"],{"ignis":5,"perditio":5},{"S":"shattered_avarice_pearl","M":"minecraft:glistering_melon_slice","P":"minecraft:potion"}),
 (8,["BPB","iWi","si "],{"aer":3,"perditio":3},{"B":"minecraft:bucket","P":"minecraft:ender_pearl","W":"thaumcraft:water_shard"}),
]
for number,pattern,vis,extra in glyph_specs:
    key={"i":item("aqua_ink"),"s":item("thaumcraft:arcane_stone_slab"),**{k:item(v) for k,v in extra.items()}}
    if number==7:key["P"]={"type":"forge:partial_nbt","item":"minecraft:potion","nbt":'{Potion:"minecraft:strong_swiftness"}'}
    recipe(f"tank_glyph_{number}",{"type":"thaumcraft:arcane","research":"automagy:THIRSTYTANK_GLYPH","vis":{"aqua":16 if number==8 else 3,**vis},"pattern":pattern,"key":key,"result":item(f"tank_glyph_{number}")})
for animal,food in [("cow","minecraft:milk_bucket"),("mooshroom","minecraft:mushroom_stew")]:
    recipe("tank_glyph_9_"+animal,{"type":"thaumcraft:arcane","research":"automagy:THIRSTYTANK_GLYPH","vis":{"aqua":3,"terra":15},"pattern":["i i","LiL","sME"],"key":{"i":item("aqua_ink"),"s":item("thaumcraft:arcane_stone_slab"),"L":item("minecraft:leather"),"M":item(food),"E":{"type":"forge:partial_nbt","item":"automagy:entity_eye","nbt":'{EntityType:"minecraft:'+animal+'"}'}},"result":item("tank_glyph_9")})
research("THIRSTYTANK_GLYPH","aqua_ink",-1,8,[text_page("THIRSTYTANK_GLYPH",1),recipe_page("aqua_ink"),text_page("THIRSTYTANK_GLYPH",2),text_page("THIRSTYTANK_GLYPH",3)]+[recipe_page(f"tank_glyph_{i}") for i in range(1,9)],{"fames":3,"aqua":3,"machina":2},["THIRSTYTANK"])
RESEARCH[-1].update(complexity=1,parentsHidden=["automagy:AVARICIOUS"],isConcealed=True)
research("THIRSTYTANK_GLYPH_BOVINE","tank_glyph_9",-2,8,[text_page("THIRSTYTANK_GLYPH_BOVINE",1),recipe_page("tank_glyph_9_cow"),recipe_page("tank_glyph_9_mooshroom")],{"fames":2,"aqua":2,"bestia":3,"messis":3},["THIRSTYTANK_GLYPH"],secondary=True)
RESEARCH[-1].update(complexity=2,parentsHidden=["automagy:ENTITYSIGNAL"],isConcealed=True)

EN["gui.automagy.workbench.heat"]="Heat: %s"
ZH["gui.automagy.workbench.heat"]="热量：%s"
lang("block.automagy.golem_workbench","tile.blockGolemWorkbench.name")

for number,element in [(0,"air"),(1,"fire"),(3,"earth"),(5,"entropy")]:
    external(f"textures/item/workbench_upgrade_{number}.png",f"textures/items/workbench-up-{element}.png")
    lang(f"item.automagy.workbench_upgrade_{number}",f"item.golemWorkbenchUpgrade.{number}.name")
    lang(f"tooltip.automagy.workbench_upgrade.{number}",f"Automagy.tip.golemWorkbenchUpgrade.{number}")
    write(f"assets/automagy/models/item/workbench_upgrade_{number}.json",{"parent":"minecraft:item/generated","textures":{"layer0":f"automagy:item/workbench_upgrade_{number}"}})
recipe("golem_workbench",{"type":"thaumcraft:infusion","research":"automagy:GOLEMWORKBENCH","instability":3,"aspects":{"fabrico":10,"humanus":5,"ordo":5},"center":{"ingredient":item("minecraft:crafting_table")},"components":[{"ingredient":item("white_filter")},{"ingredient":item("minecraft:chest")}],"result":item("golem_workbench")})
for number,element,aspect,pattern,extra in [
 (0,"air","aer",["FTN","TGT","RTQ"],{"F":"minecraft:feather","R":"minecraft:redstone_block","Q":"minecraft:quartz_block"}),
 (1,"fire","ignis",["BTN","TGT","WTO"],{"B":"minecraft:blaze_powder","W":"white_filter","O":"minecraft:obsidian"}),
 (3,"earth","terra",["VTN","TGT","RTQ"],{"V":"minecraft:vine","R":"minecraft:redstone_block","Q":"minecraft:quartz_block"}),
 (5,"entropy","perditio",["eTN","TGT","BTD"],{"e":"thaumcraft:wisp_essence","B":"enchanted_tome","D":"minecraft:diamond"})]:
    key={"T":item("thaumcraft:thaumium_ingot"),"N":item("thaumcraft:nitor"),"G":item("thaumcraft:golem_upgrade_"+element),**{k:item(v) for k,v in extra.items()}}
    if number==5:key["e"]={"type":"forge:partial_nbt","item":"thaumcraft:wisp_essence","nbt":'{Aspects:[{key:"perditio",amount:2}]}'}
    recipe(f"workbench_upgrade_{number}",{"type":"thaumcraft:arcane","research":"automagy:GOLEMWORKBENCH_UPGRADE","vis":{aspect:15},"pattern":pattern,"key":key,"result":item(f"workbench_upgrade_{number}")})
research("GOLEMWORKBENCH","golem_workbench",7,6,[text_page("GOLEMWORKBENCH",1),recipe_page("golem_workbench"),text_page("GOLEMWORKBENCH",2)],{"fabrico":8,"ordo":3,"humanus":5,"spiritus":5},["ENCHANTEDPAPER"])
RESEARCH[-1].update(complexity=1,parentsHidden=["COREEMPTY"],isConcealed=True)
research("GOLEMWORKBENCH_UPGRADE","workbench_upgrade_0",8,6,[text_page("GOLEMWORKBENCH_UPGRADE",1),recipe_page("workbench_upgrade_0"),text_page("GOLEMWORKBENCH_UPGRADE",2),recipe_page("workbench_upgrade_1"),text_page("GOLEMWORKBENCH_UPGRADE",3),recipe_page("workbench_upgrade_3"),recipe_page("workbench_upgrade_5")],{"fabrico":5,"humanus":1,"spiritus":1},["GOLEMWORKBENCH"],secondary=True)
RESEARCH[-1].update(complexity=1,isConcealed=True,parentsHidden=["UPGRADEAIR","UPGRADEEARTH","UPGRADEFIRE","UPGRADEWATER","UPGRADEORDER","UPGRADEENTROPY"])
write("data/automagy/loot_tables/blocks/golem_workbench.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:golem_workbench"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})

external("textures/models/golem_workbench.png","textures/models/golemWorkbench.png")
workbench_parts=[scribe_part(x,0,z,4,4,4,0,16) for x in [1,11] for z in [1,11]]
workbench_parts += [scribe_part(0,4,0,16,4,16,0,0),scribe_part(12,2,5,2,2,6,18,24),scribe_part(2,2,5,2,2,6,18,24),scribe_part(5,2,12,6,2,2,35,23),scribe_part(5,2,2,6,2,2,35,23)]
write("assets/automagy/models/block/golem_workbench.json",{"textures":{"table":"automagy:models/golem_workbench","particle":"automagy:models/golem_workbench"},"elements":workbench_parts})
write("assets/automagy/models/item/golem_workbench.json",{"parent":"automagy:block/golem_workbench"})
write("assets/automagy/blockstates/golem_workbench.json",{"variants":{"":{"model":"automagy:block/golem_workbench"}}})

# Ordinary mob lure: hunger essentia boosts its range, not its charm strength.
write("data/minecraft/tags/blocks/needs_iron_tool.json",{"replace":False,"values":["automagy:mob_lure"]})
lang("block.automagy.mob_lure","tile.blockMobLure.name")
external("textures/block/mob_lure.png","textures/blocks/blockMobLure.png")
external("textures/block/mob_lure_overlay.png","textures/blocks/blockMobLureOverlay.png")
write("assets/automagy/models/block/mob_lure.json",{"parent":"minecraft:block/cube_all","textures":{"all":"automagy:block/mob_lure"}})
write("assets/automagy/blockstates/mob_lure.json",{"variants":{f"active={str(active).lower()}":{"model":"automagy:block/mob_lure"} for active in [False,True]}})
write("assets/automagy/models/item/mob_lure.json",{"parent":"automagy:block/mob_lure"})
write("data/automagy/loot_tables/blocks/mob_lure.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:mob_lure"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
recipe("mob_lure",{"type":"thaumcraft:infusion","research":"automagy:MOBLURE","instability":3,"aspects":{"ignis":16,"cognitio":10,"fames":10,"tenebrae":8},"center":{"ingredient":item("fire_crystal_brain")},"components":[{"ingredient":item(i)} for i in ["minecraft:fermented_spider_eye","minecraft:carrot_on_a_stick","runed_obsidian","thaumcraft:entropy_shard","thaumcraft:fire_shard","thaumcraft:obsidian_totem"]],"result":item("mob_lure")})
research("MOBLURE","mob_lure",12,-4,[text_page("MOBLURE",1),recipe_page("mob_lure")]+[text_page("MOBLURE",i) for i in [2,3,4]],{"ignis":5,"cognitio":5,"fames":5},["NETHERMIND"],secondary=True)
RESEARCH[-1].update(complexity=2,parentsHidden=["automagy:CRYSTALBRAIN"],isConcealed=True)

# Ember heart and nether wisp; original art stays in the player-supplied jar.
external("textures/item/nether_heart.png","textures/items/netherHeart.png")
external("textures/item/nether_heart.png.mcmeta","textures/items/netherHeart.png.mcmeta")
lang("item.automagy.nether_heart","item.netherHeart.name")
EN["entity.automagy.nether_wisp"]="Nether Wisp"
ZH["entity.automagy.nether_wisp"]="下界精灵"
write("assets/automagy/models/item/nether_heart.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/nether_heart"}})
ASPECT_ENTRIES.append({"items":["automagy:nether_heart"],"aspects":aspects({"ignis":6,"praecantatio":6,"bestia":5})})
write("data/forge/loot_modifiers/global_loot_modifiers.json",{"replace":False,"entries":["automagy:nether_heart"]})
write("data/automagy/loot_modifiers/nether_heart.json",{"type":"automagy:nether_heart","conditions":[{"condition":"forge:loot_table_id","loot_table_id":"minecraft:blocks/spawner"}]})
write("data/automagy/loot_tables/entities/nether_wisp.json",{"type":"minecraft:entity","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"thaumcraft:wisp_essence","functions":[{"function":"minecraft:copy_nbt","source":"this","ops":[{"source":"Essence","target":"Aspects","op":"replace"}]}]}]}]})

# Nethermind controller shell. Both enabled faces use an emissive renderer overlay.
lang("block.automagy.nethermind","tile.blockNethermind.name")
for suffix,old in [("",""),("_side","Side"),("_bottom","Bottom"),("_top_glow","TopGlow"),("_bottom_glow","BottomGlow")]:
    external(f"textures/block/nethermind{suffix}.png",f"textures/blocks/blockNethermind{old}.png")
write("assets/automagy/models/block/nethermind.json",{"parent":"minecraft:block/cube_bottom_top","textures":{"top":"automagy:block/nethermind","side":"automagy:block/nethermind_side","bottom":"automagy:block/nethermind_bottom"}})
write("assets/automagy/blockstates/nethermind.json",{"variants":{f"active={str(active).lower()}":{"model":"automagy:block/nethermind"} for active in [False,True]}})
write("assets/automagy/models/item/nethermind.json",{"parent":"automagy:block/nethermind"})
write("data/automagy/loot_tables/blocks/nethermind.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:nethermind"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})

# Dimensional lure shell; animated overlay is rendered separately by the client.
lang("block.automagy.dimension_lure","tile.blockDimensionLure.name")
for suffix,old in [("",""),("_side","Side"),("_overlay","Overlay")]:
    external(f"textures/block/dimension_lure{suffix}.png",f"textures/blocks/blockDimensionLure{old}.png")
write("assets/automagy/models/block/dimension_lure.json",{"parent":"minecraft:block/cube_bottom_top","textures":{"top":"automagy:block/dimension_lure","side":"automagy:block/dimension_lure_side","bottom":"automagy:block/dimension_lure_side"}})
write("assets/automagy/blockstates/dimension_lure.json",{"variants":{f"active={str(active).lower()}":{"model":"automagy:block/dimension_lure"} for active in [False,True]}})
write("assets/automagy/models/item/dimension_lure.json",{"parent":"automagy:block/dimension_lure"})
write("data/automagy/loot_tables/blocks/dimension_lure.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:dimension_lure"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})

# Nether rune block variants; processing is owned by the Nethermind controller.
for key,texture,index,creature in [("rock","Rock",0,False),("lava","Lava",1,False),("crystal","Crystal",2,False),("light","Light",3,False),("soul","Soul",4,False),("celerity","Celerity",6,False),("zombie","Zombie",0,True),("blaze","Blaze",1,True),("ghast","Ghast",2,True),("skeleton","Skull",3,True),("slime","Slime",4,True),("bat","Bat",5,True),("wisp","Wisp",6,True)]:
    name="nether_rune_"+key
    lang("block.automagy."+name,"tile."+("blockNetherRuneMob" if creature else "blockNetherRune")+f".{index}.name")
    for active in [False,True]:
        suffix="_active" if active else ""
        external(f"textures/block/{name}{suffix}.png",f"textures/blocks/blockNetherRune{texture}"+("" if active or key=="celerity" else "Dim")+".png")
        write(f"assets/automagy/models/block/{name}{suffix}.json",{"parent":"minecraft:block/cube_all","textures":{"all":f"automagy:block/{name}{suffix}"}})
    write(f"assets/automagy/blockstates/{name}.json",{"variants":{f"active={str(active).lower()}":{"model":f"automagy:block/{name}"+("_active" if active else "")} for active in [False,True]}})
    write(f"assets/automagy/models/item/{name}.json",{"parent":f"automagy:block/{name}_active"})
    write(f"data/automagy/loot_tables/blocks/{name}.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":f"automagy:{name}"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
lang("block.automagy.runed_obsidian","tile.blockRunedObsidian.name")
for variant in range(6):
    texture="blockRunedObsidian"+(str(variant+2) if variant<5 else "")
    external(f"textures/block/runed_obsidian_{variant}.png",f"textures/blocks/{texture}.png")
    write(f"assets/automagy/models/block/runed_obsidian_{variant}.json",{"parent":"minecraft:block/cube_all","textures":{"all":f"automagy:block/runed_obsidian_{variant}"}})
write("assets/automagy/blockstates/runed_obsidian.json",{"variants":{f"rune={i}":{"model":f"automagy:block/runed_obsidian_{i}"} for i in range(6)}})
write("assets/automagy/models/item/runed_obsidian.json",{"parent":"automagy:block/runed_obsidian_0"})
write("data/automagy/loot_tables/blocks/runed_obsidian.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:runed_obsidian"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
ASPECT_ENTRIES.append({"items":["automagy:runed_obsidian"],"aspects":aspects({"ignis":4,"terra":3,"potentia":1,"tenebrae":1,"vacuos":1,"permutatio":1})})

# Thaumostatic tower: ordinary segments, controller and two-block formed spire.
def tower_box(start,end,uv,size):
    x,y,z=[end[i]-start[i] for i in range(3)];u,v=uv;w,h=size
    rects={"west":[u,v+z,u+z,v+z+y],"north":[u+z,v+z,u+z+x,v+z+y],"east":[u+z+x,v+z,u+z+x+z,v+z+y],"south":[u+z+x+z,v+z,u+z+x+z+x,v+z+y],"up":[u+z,v,u+z+x,v+z],"down":[u+z+x,v,u+z+x+x,v+z]}
    return {"from":start,"to":end,"faces":{face:{"texture":"#all","uv":[r[0]*16/w,r[1]*16/h,r[2]*16/w,r[3]*16/h]} for face,r in rects.items()}}
for name,old in [("thaumostatic_pylon","thaumostaticPylon"),("thaumostatic_controller","thaumostaticController")]:
    external(f"textures/models/{old}.png",f"textures/models/{old}.png")
    elements=[tower_box([3,0,3],[13,16,13],[20,0],[64,32])]+[tower_box([x,0,z],[x+4,16,z+4],[0,0],[64,32]) for x in [0,12] for z in [0,12]]
    write(f"assets/automagy/models/block/{name}.json",{"textures":{"all":f"automagy:models/{old}","particle":"minecraft:block/quartz_block_side"},"elements":elements})
    write(f"assets/automagy/models/item/{name}.json",{"parent":f"automagy:block/{name}"})
    write(f"assets/automagy/blockstates/{name}.json",{"variants":{"":{"model":f"automagy:block/{name}"}}})
    write(f"data/automagy/loot_tables/blocks/{name}.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:"+name}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
lang("block.automagy.thaumostatic_pylon","tile.blockThaumostaticPylon.0.name")
lang("block.automagy.thaumostatic_controller","tile.blockThaumostaticPylon.1.name")
lang("block.automagy.thaumostatic_spire","tile.blockThaumostaticSpire.name")
external("textures/models/thaumostaticSpire.png","textures/models/thaumostaticSpire.png")
spire_parts=[([0,0,0],[16,2,16],[0,0]),([1,2,1],[15,4,15],[0,20]),([3,4,3],[13,10,13],[0,38]),([5,10,5],[11,19,11],[41,38]),([7,19,7],[9,20,9],[57,34]),([7.5,20,7.5],[8.5,32,8.5],[58,20])]
write("assets/automagy/models/block/thaumostatic_spire.json",{"textures":{"all":"automagy:models/thaumostaticSpire","particle":"minecraft:block/iron_block"},"elements":[tower_box(a,b,uv,[128,64]) for a,b,uv in spire_parts]})
write("assets/automagy/models/block/thaumostatic_spire_top.json",{"textures":{"particle":"minecraft:block/iron_block"},"elements":[]})
write("assets/automagy/models/item/thaumostatic_spire.json",{"parent":"automagy:block/thaumostatic_spire"})
write("assets/automagy/blockstates/thaumostatic_spire.json",{"variants":{"top=false":{"model":"automagy:block/thaumostatic_spire"},"top=true":{"model":"automagy:block/thaumostatic_spire_top"}}})
write("data/automagy/loot_tables/blocks/thaumostatic_spire.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"thaumcraft:thaumium_block"}],"conditions":[{"condition":"minecraft:block_state_property","block":"automagy:thaumostatic_spire","properties":{"top":"false"}},{"condition":"minecraft:survives_explosion"}]}]})
recipe("thaumostatic_pylon",{"type":"thaumcraft:arcane","research":"automagy:THAUMOSTATICREFUELER","vis":{"aer":35,"ordo":20,"ignis":20},"pattern":["QIQ","ABA","QIQ"],"key":{"Q":item("minecraft:quartz"),"I":item("minecraft:iron_ingot"),"A":item("thaumcraft:air_shard"),"B":item("thaumcraft:essentia_tube")},"result":item("thaumostatic_pylon")})
recipe("thaumostatic_controller",{"type":"thaumcraft:infusion","research":"automagy:THAUMOSTATICREFUELER","instability":4,"aspects":{"aer":50,"cognitio":20,"potentia":20,"volatus":16,"iter":8,"permutatio":8},"center":{"ingredient":item("air_crystal_brain")},"components":[{"ingredient":item(i)} for i in ["minecraft:compass","minecraft:feather","thaumcraft:alchemical_construct","thaumcraft:vis_relay","minecraft:feather","thaumostatic_pylon"]],"result":item("thaumostatic_controller")})
research("THAUMOSTATICREFUELER","thaumostatic_spire",10,1,[text_page("THAUMOSTATICREFUELER",1),recipe_page("thaumostatic_pylon"),text_page("THAUMOSTATICREFUELER",2),recipe_page("thaumostatic_controller"),text_page("THAUMOSTATICREFUELER",3),text_page("THAUMOSTATICREFUELER",4)],{"cognitio":5,"aer":5,"volatus":5,"potentia":5},["CRYSTALBRAIN"])
RESEARCH[-1].update(complexity=2,isHidden=True,itemTriggers=[stack("air_crystal_brain")],parentsHidden=["HOVERHARNESS","VISPOWER","THAUMIUM","DISTILESSENTIA"])
ASPECT_ENTRIES.append({"items":["automagy:thaumostatic_pylon"],"aspects":aspects({"aer":10,"metallum":8,"vitreus":6,"potentia":10,"praecantatio":2,"permutatio":1})})

# Nether machinery: resource-pack recipes and research, no main-mod changes.
recipe("runed_obsidian",{"type":"thaumcraft:arcane","mode":"shapeless","research":"automagy:NETHERMIND","vis":{"ignis":6,"terra":3,"aer":1,"perditio":1},"ingredients":[item("fire_sliver"),item("minecraft:obsidian"),item("minecraft:quartz")],"result":item("runed_obsidian")})
recipe("runed_obsidian_tile",{**RECIPES["runed_obsidian"],"ingredients":[item("fire_sliver"),item("thaumcraft:obsidian_tile"),item("minecraft:quartz")]})
recipe("nethermind",{"type":"thaumcraft:infusion","research":"automagy:NETHERMIND","instability":5,"aspects":{"ignis":64,"cognitio":32,"iter":16,"tenebrae":16,"alienis":16},"center":{"ingredient":item("fire_crystal_brain")},"components":[{"ingredient":item(i),"damage":-1} for i in ["nether_heart","minecraft:lava_bucket","runed_obsidian","minecraft:soul_sand","minecraft:netherrack","runed_obsidian","avarice_pearl","minecraft:nether_wart","runed_obsidian"]],"result":item("nethermind")})
RUNE_RECIPES=[
("rock","NETHERMIND",{"ignis":3,"terra":6},["BSB","NRN","BSB"],{"B":"minecraft:nether_brick","N":"minecraft:netherrack"}),
("lava","NETHERMIND",{"ignis":30,"terra":10},["BSM","LRL","MSB"],{"B":"minecraft:blaze_rod","M":"minecraft:magma_cream","L":"minecraft:lava_bucket"}),
("crystal","NETHERRUNE_CRYSTAL",{"ignis":30,"terra":10,"ordo":20},["FSD","QRQ","DSO"],{"F":"thaumcraft:fire_shard","O":"thaumcraft:order_shard","D":"minecraft:diamond","Q":"minecraft:quartz_block"}),
("light","NETHERRUNE_LIGHT",{"ignis":30,"aer":30},["ASg","GRG","gSF"],{"A":"thaumcraft:air_shard","F":"thaumcraft:fire_shard","g":"minecraft:glowstone_dust","G":"minecraft:glowstone"}),
("soul","NETHERRUNE_SOUL",{"ignis":10,"terra":20,"perditio":10},["ASG","BRB","GSA"],{"A":"thaumcraft:amber_block","G":"minecraft:ghast_tear","B":"minecraft:soul_sand"}),
("celerity","NETHERRUNE_CELERITY",{"ignis":30,"terra":25,"aer":30},["ESA","PRP","ASE"],{"E":"minecraft:emerald","A":"avarice_pearl","P":"minecraft:potion"}),
("zombie","NETHERRUNE_ZOMBIE",{"ignis":20,"terra":15,"perditio":15},["GST","BRB","TSG"],{"G":"minecraft:golden_sword","T":"thaumcraft:tallow_block","B":"thaumcraft:zombie_brain"}),
("blaze","DIMENSIONLURE",{"ignis":25,"aer":10},["PSF","BRB","FSP"],{"P":"minecraft:blaze_powder","F":"fire_sliver","B":"minecraft:blaze_rod"}),
("ghast","NETHERRUNE_GHAST",{"ignis":20,"aer":15,"perditio":15},["FSs","TRT","sSF"],{"F":"minecraft:fire_charge","s":"minecraft:soul_sand","T":"minecraft:ghast_tear"}),
("skeleton","NETHERRUNE_SKELETON",{"terra":15,"perditio":20},["sSB","HRH","BSs"],{"s":"minecraft:stone_sword","B":"minecraft:nether_bricks","H":"minecraft:wither_skeleton_skull"}),
("slime","NETHERRUNE_SLIME",{"ignis":20,"terra":10,"aqua":10},["sSP","MRM","PSs"],{"s":"minecraft:slime_ball","P":"minecraft:blaze_powder","M":"minecraft:magma_cream"}),
("bat","NETHERRUNE_BAT",{"ignis":20,"aer":15,"perditio":15},["FST","GRG","TSF"],{"F":"fire_sliver","T":"minecraft:tnt","G":"minecraft:gunpowder"}),
("wisp","NETHERRUNE_WISP",{"ignis":40,"aer":20,"perditio":10},["DSs","ERE","sSD"],{"D":"minecraft:diamond","s":"thaumcraft:salis_mundus","E":"thaumcraft:wisp_essence"}),
]
for name,unlock,vis,pattern,ingredients in RUNE_RECIPES:
    keys={letter:item(value) for letter,value in ingredients.items()}
    keys.update(R=item("runed_obsidian"),S=item("thaumcraft:arcane_stone"))
    if name=="celerity":keys["P"].update(type="forge:partial_nbt",nbt='{Potion:"minecraft:swiftness"}')
    if name=="wisp":keys["E"].update(type="forge:partial_nbt",nbt='{Aspects:[{key:"ignis",amount:2}]}')
    recipe("nether_rune_"+name,{"type":"thaumcraft:arcane","research":"automagy:"+unlock,"vis":vis,"pattern":pattern,"key":keys,"result":item("nether_rune_"+name)})
for suffix,enchanted in [("","minecraft:fishing_rod"),("_book","minecraft:enchanted_book")]:
    recipe("dimension_lure"+suffix,{"type":"thaumcraft:infusion","research":"automagy:DIMENSIONLURE","instability":6,"aspects":{"ignis":64,"fames":32,"lucrum":8,"vinculum":16,"iter":16,"sensus":8},"center":{"ingredient":item("mob_lure")},"components":[{"ingredient":{"type":"automagy:enchanted","item":enchanted,"enchantment":"minecraft:lure","minimum":1},"damage":-1}]+[{"ingredient":item(i)} for i in ["minecraft:golden_apple","earth_crystal_brain","minecraft:ender_pearl","minecraft:sticky_piston","minecraft:cobweb","minecraft:oak_trapdoor"]],"result":item("dimension_lure")})
research("NETHERMIND","nethermind",11,-2,[text_page("NETHERMIND",1),recipe_page("runed_obsidian"),recipe_page("runed_obsidian_tile"),recipe_page("nethermind"),text_page("NETHERMIND",2),text_page("NETHERMIND",3),text_page("NETHERMIND",4),recipe_page("nether_rune_rock"),text_page("NETHERMIND",5),recipe_page("nether_rune_lava")],{"cognitio":5,"ignis":10,"alienis":5,"permutatio":5},["CRYSTALBRAIN"])
RESEARCH[-1].update(isHidden=True,itemTriggers=[stack("fire_crystal_brain")],parentsHidden=["automagy:GREEDYCHEST","VISPOWER","DISTILESSENTIA"])
for name,x,y,cost in [("crystal",13,-2,{"vitreus":9,"potentia":6,"terra":3}),("soul",13,-1,{"spiritus":9,"vinculum":3,"terra":3,"perditio":3}),("light",13,0,{"lux":9,"sensus":6}),("celerity",12,0,{"alienis":9,"iter":6,"permutatio":3})]:
    key="NETHERRUNE_"+name.upper()
    research(key,"nether_rune_"+name,x,y,[text_page(key,1),recipe_page("nether_rune_"+name)],cost,["NETHERMIND"],secondary=True)
    RESEARCH[-1]["isConcealed"]=True
    if name=="celerity":RESEARCH[-1]["parentsHidden"]=["automagy:GREEDYCHEST"]
research("DIMENSIONLURE","dimension_lure",11,-6,[text_page("DIMENSIONLURE",1),recipe_page("dimension_lure"),recipe_page("dimension_lure_book")]+[text_page("DIMENSIONLURE",i) for i in [2,3,4,5]]+[recipe_page("nether_rune_blaze"),text_page("DIMENSIONLURE",6)],{"ignis":5,"alienis":5,"fames":5,"iter":5,"vinculum":5},["MOBLURE","ENTITYSIGNAL"])
RESEARCH[-1].update(complexity=2,isConcealed=True,parentsHidden=["automagy:NETHERMIND"])
for name,x,y,cost,entity in [("zombie",10,-8,{"vinculum":3,"lucrum":3,"cognitio":3},"minecraft:zombified_piglin"),("ghast",11,-8,{"vinculum":3,"spiritus":3,"exanimis":3},"minecraft:ghast"),("skeleton",12,-8,{"vinculum":3,"mortuus":3,"telum":3},"minecraft:wither_skeleton"),("slime",13,-7,{"vinculum":3,"limus":3,"ignis":3},"minecraft:magma_cube"),("bat",13,-6,{"vinculum":3,"bestia":3,"ignis":3},"thaumcraft:fire_bat"),("wisp",13,-8,{"vinculum":3,"auram":3,"ignis":3},"automagy:nether_wisp")]:
    key="NETHERRUNE_"+name.upper()
    pages=[text_page(key,1),recipe_page("nether_rune_"+name)]
    if name in ["ghast","wisp"]:pages.append(text_page(key,2))
    research(key,"nether_rune_"+name,x,y,pages,cost,["DIMENSIONLURE"],secondary=True)
    RESEARCH[-1].update(isHidden=True) # NetherResearchEvents requires both scan and kill.
    if name=="zombie":RESEARCH[-1]["parentsHidden"]=["TALLOW"]

# Authored structure/cost pages avoid copying old prose or leaving printf placeholders.
def authored_page(key,en,zh):
    EN[key]=en; ZH[key]=zh; LANG_MAP.pop(key,None)
    return {"type":"TEXT","text":key,"aspects":[]}
def insert_page(research_key,index,page):
    pages=next(p["pages"] for p in PAGES if p["key"]=="automagy:"+research_key)
    pages.insert(index,page)
    next(r for r in RESEARCH if r["key"]=="automagy:"+research_key)["pageCount"]=len(pages)
insert_page("THAUMOSTATICREFUELER",5,authored_page("automagy.page.thaumostatic_structure",
    "Build one vertical column: chiseled quartz at the bottom, one controller and at least two pylons, then a thaumium block exposed to the sky with an empty block above it. The controller may sit anywhere between the quartz and the cap. Up to six pylons improve performance.<BR>Tap a horizontal controller face with a wand: 50 Aer, 20 Ignis, 20 Ordo, 10 Aqua. The cap becomes a two-block spire. Supply Aer centivis to the spire or controller and Potentia essentia through side pipes. Wand-tap each side to cycle Potentia, Aer and Volatus suction. Aer increases range; Volatus increases transfer speed. Each booster lasts 1200 powered ticks.",
    "搭建竖直塔柱：底部为錾制石英块，中间放一个控制器及至少两个塔节，顶部放神秘锭块。顶部需露天，且其上方一格为空气。控制器可处于塔柱中任意位置，最多六个塔节参与强化。<BR>用法杖点击控制器侧面启动，消耗风 50、火 20、秩序 20、水 10；顶部转化为两格高尖塔。向尖塔或控制器供应风厘魔力，并从侧面管道输入能量源质。再次用法杖点击侧面，可轮换该面的能量、风、飞行吸力。风源质扩大范围，飞行源质提高速率；每单位强化持续 1200 个供能刻。"))
insert_page("NETHERMIND",5,authored_page("automagy.page.nethermind_structure",
    "Structure (N = Nethermind, R = rune, . = air):<BR>...R...<BR>...R...<BR>..RRR..<BR>RRRNRRR<BR>..RRR..<BR>...R...<BR>...R...<BR>Add one rune immediately below the end of each of the four arms. There are 20 runes in total. Keep the space underneath clear. Start with a wand holding 75 Ignis and 25 Ordo; supply Ignis centivis through a relay above the controller.",
    "结构俯视图（N 为下界之脑，R 为符文，点为空气）：<BR>...R...<BR>...R...<BR>..RRR..<BR>RRRNRRR<BR>..RRR..<BR>...R...<BR>...R...<BR>四条臂的末端各向下添加一个符文，共需 20 个符文。保持下方空间畅通。用含 75 火与 25 秩序魔力的法杖启动，并从控制器上方供应火厘魔力。"))
insert_page("DIMENSIONLURE",4,authored_page("automagy.page.dimension_lure_structure",
    "Place the lure directly 6–10 blocks below the Nethermind (8–10 for ghasts). Keep a 5 by 5 shaft clear between them. At the lure's level, build this ring of BASIC runed obsidian (L = lure):<BR>..RRR..<BR>.R...R.<BR>R.....R<BR>R..L..R<BR>R.....R<BR>.R...R.<BR>..RRR..<BR>Activate the lure before the Nethermind. Do not mix block and creature runes; use at least four creature runes.",
    "将诱饵放在下界之脑正下方 6–10 格处；召唤恶魂需要 8–10 格。保持中间 5×5 的竖井畅通。与诱饵同高处，用基础符文黑曜石搭建下图圆环（L 为诱饵）：<BR>..RRR..<BR>.R...R.<BR>R.....R<BR>R..L..R<BR>R.....R<BR>.R...R.<BR>..RRR..<BR>先启动诱饵，再启动下界之脑。至少使用四个生物符文，不能混用方块符文与生物符文。"))
# The original stabilization illustration is read from the player's reference jar.
external("textures/gui/dimensional-lure.png","textures/gui/dimensional-lure.png")
for key,index,en,zh in [
("NETHERMIND",4,"Rock runes increase the chance of netherrack. Four output runes exclude basic random output; more copies increase that output's weight. Cost per netherrack: Ignis 1, Terra 2 (basic runes: Ignis 2, Terra 4).","岩石符文增加地狱岩的生成概率；四个指定产出的符文可排除基础随机产出，同种符文越多则对应权重越高。每次消耗：火 1、地 2；基础符文生成地狱岩消耗火 2、地 4。"),
("NETHERMIND",5,"Sea runes conjure lava. Cost per conjuration: Ignis 6, Terra 2. Four output runes exclude basic random output.","海洋符文生成熔岩，每次消耗火 6、地 2。四个指定产出的符文可排除基础随机产出。"),
("NETHERRUNE_CRYSTAL",1,"Conjures Nether quartz ore. Cost: Vitreus 3, Terra 1.","生成下界石英矿石。每次消耗：水晶 3、地 1。"),
("NETHERRUNE_LIGHT",1,"Conjures glowstone. Cost: Lux 10, Sensus 3.","生成荧石。每次消耗：光 10、感知 3。"),
("NETHERRUNE_SOUL",1,"Conjures soul sand. Cost: Terra 1, Vinculum 1, Spiritus 1.","生成灵魂沙。每次消耗：地 1、束缚 1、灵魂 1。"),
]:
    authored_page(f"tc.research_page.automagy:{key}.{index}",en,zh)
for key,index,cost,uses,bait,extra in [
("DIMENSIONLURE",5,"Ignis 4, Praecantatio 2",10,"Fames 10, Victus 5","A second blaze sometimes appears at no additional essentia cost, but raises instability."),
("NETHERRUNE_ZOMBIE",1,"Corpus 2, Ignis 2",15,"Fames 15, Cognitio 9, Lucrum 3","Conjures zombified piglins."),
("NETHERRUNE_GHAST",2,"Exanimis 1, Spiritus 2",6,"Fames 6, Permutatio 1, Spiritus 4, Aqua 4","High instability; requires a gap of at least eight blocks."),
("NETHERRUNE_SKELETON",1,"Mortuus 1, Corpus 1",20,"Fames 20, Telum 5, Sensus 5","Usually conjures a wither skeleton, occasionally a regular skeleton."),
("NETHERRUNE_SLIME",1,"Limus 1, Ignis 2",8,"Fames 16, Limus 1","Conjures magma cubes."),
("NETHERRUNE_BAT",1,"Bestia 1, Ignis 1, Perditio 1",10,"Fames 10, Humanus 1","Conjures fire bats."),
("NETHERRUNE_WISP",2,"Auram 1, Ignis 2, Aer 1",8,"Fames 8, Auram 3","Very high instability."),
]:
    authored_page(f"tc.research_page.automagy:{key}.{index}",f"Nethermind cost per conjuration: {cost}.<BR>Lure cost per {uses} conjurations: {bait}.<BR>{extra}",f"下界之脑每次召唤消耗：{cost}。<BR>维度诱饵每 {uses} 次召唤消耗：{bait}。<BR>以上为默认消耗；要素使用拉丁标签标识。")

# Experience equipment. Brain balancing needs the main mod's quantitative XP API.
for name,old,texture in [("amnesiac_stone","blockAmnesiac","blockAmnesiac"),("experience_stone","blockXPStone","blockSoulStone")]:
    external(f"textures/block/{name}.png",f"textures/blocks/{texture}.png")
    lang(f"block.automagy.{name}",f"tile.{old}.name")
    write(f"assets/automagy/models/block/{name}.json",{"parent":"minecraft:block/cube_all","textures":{"all":f"automagy:block/{name}"}})
    write(f"assets/automagy/models/item/{name}.json",{"parent":f"automagy:block/{name}"})
    write(f"assets/automagy/blockstates/{name}.json",{"variants":{"":{"model":f"automagy:block/{name}"}}})
    write(f"data/automagy/loot_tables/blocks/{name}.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":f"automagy:{name}"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
external("textures/block/experience_stone_overlay.png","textures/blocks/blockSoulStoneOverlay.png")
external("textures/item/experience_phial.png","textures/items/phialExtra.png")
lang("item.automagy.experience_phial","item.phialExtra.name")
lang("block.automagy.experience_jar","tile.blockXPJar.name")
EN["tooltip.automagy.experience_jar"]="Experience: %s levels (%s points)"
ZH["tooltip.automagy.experience_jar"]="经验：%s 级（%s 点）"
EN["tooltip.automagy.experience_phial"]="Contains %s experience points"
ZH["tooltip.automagy.experience_phial"]="含有 %s 点经验"
write("assets/automagy/models/item/experience_phial.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/experience_phial"}})
write("assets/automagy/models/block/experience_jar.json",{"parent":"thaumcraft:block/essentia_jar"})
write("assets/automagy/models/item/experience_jar.json",{"parent":"builtin/entity","gui_light":"front"})
write("assets/automagy/blockstates/experience_jar.json",{"variants":{"":{"model":"automagy:block/experience_jar"}}})
write("data/automagy/loot_tables/blocks/experience_jar.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:experience_jar","functions":[{"function":"minecraft:copy_nbt","source":"block_entity","ops":[{"source":"XP","target":"BlockEntityTag.XP","op":"replace"}]}]}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
recipe("amnesiac_stone",{"type":"thaumcraft:arcane","research":"automagy:AMNESIASTONE","vis":{"perditio":20,"terra":4,"aqua":4},"pattern":["SES","SBS"],"key":{"S":item("thaumcraft:arcane_stone_bricks"),"E":item("thaumcraft:entropy_shard"),"B":item("entropy_crystal_brain")},"result":item("amnesiac_stone")})
recipe("experience_stone",{"type":"thaumcraft:arcane","research":"automagy:XPSTONE","vis":{"perditio":60,"terra":30,"ordo":10},"pattern":["ESE","ABA","ESE"],"key":{"E":item("thaumcraft:entropy_shard"),"S":item("minecraft:soul_sand"),"A":item("thaumcraft:arcane_stone"),"B":item("thaumcraft:zombie_brain")},"result":item("experience_stone")})
research("AMNESIASTONE","amnesiac_stone",5,-7,[text_page("AMNESIASTONE",1),recipe_page("amnesiac_stone"),text_page("AMNESIASTONE",2)],{"cognitio":5,"vacuos":5},["GOLEMINHIBITOR","JARBRAIN"],secondary=True)
RESEARCH[-1]["parents"]=["automagy:GOLEMINHIBITOR","JARBRAIN"]
RESEARCH[-1].update(complexity=1,parentsHidden=["automagy:CRYSTALBRAIN"],isConcealed=True)
research("XPSTONE","experience_stone",4,-7,[text_page("XPSTONE",1),recipe_page("experience_stone")]+[text_page("XPSTONE",i) for i in [2,3,4]],{"cognitio":2,"terra":2,"perditio":2,"permutatio":2},["AMNESIASTONE"])
RESEARCH[-1].update(complexity=1,warp=1)
ASPECT_ENTRIES.append({"items":["automagy:experience_phial"],"aspects":aspects({"cognitio":30})})

# Placeable soup fluids and their buckets. Binary art remains external.
for name,old in [("mushroom","Mushroom"),("vishroom","Vishroom")]:
    external(f"textures/block/{name}_fluid.png",f"textures/blocks/blockFluid{old}.png")
    external(f"textures/block/{name}_fluid.png.mcmeta",f"textures/blocks/blockFluid{old}.png.mcmeta")
    external(f"textures/item/{name}_bucket.png",f"textures/items/bucket{old}.png")
    lang(f"item.automagy.{name}_bucket",f"item.bucket{old}.name")
    lang(f"fluid.automagy.{name}",f"tile.blockFluid{old}.name")
    write(f"assets/automagy/models/item/{name}_bucket.json",{"parent":"minecraft:item/generated","textures":{"layer0":f"automagy:item/{name}_bucket"}})
    write(f"assets/automagy/models/block/{name}_fluid.json",{"textures":{"particle":f"automagy:block/{name}_fluid"},"elements":[]})
    write(f"assets/automagy/blockstates/{name}_fluid.json",{"variants":{"":{"model":f"automagy:block/{name}_fluid"}}})
    write(f"data/automagy/tags/fluids/{name}.json",{"replace":False,"values":[f"automagy:{name}",f"automagy:flowing_{name}"]})
    values={"fames":3,"herba":1,"tenebrae":1,"aqua":1,"metallum":8}
    if name=="vishroom":values.update(praecantatio=2,venenum=1)
    else:values.update(vacuos=1)
    ASPECT_ENTRIES.append({"items":[f"automagy:{name}_bucket"],"aspects":aspects(values)})
recipe("mushroom_stew_bucket",{"type":"minecraft:crafting_shapeless","ingredients":[item("mushroom_bucket"),item("minecraft:bowl")],"result":item("minecraft:mushroom_stew")})

# Liquid distillation uses external Automagy geometry and textures.
external("models/external/boiler.obj","textures/models/boiler.obj")
for suffix in ["","-lit","-filled","-lit-filled"]:external(f"textures/models/boiler{suffix}.png",f"textures/models/boiler{suffix}.png")
external("textures/gui/boiler.png","textures/gui/boiler.png")
lang("block.automagy.boiler","tile.blockBoiler.name")
write("assets/automagy/models/block/boiler.json", {"textures":{"particle":"minecraft:block/iron_block"},"elements":[]})
write("assets/automagy/models/item/boiler.json", {"parent":"builtin/entity","gui_light":"front"})
write("assets/automagy/blockstates/boiler.json", {"variants":{"":{"model":"automagy:block/boiler"}}})
write("data/automagy/loot_tables/blocks/boiler.json", {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:boiler"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
recipe("boiler", {"type":"thaumcraft:arcane","research":"automagy:ALCHEMYBOILER","vis":{"aqua":7,"ignis":5},"pattern":["ICI","IFI","III"],"key":{"I":item("minecraft:iron_ingot"),"C":item("thaumcraft:crucible"),"F":item("minecraft:furnace")},"result":item("boiler")})
research("ALCHEMYBOILER","boiler",-2,11,[text_page("ALCHEMYBOILER",1),recipe_page("boiler")],{"praecantatio":2,"aqua":5,"limus":2,"ignis":3},[])
RESEARCH[-1].update(parentsHidden=["DISTILESSENTIA"],complexity=2)

write("data/automagy/item_aspects/content.json", {"entries":ASPECT_ENTRIES})
for tool_tag in ["mineable/pickaxe","needs_iron_tool"]:
    path=f"data/minecraft/tags/blocks/{tool_tag}.json"
    tag=json.loads((ROOT/path).read_text(encoding="utf-8"))
    tag["values"]=list(dict.fromkeys(tag["values"]+["automagy:thaumostatic_"+part for part in ["pylon","controller","spire"]]))
    write(path,tag)
external("textures/item/thinking_cap.png","textures/items/thinkingCap.png")
lang("item.automagy.thinking_cap","item.creativeTool.0.name")
lang("tooltip.automagy.creative_only","Automagy.tip.creativeModeOnly")
lang("tooltip.automagy.thinking_cap","Automagy.tip.creativeTool.0")
write("assets/automagy/models/item/thinking_cap.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/thinking_cap"}})
write("data/automagy/recipes/advanced_golem.json",{"type":"automagy:advanced_golem","category":"misc"})
# Creative-only infinity jar. The base shell is reused, replacing the two legacy infinity textures.
for face in ["top","side"]:external(f"textures/block/jar_{face}_infinity.png",f"textures/blocks/jar_{face}_infinity.png")
lang("block.automagy.infinity_jar","tile.blockCreativeJar.3.name")
write("assets/automagy/models/block/infinity_jar.json",{"parent":"thaumcraft:block/essentia_jar","textures":{"top":"automagy:block/jar_top_infinity","side":"automagy:block/jar_side_infinity","particle":"automagy:block/jar_side_infinity"}})
write("assets/automagy/models/item/infinity_jar.json",{"parent":"automagy:block/infinity_jar"})
write("assets/automagy/blockstates/infinity_jar.json",{"variants":{"":{"model":"automagy:block/infinity_jar"}}})
write("data/automagy/loot_tables/blocks/infinity_jar.json",{"type":"minecraft:block","pools":[]})

# Ender-disjunction rings, equipped through the base mod's Curios ring slots.
for name,texture,meta in [("ender_disjunction_ring","baubleEnderDisjunction",0),("lesser_ender_disjunction_ring","baubleEnderDisjunctionLesser",1)]:
    external(f"textures/item/{name}.png",f"textures/items/{texture}.png")
    write(f"assets/automagy/models/item/{name}.json",{"parent":"minecraft:item/generated","textures":{"layer0":f"automagy:item/{name}"}})
    lang(f"item.automagy.{name}",f"item.baubleEnderDisjunction.{meta}.name")
write("data/curios/tags/items/ring.json",{"replace":False,"values":["automagy:ender_disjunction_ring","automagy:lesser_ender_disjunction_ring"]})
lang("tooltip.automagy.disjunction_lesser","Automagy.tip.enderDisjunction")
recipe("ender_disjunction_ring",{"type":"thaumcraft:infusion","research":"automagy:ENDERDISJUNCTION","instability":4,"aspects":{"vinculum":10,"perditio":15,"alienis":2,"telum":4},"center":{"ingredient":item("thaumcraft:mundane_ring")},"components":[{"ingredient":item(i)} for i in ["avarice_pearl","minecraft:string","thaumcraft:amber","minecraft:tripwire_hook","minecraft:string","minecraft:cobweb"]],"result":item("ender_disjunction_ring")})
research("ENDERDISJUNCTION","ender_disjunction_ring",2,11,[text_page("ENDERDISJUNCTION",1),recipe_page("ender_disjunction_ring")],{"perditio":4,"vinculum":5,"alienis":3},["AVARICIOUS"])
RESEARCH[-1].update(complexity=2,isConcealed=True,parentsHidden=["automagy:GREEDYCHEST"])
for name,values in [("ender_disjunction_ring",{"vinculum":10,"perditio":10,"alienis":6,"lucrum":5,"telum":4,"iter":4,"vitreus":2,"praecantatio":2,"sensus":1}),("lesser_ender_disjunction_ring",{"vinculum":5,"perditio":5,"alienis":3,"lucrum":2,"telum":2,"iter":2,"vitreus":1,"praecantatio":1,"sensus":1})]:
    ASPECT_ENTRIES.append({"items":["automagy:"+name],"aspects":aspects(values)})

# Natural Nether spires, one configurable attempt per chunk, new terrain only.
write("data/automagy/worldgen/configured_feature/nether_spire.json",{"type":"automagy:nether_spire","config":{}})
write("data/automagy/worldgen/placed_feature/nether_spire.json",{"feature":"automagy:nether_spire","placement":[]})
write("data/automagy/forge/biome_modifier/nether_spire.json",{"type":"forge:add_features","biomes":"#minecraft:is_nether","features":"automagy:nether_spire","step":"surface_structures"})
def loot_item(name,minimum=1,maximum=None):
    entry={"type":"minecraft:item","name":item(name)["item"]}
    maximum=minimum if maximum is None else maximum
    if minimum!=1 or maximum!=1:entry["functions"]=[{"function":"minecraft:set_count","count":minimum if minimum==maximum else {"type":"minecraft:uniform","min":minimum,"max":maximum}}]
    return entry
bonus_tables={
    "pearls":(40,[loot_item("avarice_pearl",2,3)]),
    "paper":(15,[loot_item("enchanted_paper",8,11),loot_item("enchanted_tome",1,2)]),
    "tomes":(15,[loot_item("enchanted_tome",3,6),loot_item("enchanted_paper",2,4)]),
    "slivers":(20,[loot_item("fire_sliver",5,10)]),
    "amalgam":(10,[loot_item("mercatus_amalgam"),loot_item("avarice_pearl")]),
}
for name,(weight,entries) in bonus_tables.items():
    write(f"data/automagy/loot_tables/chests/nether_spire_{name}.json",{"type":"minecraft:chest","pools":[{"rolls":1,"entries":[entry]} for entry in entries]})
write("data/automagy/loot_tables/chests/nether_spire.json",{"type":"minecraft:chest","pools":[
    {"rolls":2,"entries":[{"type":"minecraft:loot_table","name":"minecraft:chests/simple_dungeon"}]},
    {"rolls":1,"entries":[{"type":"minecraft:loot_table","name":f"automagy:chests/nether_spire_{name}","weight":weight} for name,(weight,_) in bonus_tables.items()]}
]})

# Explicit scanning identities for the standalone Nether spirit and machinery.
write("data/automagy/entity_aspects/nether_wisp.json",{"format":1,"entries":[{"entity":"automagy:nether_wisp","legacy_name":"Automagy.WispNether","variant":"any","legacy_qualifier":"Typeignis","aspects":aspects({"ignis":3,"praecantatio":1,"aer":1})}]})
for name,values in {
    "nethermind":{"ignis":10,"cognitio":10,"iter":10,"alienis":9,"praecantatio":9,"tenebrae":8,"lucrum":5,"terra":4,"permutatio":3},
    "mob_lure":{"ignis":10,"cognitio":10,"fames":10,"tenebrae":8,"vitreus":9,"lucrum":3,"sensus":3,"alienis":2,"vinculum":2,"perditio":2,"praecantatio":2},
    "amnesiac_stone":{},
    "experience_stone":{"perditio":13,"terra":8,"praecantatio":4,"cognitio":4,"exanimis":2,"spiritus":2},
}.items():
    ASPECT_ENTRIES.append({"items":["automagy:"+name],"aspects":aspects(values)})
write("data/automagy/item_aspects/content.json", {"entries":ASPECT_ENTRIES})
# Restored by the base's public dynamic arcane-result transform.
for name,source,target,dye,cost in [("filter_to_white","black_filter","white_filter","white","ordo"),("filter_to_black","white_filter","black_filter","black","perditio")]:
    recipe(name,{"type":"thaumcraft:arcane","mode":"shapeless","research":"automagy:ENCHANTEDPAPER","vis":{cost:2},"ingredients":[item(source),{"tag":"forge:dyes/"+dye}],"result":item(target),"result_transform":{"type":"automagy:preserve_filter"}})
for name,a,b in [("white","white_filter","white_filter"),("black","black_filter","black_filter"),("mixed","white_filter","black_filter")]:
    recipe("filter_copy_"+name,{"type":"thaumcraft:arcane","mode":"shapeless","research":"automagy:ENCHANTEDPAPER","vis":{"ordo":1},"ingredients":[item(a),item(b)],"result":item(a),"result_transform":{"type":"automagy:preserve_filter","copy":True}})
# Mirror delivery now uses the base mod's public output queue API.
external("textures/item/conflux_disc.png","textures/items/confluxDisc.png")
write("assets/automagy/models/item/conflux_disc.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/conflux_disc"}})
EN["item.automagy.conflux_disc"]="Conflux Disc"
ZH["item.automagy.conflux_disc"]="汇流圆盘"
for name,legacy,en,zh in [("melding_mirror","mirrorInputFrame","Melding Mirror","融合魔镜"),("multi_dest_mirror","mirrorMultiDestFrame","Multi-Destination Mirror","多目的地魔镜")]:
    EN["block.automagy."+name]=en;ZH["block.automagy."+name]=zh
    variants={}
    for socketed in [False,True]:
        suffix="_socketed" if socketed else ""
        external(f"textures/block/{name}{suffix}.png",f"textures/blocks/{legacy}"+("-socketed" if socketed and name=="melding_mirror" else "")+".png")
        model={"parent":"minecraft:block/block","render_type":"minecraft:cutout","textures":{"particle":f"automagy:block/{name}{suffix}","frame":f"automagy:block/{name}{suffix}","back":"minecraft:block/obsidian"},"elements":[
            {"from":[1,0,1],"to":[15,1.5,15],"faces":{face:{"texture":"#back"} for face in ["down","north","south","east","west"]}},
            {"from":[1,1.5,1],"to":[15,1.5,15],"faces":{"up":{"texture":"#frame","uv":[0,0,16,16]}}}]}
        write(f"assets/automagy/models/block/{name}{suffix}.json",model)
        for facing,x,y in [("up",0,0),("down",180,0),("north",90,0),("south",90,180),("east",90,90),("west",90,270)]:
            variants[f"facing={facing},socketed={str(socketed).lower()}"]={"model":f"automagy:block/{name}{suffix}","x":x,"y":y}
    write(f"assets/automagy/blockstates/{name}.json",{"variants":variants})
    write(f"assets/automagy/models/item/{name}.json",{"parent":f"automagy:block/{name}"})
    write(f"data/automagy/loot_tables/blocks/{name}.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"automagy:"+name}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
path="data/minecraft/tags/blocks/mineable/pickaxe.json"
mining=json.loads((ROOT/path).read_text(encoding="utf-8"));mining["values"] += ["automagy:melding_mirror","automagy:multi_dest_mirror"];write(path,mining)
recipe("conflux_disc",{"type":"thaumcraft:infusion","research":"automagy:MIRRORINPUT","instability":8,"aspects":{"iter":8,"tenebrae":8,"permutatio":8},"center":{"ingredient":item("minecraft:compass")},"components":[{"ingredient":item(i)} for i in ["thaumcraft:quicksilver","minecraft:ender_pearl","thaumcraft:void_ingot"]],"result":{**item("conflux_disc"),"count":2}})
recipe("duplicate_conflux_disc",{"type":"thaumcraft:crucible","research":"automagy:MIRRORINPUT","catalyst":item("conflux_disc"),"aspects":{"iter":2,"tenebrae":2,"permutatio":2,"alienis":2,"metallum":8},"result":{**item("conflux_disc"),"count":2}})
recipe("melding_mirror",{"type":"thaumcraft:arcane","research":"automagy:MIRRORINPUT","vis":{"aer":8,"perditio":8,"terra":4},"pattern":[" g ","gGg"," C "],"key":{"g":item("minecraft:gold_nugget"),"G":item("thaumcraft:mirrored_glass"),"C":item("conflux_disc")},"result":item("melding_mirror")})
recipe("multi_dest_mirror",{"type":"thaumcraft:infusion","research":"automagy:MIRRORMULTIDEST","instability":5,"aspects":{"cognitio":32,"lucrum":8,"ordo":16},"center":{"ingredient":item("melding_mirror")},"components":[{"ingredient":item(i)} for i in ["minecraft:iron_bars","enchanted_tome","minecraft:map"]],"result":item("multi_dest_mirror")})
for name,metal,travel,mind in [("conflux_disc",4,2,0),("melding_mirror",6,3,0),("multi_dest_mirror",7,3,11)]:
    values={"iter":travel,"tenebrae":2,"permutatio":2,"alienis":2,"metallum":metal}
    if mind:values["cognitio"]=mind
    ASPECT_ENTRIES.append({"items":["automagy:"+name],"aspects":aspects(values)})
write("data/automagy/item_aspects/content.json",{"entries":ASPECT_ENTRIES})
research("MIRRORINPUT","melding_mirror",9,8,[text_page("MIRRORINPUT",1),recipe_page("melding_mirror"),text_page("MIRRORINPUT",2),text_page("MIRRORINPUT",3),recipe_page("conflux_disc"),recipe_page("duplicate_conflux_disc"),{"type":"TEXT","aspects":[],"text":"guide.automagy.mirrors"}],{"iter":4,"alienis":2,"tenebrae":2,"sensus":2})
RESEARCH[-1].update(complexity=2,isConcealed=True,parentsHidden=["MIRROR","VOIDMETAL","automagy:REMOTECOMPARATOR"])
research("MIRRORMULTIDEST","multi_dest_mirror",10,6,[text_page("MIRRORMULTIDEST",1),recipe_page("multi_dest_mirror"),text_page("MIRRORMULTIDEST",2),text_page("MIRRORMULTIDEST",3),{"type":"TEXT","aspects":[],"text":"guide.automagy.mirrors"}],{"iter":2,"alienis":2,"tenebrae":2,"cognitio":2},["MIRRORINPUT","INVENTARIUM_DELIVERY"],secondary=True)
EN["guide.automagy.mirrors"]="Bind a Crystal Eye to a paired Thaumcraft mirror, then insert it into a Melding Mirror to send incoming items to that selected mirror's output queue. Shift-use a wand to remove the eye. To bind an eye to a Melding Mirror itself, sneak while using the eye. Link a Multi-Destination Mirror to an Inventarium with a wand; it forwards only requested items to the requesting tome's output mirror. On a Requisition Tome, insert a bound eye normally for input, or while sneaking for output. Both selected mirrors must be within 5 blocks. Use the tome with an item to deposit through the input mirror; empty-hand opens requests. Shift-use a wand to remove both eyes. Endpoints must be loaded; unaccepted items stay with the sender."
ZH["guide.automagy.mirrors"]="将水晶眼绑定到已配对的本体魔镜，再装入融合魔镜，输入物品会进入指定魔镜的输出队列。潜行使用法杖可取出水晶眼；潜行使用水晶眼可将眼绑定到融合魔镜本身。用法杖将多目的地魔镜连接至存储核心，它只接收已有请求的物品，并送往请求之书指定的输出魔镜。对请求之书正常使用已绑定的水晶眼设置输入镜，潜行使用设置输出镜；两者均须位于书周围5格内。手持物品使用书可通过输入镜存入，空手打开请求界面。潜行使用法杖取回两只眼。目标必须已加载；未接收物品留在发送端。"
EN["message.automagy.mirror.input"]="Input mirror installed."
ZH["message.automagy.mirror.input"]="已安装输入镜水晶眼。"
EN["message.automagy.mirror.output"]="Output mirror installed."
ZH["message.automagy.mirror.output"]="已安装输出镜水晶眼。"
EN["guide.automagy.requisition.delivery"] += " Multi-Destination Mirrors now route requested items to each tome's selected output mirror; see Melding Mirrors for eye configuration."
ZH["guide.automagy.requisition.delivery"] += " 多目的地魔镜现可按请求分发至各本书指定的输出镜；水晶眼设置见融合魔镜研究。"
for entry in PAGES:
    if entry["key"]=="automagy:ENCHANTEDPAPER":entry["pages"] += [recipe_page("filter_to_white"),recipe_page("filter_to_black")]
for entry in RESEARCH:
    if entry["key"]=="automagy:ENCHANTEDPAPER":entry["pageCount"]+=2

# Crafting focus: upgrades are named startup definitions, not numeric enum extensions.
for name,legacy in [("crafting_focus","focusCrafting"),("crafting_focus_depth","focusCrafting-depth"),("crafting_focus_ornament","focus_orn_transposing")]:
    external(f"textures/item/{name}.png",f"textures/items/{legacy}.png")
write("assets/automagy/models/item/crafting_focus.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/crafting_focus"}})
write("assets/automagy/models/item/crafting_focus_ornament_renderer.json",{"parent":"minecraft:builtin/entity"})
write("assets/automagy/models/item/crafting_focus_ornament.json",{"parent":"minecraft:item/generated","textures":{"layer0":"automagy:item/crafting_focus_ornament"}})
write("assets/minecraft/atlases/blocks.json",{"sources":[{"type":"minecraft:single","resource":"automagy:item/crafting_focus_depth","sprite":"automagy:item/crafting_focus_depth"}]})
external("textures/gui/portable_crafting.png","textures/gui/focusCrafting.png")
lang("item.automagy.crafting_focus","item.focusCrafting.name")
upgrades=[]
for key,tags in [("gratis",["fames","praecantatio"]),("storage",["alienis"]),("transposing",["permutatio","instrumentum"])]:
    external(f"textures/foci/{key}.png",f"textures/foci/{key}.png")
    name=f"focus.upgrade.automagy.{key}.name";description=f"focus.upgrade.automagy.{key}.text"
    lang(name,f"Automagy.focus.upgrade.{key}.name");lang(description,f"Automagy.focus.upgrade.{key}.text")
    upgrades.append({"key":"automagy:"+key,"name":name,"description":description,"texture":f"automagy:textures/foci/{key}.png","aspects":tags})
write("data/automagy/thaumcraft/focus_upgrades.json",upgrades)
recipe("crafting_focus",{"type":"thaumcraft:arcane","research":"automagy:FOCUSCRAFTING","vis":{"terra":10,"ordo":30,"perditio":5},"pattern":["OQE","QTQ","BQO"],"key":{"O":item("thaumcraft:order_shard"),"Q":item("minecraft:quartz"),"E":item("thaumcraft:earth_shard"),"B":item("thaumcraft:balanced_shard"),"T":item("thaumcraft:arcane_workbench")},"result":item("crafting_focus")})
EN["guide.automagy.focus.open"]="Attach the Crafting focus to a wand and use it to open a portable 3x3 workbench. Opening costs 7 Ordo vis. Arcane recipes cost their normal vis plus another 7 Ordo; ordinary recipes have no extra charge. Gratis removes the opening and additional costs. Storage keeps unused ingredients in the focus. C opens the focus; Transposing also finds it in your inventory or focus pouch and exchanges the attached focus. Staffs support ordinary recipes only."
ZH["guide.automagy.focus.open"]="将合成核心安装到法杖后使用，可打开便携3×3工作台。打开消耗7点秩序vis；奥术配方另收7点秩序vis，普通配方不额外收费。免费升级免除打开和附加费用；储存升级保存未用完的材料。按C打开；换位升级还可从背包或核心袋中找到合成核心并与当前核心交换。长杖仅支持普通合成。"
research("FOCUSCRAFTING","crafting_focus",-4,7,[{"type":"TEXT","aspects":[],"text":"guide.automagy.focus.open"},recipe_page("crafting_focus"),text_page("FOCUSCRAFTING",2)],{"fabrico":5,"praecantatio":5,"instrumentum":2},[])
RESEARCH[-1]["parentsHidden"]=["FOCUSFIRE"]
research("FOCUSCRAFTING_UPGRADE","crafting_focus",-5,8,[text_page("FOCUSCRAFTING_UPGRADE",1),text_page("FOCUSCRAFTING_UPGRADE",2)],{"fabrico":3,"vacuos":3,"alienis":3,"permutatio":3},["FOCUSCRAFTING"],secondary=True)
RESEARCH[-1].update(complexity=2,isConcealed=True,parentsHidden=["FOCALMANIPULATION"])
ASPECT_ENTRIES.append({"items":["automagy:crafting_focus"],"aspects":aspects({"fabrico":5,"praecantatio":4,"permutatio":2,"vacuos":1,"vitreus":2,"instrumentum":1})})
write("data/automagy/item_aspects/content.json",{"entries":ASPECT_ENTRIES})
write("data/automagy/research_catalog/automagy.json", {"format":1,"categories":[{"key":CATEGORY,
      "icon":"automagy:textures/gui/research_icon.png","background":"thaumcraft:textures/gui/gui_researchback.png","research":RESEARCH}]})
write("data/automagy/research_pages/automagy.json", {"format":1,"entries":PAGES})
write("data/automagy/research_page_recipes/automagy.json", {"format":1,"entries":DISPLAYS})
for key in ["visLow","staff.1","staff.2"]:
    lang("gui.automagy.crafting."+key,"Automagy.gui.focusCrafting."+key)
for key,en,zh in [
 ("fluid_empty","Empty / %s mB","空 / %s mB"),
 ("fluid","%s / %s mB %s","%s / %s mB %s"),
 ("timer","Timer: %s s","定时：%s秒"),("remaining","Remaining: %s s","剩余：%s秒"),
 ("power","Signal: %s (+%s)","信号：%s（+%s）"),("cap","Signal cap: %s","信号上限：%s"),("minimum","Minimum signal: %s","最低信号：%s"),
 ("omit","Affects unlinked golems","影响未连接的傀儡"),("exclusive","Affects linked golems only","仅影响已连接的傀儡"),
 ("experience","XP: %s (level %s)","经验：%s（%s级）"),("link","Linked to %s [%s, %s, %s]","连接至%s［%s，%s，%s］"),
 ("delivery","Inventarium delivery in progress","正在执行物品目录运输任务")]:
    EN["jade.automagy."+key]=en;ZH["jade.automagy."+key]=zh
for key,en,zh in [("machines","Automagy machine details","自动化魔法设备详情"),("golem_link","Automagy golem links","自动化魔法傀儡连接"),("experience_stone","Experience stone contents","经验石储量"),("brain_jar","Brain jar experience","缸中之脑经验")]:
    EN["config.jade.plugin_automagy."+key]=en;ZH["config.jade.plugin_automagy."+key]=zh
EN["jei.automagy.arcane"]="Arcane Crafting"
ZH["jei.automagy.arcane"]="奥术合成"
EN["jei.automagy.research_locked"]="Research required"
ZH["jei.automagy.research_locked"]="尚未完成所需研究"
EN["jei.automagy.staff"]="Staffs cannot perform arcane crafting."
ZH["jei.automagy.staff"]="长杖不能进行奥术合成。"
EN["jei.automagy.focus_fee"]="Base vis cost; the crafting focus also charges 7 Ordo unless upgraded with Gratis. Wand discounts apply."
ZH["jei.automagy.focus_fee"]="显示基础vis费用；合成核心额外收取7点秩序，免费升级可免除附加费。实际费用受法杖减耗影响。"
for name in ["mob_lure","dimension_lure"]:
    write(f"assets/automagy/models/item/{name}.json",{"parent":"builtin/entity","textures":{"particle":f"automagy:block/{name}"},"gui_light":"side","display":{"gui":{"rotation":[30,225,0],"scale":[.625,.625,.625]},"ground":{"translation":[0,3,0],"scale":[.25,.25,.25]},"fixed":{"scale":[.5,.5,.5]},"thirdperson_righthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.375,.375,.375]},"thirdperson_lefthand":{"rotation":[75,45,0],"translation":[0,2.5,0],"scale":[.375,.375,.375]},"firstperson_righthand":{"rotation":[0,45,0],"scale":[.4,.4,.4]},"firstperson_lefthand":{"rotation":[0,225,0],"scale":[.4,.4,.4]}}})
EN.update({"command.automagy.scan.empty":"The target player is not holding an item.","command.automagy.scan.missing":"No matching item scan record exists.","command.automagy.scan.conflict":"Knowledge changed; retry the command.","command.automagy.scan.done":"Reset %2$s item scan records for %1$s."})
ZH.update({"command.automagy.scan.empty":"目标玩家主手没有物品。","command.automagy.scan.missing":"没有对应的物品扫描记录。","command.automagy.scan.conflict":"知识记录已变更，请重新执行。","command.automagy.scan.done":"已清除 %1$s 的 %2$s 条物品扫描记录。"})
EN["key.automagy.craft"]="Wand Focus: Crafting"
ZH["key.automagy.craft"]="法杖核心：合成"
EN["key.categories.automagy"]="Automagy"
ZH["key.categories.automagy"]="自动化魔法"
EN["tooltip.automagy.focus_stored"]="Stored ingredient slots: %s / 9"
ZH["tooltip.automagy.focus_stored"]="已储存材料槽：%s / 9"
EN["message.automagy.tower_suction"]="This side draws %s."
ZH["message.automagy.tower_suction"]="此面吸收 %s。"
for locale, values in [("en_us", EN), ("zh_cn", ZH)]:
    values["tc.research_category.AUTOMAGY"] = "Automagy" if locale == "en_us" else "自动化魔法"
    write(f"assets/automagy/lang/{locale}.json", values)
MAPPING["assets/automagy/textures/gui/widgets_recolor.png"] = "assets/automagy/textures/gui/widgetsRecolor.png"
write("data/automagy/thaumcraft/original_resources.json", {
    "name":"Automagy 1.7.10-0.28.2", "legacy_modid":"Automagy", "sha256":"4c1d78e0adee1754675d93ffbadc8c9fe205710cb77538b953e57810f9528efc",
    "download":"https://www.curseforge.com/minecraft/mc-mods/automagy/files/2285272", "mapping":MAPPING,
    "languages":{f"assets/automagy/lang/{locale}.json":LANG_MAP for locale in ["en_us","zh_cn"]}})

# Loonium's optional runtime hook applies the world setting; explicit datapack tags still win.
write("data/botania/tags/items/loonium_blacklist.json",{"replace":False,"values":[]})

# Legacy wood scribe uses axes; NiceStone maws use pickaxes but do not require
# a tool to drop (MaterialNiceStone deliberately does not call setRequiresTool).
for tool, names in [("axe", ["unseen_scribe"]), ("pickaxe", ["hungry_maw", "finical_maw", "requisition_tome"])]:
    path = f"data/minecraft/tags/blocks/mineable/{tool}.json"
    mining = json.loads((ROOT / path).read_text(encoding="utf-8"))
    mining["values"] = list(dict.fromkeys(mining["values"] + ["automagy:" + name for name in names]))
    write(path, mining)

from legacy_config_resources import generate as generate_legacy_config_resources
generate_legacy_config_resources()

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
