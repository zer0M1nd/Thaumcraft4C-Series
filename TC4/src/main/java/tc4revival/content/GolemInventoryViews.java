package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

/** Native sided capability views, including the combined handler of a double chest. */
public  class GolemInventoryViews {
    private GolemInventoryViews() {}
    public static boolean exists(ServerLevel level,BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
        if (level.getBlockEntity(pos) instanceof Container) return true;
        if (GolemItemTransfer.at(level,pos,null)!=null) return true;
        for (var face:Direction.values()) if (GolemItemTransfer.at(level,pos,face)!=null) return true;
        return false;
    }
    public static IItemHandler single(ServerLevel level,BlockPos pos,Direction face) {
        if (pos==null || !level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) return null;
        var handler=GolemItemTransfer.at(level,pos,face);
        if (handler!=null) return handler;
        var entity=level.getBlockEntity(pos);
        if (entity instanceof WorldlyContainer inventory && face!=null) return new SidedInvWrapper(inventory,face);
        if (entity instanceof Container inventory) return new InvWrapper(inventory);
        return GolemItemTransfer.at(level,pos,face);
    }
    public static BlockPos partner(ServerLevel level,BlockPos pos) {
        if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof ChestBlockEntity)) return null;
        var state=level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE)==ChestType.SINGLE) return null;
        var partner=pos.relative(ChestBlock.getConnectedDirection(state));
        if (!level.hasChunkAt(partner)) return null;
        var other=level.getBlockState(partner);
        return other.is(state.getBlock()) && other.getValue(ChestBlock.FACING)==state.getValue(ChestBlock.FACING)
                && other.getValue(ChestBlock.TYPE)!=ChestType.SINGLE && other.getValue(ChestBlock.TYPE)!=state.getValue(ChestBlock.TYPE)
                && level.getBlockEntity(partner) instanceof ChestBlockEntity ? partner : null;
    }
    public static boolean same(ServerLevel level,BlockPos first,BlockPos second) {
        return first!=null && second!=null && (first.equals(second) || second.equals(partner(level,first)));
    }
}
