"""Build a source index of every actually registered TC4 entity, not a parity score."""
from pathlib import Path
import argparse
import re

ROOT = Path(__file__).resolve().parents[1]
OLD = Path("references/deobfuscated_src/thaumcraft/common/config/ConfigEntities.java")
NEW = Path("src/main/java/tc4revival/content/TCEntities.java")
RENDERERS = Path("src/main/java/tc4revival/client/TCClientRegistration.java")
EXCEPTIONS = {
    "EntityDart": "GolemDartEntity", "EntityBottleTaint": "TaintBottleEntity",
    "EntityGolemBase": "GolemEntity", "EntityTaintacleSmall": "SmallTaintacleEntity",
    "EntityTaintacleGiant": "GiantTaintacleEntity", "EntityTaintSporeSwarmer": "TaintHiveEntity",
    "EntityItemGrate": "GrateItemEntity", "EntityTaintCreeper": "TaintedCreeperEntity",
}
ANIMALS = {"EntityTaint" + kind: "TAINTED_" + kind.upper() for kind in ("Chicken", "Cow", "Pig", "Sheep", "Villager")}


def read(path):
    return (ROOT / path).read_text(encoding="utf-8-sig")


def inventory():
    old = re.findall(r'registerModEntity\((\w+)\.class, \(String\)"([^"]+)"', read(OLD))
    new = re.findall(r'RegistryObject<EntityType<(\w+)>>\s+([A-Z][A-Z0-9_]+)\s*=', read(NEW))
    renderers = read(RENDERERS)
    rows, used = [], set()
    for legacy_class, legacy_name in old:
        modern = EXCEPTIONS.get(legacy_class, legacy_class.removeprefix("Entity") + "Entity")
        if legacy_class in ANIMALS:
            modern, field = "TaintedAnimalEntity", ANIMALS[legacy_class]
        else:
            candidates = [field for cls, field in new if cls == modern]
            if len(candidates) != 1:
                raise ValueError(f"Resolve explicit entity mapping: {legacy_class}: {candidates}")
            field = candidates[0]
        if (modern, field) not in new or field in used:
            raise ValueError(f"Missing or duplicate registration mapping: {legacy_class}: {field}")
        used.add(field)
        source = Path("src/main/java/tc4revival/content") / (modern + ".java")
        if not (ROOT / source).is_file():
            raise ValueError(f"Missing implementation source: {source}")
        if field.startswith("TAINTED_"):
            if "Kind.values())event.registerEntityRenderer(tc4revival.content.TCEntities.taintedAnimal(kind)" not in renderers:
                raise ValueError("Inspect changed tainted-animal renderer dispatch")
            renderer = "TaintedAnimalRenderer（Kind 分派）"
        else:
            match = re.search(r"registerEntityRenderer\(tc4revival\.content\.TCEntities\." + field + r"\.get\(\),\s*([\w.]+)::new\)", renderers)
            if not match:
                match = re.search(r"registerEntityRenderer\(tc4revival\.content\.TCEntities\." + field + r"\.get\(\),\s*\w+\s*->\s*new\s+([\w.]+)", renderers)
            if not match:
                raise ValueError(f"Inspect renderer registration for {field}")
            renderer = match[1].split(".")[-1]
        rows.append(f"| {legacy_name} | {legacy_class} | {field} | {modern} | {renderer} |")
    extra = sorted({field for _, field in new} - used)
    header = f"""# 旧版实体实现入口清单

由 `python tools/inventory_legacy_entities.py --write` 从当前源码生成。

- 范围：旧 `{OLD.as_posix()}` 中实际调用 registerModEntity 的 {len(old)} 项；不把 import、白名单引用或未注册旧类算作可生成实体。
- 现代 `{NEW.as_posix()}` 有 {len(new)} 个实体字段；本清单逐项确认实现源文件和 `{RENDERERS.as_posix()}` 的渲染注册入口。六种腐化动物按 Kind 分派。
- 此为源码导航与遗漏检查，**不是行为、存档、网络、视觉或完整 mod 的验收结果**。DeferredRegister 字段及 renderer 调用存在，不能证明运行时所有分支正确。
- 现代额外字段：{', '.join(extra) if extra else '无'}。
- 后续验收须逐系统核对实际生成途径、AI/交互、数据保存恢复、多人同步和客户端表现；结果以 DEVELOPMENT.md 中明确的验证记录及对应日志为准，未覆盖项不推定通过。
- 尤其要区分实体本身已实现与生成入口齐全：本轮连续核对曾发现火蝠自然生成、格栅专用输出物等遗漏，不能以总数相等代替内容审查。

现代实现源文件均位于 `src/main/java/tc4revival/content/`；旧源文件可按旧类名在 `references/deobfuscated_src/thaumcraft/common/entities/` 下定位。

| 旧注册名 | 旧实现类 | 现代注册字段 | 现代实现类 | 渲染入口 |
| --- | --- | --- | --- | --- |
"""
    return header + "\n".join(rows) + "\n", len(rows)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    document, count = inventory()
    if parser.parse_args().write:
        (ROOT / "docs/ENTITY-IMPLEMENTATION.md").write_text(document, encoding="utf-8")
    print(f"Indexed {count} legacy registrations with implementation files and renderer entry points. Behavioral parity is not implied.")
