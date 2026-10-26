#!/usr/bin/env python3
"""Stage-three acceptance helpers. Device writes are restricted to the independent PS product."""
import json
from pathlib import Path
import re
import shlex
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ADB = str(Path.home() / 'Library/Android/sdk/platform-tools/adb')
SERIAL = '403G7R006F310V0'


def adb(*args):
    return subprocess.check_output([ADB, '-s', SERIAL, *args])


def shell(*args):
    return adb('shell', shlex.join(args)).decode()


def nodes():
    shell('uiautomator', 'dump', '/data/local/tmp/gamepad-ps-stage3.xml')
    return list(ET.fromstring(adb('exec-out', 'cat /data/local/tmp/gamepad-ps-stage3.xml')).iter('node'))


def tap_node(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    shell('input', 'tap', str((x1+x2)//2), str((y1+y2)//2))


def stop_game():
    top = shell('dumpsys', 'activity', 'activities').split('topResumedActivity=')[-1].splitlines()[0]
    if 'com.gamepad.ps/' not in top:
        raise RuntimeError('GamePad PS is not foreground; no UI input sent')
    current = nodes()
    if any(n.get('text') == '游戏库' for n in current):
        return
    close = next((n for n in current if n.get('text') == '关闭'), None)
    if close is None:
        shell('input', 'keyevent', 'KEYCODE_BACK')
        current = nodes()
        close = next((n for n in current if n.get('text') == '关闭'), None)
    if close is None:
        raise RuntimeError('Cannot locate the normal stop-game action; no process was killed')
    tap_node(close)
    if not any(n.get('text') == '游戏库' for n in nodes()):
        raise RuntimeError('Game has not returned to the library')


def cold_start():
    shell('am', 'force-stop', 'com.gamepad.ps')
    shell('am', 'start', '-n', 'com.gamepad.ps/com.armsx2.BootSplashActivity')


def read_report():
    return json.loads(adb('exec-out', 'cat /data/user/0/com.gamepad.ps/files/gamepad-ps/assembly-report.json'))


def snapshot(name):
    (ROOT/'stage3'/f'{name}.png').write_bytes(adb('exec-out', 'screencap -p'))
    current = nodes()
    (ROOT/'stage3'/f'{name}.xml').write_bytes(adb('exec-out', 'cat /data/local/tmp/gamepad-ps-stage3.xml'))
    labels = [(n.get('text') or n.get('content-desc'), n.get('bounds')) for n in current if n.get('text') or n.get('content-desc')]
    print(json.dumps(labels, ensure_ascii=False))


if __name__ == '__main__':
    snapshot('current')
