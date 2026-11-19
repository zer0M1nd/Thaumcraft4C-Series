package automagy.content;

import automagy.logic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import tc4revival.aspect.*;
import tc4revival.content.*;

public final class BoilerBlockEntity extends InventoryBlockEntity implements automagy.api.EssentiaDistillery,automagy.api.AspectReadingProvider,RevealedAspectContainer {
    private final BoilerCycle cycle=new BoilerCycle();
    private final Map<Aspect,Integer> essentia=new LinkedHashMap<>();
    public final FluidTank tank=new FluidTank(BoilerCycle.FLUID_CAPACITY){@Override protected void onContentsChanged(){sync();}};
    private LazyOptional<IFluidHandler> fluid=LazyOptional.of(()->tank);
    private int ticks,smeltTime=100;
    private int clientVenting;
    public BoilerBlockEntity(BlockPos pos,BlockState state){super(Content.BOILER_TILE.get(),pos,state,1);}
    @Override protected boolean accepts(int slot,ItemStack stack){return AlchemyFurnaceBlockEntity.fuelTime(stack)>0;}
    public net.minecraftforge.items.IItemHandlerModifiable fuelInventory(){return inventory;}
    public BoilerCycle.State process(){return cycle.state();}
    public int smeltTime(){return smeltTime;}
    @Override public boolean triggerEvent(int id,int data){
        if(id==1){if(level!=null&&level.isClientSide){clientVenting=10;level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.1f,1+level.random.nextFloat()*.1f,false);}return true;}
        return super.triggerEvent(id,data);
    }
    public static void clientTick(Level level,BlockPos pos,BlockState state,BoilerBlockEntity tile){
        if(tile.clientVenting>0){tile.clientVenting--;automagy.client.BoilerEffects.vent(level,pos);}
    }
    @Override public int burnTime(){return cycle.state().burn();}
    @Override public void burnTime(int ticks){var state=cycle.state();cycle.restore(new BoilerCycle.State(ticks,Math.max(state.currentBurn(),ticks),state.cook(),state.boosted()));sync();}
    @Override public boolean speedBoosted(){return cycle.state().boosted();}
    @Override public void speedBoosted(boolean boosted){var state=cycle.state();cycle.restore(new BoilerCycle.State(state.burn(),state.currentBurn(),state.cook(),boosted));sync();}
    @Override public int storedEssentia(){return (int)Math.min(Integer.MAX_VALUE,essentia.values().stream().mapToLong(Integer::longValue).sum());}
    @Override public int essentiaCapacity(){return BoilerCycle.ESSENTIA_CAPACITY;}
    @Override public AspectAmounts revealedAspects(){var values=new AspectAmounts();essentia.forEach(values::add);return values;}
    @Override public AspectReading aspectReading(){return new AspectReading(AspectReadings.map(revealedAspects()),Map.of(),50,50,false,true);}
    private BoilerCycle.Fuel consumeFuel(){
        var stack=stored(0);int duration=AlchemyFurnaceBlockEntity.fuelTime(stack);if(duration<=0)return new BoilerCycle.Fuel(0,false);
        boolean boosted=stack.is(TCItems.ALUMENTUM.get());var remainder=stack.hasCraftingRemainingItem()?stack.getCraftingRemainingItem():ItemStack.EMPTY;
        var rest=stack.copy();rest.shrink(1);inventory.setStackInSlot(0,rest.isEmpty()?remainder:rest);
        if(!rest.isEmpty()&&!remainder.isEmpty())net.minecraft.world.level.block.Block.popResource(level,worldPosition,remainder);
        return new BoilerCycle.Fuel(duration,boosted);
    }
    private void distribute(){
        var column=new ArrayList<BoilerOutput.Vessel<Aspect>>();
        for(int i=1;i<=5;i++){
            var pos=worldPosition.above(i);if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic))break;
            column.add(new BoilerOutput.Vessel<>(){
                public Aspect aspect(){return alembic.aspect();}public Aspect filter(){return alembic.filter();}public int amount(){return alembic.amount();}
                public boolean acceptOne(Aspect aspect){return alembic.amount()<alembic.capacity()&&(alembic.filter()==null||alembic.filter().equals(aspect))&&alembic.addToContainer(aspect,1)==0;}
            });
        }
        if(BoilerOutput.distribute(essentia,column,new Random(level.random.nextLong()))>0)sync();
    }
    public static void tick(Level level,BlockPos pos,BlockState state,BoilerBlockEntity tile){
        if(level.isClientSide)return;tile.ticks++;
        if(tile.ticks%tile.cycle.outputInterval()==0&&!tile.essentia.isEmpty())tile.distribute();
        var before=tile.cycle.state();var input=BoilerFluids.aspects(tile.tank.getFluid());int amount=(int)Math.min(Integer.MAX_VALUE,input.total());
        int bellows=BellowsBlockEntity.count(level,pos,List.of(Direction.values()));tile.smeltTime=BoilerCycle.duration(amount,bellows);
        if(tile.cycle.tick(tile.tank.getFluidAmount(),tile.storedEssentia(),amount,bellows,tile::consumeFuel)){
            // Capture input aspects above before draining the last bucket from the tank.
            var drained=tile.tank.drain(1000,IFluidHandler.FluidAction.EXECUTE);
            if(drained.getAmount()==1000){input.view().forEach((aspect,n)->tile.essentia.merge(aspect,n,Integer::sum));tile.sync();
                level.blockEvent(pos,state.getBlock(),1,0);
            }
        }
        if(!before.equals(tile.cycle.state()))tile.setChanged();
        boolean burning=tile.burnTime()>0;if(state.getValue(BoilerBlock.LIT)!=burning)level.setBlock(pos,state.setValue(BoilerBlock.LIT,burning),3);
        if(tile.ticks%20==0)tile.sync();
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){
        if(side==Direction.UP&&cap==ForgeCapabilities.FLUID_HANDLER)return LazyOptional.empty();
        if(cap==ForgeCapabilities.FLUID_HANDLER)return isRemoved()?LazyOptional.empty():fluid.cast();return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();fluid.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();fluid=LazyOptional.of(()->tank);}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);tag.put("Tank",tank.writeToNBT(new CompoundTag()));AspectNbt.write(tag,revealedAspects());var state=cycle.state();
        tag.putInt("Burn",state.burn());tag.putInt("CurrentBurn",state.currentBurn());tag.putInt("Cook",state.cook());tag.putBoolean("Boosted",state.boosted());tag.putInt("SmeltTime",smeltTime);
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);tank.readFromNBT(tag.getCompound("Tank"));essentia.clear();essentia.putAll(AspectNbt.read(tag,TCAspects.catalog()).amounts().view());
        cycle.restore(new BoilerCycle.State(tag.getInt("Burn"),tag.getInt("CurrentBurn"),tag.getInt("Cook"),tag.getBoolean("Boosted")));smeltTime=Math.max(1,tag.getInt("SmeltTime"));
    }
}
