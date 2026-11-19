"""Own JSON geometry; all legacy texture bytes remain in the player's external jar."""
def apply(write, texture, mod, mapping):
    def box(start,end,tex,uv=None):
        return {'from':start,'to':end,'faces':{side:dict(texture=tex,**({'uv':uv} if uv else {})) for side in ['down','up','north','south','west','east']}}
    def model(name,data):write(f'assets/{mod}/models/block/{name}.json',data)
    for name in ['essentia_provider','infusion_provider','advanced_infusion_provider','essentia_vibration_chamber','vis_interface','creative_vis_interface','knowledge_inscriber','iron_gearbox','thaumium_gearbox','arcane_terminal','arcane_assembler','distillation_encoder']:
        variants={}
        for facing,rotation in [('north',0),('east',90),('south',180),('west',270)]:
            for state in range(3):
                suffix=f'_{state}' if name=='essentia_vibration_chamber' else '_active' if state and name.endswith('provider') else ''
                variants[f'facing={facing},activity={state}']={'model':f'{mod}:block/{name}{suffix}','y':rotation}
        write(f'assets/{mod}/blockstates/{name}.json',{'variants':variants})
    for name in ['knowledge_inscriber','distillation_encoder']:
        top='knowledge.inscriber.top' if name=='knowledge_inscriber' else 'distillation.encoder.face'
        model(name,{'parent':'minecraft:block/cube_bottom_top','textures':{'side':texture('blocks/knowledge.inscriber.side'),'bottom':texture('blocks/knowledge.inscriber.bottom'),'top':texture('blocks/'+top)}})
    for state,face in enumerate(['off','ignis','potentia']):
        source='blocks/e.vibration.face.'+face
        model('essentia_vibration_chamber_'+str(state),{'parent':'minecraft:block/orientable','textures':{'front':texture(source),'side':texture('blocks/e.vibration.input'),'top':texture('blocks/e.vibration.input')}})
        if state:mapping[f'assets/{mod}/textures/{source}.png.mcmeta']=f'assets/{mod}/textures/{source}.png.mcmeta'
    model('essentia_vibration_chamber',{'parent':f'{mod}:block/essentia_vibration_chamber_0'})
    for name in ['essentia_provider','infusion_provider','advanced_infusion_provider']:
        base='essentia.provider' if name=='essentia_provider' else 'infusion.provider'
        model(name+'_active',{'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':{'particle':texture('blocks/'+base),'base':texture('blocks/'+base),'overlay':texture('blocks/'+base+'.overlay')},'elements':[box([0,0,0],[16,16,16],'#base'),box([-.005,-.005,-.005],[16.005,16.005,16.005],'#overlay',[0,0,16,16])]})
    # Framed translucent chamber, rather than the fallback cube used by disabled TESRs.
    elements=[]
    for axis in range(3):
        for a in [0,15]:
            for b in [0,15]:
                low=[a,b,0];high=[a+1,b+1,16]
                if axis==0:low=[0,a,b];high=[16,a+1,b+1]
                if axis==1:low=[a,0,b];high=[a+1,16,b+1]
                elements.append(box(low,high,'#frame',[0,0,8.5,1]))
    elements.append(box([1,1,1],[15,15,15],'#glass',[.25,9.5,6.75,16]))
    model('arcane_assembler',{'parent':'minecraft:block/block','render_type':'minecraft:translucent','textures':{'particle':texture('models/arcane.assembler'),'frame':texture('models/arcane.assembler'),'glass':texture('models/arcane.assembler')},'elements':elements})
    for name,material in [('iron_gearbox','iron'),('thaumium_gearbox','thaumium')]:
        elements=[box([1.28,1.28,1.28],[14.72,14.72,14.72],'#body',[4,3.5,7.5,7])]
        # Shafts and spokes are rendered dynamically by MachineRenderer.
        model(name,{'parent':'minecraft:block/block','textures':{'particle':texture('models/gearbox.'+material),'body':texture('models/gearbox.'+material)},'elements':elements})

    face=texture('blocks/parts/arcane.crafting.overlay1')
    side=texture('blocks/parts/arcane.crafting.side')
    panel=box([2,2,0],[14,14,2],'#side')
    panel['faces']['north']={'texture':'#face','uv':[0,0,16,16]}
    definition={'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':{'particle':side,'side':side,'face':face},'elements':[panel]}
    write(f'assets/{mod}/models/part/arcane_terminal.json',definition)
    write(f'assets/{mod}/models/item/arcane_terminal.json',{'parent':f'{mod}:part/arcane_terminal'})

    for index,name in enumerate(['input','output','creative']):
        definition={'parent':'minecraft:block/block','textures':{'particle':texture('blocks/parts/vis.interface'),'body':texture('blocks/parts/vis.interface'),'rim':'minecraft:block/'+['iron_block','gold_block','diamond_block'][index]},'elements':[box([4,4,2],[12,12,4],'#rim'),box([5,5,0],[11,11,2],'#body')]}
        write(f'assets/{mod}/models/part/vis_{name}.json',definition)
    for name,model_name in [('vis_interface','input'),('creative_vis_interface','creative')]:
        write(f'assets/{mod}/models/item/{name}.json',{'parent':f'{mod}:part/vis_{model_name}'})
