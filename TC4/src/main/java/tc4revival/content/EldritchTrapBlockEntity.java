package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import tc4revival.network.FocusEffectPacket;
import tc4revival.network.TCNetwork;
import tc4revival.network.WarpGainPacket;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge;

public  class EldritchTrapBlockEntity extends BlockEntity {
    private int delay=20;
    public EldritchTrapBlockEntity(BlockPos pos,BlockState state) { super(TCEldritch.TRAP_ENTITY.get(),pos,state); }
    public void tick() {
        if (!(level instanceof ServerLevel server) || delay-->0) return;
        delay=10+server.random.nextInt(25); Vec3 center=Vec3.atCenterOf(worldPosition);
        var player=server.getNearestPlayer(center.x,center.y,center.z,3,false);
        if (player==null) return;
        player.hurt(server.damageSources().magic(),2);
        if (server.random.nextBoolean()) {
            int amount=1+server.random.nextInt(2);
            if (player instanceof ServerPlayer recipient && !tc4revival.config.TCConfig.SERVER.disableWarp.get()) {
                KnowledgeSavedData.get(server.getServer()).update(recipient.getUUID(),server.random::nextInt,knowledge->{
                    knowledge.changeWarp(PlayerKnowledge.WarpKind.TEMPORARY,amount); knowledge.setWarpCounter(knowledge.totalWarp()); return null;
                });
                TCNetwork.send(recipient,new WarpGainPacket(PlayerKnowledge.WarpKind.TEMPORARY,amount));
            }
        }
        FocusEffectPacket.lightning(server,center,player.getEyePosition());
    }
}
