"""Save and close only the current GamePad PS session before the authorized update."""
from verify_stage5 import *
P=ROOT/'stage6'
for attempt in range(9):
 cur=nodes()
 exitnode=next((n for n in cur if n.get('resource-id','').endswith('/exit_button')),None)
 if exitnode is not None:
  tap_node(exitnode);time.sleep(2);break
 close=next((n for n in cur if n.get('text')=='关闭游戏'),None)
 if close is not None:tap_node(close);time.sleep(.4);continue
 cs=[n for n in cur if n.get('resource-id','').endswith('/card_game_grid')]
 if len(cs)==3:
  tap_node(cs[1]);time.sleep(5)
 back()
else:raise AssertionError('Could not close session normally')
cur=nodes();assert sum(n.get('resource-id','').endswith('/card_game_grid') for n in cur)==3
pid=shell('pidof',PACKAGE).strip();log=shell('logcat','-d','--pid='+pid,'-t','2500');P.joinpath('preinstall-runtime.log').write_text(log)
assert 'Autosave completed before shutdown: true' in log
print('Current game saved and closed normally',flush=True)
