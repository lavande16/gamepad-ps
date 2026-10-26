#!/usr/bin/env python3
"""Inventory every file in the pinned Android/front-end reference archives."""
import collections
import hashlib
import json
import pathlib
import re
import tarfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
STAGE = ROOT / "stage1"


def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n")


def family(path):
    low = path.lower()
    groups = [
        ("external-display-deferred", ("externaldisplay", "reardisplay", "rearscreen", "reartouch")),
        ("switch-specific", ("amiibo", "applet", "firmwareavatar", "addon", "ultracam", "cabinet")),
        ("runtime-lifecycle-adaptation", ("emulation", "yuzuapplication", "nativelibrary", "nativeconfig", "foregroundservice", "lifecycle", "powerstate", "memoryutil")),
        ("shared-layout-and-collections", ("/layout/", "abstract", "itemdecoration", "viewutils", "insetshelper", "fullscreenhelper", "aspectratio", "fixedratio", "squareimage", "gradientborder", "shelllayout")),
        ("file-metadata-and-support", ("gamehelper", "gameicon", "gamemetadata", "serializable", "pathutil", "compatutils", "netdatavalidators", "selectableitem")),
        ("setup-and-install-adaptation", ("setup", "installable", "/model/patch", "taskviewmodel")),
        ("settings", ("settings", "setting", "list_item", "preference")),
        ("dialogs-and-progress", ("dialog", "progress", "loading", "warning", "error")),
        ("controller-and-input", ("controller", "input", "haptic", "stick", "dpad", "abxy", "button_z", "button_l", "button_r")),
        ("viewport-and-device", ("viewport", "orientation", "displaymode", "displaysize", "windowpolicy", "socgate", "productcontract")),
        ("cartridge-and-library", ("card_game", "cartridge", "gameadapter", "gamesfragment", "fragment_games", "shelf", "cover")),
        ("home-and-navigation", ("home", "mainactivity", "navigation", "toolbar", "menu")),
        ("materials-and-storage", ("assembly", "assembler", "package", "directory", "fileutil", "folder", "lossless", "storage", "document")),
        ("game-metadata-and-save", ("gameinfo", "gameproperties", "save", "backup", "restore", "gamesviewmodel", "/model/game")),
        ("driver-tools", ("driver", "freedreno")),
        ("network-and-profile", ("netplay", "lobby", "chat", "profile", "user", "keyboard")),
        ("shared-visual-system", ("pixel", "theme", "style", "dimen", "color", "font", "drawable", "animation", "/anim/")),
        ("about-and-system-tools", ("license", "about", "systeminfo", "language", "log", "update")),
    ]
    return next((name for name, words in groups if any(word in low for word in words)), "other-support-files")


def mode(product, path, imports):
    low = path.lower()
    if any(x in low for x in ("externaldisplay", "rearscreen", "reardisplay", "reartouch")):
        return "deferred-not-first-release"
    if product == "NS" and ("/jni/" in low or path.endswith("NativeLibrary.kt") or path.endswith("NativeConfig.kt")):
        return "replace-switch-runtime-binding"
    if product == "PC" and ("/com/winlator/" in low or "/xserver/" in low or "/container/" in low):
        return "exclude-pc-runtime"
    if any(x in low for x in ("amiibo", "applet", "firmwareavatar", "ultracam", "cabinet")):
        return "exclude-switch-only-feature"
    if "/res/" in low:
        return "reuse-resource-original-select-applicable-content"
    if path.endswith((".kt", ".java")):
        return "reuse-code-adapt-bindings" if imports else "reuse-code-candidate"
    return "reference-build-or-support-file"


def inventory(product, lock_name):
    source = json.loads((STAGE / "locks" / lock_name).read_text())
    archive = STAGE / source["referenceArchive"]
    assert hashlib.sha256(archive.read_bytes()).hexdigest() == source["referenceArchiveSha256"]
    rows = []
    with tarfile.open(archive) as tar:
        for member in tar:
            if not member.isfile():
                continue
            data = tar.extractfile(member).read()
            text = data.decode("utf-8", errors="replace") if member.name.endswith((".kt", ".java", ".xml", ".kts", ".gradle", ".properties")) else ""
            imports = re.findall(r"(?m)^import\s+([^;\n]+)", text)
            bindings = [x for x in imports if any(t in x for t in ("NativeLibrary", "NativeConfig", "features.settings.model", "com.winlator", "app.gamenative.PrefManager"))]
            rows.append({
                "product": product,
                "sourceCommit": source["sourceCommit"],
                "sourcePath": member.name,
                "sha256": hashlib.sha256(data).hexdigest(),
                "bytes": member.size,
                "family": family(member.name),
                "reuseMode": mode(product, member.name, bindings),
                "imports": imports,
                "backendBindingImports": bindings,
                "resourceReferences": sorted(set(re.findall(r"@(?:\+)?(?:android:)?(?:[A-Za-z_]+/)[A-Za-z0-9_.]+|\bR\.[A-Za-z_]+\.[A-Za-z0-9_]+", text))),
                "integrationCompileVerified": False,
            })
    write(STAGE / "locks" / (product.lower() + "-reuse-inventory.json"), rows)
    return rows


def main():
    ns = inventory("NS", "ns-source.json")
    pc = inventory("PC", "pc-source.json")
    ns_hashes = collections.defaultdict(list)
    for row in ns:
        ns_hashes[row["sha256"]].append(row["sourcePath"])
    identical = [{"ns": ns_hashes[row["sha256"]], "pc": row["sourcePath"], "sha256": row["sha256"]} for row in pc if row["sha256"] in ns_hashes]
    write(STAGE / "locks" / "identical-ns-pc-files.json", identical)
    summary = {
        "scope": "all pinned NS Android files and all pinned PC main Java/resource files",
        "nsFiles": len(ns),
        "nsCodeFiles": sum(x["sourcePath"].endswith((".kt", ".java")) for x in ns),
        "nsResourceFiles": sum("/res/" in x["sourcePath"] for x in ns),
        "pcFiles": len(pc),
        "pcFilesIdenticalToNS": len(identical),
        "families": {},
        "reviewBoundary": "File-level inventory and functional reuse review; compilation and runtime adaptation remain for later stages.",
    }
    for product, rows in (("NS", ns), ("PC", pc)):
        summary["families"][product] = dict(sorted(collections.Counter(x["family"] for x in rows).items()))
    write(STAGE / "locks" / "reuse-summary.json", summary)
    print(json.dumps({k: v for k, v in summary.items() if k != "families"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
