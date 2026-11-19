"""Merge the restored legacy configuration-dependent content without rewriting other resources."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"


def write(path, value):
    path = ROOT / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def generate():
    primals = ["air", "fire", "water", "earth", "order", "entropy"]
    for primal in primals:
        write(f"data/automagy/recipes/refining/{primal}.json", {
            "type": "automagy:entropic_refining", "research": "automagy:ENTROPICREFINING",
            "catalyst": {"tag": f"forge:ores/infused_{primal}"},
            "aspects": {"perditio": 3, "vitreus": 1},
            "result": {"item": f"automagy:{primal}_sliver", "count": 11}})
    catalog = json.loads((ROOT / "data/automagy/research_catalog/automagy.json").read_text("utf-8"))
    pages = json.loads((ROOT / "data/automagy/research_pages/automagy.json").read_text("utf-8"))
    rows = catalog["categories"][0]["research"]
    keys = {"automagy:ENTROPICREFINING", "automagy:ADVNODEJAR"}
    rows[:] = [row for row in rows if row["key"] not in keys]
    pages["entries"][:] = [row for row in pages["entries"] if row["key"] not in keys]
    entries = [
        ("ENTROPICREFINING", "automagy:air_sliver", -5, -2, 1,
         {"praecantatio": 1, "perditio": 3, "perfodio": 2}, ["ENTROPICPROCESSING"],
         [{"type": "TEXT", "text": "tc.research_page.automagy:ENTROPICREFINING.1"}] + [
             {"type": "CRUCIBLE_CRAFTING", "recipe": {"kind": "reference", "keys": [f"automagy:refining/{p}"]}}
             for p in primals]),
        ("ADVNODEJAR", "thaumcraft:node_jar", -7, 6, 3,
         {"auram": 7, "lucrum": 4, "ordo": 4, "iter": 4}, ["NODEJAR", "FOCUSWARDING", "WARDEDARCANA"],
         [{"type": "TEXT", "text": f"tc.research_page.automagy:ADVNODEJAR.{n}"} for n in (1, 2)])]
    for key, icon, col, row, complexity, aspects, parents, content in entries:
        entry = {"key": f"automagy:{key}", "category": "AUTOMAGY",
                 "icon_item": {"item": icon, "count": 1, "metadata": 0, "damageable": False},
                 "displayColumn": col, "displayRow": row, "complexity": complexity,
                 "pageCount": len(content), "parentsHidden": parents,
                 "aspects": [{"aspect": a, "amount": count} for a, count in aspects.items()]}
        if key == "ADVNODEJAR":
            entry.update(isSpecial=True, isConcealed=True)
        rows.append(entry)
        pages["entries"].append({"key": f"automagy:{key}", "pages": [{"aspects": [], **p} for p in content]})
    write("data/automagy/research_catalog/automagy.json", catalog)
    write("data/automagy/research_pages/automagy.json", pages)
    translations = {
        "en_us": {
            "tc.research_name.automagy:ENTROPICREFINING": "Entropic Refining",
            "tc.research_text.automagy:ENTROPICREFINING": "Refine infused stone into slivers",
            "tc.research_page.automagy:ENTROPICREFINING.1": "Dissolve infused stone in a crucible with three Perditio and one Vitreus to recover slivers of its primal aspect. The recipe display shows the yield configured for this world.",
            "tc.research_name.automagy:ADVNODEJAR": "Advanced Node Jarring",
            "tc.research_text.automagy:ADVNODEJAR": "Preserve a node's brightness",
            "tc.research_page.automagy:ADVNODEJAR.1": "Build a hollow 3 by 3 by 3 shell of warded glass around an aura node. Cover it with a 3 by 3 layer of warded silverwood logs. You must own all nine warded logs and all 26 warded glass blocks.",
            "tc.research_page.automagy:ADVNODEJAR.2": "Use a casting wand without an attached focus on the shell. The operation costs 125 of each primal vis before discounts and consumes the shell. The captured node retains its type, brightness, identity and available aspects. A denied or failed conversion leaves the structure intact."},
        "zh_cn": {
            "tc.research_name.automagy:ENTROPICREFINING": "熵解提炼",
            "tc.research_text.automagy:ENTROPICREFINING": "从蕴魔石中提炼晶屑",
            "tc.research_page.automagy:ENTROPICREFINING.1": "将蕴魔石放入含有 3 点混沌和 1 点水晶要素的坩埚，可以获得对应原始要素的晶屑。配方页显示本世界配置下的实际产量。",
            "tc.research_name.automagy:ADVNODEJAR": "高级节点装罐",
            "tc.research_text.automagy:ADVNODEJAR": "保留节点的亮度",
            "tc.research_page.automagy:ADVNODEJAR.1": "以灵气节点为中心，用守卫玻璃搭建 3×3×3 的空心外壳，再在顶部覆盖一层 3×3 的守卫银树原木。9 块原木和 26 块玻璃都必须由你认主。",
            "tc.research_page.automagy:ADVNODEJAR.2": "用没有安装核心的施法法杖右键外壳。装罐基础消耗为六种原始要素各 125 点 vis，法杖减耗正常生效，并消耗整套外壳。节点类型、亮度、标识和可用要素均会保留；权限拒绝或转换失败时结构保持完整。"}}
    for locale, values in translations.items():
        path = f"assets/automagy/lang/{locale}.json"
        lang = json.loads((ROOT / path).read_text("utf-8"))
        lang.update(values)
        write(path, lang)


if __name__ == "__main__":
    generate()

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
