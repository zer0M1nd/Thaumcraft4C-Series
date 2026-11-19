package wgmodern;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

/** Forge automation views of the real slots, excluding manual crafting previews. */
public abstract class SidedInventoryEntity extends BlockEntity implements WorldlyContainer {
    private final Map<Direction,LazyOptional<IItemHandler>> handlers=new HashMap<>();
    private boolean capabilitiesValid=true;
    protected SidedInventoryEntity(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){
        if(cap==ForgeCapabilities.ITEM_HANDLER)
            return isRemoved()||!capabilitiesValid?LazyOptional.empty():handlers.computeIfAbsent(side,
                    face->LazyOptional.of(()->new SidedInvWrapper(this,face))).cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();capabilitiesValid=false;handlers.values().forEach(LazyOptional::invalidate);handlers.clear();}
    @Override public void reviveCaps(){super.reviveCaps();capabilitiesValid=true;}
}
