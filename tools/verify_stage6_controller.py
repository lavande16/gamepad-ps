import verify_stage5 as v
v.P=v.ROOT/'stage6'
from verify_stage5 import *
def press(*keys):
 for key in keys:shell('input','keyevent',str(key));time.sleep(.15)
 time.sleep(.35)
def has(cur,text):return any(n.get('text')==text for n in cur)
def menu():
 shell('input','keycombination','-t','200','106','107');time.sleep(.6)
 assert has(nodes(),'关闭游戏')
# Current test stopped with the early-boot Continue/Exit dialog visible.
cur=nodes();assert any(n.get('resource-id','').endswith('/continue_button') for n in cur)
press(97);time.sleep(1)
assert not has(nodes(),'退出模拟')
print('Circle operates the game exit modal (Continue)',flush=True)
menu();press(97);cur=snap('controller-osd-dialog');assert has(cur,'Off')
press(97);cur=nodes();assert has(cur,'关闭游戏') and not has(cur,'Off')
print('OSD native list accepts Circle and dismisses',flush=True)
press(22,97);cur=snap('controller-game-settings');assert has(cur,'性能')
press(97);cur=snap('controller-performance');assert has(cur,'性能') and not has(cur,'关闭游戏')
press(96);assert has(nodes(),'图像')
press(96);assert has(nodes(),'关闭游戏')
print('Scoped settings Circle-enter/Cross-back passed',flush=True)
press(20,22,97);cur=snap('controller-load-confirm');assert has(cur,'使用当前槽位读取？')
press(22,97);time.sleep(1);cur=snap('controller-load-complete')
assert not has(cur,'使用当前槽位读取？') and not has(cur,'关闭游戏')
print('Circle confirms manual load and returns to game',flush=True)
menu();press(20,20,22,97);cur=snap('controller-close-confirm')
assert any(n.get('resource-id','').endswith('/exit_button') for n in cur)
press(22,97);time.sleep(2);cur=snap('controller-close-home')
assert sum(n.get('resource-id','').endswith('/card_game_grid') for n in cur)==3
print('Circle confirms close and returns to library',flush=True)
