from verify_stage5 import *

def bounds(n):return list(map(int,re.findall(r'\d+',n.get('bounds'))))
def cards(current):return [bounds(n) for n in current if n.get('resource-id','').endswith('/card_game_grid')]
def dims(c):return [(v[2]-v[0],v[3]-v[1]) for v in c]

def verify_resolution():
 shell('am','start','-n',PACKAGE+'/com.armsx2.BootSplashActivity');time.sleep(2)
 for attempt in range(6):
  base=snap('accepted-home-native');before=cards(base)
  if len(before)==3:break
  time.sleep(1)
 assert len(before)==3, 'Library not ready after waiting'
 p=prefs();g=json.loads(p['config.global']);assert g['upscaleFloat']==4 and g['lsfgEnabled'] and g['lsfgMultiplier']==2 and not g['osdShowMessages']
 assert g['aspectRatio']==0 and g['orientation']==1
 n=next(n for n in base if n.get('resource-id','').endswith('/information_button'));tap_node(n);time.sleep(.8)
 wm=shell('wm','size');assert 'Override size: 1296x2304' in wm,wm
 target=snap('accepted-home-2304x1296');after=cards(target)
 assert dims(before)==dims(after),(dims(before),dims(after))
 n=next(n for n in target if n.get('resource-id','').endswith('/information_button'));tap_node(n);time.sleep(.8)
 assert 'Override size:' not in shell('wm','size')
 current=snap('accepted-home-restored');assert dims(cards(current))==dims(before)
 result={'nativeCards':before,'targetCards':after,'sameCardSizes':True,'target':[2304,1296],'restoredOriginal':True,'globalUpscale':g['upscaleFloat'],'lgfsEnabled':g['lsfgEnabled'],'lgfsMultiplier':g['lsfgMultiplier'],'manualGameOverrides':[k for k in p if k.startswith('config.game.')],'stretchDefault':g['aspectRatio']==0}
 P.joinpath('final-resolution.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
 print('Verified native -> 2304x1296 -> native; cartridge dimensions unchanged',dims(before),flush=True)

def verify_circle_and_session():
 foreground();pid=shell('pidof',PACKAGE).strip()
 def launches():return shell('logcat','-d','--pid='+pid,'-s','System.out:I','-t','2000').count('@@ANDROID_LAUNCH_GAME@@')
 count=launches();shell('input','keyevent','96');time.sleep(.3)
 assert launches()==count,'Cross unexpectedly launched a game'
 shell('input','keyevent','97');time.sleep(7)
 assert launches()==count+1,'Circle did not launch game'
 snap('accepted-circle-launched')
 shell('input','keycombination','-t','200','106','107');time.sleep(.4)
 current=snap('accepted-pause-menu')
 labels=[n.get('text') for n in current if n.get('text')]
 assert all(x in labels for x in ['屏幕显示','游戏设置','保存存档','读取存档','继续游戏','关闭游戏']),labels
 tap_text('继续游戏');time.sleep(.4)
 # Return to library while preserving this exact VM, then resume the same card.
 foreground();shell('input','tap','2113','94');time.sleep(.5)
 current=snap('accepted-home-paused');assert len(cards(current))==3
 n=[n for n in current if n.get('resource-id','').endswith('/card_game_grid')][0];tap_node(n);time.sleep(.5)
 assert launches()==count+1,'HOME resume booted a new VM'
 back();current=snap('accepted-back-dialog')
 assert any(n.get('text')=='退出模拟' for n in current)
 tap_id('exit_button');time.sleep(1)
 current=snap('accepted-exit-library');assert len(cards(current))==3
 log=shell('logcat','-d','--pid='+pid,'-t','4000');P.joinpath('accepted-runtime.log').write_text(log)
 assert 'Autosave completed before shutdown: true' in log
 # Same game's next boot must load its new dedicated autosave.
 n=[n for n in current if n.get('resource-id','').endswith('/card_game_grid')][0];tap_node(n);time.sleep(7)
 log=shell('logcat','-d','--pid='+pid,'-t','4000');P.joinpath('accepted-runtime.log').write_text(log)
 assert 'Restored automatic state for SLPS-25589' in log
 snap('accepted-autoload')
 back();tap_id('exit_button');time.sleep(1);snap('accepted-finished-home')
 print('Verified Cross back, Circle launch, pause menu, HOME resume, save/exit and automatic restore',flush=True)
if __name__=='__main__':
 verify_resolution();verify_circle_and_session()
