package technomancy;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;
import thaumcraft.api.wands.WandRod;

public final class TechWands {
    public static void setup(){WandRod.registerUpdate("technomancy:electric",TechWands::chargeFromInventory);}
    private static void chargeFromInventory(ItemStack stack,Player player) {
        if(player.level().isClientSide||!(stack.getItem() instanceof CastingWandItem wand))return;
        int stored=stack.hasTag()?stack.getTag().getInt("technomancyEnergy"):0;
        if(TCAspects.catalog().primals().stream().allMatch(a->wand.vis(stack,a)>=wand.capacity(stack)))return;
        for(var aspect:TCAspects.catalog().primals()) {
            if(wand.vis(stack,aspect)>=wand.capacity(stack))continue;
            for(var battery:player.getInventory().items) {
                if(battery==stack||battery.isEmpty()||stored>=20000)continue;
                var energy=battery.getCapability(ForgeCapabilities.ENERGY).resolve();if(energy.isEmpty()||!energy.get().canExtract())continue;
                int offer=energy.get().extractEnergy(20000-stored,true);
                if(offer>0)stored+=Math.min(20000-stored,energy.get().extractEnergy(offer,false));
            }
            while(stored>=100&&wand.vis(stack,aspect)<wand.capacity(stack)) {
                int filled=wand.receiveVis(stack,aspect,Math.min(100,stored/100),false);
                if(filled==0)break;stored-=filled*100;
            }
        }
        stack.getOrCreateTag().putInt("technomancyEnergy",stored);
    }
    public static class Pen extends ScribingToolsItem {
        @Override public int getMaxDamage(ItemStack stack){return 3000;}
    }
    public static class Scepter extends CastingWandItem {
        @Override public WandParts.Rod rod(ItemStack stack){return WandParts.Rod.require("technomancy:technoturge");}
        @Override public boolean isSceptre(ItemStack stack){return true;}
        @Override public Component getName(ItemStack stack){return Component.translatable("item.technomancy.technoturge_scepter");}
        @Override public ItemStack getDefaultInstance(){return create(rod(ItemStack.EMPTY),WandParts.Cap.require("thaumium"),true,false);}
        @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
            var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();var state=level.getBlockState(pos);
            if(player==null||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.FAIL;
            if(state.getBlock() instanceof MachineBlock&&player.isShiftKeyDown()) {
                if(!level.isClientSide&&!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.BlockEvent.BreakEvent(level,pos,state,player)))level.destroyBlock(pos,true,player);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if(state.getBlock() instanceof MachineBlock machine&&machine.kind==MachineKind.NODE_FABRICATOR) {
                if(!level.isClientSide&&level.getBlockEntity(pos) instanceof MachineEntity m)NodeStructure.rotate(m,player);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            var rotated=state.rotate(level,pos,Rotation.CLOCKWISE_90);
            if(rotated!=state) {
                if(!level.isClientSide&&!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.BlockEvent.BreakEvent(level,pos,state,player)))level.setBlock(pos,rotated,3);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return super.onItemUseFirst(stack,context);
        }
        @Override public ICapabilityProvider initCapabilities(ItemStack stack,@Nullable CompoundTag nbt) {
            return new ICapabilityProvider(){
                private final LazyOptional<IEnergyStorage> energy=LazyOptional.of(()->new IEnergyStorage(){
                    public int receiveEnergy(int amount,boolean simulate){
                        int buffered=stack.hasTag()?stack.getTag().getInt("technomancyEnergy"):0;
                        int missing=TCAspects.catalog().primals().stream().mapToInt(a->Math.max(0,capacity(stack)-vis(stack,a))).sum();
                        int accepted=(int)Math.min(Math.max(0,amount),Math.max(0L,(long)missing*100-buffered));
                        if(simulate||accepted==0)return accepted;
                        int remaining=buffered+accepted;
                        for(var a:TCAspects.catalog().primals())while(remaining>=100&&vis(stack,a)<capacity(stack)) {
                            int filled=receiveVis(stack,a,Math.min(100,remaining/100),false);if(filled==0)break;remaining-=filled*100;
                        }
                        stack.getOrCreateTag().putInt("technomancyEnergy",remaining);return accepted;
                    }
                    public int extractEnergy(int amount,boolean simulate){return 0;}
                    public int getEnergyStored(){return TCAspects.catalog().primals().stream().mapToInt(a->vis(stack,a)).sum()*100+(stack.hasTag()?stack.getTag().getInt("technomancyEnergy"):0);}
                    public int getMaxEnergyStored(){return capacity(stack)*TCAspects.catalog().primals().size()*100;}
                    public boolean canExtract(){return false;}public boolean canReceive(){return true;}
                });
                @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@Nullable Direction side){return cap==ForgeCapabilities.ENERGY?energy.cast():LazyOptional.empty();}
            };
        }
    }
    private TechWands(){}
}
