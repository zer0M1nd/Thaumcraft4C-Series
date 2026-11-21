"""Read-only check of the designated TC4Tester live scan validation save (requires nbtlib)."""
import argparse
import hashlib
import uuid
from pathlib import Path

import nbtlib


parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("save", type=Path)
parser.add_argument("--automatic", action="store_true", help="Also require the 22 legacy automatic research markers")
args = parser.parse_args()
digest = hashlib.md5(b"OfflinePlayer:TC4Tester").digest()
player_id = uuid.UUID(bytes=digest, version=3)
root = nbtlib.load(args.save)
rows = root["data"]["players"]
players = [row for row in rows if uuid.UUID(bytes=b"".join(
    (int(part) & 0xffffffff).to_bytes(4, "big") for part in row["uuid"])) == player_id]
assert len(players) == 1, "Expected exactly one TC4Tester record"
knowledge = players[0]["knowledge"]
markers = [str(row["key"]) for row in knowledge["research"]]
expected = ["@THAUMIUM", "@FOCUSHELLBAT"]
if args.automatic:
    expected += ["WARP", "RESEARCH", "ENCHANT", "NODES", "ORE", "ASPECTS", "KNOWFRAG",
                 "THAUMONOMICON", "PLANTS", "PECH", "BASICTHAUMATURGY", "CAP_iron", "ROD_wood",
                 "CRUCIBLE", "PHIAL", "TABLE", "BASICARTIFACE", "THAUMOMETER", "RESTABLE",
                 "GRATE", "ARCTABLE", "ARCANESTONE"]
assert markers == expected, markers
assert len(knowledge["scans_object"]) == 6
assert not knowledge["scans_entity"] and not knowledge["scans_phenomenon"]
pools = {str(row["key"]): int(row["amount"]) for row in knowledge["aspects"]}
assert pools == dict(aer=17, ignis=17, terra=25, aqua=21, ordo=15,
                     perditio=18, vitreus=3, metallum=32), pools
assert all(int(knowledge[key]) == 0 for key in (
    "warp_sticky", "warp_temporary", "warp_permanent", "warp_counter"))
print("PASS: TC4Tester retains @THAUMIUM and @FOCUSHELLBAT clues, "
      "6 object scans, exact earned pools, zero warp; automatic research:", args.automatic)
print("Player:", player_id)
print("Markers:", markers)
print("Pools:", pools)
print("File SHA256:", hashlib.sha256(args.save.read_bytes()).hexdigest())
