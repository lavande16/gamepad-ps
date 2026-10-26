#!/usr/bin/env python3
"""Temporarily hold this product's app data, verify first-run import, then restore it exactly."""
from stage3_device import *
import hashlib

paths=['/data/user/0/com.gamepad.ps/shared_prefs','/data/user/0/com.gamepad.ps/files',
       '/storage/emulated/0/Android/data/com.gamepad.ps/files']
held=[];generated=[]
prefs_file='/data/user/0/com.gamepad.ps/shared_prefs/ARMSX2.xml'
before=None
record={}
try:
 stop_game();shell('am','force-stop','com.gamepad.ps')
 before=hashlib.sha256(adb('exec-out','cat '+prefs_file)).hexdigest()
 for path in paths:
  assert '/com.gamepad.ps/' in path
  backup=path+'.stage3-held'
  try:shell('test','-e',backup)
  except subprocess.CalledProcessError:pass
  else:raise RuntimeError('Original-data holding folder already exists')
  shell('mv',path,backup);held.append((path,backup))
 cold_start()
 for _ in range(40):
  try:
   report=read_report()
   if report['ready']:break
  except (subprocess.CalledProcessError,ValueError):pass
  time.sleep(.5)
 else:raise AssertionError('Fresh-data initialization did not complete')
 p={n.get('name'):n.text if n.tag=='string' else n.get('value') for n in ET.fromstring(adb('exec-out','cat '+prefs_file))}
 defaults=json.loads((ROOT/'defaults/armsx2-defaults.json').read_text().replace('${ROOT}/LSFG/Lossless.dll','/data/user/0/com.gamepad.ps/files/lsfg/Lossless.dll'))
 defaults['global'].update(memoryCardSlot1Filename='slot1.ps2',memoryCardSlot2Filename='slot2.ps2')
 assert json.loads(p['config.global'])==defaults['global']
 assert json.loads(p['config.game.SLPS-25837'])==defaults['games']['SLPS-25837']
 for source,target in [('/storage/emulated/0/GamePad PS/BIOS/SCPH-10000.BIN',report['bios']),('/storage/emulated/0/GamePad PS/LSFG/Lossless.dll',report['dll'])]:
  assert shell('sha256sum',source).split()[0]==shell('sha256sum',target).split()[0]
 current=nodes();assert any(n.get('text')=='游戏总数: 1' for n in current)
 tap_node(next(n for n in current if any(s in (n.get('text') or n.get('content-desc') or '') for s in ['Naruto','NARUTO'])))
 for _ in range(35):
  log=adb('exec-out','cat /storage/emulated/0/Android/data/com.gamepad.ps/files/logs/emulog.txt').decode()
  if 'ELF changed, active CRC B576177F' in log:break
  time.sleep(.5)
 else:raise AssertionError('Fresh-data game boot failed')
 assert 'LSFG: frame generation active (2x' in log and 'McdSlot 0 [File]: slot1.ps2 [64 MB, Formatted]' in log
 (ROOT/'stage3/fresh-initialization-runtime.log').write_text(log)
 record={'freshApplicationData':True,'defaultGlobalFields':242,'gameOverrideFields':16,
         'biosImportSha256Matches':True,'lsfgImportSha256Matches':True,
         'gameBootCrc':'B576177F','lsfg2xActive':True,'existingPortableCardMounted':True}
 print('PASS: empty app data automatically imported BIOS/DLL, all captured defaults, game and existing card',flush=True)
 stop_game()
finally:
 shell('am','force-stop','com.gamepad.ps')
 for path,backup in reversed(held):
  try:shell('test','-e',path)
  except subprocess.CalledProcessError:pass
  else:
   temp=path+'.stage3-generated'
   shell('mv',path,temp);generated.append(temp)
  shell('mv',backup,path)
 assert before is None or hashlib.sha256(adb('exec-out','cat '+prefs_file)).hexdigest()==before
 record['originalPreferencesRestoredByteForByte']=True
 (ROOT/'stage3/first-run-test.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
 # These paths were produced only by the isolated fresh-data run; held originals are restored.
 for path in generated:shell('rm','-r',path)
 cold_start()
