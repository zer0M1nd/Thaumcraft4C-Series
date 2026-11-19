"""Check that the copper observer adds only the three source-defined conditional research entries."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "docs/legacy-baseline/2026-09-08"
COPPER = ROOT / "docs/legacy-baseline/2026-09-09-copper"
environment = json.loads((COPPER / "observation-environment.json").read_text())
assert environment == {"copper_fixture": True, "found_copper_ingot": True, "found_copper_ore": True}
research_keys = {"CAP_copper", "PURECOPPER", "TRANSCOPPER"}


def ordered(value):
    if isinstance(value, dict):
        return tuple((key, ordered(child)) for key, child in value.items())
    if isinstance(value, list):
        return tuple(ordered(child) for child in value)
    return value


for filename in ("research-catalog.json", "research-pages.json", "research-page-recipes.json"):
    original = json.loads((BASE / filename).read_text(encoding="utf-8"))
    copper = json.loads((COPPER / filename).read_text(encoding="utf-8"))
    if filename == "research-catalog.json":
        assert [{k: v for k, v in c.items() if k != "research"} for c in original] == [
            {k: v for k, v in c.items() if k != "research"} for c in copper]
        original = [r for c in original for r in c["research"]]
        copper = [r for c in copper for r in c["research"]]
    old_by_key = {row["key"]: row for row in original}
    new_by_key = {row["key"]: row for row in copper}
    expected = {"WandCapCopper", "PureCopper", "TransCopper"} if filename == "research-page-recipes.json" else research_keys
    assert new_by_key.keys() - old_by_key.keys() == expected
    assert old_by_key.keys() <= new_by_key.keys()
    assert all(ordered(new_by_key[key]) == ordered(value) for key, value in old_by_key.items()), filename
    assert ordered([r for r in copper if r["key"] in old_by_key]) == ordered(original), "Existing iteration order changed"
    print(filename, "PASS: existing entries/order unchanged; added", sorted(expected))
print("PASS: isolated copper observation enabled both flags and changes only the intended research data")
