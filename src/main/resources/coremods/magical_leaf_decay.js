/* Restore the old log/leaf removal notification across vanilla blocks as well as TC wood. */
function initializeCoreMod() {
    var ASMAPI=Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Opcodes=Java.type('org.objectweb.asm.Opcodes');
    var MethodInsnNode=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var VarInsnNode=Java.type('org.objectweb.asm.tree.VarInsnNode');
    var TypeInsnNode=Java.type('org.objectweb.asm.tree.TypeInsnNode');
    var InsnList=Java.type('org.objectweb.asm.tree.InsnList');
    var state='Lnet/minecraft/world/level/block/state/BlockState;';
    var level='Lnet/minecraft/world/level/Level;';
    var pos='Lnet/minecraft/core/BlockPos;';
    return { 'magical_leaf_decay': {
        'target': { 'type':'CLASS', 'name':'net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase' },
        'transformer':function(node) {
            var changed=0;
            for (var i=0;i<node.methods.size();i++) {
                var method=node.methods.get(i);
                if (method.desc!=='('+level+pos+state+'Z)V') continue;
                var removal=false;
                for (var insn=method.instructions.getFirst();insn!==null;insn=insn.getNext()) {
                    if (insn instanceof MethodInsnNode && (insn.name==='onRemove' || insn.name===ASMAPI.mapMethod('m_6810_')) && insn.desc==='('+state+level+pos+state+'Z)V') removal=true;
                }
                if (!removal) continue;
                var patch=new InsnList();
                patch.add(new VarInsnNode(Opcodes.ALOAD,0));
                patch.add(new TypeInsnNode(Opcodes.CHECKCAST,'net/minecraft/world/level/block/state/BlockState'));
                patch.add(new VarInsnNode(Opcodes.ALOAD,1));
                patch.add(new VarInsnNode(Opcodes.ALOAD,2));
                patch.add(new VarInsnNode(Opcodes.ALOAD,3));
                patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'tc4revival/content/MagicalLeavesBlock','removed','('+state+level+pos+state+')V',false));
                method.instructions.insert(patch); method.maxStack=Math.max(method.maxStack,4); changed++;
            }
            if (changed!==1) throw new Error('TC4 magical leaf removal expected one dispatch, found '+changed);
            return node;
        }
    }};
}
