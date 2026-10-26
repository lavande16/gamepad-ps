#!/usr/bin/env python3
"""Verify frozen stage-one references; pass --full to rehash the large ISO too."""
import hashlib
import json
import pathlib
import sys
import tarfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
STAGE = ROOT / "stage1"


def digest(path):
    h = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(8 * 1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def load(name):
    return json.loads((STAGE / "locks" / name).read_text())


def main():
    result = {}
    for product in ("ns", "pc"):
        source = load(product + "-source.json")
        archive = STAGE / source["referenceArchive"]
        assert digest(archive) == source["referenceArchiveSha256"]
        rows = load(product + "-reuse-inventory.json")
        expected = {row["sourcePath"]: row for row in rows}
        with tarfile.open(archive) as tar:
            actual = {}
            for member in tar:
                if member.isfile():
                    actual[member.name] = hashlib.sha256(tar.extractfile(member).read()).hexdigest()
        assert actual.keys() == expected.keys()
        assert all(actual[name] == expected[name]["sha256"] for name in actual)
        result[product + "ReferenceFilesVerified"] = len(rows)
    source = load("armsx2-source.json")
    for name, meta in source["referenceFiles"].items():
        assert digest(STAGE / "references/armsx2-2.8.4" / name) == meta["sha256"]
    result["armsx2PinnedFilesVerified"] = len(source["referenceFiles"])
    installed = load("installed-armsx2.json")
    assert digest(pathlib.Path(installed["backupApk"])) == installed["apkSha256"] == source["releaseApkSha256"]
    result["installedApkMatchesRelease"] = True
    for name, value in load("defaults-files.json").items():
        assert digest(ROOT / name) == value
    for item in load("visual-baseline.json"):
        assert digest(STAGE / item["file"]) == item["sha256"]
    for item in load("licenses.json")["sourceLicenseFiles"]:
        assert digest(STAGE / item["referenceFile"]) == item["sha256"]
    checked = 0
    for item in load("materials.json")["materials"]:
        path = pathlib.Path(item["snapshot"])
        assert path.stat().st_size == item["bytes"]
        if "--full" in sys.argv or item["bytes"] < 1024 * 1024 * 1024:
            assert digest(path) == item["sha256"]
            checked += 1
        assert item["destinationReadbackVerified"]
    result["materialSnapshotsRehashed"] = checked
    result["largeISOPreviouslyHashedAndSizeChecked"] = "--full" not in sys.argv
    result["sourceAndMaterialsLocked"] = True
    result["runtimeBuildVerified"] = False
    (STAGE / "verification.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
