package technomancy;

import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import tc4revival.aspect.AspectAmounts;
import tc4revival.content.*;

/** Original 80-tick heating bursts and six-tick infernal acceleration, preserving furnace state. */
public final class BellowsLogic {
    public static void tick(MachineEntity m){
        boolean wasActive=m.active;m.active=false;
        if(m.runs()){
            var target=MachineLogic.loaded(m,m.getBlockPos().relative(m.facing()));
            var further=MachineLogic.loaded(m,m.getBlockPos().relative(m.facing(),2));
            if(further instanceof InfernalFurnaceBlockEntity f&&f.core())target=f;
            int base=TechConfig.BELLOWS.get(),burst=base*6;
            if(target instanceof InfernalFurnaceBlockEntity furnace&&furnace.core()){
                if(furnace.process().cookTime()>6&&m.energy>=base){
                    var tag=new CompoundTag();furnace.process().save(tag);tag.putInt("CookTime",tag.getInt("CookTime")-6);furnace.process().load(tag);furnace.changed();m.pay(base);m.active=true;
                }
            }else if(target instanceof AlchemyFurnaceBlockEntity furnace){
                var aspects=AlchemyFurnaceBlockEntity.inputAspects(furnace.getItem(0));
                if(m.energy>=burst&&furnace.getItem(1).isEmpty()&&!aspects.isEmpty()&&furnace.vis()+aspects.total()<furnace.capacity()&&furnace.data.get(1)<=2){
                    var tag=furnace.saveWithoutMetadata();tag.putInt("BurnTime",80);tag.putBoolean("speedBoost",true);furnace.load(tag);sync(furnace);m.pay(burst);m.active=true;
                }
            }else if(target instanceof AbstractFurnaceBlockEntity furnace&&m.energy>=burst){
                RecipeType<? extends AbstractCookingRecipe> type=furnace instanceof BlastFurnaceBlockEntity?RecipeType.BLASTING:furnace instanceof SmokerBlockEntity?RecipeType.SMOKING:RecipeType.SMELTING;
                var recipe=m.server().getRecipeManager().getRecipeFor(type,furnace,m.server());
                if(recipe.isPresent()){
                    var output=recipe.get().getResultItem(m.server().registryAccess());var slot=furnace.getItem(2);
                    if(!output.isEmpty()&&(slot.isEmpty()||ItemStack.isSameItemSameTags(output,slot)&&output.getCount()+slot.getCount()<=Math.min(slot.getMaxStackSize(),furnace.getMaxStackSize()))){
                        var tag=furnace.saveWithoutMetadata();boolean changed=false;
                        if(tag.getInt("BurnTime")<=2){tag.putShort("BurnTime",(short)80);tag.putShort("CookTimeTotal",(short)recipe.get().getCookingTime());m.pay(burst);changed=true;}
                        if(m.ticks%2==0&&tag.getInt("CookTime")>0&&tag.getInt("CookTime")<tag.getInt("CookTimeTotal")-1){tag.putShort("CookTime",(short)(tag.getInt("CookTime")+1));changed=true;}
                        if(changed){furnace.load(tag);sync(furnace);var state=furnace.getBlockState();if(state.hasProperty(BlockStateProperties.LIT))m.server().setBlock(furnace.getBlockPos(),state.setValue(BlockStateProperties.LIT,true),3);m.active=true;}
                    }
                }
            }else if(target!=null&&m.energy>=burst&&AutomagyHeat.heat(target)){m.pay(burst);m.active=true;}
            else if(target instanceof thaumcraft.api.devices.ExternalHeatReceiver receiver){
                int cost=receiver.receiveHeat(m.energy,Math.max(1,burst/80),false,false);if(cost>0){m.pay(cost);m.active=true;}
            }
        }
        if(wasActive!=m.active)m.changed();
    }
    private static void sync(BlockEntity entity){entity.setChanged();entity.getLevel().sendBlockUpdated(entity.getBlockPos(),entity.getBlockState(),entity.getBlockState(),3);}

    /** Optional public API bridge. No private fields or mixins and no class loading when absent. */
    private static final class AutomagyHeat {
        private static boolean checked,failed;
        private static Class<?> api;
        private static Method burn,writeBurn,stored,capacity,boost,aspects;
        private static boolean heat(BlockEntity entity){
            if(failed||!net.minecraftforge.fml.ModList.get().isLoaded("automagy"))return false;
            try{
                if(!checked){
                    checked=true;api=Class.forName("automagy.api.EssentiaDistillery");burn=api.getMethod("burnTime");writeBurn=api.getMethod("burnTime",int.class);
                    stored=api.getMethod("storedEssentia");capacity=api.getMethod("essentiaCapacity");boost=api.getMethod("speedBoosted",boolean.class);
                    aspects=Class.forName("automagy.logic.BoilerFluids").getMethod("aspects",FluidStack.class);
                }
                if(!api.isInstance(entity)||(int)burn.invoke(entity)>2)return false;
                var items=entity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
                if(items.isEmpty())return false;
                for(int i=0;i<items.get().getSlots();i++)if(!items.get().getStackInSlot(i).isEmpty())return false;
                var fluid=entity.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve();if(fluid.isEmpty()||fluid.get().getTanks()==0)return false;
                var input=fluid.get().getFluidInTank(0);if(input.getAmount()<1000)return false;
                var yield=(AspectAmounts)aspects.invoke(null,input);if(yield.isEmpty()||yield.total()+(int)stored.invoke(entity)>(int)capacity.invoke(entity))return false;
                writeBurn.invoke(entity,80);boost.invoke(entity,true);sync(entity);return true;
            }catch(ReflectiveOperationException|LinkageError ex){
                failed=true;org.slf4j.LoggerFactory.getLogger("Technomancy").warn("Automagy public distillery API is unavailable; electric heating disabled for this integration",ex);return false;
            }
        }
    }
    private BellowsLogic(){}
}
