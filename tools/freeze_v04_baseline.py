"""Freeze the user-approved V0.4 binary, complete source and scoped evidence without device writes."""
from pathlib import Path
from datetime import datetime
from zoneinfo import ZoneInfo
import hashlib, json, os, shutil, tarfile

project=Path(__file__).resolve().parents[1]
workspace=project.parent
out=workspace/'baselines/GamePad-PS-V0.4'
assert not (out/'BASELINE.json').exists(),f'Sealed baseline already exists; refusing to overwrite: {out}'
record=json.loads((project/'stage5/artifact.json').read_text())
installed=json.loads((project/'stage5/installed-artifact.json').read_text())
assert record['package']=='com.gamepad.pspad' and record['versionName']=='0.4.0' and record['versionCode']==7
assert record['unitTests']=={'tests':201,'failures':0,'errors':0}
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for chunk in iter(lambda:f.read(1024*1024),b''):h.update(chunk)
 return h.hexdigest()
apk=Path(record['apk'])
assert sha(apk)==record['sha256']==installed['installedSha256']
out.mkdir(parents=True,exist_ok=True)
(out/'source').mkdir(exist_ok=True);(out/'evidence').mkdir(exist_ok=True);(out/'configuration').mkdir(exist_ok=True)
shutil.copy2(apk,out/'GamePad-PS-V0.4.apk')
shutil.copytree(project/'defaults',out/'configuration/defaults',dirs_exist_ok=True)
for name in ['README.md','IMPLEMENTATION_PLAN.md','MATERIALS.md','SOURCE_LOCK.md','REUSE_INVENTORY.md']:
 shutil.copy2(project/name,out/'source'/name)
# Preserve exact buildable sources, rather than relying on an app-only patch against upstream.
exclude={'.git','.gradle','.kotlin','.cxx','build','node_modules','__pycache__'}
files=[]
for root,dirs,names in os.walk(project/'runtime'):
 dirs[:]=sorted(d for d in dirs if d not in exclude)
 for name in sorted(names):
  p=Path(root)/name
  if name in {'local.properties','.DS_Store'} or not p.is_file():continue
  files.append(p)
files+=sorted(p for p in (project/'tools').rglob('*') if p.is_file() and '__pycache__' not in p.parts)
files+=sorted(p for p in (project/'defaults').rglob('*') if p.is_file())
manifest=[]
archive=out/'source/GamePad-PS-V0.4-source.tar.gz'
with tarfile.open(archive,'w:gz',compresslevel=3,dereference=True) as tar:
 for p in files:
  rel=p.relative_to(project).as_posix()
  manifest.append({'path':rel,'bytes':p.stat().st_size,'sha256':sha(p)})
  tar.add(p,arcname='gamepad-ps/'+rel,recursive=False)
(out/'source/source-files.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print(f'Archived {len(manifest)} source/config/tool files',flush=True)
# Verify every archived source entry, including assets and build scripts.
expected={'gamepad-ps/'+m['path']:m['sha256'] for m in manifest}
with tarfile.open(archive,'r:gz') as tar:
 seen=set()
 for m in tar:
  assert m.isfile() and m.name in expected,m.name
  h=hashlib.sha256(tar.extractfile(m).read()).hexdigest()
  assert h==expected[m.name],m.name
  seen.add(m.name)
 assert seen==expected.keys()
for name in ['artifact.json','installed-artifact.json','validation.json','final-resolution.json','final-landscape-defaults.json','reuse.json','popup-first-frame-analysis.json','build-final.log','final-device-check.log','final-orientation-check.log','accepted-runtime.log','upgrade-readback.json','accepted-delivery-home.png','accepted-pause-menu.png','accepted-back-dialog.png','accepted-final-textures.png','accepted-final-patches.png','accepted-final-graphics.png']:
 shutil.copy2(project/'stage5'/name,out/'evidence'/name)
metadata={
 'product':'GamePad PS','baseline':'GamePad PS V0.4','confirmedDate':'2026-10-21',
 'frozenAt':datetime.now(ZoneInfo('Asia/Shanghai')).isoformat(timespec='seconds'),
 'userAuthorization':'OK，把这个标为 GamePad PS V0.4基线',
 'applicationId':record['package'],'versionName':record['versionName'],'versionCode':record['versionCode'],
 'apk':'GamePad-PS-V0.4.apk','apkSha256':record['sha256'],'apkBytes':record['bytes'],
 'sourceArchive':'source/GamePad-PS-V0.4-source.tar.gz','sourceArchiveSha256':sha(archive),
 'sourceFiles':len(manifest),'sourceArchiveAllEntriesVerified':True,
 'runtimeCommit':record['runtimeCommit'],'nsCommit':record['nsCommit'],
 'sourceGitTag':None,'sourceState':'Complete source snapshot; extracted runtime has no Git repository',
 'unchangedEmulatorCoreFiles':record['unchangedEmulatorCoreFiles'],
 'defaults':{'rootDirectory':'GamePadPS','landscapeOnly':True,'aspectRatio':'Stretch','internalResolutionMultiplier':4,'LGFS':True,'LGFSMultiplier':2,'OSD':'Off','displayToggle':[2304,1296],'futureManualGameSettingsPreserved':True},
 'unitTests':record['unitTests'],'installedBinaryMatchesApk':True,
 'verification':'evidence/validation.json','deviceOperationsThisFreeze':False,
 'excludedFromSource':['Build outputs and caches','Machine-specific local.properties','Device BIOS, games, Lossless.dll and user save data'],
 'restore':'Install the frozen APK in place. For development extract source into a separate candidate directory, recreate SDK local.properties, then run gamepad-ps/tools/build_runtime.sh. Preserve the existing Android debug signing key to rebuild an in-place compatible APK.',
 'policy':'Do not overwrite this baseline. Later development uses a new candidate; package remains com.gamepad.pspad.'}
(out/'BASELINE.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
readme='''# GamePad PS V0.4 基线

用户于 2026-10-21 明确批准将当前版本标为基线。

- APK：GamePad-PS-V0.4.apk；包名 com.gamepad.pspad；APK 版本 0.4.0 / 7。
- 固定全程横屏、Stretch 全屏、4 倍内部渲染、LGFS 2 倍、OSD Off；显示尺寸在 2304×1296 与设备原始尺寸之间切换。
- 数据根目录 GamePadPS；支持 game 下直接镜像和游戏子目录，兼容旧 ISO 子目录。
- NS 复用界面、深灰图标、像素控件、○ 确认/× 返回、L3+R3 暂停、HOME 暂停恢复、自动保存退出与下次自动读取均随版本固定。
- 201 项单元测试通过；安装包与归档 APK 哈希一致。具体真机验证和边界见 evidence/validation.json；基线批准不扩展已有验证范围。

完整源代码、工具与默认配置在 source/GamePad-PS-V0.4-source.tar.gz，逐文件校验在 source/source-files.json。构建缓存与机器 SDK 路径不归档；BIOS、游戏、DLL、设备配置和存档不在此次冻结范围。

恢复优先原位安装本目录 APK；开发请将源码解压到新候选目录，补充 Android SDK local.properties 后运行 tools/build_runtime.sh。重新构建并覆盖安装仍需原 Android debug 签名密钥。

本目录固定后不再覆盖。后续开发以此为起点，创建新候选版本。校验方法：在本目录执行 `shasum -a 256 -c SHA256SUMS`。
'''
(out/'README.md').write_text(readme)
entries=sorted(p for p in out.rglob('*') if p.is_file())
(out/'SHA256SUMS').write_text(''.join(f'{sha(p)}  {p.relative_to(out).as_posix()}\n' for p in entries))
# Root pointer is updated only after all binary/source archive checks succeeded.
pointer=f'''# GamePad PS 当前基线

当前基线：**GamePad PS V0.4**。用户于 2026-10-21 明确批准。

- 冻结目录：`baselines/GamePad-PS-V0.4/`
- 固定 APK：`baselines/GamePad-PS-V0.4/GamePad-PS-V0.4.apk`
- 包名：`com.gamepad.pspad`；versionName `0.4.0`；versionCode `7`。
- APK SHA-256：`{record['sha256']}`。
- 完整源码归档、配置、验证记录及逐文件校验均在冻结目录内，入口 `BASELINE.json`。
- 本轮仅归档，不重新打包、不安装、不修改设备配置和存档。

默认：横屏、Stretch 拉伸全屏、4 倍渲染、LGFS 2 倍、OSD Off；根目录 GamePadPS；2304×1296 与原始分辨率往返时保持卡带尺寸。

后续 GamePad PS 修改以此基线为起点，创建新候选版本；不覆盖冻结目录，包名继续固定。真机自动检查与 201 项测试证据已保留，实体手柄与长时间实玩的验证范围以原验收记录为准。
'''
(workspace/'CURRENT_GAMEPAD_PS_BASELINE.md').write_text(pointer)
readmePath=project/'README.md';before=readmePath.read_text();readmePath.write_text(before.replace('# GamePad PS\n','# GamePad PS\n\n当前用户批准基线：**GamePad PS V0.4**（2026-10-21），见 `../CURRENT_GAMEPAD_PS_BASELINE.md`。后续开发从该基线继续，固定归档不得覆盖。\n',1))
print(json.dumps({'baseline':metadata['baseline'],'path':str(out),'apkSha256':record['sha256'],'sourceFiles':len(manifest),'allSourceEntriesVerified':True},ensure_ascii=False,indent=2))
