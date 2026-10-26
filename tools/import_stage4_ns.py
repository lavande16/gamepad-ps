"""Import the frozen NS components and their actual resource closure; never read the live NS checkout."""
from pathlib import Path
import re,hashlib,json,xml.etree.ElementTree as E,shutil
root=Path(__file__).resolve().parents[1]; src=root/'stage4/reference/src/android/app/src/main'; dst=root/'runtime/platforms/android/app/src/main'
records=[]
(dst/"res/values/gamepad_ps_ns_components.xml").unlink(missing_ok=True)
def record(p,q,data,reason='Namespace adaptation only'):
 q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(data)
 records.append(dict(source=str(p.relative_to(src)),sourceSha256=hashlib.sha256(p.read_bytes()).hexdigest(),target=str(q.relative_to(dst)),targetSha256=hashlib.sha256(data).hexdigest(),changes=reason))
code=['views/'+n+'.kt' for n in ['GameFoldShellLayout','GameFoldControllerInput','GamePadControllerVisibilityState','GameFoldViewportLayout','GameFoldViewportCalculator','PixelWordmarkView','PixelNumberView','AspectRatioFrameLayout','SquareImageView']]+['utils/'+n+'.kt' for n in ['GamePadNsProductContract','GamePadNsWindowPolicy','GamePadNsDisplayMode','GamePadNsHomeGeometry','GameFoldDialogSizing']]+['layout/MidScreenSwipeRefreshLayout.kt']
texts=[]
for n in code:
 p=src/'java/org/yuzu/yuzu_emu'/n;s=p.read_text().replace('import org.yuzu.yuzu_emu.R','import com.armsx2.R')
 if n.endswith('GamePadNsProductContract.kt'):
  s=s.replace('com.gamepad.gamepad','com.gamepad.ps').replace('"1.8"','"0.3.0"').replace('10030','3').replace('"GamePadNS"','"GamePad PS"')
 record(p,dst/'java/org/yuzu/yuzu_emu'/n,s.encode(), 'R namespace; product identity for ProductContract only')
 texts.append(s)
# Existing stage3 styles are expanded from their original definitions, rather than duplicated.
for n in ['gamepad_ps_colors.xml','gamepad_ps_dimens.xml','gamepad_ps_styles.xml']:
 (dst/'res/values'/n).unlink(missing_ok=True)
res=src/'res';target=dst/'res';index={};files={}
for p in res.glob('values/*.xml'):
 try:r=E.parse(p).getroot()
 except E.ParseError:continue
 for e in r:
  if e.get('name'):index[(e.tag,e.get('name'))]=(p,e)
for p in res.glob('*/*'):
 if p.is_file() and not p.parent.name.startswith('values'):files.setdefault((p.parent.name.split('-')[0],p.stem),[]).append(p)
queue=[];seen=set();values=[]
def refs(s):
 for typ,name in re.findall(r'@\+?(\w+)/([\w.]+)',s):
  if typ not in ['id','android']:queue.append((typ,name))
 for typ,name in re.findall(r'R\.(drawable|raw|color|string|dimen|style|fraction)\.([\w]+)',s):queue.append((typ,name))
 for name in re.findall(r'R.styleable.(Pixel\w+?)_',s):queue.append(('declare-styleable',name))
for s in texts:refs(s)
layouts=['fragment_games','fragment_home_settings','fragment_settings','card_game_grid','card_game_list','card_game_carousel','list_item_setting','list_item_setting_switch','list_item_settings_header','dialog_list','dialog_slider','dialog_edit_text','dialog_spinbox','dialog_license','dialog_system_info','dialog_emulation_exit_pixel','card_home_option']
queue += [('layout',n) for n in layouts]+[('declare-styleable','PixelWordmarkView'),('id','gamefold_display_stage'),('drawable','pixel_dialog_background'),('drawable','pixel_focus_ring_overlay'),('layout','card_game_grid_compact')]
# All existing pixel styles are the approved visual vocabulary.
queue += [k for k in index if k[0]=='style' and k[1].startswith('GameFoldPixel')]
# Preserve complete original palette and spacing, including stage3 assembly dependencies.
queue += [k for k,(p,e) in index.items() if p.name in ['pixel_colors.xml','pixel_dimens.xml','pixel_attrs.xml']]
while queue:
 key=queue.pop(0)
 if key in seen:continue
 seen.add(key);typ,name=key
 if key in files:
  for p in files[key]:
   data=p.read_bytes()
   if p.suffix=='.xml':
    s=data.decode();refs(s)
    # Product identity only; geometry/attributes retained.
    if name=='fragment_games':s=s.replace('GAMEPAD NS','GAMEPAD PS').replace('@drawable/gamefold_ns_launcher_icon','@drawable/gamepad_ps_launcher_icon')
    data=s.encode()
   record(p,target/p.relative_to(res),data,'Direct resource reuse; home brand only adapted')
 elif key in index:
  p,e=index[key];text=E.tostring(e,encoding='unicode');refs(text)
  parent=e.get('parent')
  if typ=='style' and parent and not parent.startswith('@'):queue.append(('style',parent))
  if typ=='style' and parent is None and '.' in name:queue.append(('style',name.rsplit('.',1)[0]))
  if typ=='string' and name=='gamefold_home_brand_name':e.text='GAMEPAD PS'
  # Avoid importing generic app strings already owned by ARMSX2.
  exists=False
  for q in target.glob('values/*.xml'):
   try:exists=any(c.get('name')==name and c.tag==typ for c in E.parse(q).getroot())
   except E.ParseError:pass
   if exists:break
  if not exists:values.append(e);records.append(dict(source=str(p.relative_to(src))+'#'+name,sourceSha256=hashlib.sha256(p.read_bytes()).hexdigest(),target='res/values/gamepad_ps_ns_components.xml#'+name,changes='Original element; home brand string adapted'))
 elif typ=='id':
  if name=='gamefold_display_stage':values.append(E.Element('item',{'name':name,'type':'id'}))
# XML rows' tools-only sample strings also resolve from closure above.
out=E.Element('resources');out.extend(values);E.indent(out);E.ElementTree(out).write(target/'values/gamepad_ps_ns_components.xml',encoding='utf-8',xml_declaration=True)
(root/'stage4/ns-reuse.json').write_text(json.dumps(dict(commit='0f2249a7026f9ea2e90a6891125b3537a9f53491',files=records),ensure_ascii=False,indent=2))
print('Imported',len(records),'components/resources')
