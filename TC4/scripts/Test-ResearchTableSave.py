"""Read-only assertions for the September 9 research-table multiplayer session."""
import argparse
from pathlib import Path
import nbtlib

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("before", type=Path, help="Pre-table-test backup directory")
parser.add_argument("after", type=Path, help="Post-table-test evidence directory")
args = parser.parse_args()

def knowledge(directory):
    rows = nbtlib.load(directory / "thaumcraft_knowledge.dat")["data"]["players"]
    matches = [row["knowledge"] for row in rows if any(str(r["key"]) == "@FOCUSHELLBAT" for r in row["knowledge"]["research"])]
    assert len(matches) == 1
    return matches[0]

before, after = knowledge(args.before), knowledge(args.after)
pools = lambda k: {str(a["key"]): int(a["amount"]) for a in k["aspects"]}
old, new = pools(before), pools(after)
assert new == {"aer": 13, "ignis": 15, "terra": 25, "aqua": 20, "ordo": 14,
               "perditio": 18, "vitreus": 2, "metallum": 32, "lux": 4}, new
delta = {key: new.get(key, 0) - old.get(key, 0) for key in old.keys() | new.keys()}
assert delta == {"aer": -4, "ignis": -2, "terra": 0, "aqua": -1, "ordo": -1,
                 "perditio": 0, "vitreus": -1, "metallum": 0, "lux": 4}, delta
for key in ("research", "warp_permanent", "warp_sticky", "warp_temporary", "warp_counter",
            "scans_object", "scans_entity", "scans_phenomenon"):
    assert before[key] == after[key], key
old_player = nbtlib.load(args.before / "player.dat")
new_player = nbtlib.load(args.after / "player.dat")
inventory = lambda p: {int(item["Slot"]): item for item in p["Inventory"]}
assert inventory(old_player) == inventory(new_player), "Original eight items changed or fixture items left in player inventory"
assert int(new_player["playerGameType"]) == 0, "Expected survival table test"
assert int(new_player["SelectedItemSlot"]) == 8
print("Research table live-save PASS: exact placement/erase/combine costs", delta,
      "; prior research/scans/warp and eight original inventory stacks unchanged; survival, empty hand")
