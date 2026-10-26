"""Scoped real-device evidence collection for the fixed GamePad PS identity."""
from stage3_device import *
P=ROOT/'stage5';PACKAGE='com.gamepad.pspad'
def foreground():
 focus=shell('dumpsys','window')
 assert any(PACKAGE+'/' in l for l in focus.splitlines() if 'mCurrentFocus=' in l),'GamePad PS is not foreground'
def snap(name):
 foreground();P.joinpath(name+'.png').write_bytes(adb('exec-out','screencap -p'))
 current=nodes();P.joinpath(name+'.xml').write_bytes(adb('exec-out','cat /data/local/tmp/gamepad-ps-stage3.xml'))
 return current
def tap_text(label):
 foreground();current=nodes();node=next((n for n in current if n.get('text')==label or n.get('content-desc')==label),None)
 assert node is not None,'Missing UI control: '+label
 a,b,c,d=map(int,re.findall(r'\d+',node.get('bounds')))
 shell('input','tap',str(a+min(55,(c-a)//2)),str((b+d)//2));time.sleep(.35)
def tap_id(name):
 foreground();node=next(n for n in nodes() if n.get('resource-id','').endswith('/'+name));tap_node(node);time.sleep(.35)
def prefs():
 root=ET.fromstring(adb('exec-out','cat /data/user/0/'+PACKAGE+'/shared_prefs/ARMSX2.xml'))
 return {e.get('name'):e.text if e.tag=='string' else e.get('value') for e in root}
def back():
 foreground();shell('input','keyevent','KEYCODE_BACK');time.sleep(.4)
if __name__=='__main__':
 shell('am','start','-n',PACKAGE+'/com.armsx2.BootSplashActivity');time.sleep(3)
 current=snap('home-migrated')
 p=prefs();g=json.loads(p['config.global'])
 assert g['upscaleFloat']==4 and g['lsfgEnabled'] and g['lsfgMultiplier']==2
 assert g['osdShowMessages']==False
 assert not any(k.startswith('config.game.') for k in p)
 assert 'GamePad%20PS' not in p['romsDirs'] and 'GamePadPS' in p['romsDirs']
 report=json.loads(adb('exec-out','cat /data/user/0/'+PACKAGE+'/files/gamepad-ps/assembly-report.json'))
 assert report['ready'] and len(report['games'])==2
 P.joinpath('upgrade-readback.json').write_text(json.dumps({'global':g,'gameOverrides':[],'romsDirs':p['romsDirs'],'report':report},ensure_ascii=False,indent=2))
 print('Upgrade ready, unified defaults and two games verified')
 print([(n.get('text'),n.get('content-desc'),n.get('bounds')) for n in current if n.get('text') or n.get('content-desc')])
