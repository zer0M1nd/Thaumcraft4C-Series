function initializeCoreMod() {
    var ASMAPI=Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Opcodes=Java.type('org.objectweb.asm.Opcodes');
    var MethodInsnNode=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var VarInsnNode=Java.type('org.objectweb.asm.tree.VarInsnNode');
    var InsnList=Java.type('org.objectweb.asm.tree.InsnList');
    return { 'legacy_potion_effects': {
        'target': { 'type':'CLASS', 'name':'net.minecraft.world.item.alchemy.PotionUtils' },
        'transformer':function(node) {
            var changed=0;
            for (var i=0;i<node.methods.size();i++) {
                var method=node.methods.get(i);
                if (method.desc!=='(Lnet/minecraft/nbt/CompoundTag;)Ljava/util/List;'
                        || (method.name!=='getAllEffects' && method.name!==ASMAPI.mapMethod('m_43566_'))) continue;
                for (var insn=method.instructions.getFirst();insn!==null;insn=insn.getNext()) {
                    if (insn.getOpcode()!==Opcodes.ARETURN) continue;
                    var patch=new InsnList(); patch.add(new VarInsnNode(Opcodes.ALOAD,0));
                    patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'tc4revival/content/LegacyPotionEffects','resolve',
                            '(Ljava/util/List;Lnet/minecraft/nbt/CompoundTag;)Ljava/util/List;',false));
                    method.instructions.insertBefore(insn,patch); method.maxStack=Math.max(method.maxStack,2); changed++;
                }
            }
            if (changed!==1) throw new Error('TC4 potion effects expected one return, found '+changed);
            return node;
        }
    }};
}
