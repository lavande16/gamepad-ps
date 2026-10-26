"""Normal real-device UI acceptance, scoped to GamePad PS's new package."""
from stage3_device import *
P=ROOT/'stage4b'
PACKAGE='com.gamepad.pspad'
def foreground():
 focus=shell('dumpsys','window')
 assert any(PACKAGE+'/' in l for l in focus.splitlines() if 'mCurrentFocus=' in l),'New GamePad PS is not foreground'
def snap(name):
 P.joinpath(name+'.png').write_bytes(adb('exec-out','screencap -p'))
 current=nodes();P.joinpath(name+'.xml').write_bytes(adb('exec-out','cat /data/local/tmp/gamepad-ps-stage3.xml'))
 return current
def tap_text(text):
 foreground();current=nodes();n=next((n for n in current if n.get('text')==text or n.get('content-desc')==text),None)
 assert n is not None,text
 a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds')))
 shell('input','tap',str(a+min(50,(c-a)//2)),str((b+d)//2));time.sleep(.4)
def tap_id(name):
 foreground();n=next(n for n in nodes() if n.get('resource-id','').endswith('/'+name));tap_node(n);time.sleep(.5)
def game_card(serial_index=1):
 foreground();cards=[n for n in nodes() if n.get('resource-id','').endswith('/card_game_grid')];tap_node(cards[serial_index]);time.sleep(.7)
if __name__=='__main__':
 foreground();tap_text('添加游戏');snap('add-game-dialog');tap_text('知道了')
 shell('input','swipe','1100','200','1100','640','650');time.sleep(3);snap('refresh-home')
 print('covers',shell('ls','-l','/storage/emulated/0/GamePad PS/Covers'))
 game_card();time.sleep(9);snap('game-running')
 shell('input','keyevent','KEYCODE_BACK');time.sleep(.5);snap('back-dialog')
 tap_text('继续');time.sleep(1)
 shell('input','keycombination','-t','200','106','107');time.sleep(.8);snap('stick-menu')
 print('menu labels',[n.get('text') for n in nodes() if n.get('text')][:12])
 tap_text('继续游戏');time.sleep(1)
 # HOME surface logo, observed original NS geometry; this is a UI test, not game-input implementation.
 shell('input','tap','2113','94');time.sleep(.7);snap('home-paused')
 game_card();time.sleep(1);snap('home-resumed')
 shell('input','keyevent','KEYCODE_BACK');time.sleep(.5);tap_text('退出模拟')
 time.sleep(4);snap('after-autosave-exit')
 print('states',shell('ls','-l','/storage/emulated/0/GamePad PS/game/究极觉醒2/Sstates'))
 game_card();time.sleep(6);snap('autoload-resumed')
 pid=shell('pidof',PACKAGE).strip();log=shell('logcat','-d','--pid='+pid,'-t','4000');P.joinpath('runtime.log').write_text(log)
 print('\n'.join(l for l in log.splitlines() if any(k in l for k in ['GamePadPS-Session','GamePadPS-Cover','@@ANDROID_LAUNCH_GAME@@'])))
