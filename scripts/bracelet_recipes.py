from alchemy_recipes import PRIMALS

def generate(shaped, infusion):
    for kind,cost in [('iron',1),('gold',5),('greatwood',25),('thaumium',25)]:
        cap={'greatwood':'gold','thaumium':'thauminite'}.get(kind,kind)
        capid='thaumicbases:thauminite_wand_cap' if cap=='thauminite' else 'thaumcraft:wand_cap_'+cap
        material={'iron':'#forge:ingots/iron','gold':'#forge:ingots/gold','greatwood':'thaumcraft:greatwood_log','thaumium':'thaumcraft:thaumium_ingot'}[kind]
        pattern=['CIC','I I'] if kind=='greatwood' else ['CIC','INI','N N'] if kind=='thaumium' else ['CIC','N N']
        keys={'C':capid,'I':material}
        if kind!='greatwood':keys['N']='thaumcraft:salis_mundus' if kind=='thaumium' else '#forge:nuggets/'+kind
        shaped(kind+'BraceletRec','TB.Bracelet.'+kind.capitalize(),'thaumicbases:bracelet_'+kind,pattern,keys,dict.fromkeys(PRIMALS,cost))
    cap='thaumcraft:wand_cap_thaumium'
    infusion('silverwoodBraceletRec','TB.Bracelet.Silverwood','thaumicbases:bracelet_silverwood','thaumcraft:wand_rod_silverwood',5,dict.fromkeys(PRIMALS+['praecantatio'],17),[cap,'thaumcraft:balanced_shard','thaumcraft:silverwood_log','thaumcraft:mixed_crystal_cluster']*2)
    infusion('voidBraceletRec','TB.Bracelet.Void','thaumicbases:bracelet_void','thaumicbases:void_wand_core',8,{'alienis':64,'vacuos':64,'praecantatio':64,'auram':24,'potentia':24},[cap,'thaumicbases:salis_mundus_block','thaumcraft:void_ingot','thaumicbases:crystal_mixed']*2)
    for kind,primal,extra,shard in [('reed','aer','motus','air'),('bone','perditio','exanimis','entropy'),('obsidian','terra','tenebrae','earth'),('blaze','ignis','bestia','fire'),('ice','aqua','gelum','water'),('quartz','ordo','potentia','order')]:
        infusion(kind+'BraceletRec','TB.Bracelet.'+kind.capitalize(),'thaumicbases:bracelet_'+kind,'thaumcraft:wand_rod_'+kind,2,{primal:24,extra:12,'praecantatio':8},[cap,'thaumcraft:'+shard+'_shard']*2)
    infusion('primalBraceletRec','TB.Bracelet.Primal','thaumicbases:bracelet_primal','thaumcraft:primal_charm',7,dict(dict.fromkeys(PRIMALS,48),praecantatio=96),['thaumcraft:wand_cap_void']+['thaumicbases:bracelet_'+x for x in ['reed','bone','obsidian']]+['thaumcraft:wand_cap_void']+['thaumicbases:bracelet_'+x for x in ['blaze','ice','quartz']])
