/* Preserve TC4 Haste's weight of three, including native enchanting-table selections. */
function initializeCoreMod() {
    var Opcodes=Java.type('org.objectweb.asm.Opcodes');
    var MethodInsnNode=Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var VarInsnNode=Java.type('org.objectweb.asm.tree.VarInsnNode');
    var InsnList=Java.type('org.objectweb.asm.tree.InsnList');
    var enchant='Lnet/minecraft/world/item/enchantment/Enchantment;';
    return { 'tc4_enchantment_weight': {
        'target': { 'type':'CLASS', 'name':'net.minecraft.world.item.enchantment.EnchantmentInstance' },
        'transformer':function(node) {
            var changed=0;
            for (var i=0;i<node.methods.size();i++) {
                var method=node.methods.get(i);
                if (method.name!=='<init>' || method.desc!=='('+enchant+'I)V') continue;
                for (var insn=method.instructions.getFirst();insn!==null;insn=insn.getNext()) {
                    if (!(insn instanceof MethodInsnNode) || insn.name!=='<init>' || insn.desc!=='(I)V'
                            || insn.owner!=='net/minecraft/util/random/WeightedEntry$IntrusiveBase') continue;
                    var patch=new InsnList(); patch.add(new VarInsnNode(Opcodes.ALOAD,1));
                    patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC,'tc4revival/content/LegacyHasteEnchantment',
                            'selectionWeight','(I'+enchant+')I',false));
                    method.instructions.insertBefore(insn,patch); method.maxStack=Math.max(method.maxStack,3); changed++;
                }
            }
            if (changed!==1) throw new Error('TC4 enchantment weight expected one constructor, found '+changed);
            return node;
        }
    }};
}
