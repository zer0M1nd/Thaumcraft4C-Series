function initializeCoreMod() {
    var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
    var O = Java.type('org.objectweb.asm.Opcodes');
    var List = Java.type('org.objectweb.asm.tree.InsnList');
    var Var = Java.type('org.objectweb.asm.tree.VarInsnNode');
    var Call = Java.type('org.objectweb.asm.tree.MethodInsnNode');
    return { 'traveller_boots': {
        'target': { 'type': 'CLASS', 'name': 'net.minecraft.world.entity.player.Player' },
        'transformer': function(node) {
            var changed = 0;
            for (var i = 0; i < node.methods.size(); i++) {
                var m = node.methods.get(i);
                if (m.desc !== '()F' || (m.name !== 'getFlyingSpeed' && m.name !== ASMAPI.mapMethod('m_274460_'))) continue;
                var instructions = m.instructions.toArray();
                for (var j = 0; j < instructions.length; j++) if (instructions[j].getOpcode() === O.FRETURN) {
                    var code = new List(); code.add(new Var(O.ALOAD, 0));
                    code.add(new Call(O.INVOKESTATIC, 'tc4revival/content/TravellerBootsItem', 'airSpeed', '(FLnet/minecraft/world/entity/player/Player;)F', false));
                    m.instructions.insertBefore(instructions[j], code);
                }
                m.maxStack = Math.max(m.maxStack, 2); changed++;
            }
            if (changed !== 1) throw new Error('Traveller boots expected one air acceleration method, found ' + changed);
            return node;
        }
    }};
}
