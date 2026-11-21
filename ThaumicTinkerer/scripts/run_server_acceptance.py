"""Run the addon's GameTests sequentially against its local dedicated server."""
import argparse
import json
import re
import time
from pathlib import Path
from server_console import command

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--include", default=".*")
parser.add_argument("--exclude", default="^$")
parser.add_argument("--report", default="baseline-tests")
parser.add_argument("--server-directory", default="run-server")
parser.add_argument("--snake-case", action="store_true", help="GregTech's development mixin changes GameTest names")
args = parser.parse_args()
tests = []
for source in sorted((ROOT / "src/main/java/ttmodern").glob("*ServerTests.java")):
    for name in re.findall(r"@GameTest\([^)]*\)\s*public static void (\w+)\(", source.read_text(encoding="utf-8")):
        if re.search(args.include, name, re.I) and not re.search(args.exclude, name, re.I):
            registered = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower() if args.snake_case else name.lower()
            tests.append((registered, source.name))
log = ROOT / args.server_directory / "logs/latest.log"
report = ROOT / ".tooling" / (args.report + ".json")
results = []
command("gamerule doMobSpawning false")
command("gamerule doWeatherCycle false")
for index, (name, source) in enumerate(tests):
    offset = log.stat().st_size
    x, z = 2000 + (index % 10) * 80, 2000 + (index // 10) * 80
    response = command(f"execute positioned {x} 10 {z} run test run {name}")
    started = time.monotonic()
    status, evidence = "TIMEOUT", response
    while time.monotonic() - started < 90:
        with log.open("rb") as stream:
            stream.seek(offset)
            tail = stream.read().decode("utf-8", errors="replace")
        match = re.search(r"TT_ACCEPT (PASS|FAIL) " + re.escape(name) + r"\b", tail)
        if match:
            status, evidence = match[1], tail[match.start():]
            break
        if response.strip() and ("No such test" in response or "error" in response.lower()):
            status = "COMMAND_ERROR"
            break
        time.sleep(.2)
    result = dict(name=name, source=source, status=status, seconds=round(time.monotonic()-started, 2), evidence=evidence)
    results.append(result)
    report.write_text(json.dumps(results, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"{index+1}/{len(tests)} {status} {name}", flush=True)
    if status in ("TIMEOUT", "COMMAND_ERROR"):
        break
print(f"Report: {report}", flush=True)
raise SystemExit(0 if len(results) == len(tests) and all(r["status"] == "PASS" for r in results) else 1)
