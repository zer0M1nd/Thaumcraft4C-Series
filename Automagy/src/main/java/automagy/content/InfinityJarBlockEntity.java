package automagy.content;
import automagy.logic.InfinityJarBuffer;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.*;
public final class InfinityJarBlockEntity extends InventoryBlockEntity implements EssentiaVessel,thaumcraft.api.aspects.IAspectSource,thaumcraft.api.aspects.IEssentiaTransport,RevealedAspectContainer {
 private final InfinityJarBuffer buffer=new InfinityJarBuffer();
 public InfinityJarBlockEntity(BlockPos pos,BlockState state){super(Content.INFINITY_JAR_TILE.get(),pos,state,0);}
 public void select(Aspect aspect){buffer.select(aspect.tag());sync();}
 @Override public Aspect aspect(){return TCAspects.catalog().find(buffer.aspect()).orElse(null);}
 @Override public int amount(){return buffer.amount();}@Override public int capacity(){return 64;}
 @Override public boolean accepts(Aspect aspect){return aspect!=null;}
 @Override public boolean acceptsPhials(){return true;}
 @Override public int addToContainer(Aspect aspect,int amount){if(aspect==null)return amount;int rest=buffer.insert(aspect.tag(),amount);if(rest!=amount)sync();return rest;}
 @Override public boolean contains(Aspect aspect,int amount){return aspect!=null&&buffer.contains(aspect.tag(),amount);}
 @Override public boolean takeFromContainer(Aspect aspect,int amount){if(aspect==null)return false;boolean taken=buffer.extract(aspect.tag(),amount);if(taken)sync();return taken;}
 @Override public AspectAmounts revealedAspects(){var aspect=aspect();return aspect==null||amount()==0?new AspectAmounts():new AspectAmounts().add(aspect,amount());}
 @Override public boolean connectable(Direction side){return side==Direction.UP;}@Override public boolean canInput(Direction side){return connectable(side);}@Override public boolean canOutput(Direction side){return connectable(side);}
 @Override public Aspect suctionType(Direction side){return null;}@Override public int suction(Direction side){return 0;}@Override public int minimumSuction(){return 0;}
 @Override public Aspect essentiaType(Direction side){return aspect();}@Override public int essentiaAmount(Direction side){return amount();}
 @Override public int takeEssentia(Aspect aspect,int amount,Direction side){return canOutput(side)&&takeFromContainer(aspect,amount)?amount:0;}
 @Override public int addEssentia(Aspect aspect,int amount,Direction side){return canInput(side)?amount-addToContainer(aspect,amount):0;}
 public static void tick(Level level,BlockPos pos,BlockState state,InfinityJarBlockEntity tile){var before=tile.buffer.snapshot();tile.buffer.tick(level.random.nextBoolean());var after=tile.buffer.snapshot();if(before.amount()!=after.amount())tile.sync();else if(!before.equals(after))tile.setChanged();}
 @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);var state=buffer.snapshot();tag.putString("Aspect",state.aspect());tag.putInt("Amount",state.amount());tag.putInt("Refill",state.refill());}
 @Override public void load(CompoundTag tag){super.load(tag);buffer.restore(new InfinityJarBuffer.Snapshot(tag.getString("Aspect"),tag.getInt("Amount"),tag.getInt("Refill")));}
}
