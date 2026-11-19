/* TC4's isBeaconBase/canSustainLeaves hooks became context-free tag queries.
 * Substitute the saved state at these three query sites, not in general world reads.
 * This configuration is hand-authored code, not generated game resource data. */
function initializeCoreMod() {
    var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var Opcodes = Java.type('org.objectweb.asm.Opcodes');
    var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
    var helper = 'tc4revival/content/WardedBlock';
    var state = 'Lnet/minecraft/world/level/block/state/BlockState;';
    var getter = 'Lnet/minecraft/world/level/BlockGetter;';
    var position = 'Lnet/minecraft/core/BlockPos;';
    function named(actual, official, srg) { return actual === official || actual === ASMAPI.mapMethod(srg); }
    function reads(method) {
        var changed = 0;
        for (var insn = method.instructions.getFirst(); insn !== null;) {
            var next = insn.getNext();
            if (insn instanceof MethodInsnNode && named(insn.name, 'getBlockState', 'm_8055_') && insn.desc === '(' + position + ')' + state) {
                method.instructions.set(insn,new MethodInsnNode(Opcodes.INVOKESTATIC,helper,'queryOriginal','(' + getter + position + ')' + state,false));
                changed++;
            }
            insn = next;
        }
        if (changed !== 1) throw new Error('TC4 ward query expected one world read in ' + method.name + ', found ' + changed);
    }
    return {
        'ward_beacon_base': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.world.level.block.entity.BeaconBlockEntity' },
            'transformer': function(node) {
                var changed = 0;
                for (var i=0;i<node.methods.size();i++) {
                    var method=node.methods.get(i);
                    if (named(method.name,'updateBase','m_155092_') && method.desc === '(Lnet/minecraft/world/level/Level;III)I') { reads(method); changed++; }
                }
                if (changed !== 1) throw new Error('TC4 beacon base hook not found');
                return node;
            }
        },
        'ward_leaf_support': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.world.level.block.LeavesBlock' },
            'transformer': function(node) {
                var changed = 0;
                for (var i=0;i<node.methods.size();i++) {
                    var method=node.methods.get(i);
                    if (named(method.name,'updateDistance','m_54435_')) { reads(method); changed++; }
                    if (!named(method.name,'updateShape','m_7417_')) continue;
                    var replacements=0;
                    for (var insn=method.instructions.getFirst();insn !== null;insn=insn.getNext()) {
                        if (!(insn instanceof MethodInsnNode) || !named(insn.name,'getDistanceAt','m_54463_') || insn.desc !== '(' + state + ')I') continue;
                        var patch=new InsnList();
                        patch.add(new VarInsnNode(Opcodes.ALOAD,4));
                        patch.add(new VarInsnNode(Opcodes.ALOAD,6));
                        patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC,helper,'unwrap','(' + state + getter + position + ')' + state,false));
                        method.instructions.insertBefore(insn,patch);
                        replacements++;
                    }
                    if (replacements !== 1) throw new Error('TC4 leaf neighbor hook expected one distance query');
                    method.maxStack+=2;
                    changed++;
                }
                if (changed !== 2) throw new Error('TC4 leaf support hooks not found');
                return node;
            }
        }
    };
}
