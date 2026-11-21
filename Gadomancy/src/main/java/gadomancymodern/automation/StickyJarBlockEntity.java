package gadomancymodern.automation;

import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;

/** The core jar owns storage, phials, labels and NBT; this adapter rotates its sole pipe connection. */
public class StickyJarBlockEntity extends EssentiaJarBlockEntity {
    protected Direction support=Direction.DOWN;
    protected int stickyTicks;
    public StickyJarBlockEntity(BlockPos pos,BlockState state){this(Gadomancy.STICKY_BE.get(),pos,state);}
    protected StickyJarBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    public Direction support(){return support;}
    public void support(Direction side){support=side;changed();}
    @Override public boolean voidJar(){return getBlockState().getValue(StickyJarBlock.VOID);}
    public Direction local(Direction face){
        if(face==null)return null;
        if(support==Direction.DOWN)return face;
        if(support==Direction.UP)return face.getAxis()==Direction.Axis.Y?face.getOpposite():face;
        if(face==Direction.UP)return Direction.NORTH;if(face==Direction.DOWN)return Direction.SOUTH;
        if(face==support)return Direction.DOWN;if(face==support.getOpposite())return Direction.UP;
        return switch(support){case EAST->face==Direction.NORTH?Direction.WEST:Direction.EAST;case SOUTH->face.getOpposite();case WEST->face==Direction.SOUTH?Direction.WEST:Direction.EAST;default->face;};
    }
    @Override public boolean connectable(Direction side){return side==support.getOpposite();}
    @Override public ItemStack asItem(){var stack=super.asItem();stack.getOrCreateTag().putBoolean("isStickyJar",true);return stack;}
    @Override public void tick(){
        if(level==null||level.isClientSide||++stickyTicks%5!=0||!voidJar()&&amount()==capacity())return;
        Direction input=support.getOpposite();BlockPos adjacent=worldPosition.relative(input);if(!level.hasChunkAt(adjacent))return;
        if(!(level.getBlockEntity(adjacent) instanceof EssentiaTransport source)||!source.connectable(input.getOpposite())||!source.canOutput(input.getOpposite()))return;
        var wanted=filter()!=null?filter():aspect();var face=input.getOpposite();
        if(wanted==null&&source.essentiaAmount(face)>0&&source.suction(face)<suction(input)&&suction(input)>=source.minimumSuction())wanted=source.essentiaType(face);
        if(wanted!=null&&source.suction(face)<suction(input))addToContainer(wanted,source.takeEssentia(wanted,1,face));
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putByte("GadSupport",(byte)support.get3DDataValue());}
    @Override public void load(CompoundTag tag){super.load(tag);support=Direction.from3DDataValue(tag.getByte("GadSupport"));}
}
