package tc4revival.content;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

/** Non-ticking: only the plant's random tick or world generation advances growth. */
public  class ManaPodBlockEntity extends BlockEntity implements RevealedAspectContainer {
    private Aspect aspect;
    public ManaPodBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.MANA_POD_ENTITY.get(),pos,state); }
    public Aspect aspect() { return aspect; }
    public void setAspect(Aspect value) { aspect=value; changed(); }
    public void grow() {
        if (level==null || level.isClientSide || !getBlockState().is(TCArcane.MANA_POD.get())) return;
        grow((net.minecraft.server.level.ServerLevel)level);
    }
    public void grow(net.minecraft.world.level.WorldGenLevel world) {
        if (!getBlockState().is(TCArcane.MANA_POD.get())) return;
        var neighbours=new ArrayList<Aspect>();
        for (var direction:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
            var pos=worldPosition.relative(direction);
            if (world.hasChunkAt(pos) && world.getBlockEntity(pos) instanceof ManaPodBlockEntity pod && pod.aspect!=null)
                neighbours.add(pod.aspect);
        }
        int age=getBlockState().getValue(ManaPodBlock.AGE);
        var result=ManaPodGrowth.advance(age,aspect,neighbours,TCAspects.catalog(),world.getRandom()::nextInt);
        aspect=result.aspect();
        if (result.age()!=age) world.setBlock(worldPosition,getBlockState().setValue(ManaPodBlock.AGE,result.age()),3);
        setChanged();
        if (world instanceof net.minecraft.server.level.ServerLevel) changed();
    }
    @Override public AspectAmounts revealedAspects() {
        return aspect!=null && getBlockState().getValue(ManaPodBlock.AGE)==7
                ? new AspectAmounts().add(aspect,1) : new AspectAmounts();
    }
    private void changed() {
        setChanged();
        if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); if (aspect!=null) tag.putString("aspect",aspect.tag());
    }
    @Override public void load(CompoundTag tag) { super.load(tag); aspect=TCAspects.catalog().find(tag.getString("aspect")).orElse(null); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
