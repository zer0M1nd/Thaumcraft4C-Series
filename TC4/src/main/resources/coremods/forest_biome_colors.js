/* Keep vanilla colour blending while applying the local TC4 blue-forest preference. */
function initializeCoreMod() {
    var ASMAPI=Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Opcodes=Java.type('org.objectweb.asm.Opcodes');
    var MethodInsnNode=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var VarInsnNode=Java.type('org.objectweb.asm.tree.VarInsnNode');
    var InsnList=Java.type('org.objectweb.asm.tree.InsnList');
    return { 'tc4_forest_biome_colors': {
        'target': { 'type':'CLASS', 'name':'net.minecraft.world.level.biome.Biome' },
        'transformer':function(node) {
            var grass=0,foliage=0;
            for (var i=0;i<node.methods.size();i++) {
                var method=node.methods.get(i); var hook=null;
                if (method.desc==='(DD)I' && (method.name==='getGrassColor' || method.name===ASMAPI.mapMethod('m_47464_'))) hook='grass';
                if (method.desc==='()I' && (method.name==='getFoliageColor' || method.name===ASMAPI.mapMethod('m_47542_'))) hook='foliage';
                if (hook===null) continue;
                for (var insn=method.instructions.getFirst();insn!==null;insn=insn.getNext()) {
                    if (insn.getOpcode()!==Opcodes.IRETURN) continue;
                    var patch=new InsnList(); patch.add(new VarInsnNode(Opcodes.ALOAD,0));
                    patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'tc4revival/content/ForestBiomeColors',hook,
                        '(ILnet/minecraft/world/level/biome/Biome;)I',false));
                    method.instructions.insertBefore(insn,patch); method.maxStack=Math.max(method.maxStack,2);
                    if (hook==='grass') grass++; else foliage++;
                }
            }
            if (grass!==1 || foliage!==1) throw new Error('TC4 forest palette expected one return per colour: '+grass+'/'+foliage);
            return node;
        }
    }};
}
