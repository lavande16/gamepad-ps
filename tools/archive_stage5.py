"""Archive the complete Android delta against the pinned runtime and verify the emulator core."""
from pathlib import Path
import json,hashlib,tarfile,difflib,xml.etree.ElementTree as E,zipfile,shutil
r=Path(__file__).resolve().parents[1]; app=r/'runtime/platforms/android/app'; out=r/'stage5'; dest=r.parent/'artifacts/gamepad-ps-stage5-20261021';dest.mkdir(exist_ok=True)
sha=lambda b:hashlib.sha256(b).hexdigest()
files={str(p.relative_to(r/'runtime')):p for p in app.rglob('*') if p.is_file() and not any(x in p.relative_to(app).parts for x in ['build','.cxx','.gradle'])}
changes=[];deletions=[];patch=[];core=0
with tarfile.open(r/'stage1/references/ARMSX2-25e7084-source.tar.gz') as t:
 prefix=t.getmembers()[0].name.split('/')[0]+'/'
 for m in t:
  if not m.isfile():continue
  rel=m.name.removeprefix(prefix)
  if rel.startswith(('pcsx2/','common/')):
   assert t.extractfile(m).read()==(r/'runtime'/rel).read_bytes(),rel
   core+=1
  if not rel.startswith('platforms/android/app/'):continue
  before=t.extractfile(m).read();p=files.pop(rel,None)
  if p is None:deletions.append(rel);continue
  after=p.read_bytes()
  if before==after:continue
  changes.append({'path':rel,'sha256':sha(after),'sourceSha256':sha(before)})
  try:patch.extend(difflib.unified_diff(before.decode().splitlines(True),after.decode().splitlines(True),fromfile='a/'+rel,tofile='b/'+rel))
  except UnicodeDecodeError:pass
for rel,p in files.items():
 after=p.read_bytes();changes.append({'path':rel,'sha256':sha(after),'added':True})
 try:patch.extend(difflib.unified_diff([],after.decode().splitlines(True),fromfile='/dev/null',tofile='b/'+rel))
 except UnicodeDecodeError:pass
(out/'runtime-changes.patch').write_text(''.join(patch));(out/'runtime-changes.json').write_text(json.dumps({'files':changes,'deletions':deletions,'unchangedCoreFiles':core},ensure_ascii=False,indent=2))
with tarfile.open(out/'runtime-source-delta.tar.gz','w:gz') as t:
 for c in changes:t.add(r/'runtime'/c['path'],arcname=c['path'])
 for name in ['runtime-changes.json','validation.json']:t.add(out/name,arcname='stage5/'+name)
src=app/'build/outputs/apk/github/release/app-github-release.apk';apk=dest/'GamePad-PS-0.4.0.apk';shutil.copy2(src,apk)
rs=[E.parse(p).getroot() for p in (app/'build/test-results/testGithubReleaseUnitTest').glob('TEST-*.xml')]
tests={k:sum(int(e.get(k,'0')) for e in rs) for k in ['tests','failures','errors']};assert tests['tests']>=201 and tests['failures']==tests['errors']==0,tests
with zipfile.ZipFile(apk) as z:assert not any(n.lower().endswith(('.iso','.ps2','lossless.dll')) for n in z.namelist())
record={'package':'com.gamepad.pspad','versionName':'0.4.0','versionCode':7,'apk':str(apk),'sha256':sha(apk.read_bytes()),'bytes':apk.stat().st_size,'runtimeCommit':'25e7084fdefcf1e9069481a583c38c103b03e8f1','nsCommit':'0f2249a7026f9ea2e90a6891125b3537a9f53491','unchangedEmulatorCoreFiles':core,'unitTests':tests,'device':'403G7R006F310V0 / iPA2691','scope':'Stage 5: unified global defaults, GamePadPS migration, direct image discovery, first-frame centered exit, background save, shared NS pixel UI, stable covers, stable 2304x1296 library, Circle-confirm controller navigation, landscape-only and Stretch defaults','biosIsoCardsLosslessNotInApk':True}
(out/'artifact.json').write_text(json.dumps(record,ensure_ascii=False,indent=2));(dest/'artifact.json').write_text(json.dumps(record,ensure_ascii=False,indent=2));print(json.dumps(record,ensure_ascii=False,indent=2))
