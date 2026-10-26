"""Archive a V0.4.1 candidate delta; the approved V0.4 baseline is never modified."""
from pathlib import Path
import os,json,hashlib,tarfile,shutil,xml.etree.ElementTree as E,zipfile
r=Path(__file__).resolve().parents[1];out=r/'stage6';dest=r.parent/'artifacts/gamepad-ps-v0.4.1-20261021';dest.mkdir(exist_ok=True)
base=r.parent/'baselines/GamePad-PS-V0.4'
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()
validation=json.loads((out/'validation.json').read_text());assert validation['status'] in {'passed','delivered-with-verification-limit'}
old={m['path']:m for m in json.loads((base/'source/source-files.json').read_text())}
skip={'.git','.gradle','.kotlin','.cxx','build','node_modules','__pycache__'};files=[]
for root,dirs,names in os.walk(r/'runtime'):
 dirs[:]=[d for d in dirs if d not in skip]
 for name in names:
  p=Path(root)/name
  if name not in {'local.properties','.DS_Store'} and p.is_file():files.append(p)
files+=list(p for p in (r/'tools').rglob('*') if p.is_file() and '__pycache__' not in p.parts)
files+=list(p for p in (r/'defaults').rglob('*') if p.is_file())
changed=[];present=set();core=0
for p in sorted(files):
 rel=p.relative_to(r).as_posix();present.add(rel);digest=sha(p);previous=old.get(rel,{}).get('sha256')
 if rel.startswith(('runtime/pcsx2/','runtime/common/')):
  assert digest==previous,rel
  core+=1
 if digest!=previous:changed.append({'path':rel,'sha256':digest,'baselineSha256':previous})
deleted=sorted(set(old)-present)
(out/'source-delta.json').write_text(json.dumps({'baseline':'GamePad PS V0.4','changed':changed,'deleted':deleted,'unchangedCoreFiles':core},indent=2))
with tarfile.open(dest/'GamePad-PS-V0.4.1-source-delta.tar.gz','w:gz',dereference=True) as t:
 for m in changed:t.add(r/m['path'],arcname='gamepad-ps/'+m['path'])
 for name in ['README.md','IMPLEMENTATION_PLAN.md']:t.add(r/name,arcname='gamepad-ps/'+name)
 for name in ['source-delta.json','validation.json']:t.add(out/name,arcname='stage6/'+name)
apk=r/'runtime/platforms/android/app/build/outputs/apk/github/release/app-github-release.apk';shutil.copy2(apk,dest/'GamePad-PS-0.4.1.apk')
rs=[E.parse(p).getroot() for p in (r/'runtime/platforms/android/app/build/test-results/testGithubReleaseUnitTest').glob('TEST-*.xml')]
tests={k:sum(int(x.get(k,'0')) for x in rs) for k in ['tests','failures','errors']};assert tests['tests']>=208 and tests['failures']==tests['errors']==0
with zipfile.ZipFile(apk) as z:assert not any(n.lower().endswith(('.iso','.ps2','lossless.dll')) for n in z.namelist())
record={'product':'GamePad PS','versionName':'0.4.1','versionCode':8,'package':'com.gamepad.pspad','apk':str(dest/'GamePad-PS-0.4.1.apk'),'sha256':sha(apk),'bytes':apk.stat().st_size,'base':'GamePad PS V0.4','approvedBaselineUnchanged':True,'sourceDeltaFiles':len(changed),'unchangedCoreFiles':core,'unitTests':tests,'status':validation['status'],'defaults':{'textureUpscale':2,'precacheTextureReplacements':True,'gsBackThreadMode':1,'fpsLimit':60,'forcedGloballyAndAllExistingGameOverrides':True,'oneTimeMigration':True}}
(out/'artifact.json').write_text(json.dumps(record,ensure_ascii=False,indent=2));shutil.copy2(out/'artifact.json',dest/'artifact.json')
for name in ['validation.json','private-defaults-probe.json','defaults-readback.json','device-check.log','final-runtime.log','installed-artifact.json']:
 shutil.copy2(out/name,dest/name)
(dest/'SHA256SUMS').write_text(''.join(f'{sha(p)}  {p.name}\n' for p in sorted(dest.iterdir()) if p.is_file() and p.name!='SHA256SUMS'))
print(json.dumps(record,ensure_ascii=False,indent=2))
