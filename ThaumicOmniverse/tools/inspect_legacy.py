import csv,json,pathlib,re,collections,sys
BASE=pathlib.Path(__file__).resolve().parents[3]
idx=json.loads((BASE/'Collaboration/list-index.json').read_text(encoding='utf-8'))
for mod in sys.argv[1:]:
 rows={}
 for f in idx:
  if f['target_modid']!=mod:continue
  for r in csv.DictReader((BASE/'Collaboration'/f['file']).open(encoding='utf-8-sig')):
   m=re.search(r'metadata\s*=\s*([^；; ]+)',r['特殊要求']);meta=m.group(1) if m else '0'
   rows.setdefault((r['物品 id'],meta),r)
 print('\n###',mod,len(rows))
 for (id,meta),r in list(rows.items())[:180]:print(id,meta,r['物品英文本地化名称'],r['要素列表'])
