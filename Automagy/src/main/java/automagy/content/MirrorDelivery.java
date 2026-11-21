package automagy.content;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import tc4revival.content.ItemMirrorBlockEntity;

/** Loaded endpoints only. Output injection must never use a mirror's forwarding capability. */
public final class MirrorDelivery {
    private MirrorDelivery() {}
    public static BlockEntity resolve(ServerLevel source,GlobalPos target) {
        if(target==null)return null;
        var world=source.getServer().getLevel(target.dimension());
        return world!=null&&world.hasChunkAt(target.pos())?world.getBlockEntity(target.pos()):null;
    }
    public static boolean output(BlockEntity tile) {
        return tile instanceof ItemMirrorBlockEntity mirror&&mirror.linked()
            ||tile instanceof DeliveryMirrorBlockEntity delivery&&!delivery.multi()&&delivery.target()!=null;
    }
    public static ItemStack enqueue(BlockEntity tile,ItemStack stack,boolean simulate) {
        if(!output(tile))return stack.copy();
        if(tile instanceof ItemMirrorBlockEntity mirror)return mirror.insertOutput(stack,simulate);
        return ((DeliveryMirrorBlockEntity)tile).insertOutput(stack,simulate);
    }
    public static ItemStack requested(ServerLevel source,InventariumBlockEntity core,GlobalPos target,ItemStack stack,boolean simulate) {
        var tile=resolve(source,target);
        if(!(tile instanceof RequisitionTomeBlockEntity tome)||tome.core()!=core||!tome.powered())return stack.copy();
        return enqueue(tome.outputMirror(),stack,simulate);
    }
}
