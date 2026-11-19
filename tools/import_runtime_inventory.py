"""Supplement static evidence with actual pinned Forge registries, never aspect values."""
import generate as g
import hashlib
table_path=g.ROOT/'run-full/world/aspect-exports/omniverse-full/table.json'
table=g.read(table_path);mods=table['mods']
assert table['client_evaluator_parity']
for name,namespace in [('TConstruct','tconstruct'),('gregtech','gtceu'),('Mekanism','mekanism'),('Avaritia','avaritia'),('TofuCraft','tofucraft'),('TofuFactory','tofucraft')]:
 p=g.ROOT/'evidence'/('inventory-'+name+'.json');inv=g.read(p)
 assert namespace in mods
 # Both metadata and the pinned binary hash remain in this evidence file.
 inv['items']=sorted(set(inv['items'])|{r['id'] for r in table['items'] if r['id'].startswith(namespace+':')})
 inv['blocks']=sorted(set(inv['blocks'])|{r['id'] for r in table['blocks'] if r['id'].startswith(namespace+':')})
 inv['runtime_inventory']={'mod_version':mods[namespace],'source':str(table_path.relative_to(g.ROOT)),'table_sha256':hashlib.sha256(table_path.read_bytes()).hexdigest()}
 g.write(p,inv)
g.write(g.ROOT/'evidence/runtime-registry.json',{'mods':mods,'items':[r['id'] for r in table['items']],'blocks':{r['id']:r['item'] for r in table['blocks']}})
print('Imported actual registry membership only; all amounts still originate in Collaboration/lists')
