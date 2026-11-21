package gadomancymodern.automation;

import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tc4revival.aspect.*;
import tc4revival.content.*;

public class BlockProtectorEntity extends EssentiaJarBlockEntity {
    protected final ProtectionCycle cycle=new ProtectionCycle();
    protected int protectorTicks;
    public BlockProtectorEntity(BlockPos pos,BlockState state){super(Gadomancy.PROTECTOR_BE.get(),pos,state);}
    public int range(){return cycle.range();}
    public int power(){return level==null?0:Math.min(15,level.getBestNeighborSignal(worldPosition));}
    public AABB area(){return new AABB(worldPosition.getX()-range(),worldPosition.getY()-range(),worldPosition.getZ()-range(),worldPosition.getX()+range(),worldPosition.getY()+range(),worldPosition.getZ()+range());}
    public Aspect order(){return TCAspects.catalog().require("ordo");}
    @Override public int capacity(){return 8;}
    @Override public boolean voidJar(){return false;}
    @Override public Aspect filter(){return order();}
    @Override public boolean accepts(Aspect aspect){return order().equals(aspect);}
    @Override public boolean acceptsPhials(){return false;}
    @Override public void label(Aspect selected,Direction side){}
    @Override public void removeLabel(){}
    @Override public Aspect suctionType(Direction side){return order();}
    @Override public int minimumSuction(){return 128;}
    @Override public int suction(Direction side){return amount()==capacity()?0:128;}
    @Override public boolean connectable(Direction side){return side==Direction.DOWN;}
    @Override public boolean canOutput(Direction side){return false;}
    @Override public ItemStack asItem(){return new ItemStack(Gadomancy.PROTECTOR_ITEM.get());}
    @Override public void onLoad(){super.onLoad();ProtectorEvents.register(this);}
    @Override public void setRemoved(){ProtectorEvents.unregister(this);super.setRemoved();}
    @Override public void tick(){
        if(level==null)return;
        if(level.isClientSide){gadomancymodern.client.ProtectorEffects.tick(this);return;}
        if(++protectorTicks%5==0&&amount()<capacity()){
            var below=worldPosition.below();
            if(level.hasChunkAt(below)&&level.getBlockEntity(below) instanceof EssentiaTransport source&&source.connectable(Direction.UP)&&source.canOutput(Direction.UP)&&source.suction(Direction.UP)<suction(Direction.DOWN))addToContainer(order(),source.takeEssentia(order(),1,Direction.UP));
        }
        if(protectorTicks%15==0){int previous=range();boolean entities=cycle.advance(power(),()->takeFromContainer(order(),1));if(previous!=range())changedRange();else setChanged();if(!entities)return;}
        if(range()>0)for(var creeper:level.getEntitiesOfClass(net.minecraft.world.entity.monster.Creeper.class,area()))ProtectorEvents.resetFuse(creeper);
    }
    protected void changedRange(){changed();level.getLightEngine().checkBlock(worldPosition);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.remove("AspectFilter");tag.putInt("ProtectRange",range());tag.putInt("ProtectSaturation",cycle.saturation());}
    @Override public void load(CompoundTag tag){int previous=range();super.load(tag);cycle.restore(tag.getInt("ProtectRange"),tag.getInt("ProtectSaturation"));if(level!=null&&previous!=range())level.getLightEngine().checkBlock(worldPosition);}
}
