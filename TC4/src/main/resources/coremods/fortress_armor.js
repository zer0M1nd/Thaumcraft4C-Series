function initializeCoreMod() {
    var ASMAPI=Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var O=Java.type('org.objectweb.asm.Opcodes');
    var List=Java.type('org.objectweb.asm.tree.InsnList');
    var Var=Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var Jump=Java.type('org.objectweb.asm.tree.JumpInsnNode');
    var Label=Java.type('org.objectweb.asm.tree.LabelNode');
    var Insn=Java.type('org.objectweb.asm.tree.InsnNode');
    return { 'fortress_armor': {
        'target':{'type':'CLASS','name':'net.minecraft.world.entity.LivingEntity'},
        'transformer':function(node) {
            var changed=0;
            for (var i=0;i<node.methods.size();i++) {
                var m=node.methods.get(i);
                if (m.desc!=='(Lnet/minecraft/world/damagesource/DamageSource;F)F' ||
                    (m.name!=='getDamageAfterArmorAbsorb' && m.name!==ASMAPI.mapMethod('m_21161_'))) continue;
                var code=new List(),vanilla=new Label();
                code.add(new Var(O.ALOAD,0));
                code.add(new Call(O.INVOKESTATIC,'tc4revival/content/FortressProtection','applies','(Lnet/minecraft/world/entity/LivingEntity;)Z',false));
                code.add(new Jump(O.IFEQ,vanilla));
                code.add(new Var(O.ALOAD,0)); code.add(new Var(O.ALOAD,1)); code.add(new Var(O.FLOAD,2));
                code.add(new Call(O.INVOKESTATIC,'tc4revival/content/FortressProtection','absorb','(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)F',false));
                code.add(new Insn(O.FRETURN)); code.add(vanilla); m.instructions.insert(code); m.maxStack=Math.max(m.maxStack,3); changed++;
            }
            if (changed!==1) throw new Error('Fortress armor expected one armor stage, found '+changed);
            return node;
        }
    }};
}
