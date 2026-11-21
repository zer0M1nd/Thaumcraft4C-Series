"""Explicit legacy AEAspectRegister base assignments, with modern item IDs."""
def apply(write, mod):
    entries=[]
    def add(items, **aspects):
        if isinstance(items,str):items=[items]
        entries.append({'items':items,'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()]})
    add('ae2:certus_quartz_crystal',vitreus=2)
    add('ae2:charged_certus_quartz_crystal',vitreus=2,potentia=4)
    add('ae2:fluix_crystal',vitreus=2,potentia=6)
    add('ae2:ender_dust',alienis=2,motus=2,praecantatio=1,perditio=1)
    add('ae2:silicon',ignis=1,sensus=1,ordo=1)
    add(['ae2:sky_stone_block','ae2:smooth_sky_stone_block','ae2:sky_stone_brick','ae2:sky_stone_small_brick'],alienis=2,terra=1,tenebrae=1)
    add('ae2:matter_ball',limus=4,terra=4,volatus=4)
    add(['ae2:facade','ae2:cable_anchor'],metallum=1)
    colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black','fluix']
    for cable,extra in [('glass',{}),('covered',{'pannus':1}),('smart',{'pannus':1,'lux':1,'potentia':3}),('smart_dense',{'pannus':1,'lux':2,'potentia':5})]:
        add([f'ae2:{c}_{cable}_cable' for c in colors],**dict({'vitreus':1,'potentia':2},**extra))
    p2p=dict(metallum=6,potentia=6,vitreus=4,lucrum=4,ordo=4,sensus=4)
    for kind,bonus in [('me',{}),('item',{}),('fluid',{'aqua':4}),('light',{'lux':4}),('redstone',{'potentia':8}),('fe',{'potentia':8})]:
        add(f'ae2:{kind}_p2p_tunnel',**dict(p2p,**bonus))
    add('ae2:singularity',tenebrae=10,vacuos=10,perditio=10)
    add('ae2:quantum_entangled_singularity',potentia=8,permutatio=8,motus=4,alienis=4)
    add(['ae2:crafting_pattern','ae2:processing_pattern',mod+':arcane_pattern'],fabrico=3,cognitio=3)
    add(mod+':iron_gearbox',metallum=7,machina=5,terra=2,perditio=2)
    add(mod+':thaumium_gearbox',metallum=10,machina=10,terra=2,perditio=2,praecantatio=10)
    write(f'data/{mod}/item_aspects/main.json',{'format':1,'entries':entries})
