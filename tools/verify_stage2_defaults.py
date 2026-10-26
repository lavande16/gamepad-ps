#!/usr/bin/env python3
"""Read back the independent test app's seeded settings; never write device data."""
import argparse
import configparser
import hashlib
import json
from pathlib import Path
import shlex
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ADB = str(Path.home() / "Library/Android/sdk/platform-tools/adb")
parser = argparse.ArgumentParser()
parser.add_argument("--serial", required=True)
args = parser.parse_args()


def read(path):
    return subprocess.check_output([ADB, "-s", args.serial, "exec-out", "cat " + shlex.quote(path)])


base = "/storage/emulated/0/Android/data/com.gamepad.ps/files"
private = "/data/user/0/com.gamepad.ps"
dll = private + "/files/lsfg/Lossless.dll"
xml = ET.fromstring(read(private + "/shared_prefs/ARMSX2.xml"))
prefs = {node.attrib["name"]: node.text if node.tag == "string" else node.attrib.get("value") for node in xml}
defaults = json.loads((ROOT / "defaults/armsx2-defaults.json").read_text().replace("${ROOT}/LSFG/Lossless.dll", dll))
defaults["global"].update(memoryCardSlot1Filename="slot1.ps2", memoryCardSlot2Filename="slot2.ps2")
errors = []
verified = {}
for key, expected in [("config.global", defaults["global"])] + [("config.game." + s, v) for s, v in defaults["games"].items()]:
    actual = json.loads(prefs.get(key, "{}"))
    differences = {k: {"expected": v, "actual": actual.get(k)} for k, v in expected.items() if actual.get(k) != v}
    if differences:
        errors.append({key: differences})
    verified[key] = {"expectedFields": len(expected), "actualFields": len(actual), "differences": differences}
for key in ["gamepad.ps.defaults.v1", "setupComplete"]:
    if prefs.get(key) != "true":
        errors.append({key: prefs.get(key)})
ini = configparser.ConfigParser(interpolation=None, strict=False)
ini.optionxform = str
ini.read_string(read(base + "/PCSX2-Android.ini").decode())
cards = "/storage/emulated/0/GamePad PS/game/究极觉醒2/Memcards"
for section, key, expected in [("Folders", "MemoryCards", cards), ("MemoryCards", "Slot1_Filename", "slot1.ps2"), ("MemoryCards", "Slot2_Filename", "slot2.ps2")]:
    actual = ini.get(section, key, fallback=None)
    if actual != expected:
        errors.append({section + "/" + key: {"expected": expected, "actual": actual}})
dll_hash = hashlib.sha256(read(dll)).hexdigest()
if dll_hash != "626b196d799606cd4250b7b29e04228692ab70cf56a5d1bbb56d748c8219f0eb":
    errors.append({"dllSha256": dll_hash})
global_settings = json.loads(prefs["config.global"])
effective = {**global_settings, **json.loads(prefs["config.game.SLPS-25837"])}
result = {"serial": args.serial, "package": "com.gamepad.ps", "settings": verified, "effectiveNaruto": effective,
          "paths": {k: prefs.get(k) for k in ["bios", "biosDir", "systemDir", "romsDirs"]},
          "memoryCards": cards, "dllSha256": dll_hash, "errors": errors, "passed": not errors}
(ROOT / "stage2/device-settings-readback.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
print(json.dumps({"passed": not errors, "globalFields": len(defaults["global"]), "gameOverrideFields": len(defaults["games"]["SLPS-25837"]), "errors": errors}, ensure_ascii=False))
raise SystemExit(bool(errors))
