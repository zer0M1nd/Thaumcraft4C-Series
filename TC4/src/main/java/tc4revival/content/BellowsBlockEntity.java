package tc4revival.content;

import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

public  class BellowsBlockEntity extends BlockEntity {
    // cookingProgress -> w -> f_58318_, confirmed against this project's official and SRG mappings.
    private static final Field COOKING=ObfuscationReflectionHelper.findField(AbstractFurnaceBlockEntity.class,"f_58318_");
    private static final Field TOTAL_COOKING=ObfuscationReflectionHelper.findField(AbstractFurnaceBlockEntity.class,"f_58319_");
    private boolean furnaceAttachment,expanding,first=true;
    private int delay;
    private float inflation=1;
    public BellowsBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.BELLOWS_ENTITY.get(),pos,state); }
    public Direction facing() { return getBlockState().getValue(BellowsBlock.FACING); }
    public float inflation() { return inflation; }
    public boolean furnaceAttachment() { return furnaceAttachment; }
    public void setFurnaceAttachment(boolean value) {
        furnaceAttachment=value; setChanged();
        if (level!=null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public void tick() {
        if (level==null || level.hasNeighborSignal(worldPosition)) return;
        if (level.isClientSide) {
            if (first) { inflation=.35f+level.random.nextFloat()*.55f; first=false; }
            if (!expanding && inflation>.35f) inflation-=.075f;
            if (!expanding && inflation<=.35f) expanding=true;
            if (expanding && inflation<1) inflation+=.025f;
            if (expanding && inflation>=1) {
                expanding=false;
                level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,
                        SoundEvents.GHAST_SHOOT,SoundSource.BLOCKS,.01f,.5f+(level.random.nextFloat()-level.random.nextFloat())*.2f,false);
            }
        } else if (++delay>=2) {
            delay=0;
            var target=worldPosition.relative(facing());
            if (level.hasChunkAt(target) && level.getBlockEntity(target) instanceof AbstractFurnaceBlockEntity furnace) {
                try {
                    int progress=COOKING.getInt(furnace);
                    if (progress>0 && progress<TOTAL_COOKING.getInt(furnace)-1) { COOKING.setInt(furnace,progress+1); furnace.setChanged(); }
                } catch (IllegalAccessException error) { throw new IllegalStateException("Cannot access vanilla furnace progress",error); }
            }
        }
    }
    /** Infernal-furnace consumers use orientation and redstone; crucibles deliberately use their own count. */
    public static int count(Level level,BlockPos origin,Iterable<Direction> sides) {
        int count=0;
        for (var side : sides) {
            var pos=origin.relative(side);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof BellowsBlockEntity bellows && bellows.facing()==side.getOpposite() && !level.hasNeighborSignal(pos)) count++;
        }
        return count;
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putByte("orientation",(byte)facing().get3DDataValue()); tag.putBoolean("onVanillaFurnace",furnaceAttachment); }
    @Override public void load(CompoundTag tag) { super.load(tag); furnaceAttachment=tag.getBoolean("onVanillaFurnace"); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
