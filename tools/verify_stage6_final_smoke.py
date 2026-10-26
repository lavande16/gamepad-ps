import verify_stage5 as v
v.P=v.ROOT/'stage6'
from verify_stage5 import *
import hashlib
shell('am','start','-n',PACKAGE+'/com.armsx2.BootSplashActivity');time.sleep(2)
cur=nodes();cards=[n for n in cur if n.get('resource-id','').endswith('/card_game_grid')];assert len(cards)==3
tap_node(cards[1]);time.sleep(6)
pid=shell('pidof',PACKAGE).strip();log=shell('logcat','-d','--pid='+pid,'-t','8000');assert 'Restored automatic state for SLPS-25837' in log
back();cur=snap('final-exit-confirm');assert any(n.get('resource-id','').endswith('/exit_button') for n in cur)
shell('input','keyevent','22');shell('input','keyevent','97');time.sleep(1)
cur=nodes();assert sum(n.get('resource-id','').endswith('/card_game_grid') for n in cur)==3
log=shell('logcat','-d','--pid='+pid,'-t','8000');P.joinpath('final-runtime.log').write_text(log);assert 'Autosave completed before shutdown: true' in log
snap('finished-home')
f=ROOT/'runtime/platforms/android/app/build/outputs/apk/github/release/app-github-release.apk';expected=hashlib.sha256(f.read_bytes()).hexdigest();path=shell('pm','path',PACKAGE).strip().splitlines()[0].removeprefix('package:');actual=shell('sha256sum',path).split()[0];assert actual==expected
P.joinpath('installed-artifact.json').write_text(json.dumps({'package':PACKAGE,'versionName':'0.4.1','versionCode':8,'installedSha256':actual,'matchesBuiltApk':True},indent=2))
d=json.loads(P.joinpath('validation.json').read_text());d['status']='passed';d['finalBinarySmoke']='Auto restore, Circle close, autosave and library return passed';d['priorCandidateFullRegression']='20-second paused boot, native list confirm, scoped menus, successful manual load, close and consecutive boot passed; final change only prevents delayed initial-focus override';d['evidence']['consecutiveBoot']='consecutive-boot-runtime.log';P.joinpath('validation.json').write_text(json.dumps(d,ensure_ascii=False,indent=2))
print('PASS final binary: automatic restore, Circle exit, autosave, library and APK hash',flush=True)
