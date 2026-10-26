from verify_stage5 import *
P=ROOT/'stage6'
shell('input','keycombination','-t','200','106','107');time.sleep(.6)
cur=nodes()
assert any(n.get('text')=='读取存档' for n in cur),[(n.get('text'),n.get('bounds')) for n in cur if n.get('text')]
shell('input','keyevent','20');shell('input','keyevent','22');shell('input','keyevent','97');time.sleep(.3)
cur=nodes();P.joinpath('v04-controller-dialog-before.xml').write_bytes(adb('exec-out','cat /data/local/tmp/gamepad-ps-stage3.xml'))
print('First Circle:',[(n.get('text'),n.get('focused')) for n in cur if n.get('text')],flush=True)
shell('input','keyevent','22');shell('input','keyevent','97');time.sleep(.5)
cur=nodes();P.joinpath('v04-controller-dialog-after.xml').write_bytes(adb('exec-out','cat /data/local/tmp/gamepad-ps-stage3.xml'))
print('Circle in dialog:',[(n.get('text'),n.get('focused')) for n in cur if n.get('text')],flush=True)
