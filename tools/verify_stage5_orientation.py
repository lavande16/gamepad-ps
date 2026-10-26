"""Verify landscape lock while the device requests portrait; restore system rotation."""
from verify_stage5 import *
import struct
saved=shell('wm','user-rotation').strip()
rotation=shell('settings','get','system','user_rotation').strip()
try:
 shell('wm','user-rotation','lock','0');time.sleep(1)
 current=snap('accepted-portrait-request-landscape-home')
 raw=P.joinpath('accepted-portrait-request-landscape-home.png').read_bytes()
 width,height=struct.unpack('>II',raw[16:24]);assert width>height,(width,height)
 assert sum(n.get('resource-id','').endswith('/card_game_grid') for n in current)==3
 tap_id('settings_button');time.sleep(.4)
 snap('accepted-portrait-request-landscape-settings')
 raw=P.joinpath('accepted-portrait-request-landscape-settings.png').read_bytes()
 settingsSize=struct.unpack('>II',raw[16:24]);assert settingsSize[0]>settingsSize[1]
 tap_text('图像');time.sleep(.4)
 snap('accepted-final-graphics')
 back();back()
 p=prefs();g=json.loads(p['config.global'])
 assert g['aspectRatio']==0 and g['orientation']==1 and g['upscaleFloat']==4 and g['lsfgEnabled']
 external=json.loads(adb('exec-out','cat /sdcard/GamePadPS/Settings/armsx2-defaults.json'))
 assert external['global']['aspectRatio']==0
 P.joinpath('final-landscape-defaults.json').write_text(json.dumps({'requestedDeviceRotation':0,'librarySize':[width,height],'settingsSize':settingsSize,'globalAspectRatio':g['aspectRatio'],'globalOrientation':g['orientation'],'externalAspectRatio':external['global']['aspectRatio'],'manualGameOverridesPreserved':[k for k in p if k.startswith('config.game.')],'originalRotation':saved},ensure_ascii=False,indent=2))
 print('Landscape-only and Stretch defaults verified in preferences, portable configuration and UI',flush=True)
finally:
 shell('wm','user-rotation','lock',rotation)
 if 'free' in saved.lower():shell('wm','user-rotation','free')
