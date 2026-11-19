"""Read-only assertions for the September 9 research-item multiplayer acceptance session."""
import argparse
from pathlib import Path
import nbtlib

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("before", type=Path)
parser.add_argument("after", type=Path)
parser.add_argument("player", type=Path)
args = parser.parse_args()

def knowledge(path):
    rows = nbtlib.load(path)["data"]["players"]
    # The other retained fixture player has a deliberately different aspect/research set.
    matches = [p["knowledge"] for p in rows if any(str(r["key"]) == "@FOCUSHELLBAT" for r in p["knowledge"]["research"])]
    assert len(matches) == 1
    return matches[0]

before, after = knowledge(args.before), knowledge(args.after)
pools = lambda k: {str(a["key"]): int(a["amount"]) for a in k["aspects"]}
old, new = pools(before), pools(after)
assert old.keys() == new.keys()
delta = {key: new[key] - old[key] for key in old}
assert all(delta[key] in range(2, 5) for key in ("aer", "terra", "ignis", "aqua", "ordo", "perditio")), delta
assert delta["vitreus"] == delta["metallum"] == 0
markers = lambda k: [str(r["key"]) for r in k["research"]]
assert markers(after) == markers(before) + ["RESEARCHER1", "RESEARCHER2", "RESEARCHDUPE"]
assert int(after["warp_permanent"]) == int(before["warp_permanent"]) + 1
assert int(after["warp_counter"]) == int(before["warp_counter"]) + 1
for key in ("warp_sticky", "warp_temporary", "scans_object", "scans_entity", "scans_phenomenon"):
    assert before[key] == after[key], key

inventory = {int(item["Slot"]): item for item in nbtlib.load(args.player)["Inventory"]}
assert str(inventory[0]["id"]) == "thaumcraft:unknown_research_notes" and int(inventory[0]["Count"]) == 1
assert 2 not in inventory, "Survival discovery was not consumed"
note = inventory[1]["tag"]
assert str(inventory[1]["id"]) == "thaumcraft:research_notes"
assert str(note["key"]) == "ETHEREALBLOOM" and int(note["complete"]) == 0
assert len(note["hexgrid"]) == 33
assert {str(cell["aspect"]) for cell in note["hexgrid"] if int(cell["type"]) == 1} == {"vitium", "sano", "praecantatio", "herba"}
assert str(inventory[4]["id"]) == "thaumcraft:knowledge_fragment" and int(inventory[4]["Count"]) == 1
print("Research item live-save PASS: two fragment uses", delta,
      "; three completions; warp=1; survival consumption; crafted unknown; revealed 33-cell note; prior scans preserved")
