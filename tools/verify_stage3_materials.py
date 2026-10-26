#!/usr/bin/env python3
"""Controlled acceptance fixtures. Only this tool's files are removed; originals are moved back."""
from stage3_device import *
import hashlib
import configparser

product = '/storage/emulated/0/GamePad PS'
original = product+'/game/究极觉醒2'
renamed = product+'/game/究极觉醒2_目录装配自检'
dll = product+'/LSFG/Lossless.dll'
test_dirs = [product+'/game/装配自检_导入',product+'/game/装配自检_保留']
moves=[]
evidence={}
created=[]


def move(source,target):
    shell('test','-e',source)
    try:
        shell('test','-e',target)
    except subprocess.CalledProcessError:
        pass
    else:
        raise RuntimeError('Temporary target already exists: '+target)
    shell('mv',source,target);moves.append((source,target))


def restore():
    while moves:
        source,target=moves[-1]
        shell('mv',target,source);moves.pop()


def report_ready(ready=True, games=None):
    for _ in range(25):
        try:
            v=read_report()
            if v['ready']==ready and (games is None or len(v['games'])==games): return v
        except (subprocess.CalledProcessError,ValueError): pass
        time.sleep(.5)
    raise AssertionError('Assembly result did not reach expected state')


def launch_naruto():
    current=nodes()
    match=next(n for n in current if any(s in (n.get('text') or n.get('content-desc') or '') for s in ['Naruto','NARUTO']))
    tap_node(match)


def runtime(name, lsfg):
    for _ in range(35):
        log=adb('exec-out','cat /storage/emulated/0/Android/data/com.gamepad.ps/files/logs/emulog.txt').decode()
        if 'ELF changed, active CRC B576177F' in log and 'McdSlot 0' in log:
            assert ('LSFG: frame generation active' in log)==lsfg
            (ROOT/'stage3'/f'{name}.log').write_text(log)
            return log
        time.sleep(.5)
    raise AssertionError('Game did not finish boot: '+log[-800:])


try:
    # This tool starts only from a stopped library; the test app was already installed.
    stop_game();shell('am','force-stop','com.gamepad.ps')
    move(original,renamed);move(dll,product+'/LSFG/.Lossless.stage3.hold')
    for directory,iso in zip(test_dirs,['import.iso','preserve.iso']):
        try:shell('test','-e',directory)
        except subprocess.CalledProcessError:pass
        else:raise RuntimeError('Fixture directory already exists; preserving it: '+directory)
        shell('mkdir','-p',directory+'/ISO',directory+'/Memcards')
        created.append(directory)
        adb('push',str(ROOT/'stage3/fixtures'/iso),directory+'/ISO/'+iso)
        adb('push',str(ROOT/'stage3/fixtures/source-card.ps2'),directory+'/Memcards/mcd001.ps2')
    adb('push',str(ROOT/'stage3/fixtures/current-card.ps2'),test_dirs[1]+'/Memcards/slot1.ps2')
    adb('push',str(ROOT/'stage3/fixtures/invalid.iso'),test_dirs[0]+'/ISO/invalid.iso')
    cold_start();v=report_ready(games=3)
    assert not v['lsfgAvailable'] and len(v['rejectedGames'])==1 and len(v['imports'])==1,v
    assert {g['serial'] for g in v['games']}=={'SLPS-25837','SLUS-99998','SLUS-99999'},v
    for directory,local in zip(test_dirs,['source-card.ps2','current-card.ps2']):
        digest=shell('sha256sum',directory+'/Memcards/slot1.ps2').split()[0]
        assert digest==hashlib.sha256((ROOT/'stage3/fixtures'/local).read_bytes()).hexdigest()
    evidence['directory_discovery_import_preserve_optional_lsfg']=v
    (ROOT/'stage3/composite-report.json').write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n')
    current=nodes();assert any(n.get('text')=='游戏总数: 3' for n in current)
    launch_naruto();log=runtime('renamed-no-lsfg',False)
    assert renamed+'/Memcards' in log and renamed+'/Sstates' in log
    print('PASS: directory rename, 3 serials, invalid ISO rejection, card import/preservation, LSFG-off actual boot',flush=True)
    stop_game();shell('am','force-stop','com.gamepad.ps')
    for directory in test_dirs:shell('rm','-r',directory)
    restore()
    # Missing mandatory files must block in the reused NS panel, without creating blank saves.
    private=report_ready(games=3)['bios']
    assert 'com.gamepad.ps/' in private
    source_bios=product+'/BIOS/SCPH-10000.BIN'
    iso=original+'/ISO/gant-nsna2-zh-1.0.iso'
    move(source_bios,product+'/BIOS/.SCPH-10000.stage3.hold')
    move(private,str(Path(private).parent/'.SCPH-10000.stage3.hold'))
    move(iso,original+'/ISO/.game.stage3.hold')
    cold_start();v=report_ready(False,0)
    assert any('BIOS' in s for s in v['errors']) and any('ISO' in s for s in v['errors']),v
    evidence['mandatory_missing']=v
    (ROOT/'stage3/missing-material-report.json').write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n')
    snapshot('missing-materials')
    restore()
    tap_node(next(n for n in nodes() if n.get('text')=='重新检查'))
    report_ready(True,1)
    print('PASS: missing BIOS/ISO blocked safely; restore and Retry recovered',flush=True)
finally:
    # Never restore/rename a file beneath an active game. At this point only the library or
    # the blocked assembly page is expected; kill only this test app as a cleanup fallback.
    shell('am','force-stop','com.gamepad.ps')
    restore()
    for directory in created:
        try:shell('test','-d',directory)
        except subprocess.CalledProcessError:continue
        shell('rm','-r',directory)
    (ROOT/'stage3/material-scenarios.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n')
    cold_start()
