"""One-time live transfer into the newly installed identity; no routine backup/snapshot."""
import sys,re,shlex,json,subprocess
sys.path.insert(0,str(__import__('pathlib').Path(__file__).parent))
from stage3_device import adb,shell,ADB,SERIAL
OLD='com.gamepad.gps';NEW='com.gamepad.pspad'
uid=re.search(r'(?:userId|appId)=(\d+)',shell('dumpsys','package',NEW)).group(1)
# Old UI has returned to the library; stop the idle app before copying its preference files.
shell('am','force-stop',OLD)
for old,new in [(f'/data/user/0/{OLD}/files',f'/data/user/0/{NEW}/files'),(f'/data/user/0/{OLD}/shared_prefs',f'/data/user/0/{NEW}/shared_prefs'),(f'/storage/emulated/0/Android/data/{OLD}/files',f'/storage/emulated/0/Android/data/{NEW}/files')]:
 shell('mkdir','-p',new);shell('cp','-R',old+'/.',new+'/')
roots=[f'/data/user/0/{NEW}/files',f'/data/user/0/{NEW}/shared_prefs',f'/storage/emulated/0/Android/data/{NEW}/files']
changed=0
for root in roots:
 for path in shell('find',root,'-type','f').splitlines():
  if not path.endswith(('.xml','.json','.ini')):continue
  b=adb('exec-out',shlex.join(['cat',path]))
  # JSON serializers may escape slashes, so replace only the identity token, not entire paths.
  after=b.replace(OLD.encode(),NEW.encode())
  if b!=after:
   subprocess.run([ADB,'-s',SERIAL,'shell',shlex.join(['sh','-c','cat > '+shlex.quote(path)])],input=after,check=True);changed+=1
shell('chown','-R',uid+':'+uid,f'/data/user/0/{NEW}/files',f'/data/user/0/{NEW}/shared_prefs')
# API37 restorecon on this ROM can lose per-app MCS categories; use the
# installed app's own cache label and fix the copied external files' owner too.
label=re.search(r'u:object_r:app_data_file:s0[^ ]*',shell('ls','-Zd',f'/data/user/0/{NEW}')).group(0)
for name in ['files','shared_prefs']:
 shell('chcon','-R',label,f'/data/user/0/{NEW}/'+name)
shell('chown','-R',uid,f'/storage/emulated/0/Android/data/{NEW}/files')
shell('appops','set',NEW,'MANAGE_EXTERNAL_STORAGE','allow')
shell('am','start','-n',NEW+'/com.armsx2.BootSplashActivity')
print(json.dumps({'source':OLD,'target':NEW,'rewrittenPathFiles':changed,'sharedGameDirectoryUntouched':True,'oldPackageStopped':True,'routineBackupsCreated':False}))
