"""Legacy ingredient/cost mapping into the 1.20.1 registries; called by resources.py."""
def apply(write, MOD, research_for):
    def item(s): return {'item':s}
    def output(n): return {'item':MOD+':'+n}
    def save(n,d): write(f'data/{MOD}/recipes/{n}.json',d)
    def arc(n,vis,pattern=None,keys=None,ingredients=None):
        d={'type':'thaumcraft:arcane','research':MOD+':'+research_for[n],'vis':vis,'result':output(n)}
        if ingredients is not None:d.update(mode='shapeless',ingredients=[item(x) for x in ingredients])
        else:d.update(mode='shaped',pattern=pattern,key={k:item(v) for k,v in keys.items()})
        save(n,d)
    def infusion(n,instability,aspects,center,components):
        save(n,{'type':'thaumcraft:infusion','research':MOD+':'+research_for[n],'instability':instability,'aspects':aspects,'center':{'ingredient':item(center)},'components':[{'ingredient':item(x)} if isinstance(x,str) else x for x in components],'result':output(n)})
    allvis=lambda n:dict.fromkeys(['aer','terra','ignis','aqua','ordo','perditio'],n)
    for n,shard,core,aspect in [('diffusion_core','entropy','annihilation','perditio'),('coalescence_core','order','formation','ordo')]:
        arc(n,{'aqua':2,aspect:2},ingredients=['thaumcraft:quicksilver_drop']*3+['thaumcraft:'+shard+'_shard','ae2:'+core+'_core'])
    save('storage_casing',{'type':'minecraft:crafting_shaped','pattern':['WRW','R R','TTT'],'key':{'W':item('thaumcraft:warded_glass'),'R':item('minecraft:redstone'),'T':item('thaumcraft:thaumium_ingot')},'result':output('storage_casing')})
    tiers=['1k','4k','16k','64k','256k','1024k','4096k','16384k']
    arc('storage_component_1k',{'ignis':3,'ordo':1},['EQ ','QPQ',' QE'],{'E':'thaumcraft:wisp_essence','Q':'ae2:certus_quartz_crystal','P':'ae2:logic_processor'})
    for i,tier in enumerate(tiers[1:],1):
        corners=['thaumcraft:wisp_essence','thaumcraft:wisp_essence','thaumcraft:salis_mundus','thaumcraft:salis_mundus','thaumcraft:void_seed','thaumcraft:void_ingot','thaumcraft:primal_charm'][i-1]
        glass='ae2:quartz_glass' if i<4 else ('thaumcraft:essentia_reservoir' if i==7 else 'thaumcraft:warded_glass')
        keys={'E':corners,'P':('ae2:calculation_processor' if i==1 else 'thaumcraft:void_ingot' if i==7 else 'ae2:engineering_processor'),'C':MOD+':storage_component_'+tiers[i-1],'G':glass}
        pattern=['EPE','CGC','ECE']
        if i==2:pattern=['SPE','CGC','ECS'];keys['S']='thaumcraft:salis_mundus'
        arc('storage_component_'+tier,{'ignis':3,'ordo':2**i},pattern,keys)
    for tier in tiers:
        save('essentia_cell_'+tier+'_shaped',{'type':'minecraft:crafting_shaped','pattern':['WRW','RCR','TTT'],'key':{'W':item('thaumcraft:warded_glass'),'R':item('minecraft:redstone'),'T':item('thaumcraft:thaumium_ingot'),'C':item(MOD+':storage_component_'+tier)},'result':output('essentia_cell_'+tier)})
    for tier,pattern,keys in [('quantum',['QCQ','CEC','QCQ'],{'Q':'ae2:quantum_ring','C':MOD+':storage_component_16384k','E':'ae2:dense_energy_cell'}),('singularity',['CCC','CDC','CCC'],{'C':MOD+':essentia_cell_quantum','D':'ae2:condenser'})]:
        save('essentia_cell_'+tier,{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:item(v) for k,v in keys.items()},'result':output('essentia_cell_'+tier)})
    arc('essentia_vibration_chamber',{'aqua':2,'perditio':4,'ignis':7,'ordo':4},ingredients=['ae2:vibration_chamber',MOD+':diffusion_core','thaumcraft:essentia_jar'])
    arc('vis_interface',allvis(2),ingredients=['thaumcraft:balanced_shard','ae2:me_p2p_tunnel'])
    arc('arcane_terminal',allvis(10),ingredients=['ae2:terminal','thaumcraft:arcane_workbench','ae2:calculation_processor'])
    arc('iron_gear',{'terra':1,'ignis':1},[' I ',' W ','I I'],{'I':'minecraft:iron_ingot','W':'minecraft:stick'})
    arc('iron_gearbox',{'aer':2,'ordo':2},['SGS','GGG','SGS'],{'S':'minecraft:cobblestone','G':MOD+':iron_gear'})
    save('thaumium_gearbox',{'type':'thaumcraft:crucible','research':MOD+':VIS','catalyst':item(MOD+':iron_gearbox'),'aspects':{'metallum':16,'praecantatio':16},'result':output('thaumium_gearbox')})
    arc('golem_wireless_backpack',{'aer':6,'ignis':4,'ordo':3},['TIT','NWN','FCF'],{'T':'thaumcraft:thaumium_ingot','I':'ae2:cable_interface','N':'thaumcraft:nitor','W':'ae2:wireless_receiver','F':'ae2:fluix_crystal','C':'ae2:charger'})
    arc('wrench_focus',{'aer':10,'ignis':10},['ANF','NWN','FNA'],{'A':'thaumcraft:air_shard','F':'thaumcraft:fire_shard','N':'minecraft:quartz','W':'ae2:certus_quartz_wrench'})
    arc('knowledge_core',{'aqua':3,'ordo':3,'terra':1},['VLV','LZL','VCV'],{'V':'ae2:quartz_vibrant_glass','L':'minecraft:lapis_lazuli','Z':'thaumcraft:zombie_brain','C':'ae2:calculation_processor'})
    arc('knowledge_inscriber',allvis(5),['IPI',' T ','ILI'],{'I':'minecraft:iron_ingot','P':'ae2:semi_dark_monitor','T':'thaumcraft:thaumonomicon','L':'ae2:logic_processor'})
    arc('distillation_encoder',{'ordo':5,'perditio':5,'ignis':3},['IPI',' T ','IEI'],{'I':'minecraft:iron_ingot','P':'ae2:semi_dark_monitor','T':'thaumcraft:thaumometer','E':'ae2:engineering_processor'})
    arc('cell_microscope',{'aer':300,'terra':300,'ordo':300},['RTR','SQS','RPR'],{'Q':'ae2:quantum_link','P':'ae2:item_p2p_tunnel','S':'ae2:singularity','T':'thaumcraft:thaumometer','R':'thaumcraft:essentia_mirror'})
    aspects={'machina':64,'praecantatio':32,'ordo':32,'permutatio':16}
    infusion('essentia_provider',3,aspects,'ae2:interface',['thaumcraft:essentia_filter','thaumcraft:salis_mundus',MOD+':coalescence_core','thaumcraft:water_shard','thaumcraft:essentia_filter','thaumcraft:salis_mundus',MOD+':diffusion_core','thaumcraft:water_shard'])
    infusion('infusion_provider',4,aspects,'ae2:interface',['thaumcraft:essentia_mirror','thaumcraft:salis_mundus',MOD+':coalescence_core','thaumcraft:air_shard']*2)
    infusion('advanced_infusion_provider',10,{'machina':64,'praecantatio':64,'permutatio':64,'cognitio':64,'lucrum':64},MOD+':infusion_provider',['thaumcraft:primal_charm','thaumcraft:balanced_shard',MOD+':diffusion_core','thaumcraft:balanced_shard']*2)
    infusion('arcane_assembler',7,{'fabrico':64,'permutatio':32,'auram':16,'praecantatio':16,'metallum':8,'vitreus':8},'ae2:molecular_assembler',['thaumcraft:'+s+'_shard' for s in ['air','earth','water','balanced','order','entropy','fire']])
