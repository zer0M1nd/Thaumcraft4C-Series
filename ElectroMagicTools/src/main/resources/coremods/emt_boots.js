function initializeCoreMod() {
    var api=Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var codes=Java.type('org.objectweb.asm.Opcodes');
    var Insns=Java.type('org.objectweb.asm.tree.InsnList');
    var Load=Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Invoke=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    return {'emt_air_acceleration':{target:{type:'CLASS',name:'net.minecraft.world.entity.player.Player'},transformer:function(node){
        var found=false;
        for(var i=0;i<node.methods.size();i++){
            var method=node.methods.get(i);
            if(method.desc!=='()F'||(method.name!=='getFlyingSpeed'&&method.name!==api.mapMethod('m_274460_')))continue;
            found=true;var instructions=method.instructions.toArray();
            for(var j=0;j<instructions.length;j++)if(instructions[j].getOpcode()===codes.FRETURN){
                var hook=new Insns();hook.add(new Load(codes.ALOAD,0));
                hook.add(new Invoke(codes.INVOKESTATIC,'emtmodern/item/ElectricTravellerBoots','airSpeed','(FLnet/minecraft/world/entity/player/Player;)F',false));
                method.instructions.insertBefore(instructions[j],hook);
            }
        }
        if(!found)throw new Error('EMT player air acceleration hook not found');return node;
    }}};
}
