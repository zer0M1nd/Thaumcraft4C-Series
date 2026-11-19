package tc4revival.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.PacketDistributor;
import tc4revival.content.*;

public record InfusionRiskRequest(BlockPos pos) {
    private static final java.util.Map<ServerPlayer,Integer> LAST=new java.util.WeakHashMap<>();
    public void encode(FriendlyByteBuf buf) { buf.writeBlockPos(pos); }
    public static InfusionRiskRequest decode(FriendlyByteBuf buf) { return new InfusionRiskRequest(buf.readBlockPos()); }
    public boolean eligible(ServerPlayer player) {
        if(player==null || !RevealingGear.visible(player,false) || !player.serverLevel().hasChunkAt(pos))return false;
        double reach=Math.min(16,player.getBlockReach());
        return player.pick(reach,1,false) instanceof BlockHitResult hit && hit.getType()==HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }
    public void handle(ServerPlayer player) {
        if(player==null)return;
        int now=player.server.getTickCount();var last=LAST.get(player);if(last!=null && now-last<10)return;LAST.put(player,now);
        if(!eligible(player))return;
        if(!(player.level().getBlockEntity(pos) instanceof InfusionMatrixBlockEntity matrix))return;
        var preview=matrix.previewFor(player).orElse(null);
        var required=preview==null?new tc4revival.aspect.AspectAmounts():preview.required();
        var available=preview==null?new tc4revival.aspect.AspectAmounts():EssentiaSources.available(matrix,required);
        TCNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new InfusionRiskReply(pos,preview==null?null:preview.risk(),matrix.canRepeat(player),required,available,
                preview==null?null:preview.environment(),preview==null?java.util.List.of():preview.items()));
    }
}
