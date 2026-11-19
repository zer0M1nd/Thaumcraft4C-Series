"""Verify observed book coverage and both generated translation dictionaries."""
import collections
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
pages = json.loads((ROOT / "docs/legacy-baseline/2026-09-09-copper/research-pages.json").read_text(encoding="utf-8"))
counts = collections.Counter()
references = set()


def walk(value):
    if isinstance(value, dict):
        if "kind" in value:
            counts[value["kind"]] += 1
        if value.get("kind") == "reference":
            references.update(value["keys"])
        for child in value.values():
            walk(child)
    elif isinstance(value, list):
        for child in value:
            walk(child)


walk(pages)
assert len(pages) == 194 and sum(len(row["pages"]) for row in pages) == 576
assert counts["unresolved"] == 0 and counts["inline_arcane"] == 3 and counts["runic_augmentation_example"] == 5
assert len(references) == 347
recipes = json.loads((ROOT / "docs/legacy-baseline/2026-09-09-copper/research-page-recipes.json").read_text(encoding="utf-8"))
assert len(recipes) == 347 and {row["key"] for row in recipes} == references
walk(recipes)
assert counts["unresolved"] == 0 and counts["ore"] == 218 and counts["nbt"] == 5
for locale in ("en_us", "zh_cn"):
    language = json.loads((ROOT / f"src/generated/resources/assets/thaumcraft/lang/{locale}.json").read_text(encoding="utf-8"))
    missing = {page["text"] for row in pages for page in row["pages"]
               if page.get("text", "").startswith("tc.research_page.") and page["text"] not in language}
    legacy_locale = "en_US" if locale == "en_us" else "zh_CN"
    old_keys = {line.split("=", 1)[0] for line in (ROOT / f"references/resources/assets/thaumcraft/lang/{legacy_locale}.lang")
                .read_text(encoding="utf-8").splitlines() if "=" in line}
    assert not (missing & old_keys), (locale, "lost translations", sorted(missing & old_keys))
    if missing:
        english = json.loads((ROOT / "src/generated/resources/assets/thaumcraft/lang/en_us.json").read_text(encoding="utf-8"))
        assert missing <= english.keys(), (locale, "missing English fallback", sorted(missing))
    print(locale, "all existing legacy page translations retained; English fallback:", sorted(missing))
print("PASS: 194 entries, 576 pages, 347 named display recipes, 218 ore references, 5 NBT modifications, 8 inline examples; no unresolved descriptors")
