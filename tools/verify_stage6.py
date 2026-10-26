"""Real-device regression for V0.4.1: actual controller key routing, pause-deferred restore and defaults."""
import verify_stage5 as v
v.P=v.ROOT/'stage6'
from verify_stage5 import *

def press(*keys):
 for key in keys:shell('input','keyevent',str(key));time.sleep(.15)
 time.sleep(.35)
def has(current,text):return any(n.get('text')==text for n in current)
def logs():return shell('logcat','-d','--pid='+shell('pidof',PACKAGE).strip(),'-t','10000')
def menu():
 shell('input','keycombination','-t','200','106','107');time.sleep(.5)
 cur=nodes();assert has(cur,'关闭游戏') and has(cur,'读取存档'),'Pause menu missing'
 return cur
def wait_restored(previous):
 for attempt in range(40):
  log=logs()
  if log.count('Restored automatic state for SLPS-25837')>previous:return log
  time.sleep(.5)
 raise AssertionError('Automatic restore missing after resume')

shell('am','start','-n',PACKAGE+'/com.armsx2.BootSplashActivity');time.sleep(3)
for attempt in range(5):
 cur=nodes();cards=[n for n in cur if n.get('resource-id','').endswith('/card_game_grid')]
 if len(cards)==3:break
 time.sleep(1)
assert len(cards)==3
p=json.loads(P.joinpath('private-defaults-probe.json').read_text())['prefs'];g=json.loads(p['config.global']);expected={'textureUpscale':2,'precacheTextureReplacements':True,'gsBackThreadMode':1,'fpsLimit':60}
for k,value in expected.items():assert g[k]==value,(k,g.get(k))
pergames={k.removeprefix('config.game.'):json.loads(value) for k,value in p.items() if k.startswith('config.game.')}
for serial in ['SLPS-25589','SLPS-25837']:
 effective=g|pergames.get(serial,{})
 for k,value in expected.items():assert effective[k]==value,(serial,k,effective[k])
external=json.loads(adb('exec-out','cat /sdcard/GamePadPS/Settings/armsx2-defaults.json'))
for layer in [external['global'],*external.get('games',{}).values()]:
 for k,value in expected.items():assert layer[k]==value
P.joinpath('defaults-readback.json').write_text(json.dumps({'global':{k:g[k] for k in expected},'gameOverrides':pergames,'allTwoGameEffectiveValuesMatch':True,'externalDefaultsMatch':True,'migrationApplied':p.get('gamepad.ps.forcedDefaults.v041')},ensure_ascii=False,indent=2))
print('Four defaults verified globally, in existing overrides, both games and portable settings',flush=True)

before=logs().count('Restored automatic state for SLPS-25837')
tap_node(cards[1]);time.sleep(.1)
# Pause during boot before game identity/readiness is available, exceeding the previous 15s limit.
shell('input','keyevent','4');time.sleep(.4);cur=snap('boot-paused-dialog');assert has(cur,'退出模拟')
print('Boot paused; holding for 20s to test previous timeout',flush=True)
time.sleep(20)
mid=logs();assert mid.count('Restored automatic state for SLPS-25837')==before,'Boot restored before paused test window'
press(97)  # Circle on the actual dialog Continue button; no touch fallback.
log=wait_restored(before);P.joinpath('deferred-restore-runtime.log').write_text(log)
assert 'Boot restore waiting:' in log and 'state=PAUSED' in log
print('Circle resumed boot; automatic restore succeeded after >20s pause',flush=True)

menu();press(97);cur=snap('osd-dialog');assert has(cur,'Off')
press(97);cur=nodes();assert has(cur,'关闭游戏') and not has(cur,'Off'),'OSD native list confirm failed'
print('OSD list accepts Circle and returns to pause menu',flush=True)

# Enter scoped settings with Circle, then return with Cross.
press(22,97);cur=snap('controller-game-settings');assert has(cur,'性能')
press(97);cur=snap('controller-performance');assert has(cur,'性能') and not has(cur,'关闭游戏')
press(96);assert has(nodes(),'图像')
press(96);assert has(nodes(),'关闭游戏')
print('Scoped settings enter and back navigation verified',flush=True)

# Menus return with their default focus on Screen Display.
press(20,22,97);cur=snap('controller-load-confirm');assert has(cur,'使用当前槽位读取？')
press(22,97);time.sleep(1)
cur=snap('controller-load-complete');assert not has(cur,'使用当前槽位读取？') and not has(cur,'关闭游戏'),'Load did not dismiss/resume'
assert 'Manual state operation: saving=false slot=0 success=true' in logs()
print('Circle confirmed successful manual load through the dialog window',flush=True)

menu();press(20,20,22,97);cur=snap('controller-close-confirm');assert any(n.get('resource-id','').endswith('/exit_button') for n in cur)
press(22,97);time.sleep(2)
cur=snap('controller-close-home');assert sum(n.get('resource-id','').endswith('/card_game_grid') for n in cur)==3
log=logs();assert 'Autosave completed before shutdown: true' in log
print('Circle confirmed close; automatic save and return to library verified',flush=True)

# Consecutive boot restores once and confirms no prior boot job can run later.
before=log.count('Restored automatic state for SLPS-25837')
cards=[n for n in cur if n.get('resource-id','').endswith('/card_game_grid')];tap_node(cards[1])
log=wait_restored(before);time.sleep(1);assert logs().count('Restored automatic state for SLPS-25837')==before+1
back();cur=snap('second-boot-exit-confirm');assert any(n.get('resource-id','').endswith('/exit_button') for n in cur)
press(22,97);time.sleep(2)
cur=snap('finished-home');assert sum(n.get('resource-id','').endswith('/card_game_grid') for n in cur)==3
P.joinpath('final-runtime.log').write_text(logs())
print('Second boot restored once; final session saved and library restored',flush=True)
