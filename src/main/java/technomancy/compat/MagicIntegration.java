package technomancy.compat;

import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import technomancy.*;

public final class MagicIntegration {
    public static void tick(MachineEntity m){
        m.active=false;
        if(m.kind==MachineKind.BLOOD_FABRICATOR&&m.runs()){
            var fluid=ForgeRegistries.FLUIDS.getValue(new ResourceLocation("bloodmagic","life_essence_fluid"));
            if(fluid!=null&&m.fluid.getSpace()>=200&&m.pay(TechConfig.BLOOD_FAB.get())){m.fluid.fill(new FluidStack(fluid,200),FluidAction.EXECUTE);m.active=true;}
            if(m.fluid.getFluidAmount()>0)for(var side:Direction.values()){
                var target=MachineLogic.loaded(m,m.getBlockPos().relative(side));if(target==null)continue;
                target.getCapability(ForgeCapabilities.FLUID_HANDLER,side.getOpposite()).ifPresent(handler->{var offer=m.fluid.getFluid().copy();offer.setAmount(Math.min(200,offer.getAmount()));int accepted=handler.fill(offer,FluidAction.EXECUTE);m.fluid.drain(accepted,FluidAction.EXECUTE);});
            }
        }else if(ModList.get().isLoaded("botania"))BotaniaBridge.tick(m);
    }
    public static void pullMana(MachineEntity m,int rate){if(ModList.get().isLoaded("botania"))BotaniaBridge.pull(m,rate);}
    public static boolean payBlood(MachineEntity m,int amount){return m.owner!=null&&ModList.get().isLoaded("bloodmagic")&&BloodBridge.pay(m,amount);}
    private static final class BloodBridge {
        static boolean pay(MachineEntity m,int amount){
            var network=wayoftime.bloodmagic.util.helper.NetworkHelper.getSoulNetwork(m.owner);
            if(network==null||network.getCurrentEssence()<=amount)return false;
            return network.syphon(new wayoftime.bloodmagic.core.data.SoulTicket(amount))==amount;
        }
    }
    private static final class BotaniaBridge {
        static void pull(MachineEntity m,int rate){
            int max=m.kind==MachineKind.BOTANICAL_PURIFIER?1000000:100000;
            if(m.mana>=max)return;
            for(var pos:BlockPos.betweenClosed(m.getBlockPos().offset(-4,0,-4),m.getBlockPos().offset(4,0,4))){
                if(pos.equals(m.getBlockPos()))continue;
                if(MachineLogic.loaded(m,pos) instanceof vazkii.botania.api.mana.ManaPool pool){
                    int take=Math.min(rate,Math.min(max-m.mana,pool.getCurrentMana()));
                    if(m.kind==MachineKind.HIPPIE_DYNAMO&&take<100)continue;
                    if(take>0){pool.receiveMana(-take);m.mana+=take;m.changed();}
                }
            }
        }
        static void tick(MachineEntity m){
            if(!m.runs())return;
            if(m.kind==MachineKind.MANA_FABRICATOR){if(m.mana<=99900&&m.pay(TechConfig.MANA_FAB.get())){m.mana+=100;m.active=true;m.changed();}return;}
            if(m.kind!=MachineKind.MANA_EXCHANGER||m.energy<TechConfig.EXCHANGER.get())return;
            if(!(MachineLogic.loaded(m,m.getBlockPos().above()) instanceof vazkii.botania.api.mana.ManaPool pool))return;
            m.active=true;
            if(m.mode%2==1){if(m.fluid.getFluidAmount()>0&&pool.getCurrentMana()<=pool.getMaxMana()-1000){pool.receiveMana(1000);m.fluid.drain(1,FluidAction.EXECUTE);m.pay(TechConfig.EXCHANGER.get());}}
            else if(m.fluid.getSpace()>0&&pool.getCurrentMana()>=1000){
                var fluid=ForgeRegistries.FLUIDS.getValue(Technomancy.id("mana_condensate"));
                if(fluid!=null){pool.receiveMana(-1000);m.fluid.fill(new FluidStack(fluid,1),FluidAction.EXECUTE);m.pay(TechConfig.EXCHANGER.get());}
            }
        }
    }
    private MagicIntegration(){}
}
