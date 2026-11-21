package heartwork.compat.blade;

import java.util.List;
import mods.flammpfeil.slashblade.capability.slashblade.NamedBladeStateCapabilityProvider;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public class FluxBladeItem extends HeartworkBladeItem {
    public static final int CAPACITY=4_000_000,TRANSFER=20_000;
    public FluxBladeItem(){super("rf");}
    public static int energy(ItemStack stack){return Math.max(0,Math.min(CAPACITY,stack.getOrCreateTag().getInt("Energy")));}
    public static boolean empowered(ItemStack stack){return energy(stack)>0&&stack.getOrCreateTag().getBoolean("Empowered");}
    public static int extract(ItemStack stack,int amount,boolean simulate){
        int used=Math.min(Math.max(0,amount),energy(stack));
        if(!simulate){stack.getOrCreateTag().putInt("Energy",energy(stack)-used);if(energy(stack)==0)stack.getOrCreateTag().putBoolean("Empowered",false);}
        return used;
    }
    private static int rank(LivingEntity entity){return entity.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).map(r -> r.getRank(entity.level().getGameTime()).level).orElse(0);}
    public static void toggle(Player player,ItemStack stack){
        if(player.level().isClientSide||!(stack.getItem() instanceof FluxBladeItem))return;
        boolean enable=!empowered(stack)&&energy(stack)>0&&stack.getCapability(BLADESTATE).map(s -> !s.isBroken()).orElse(false);
        stack.getOrCreateTag().putBoolean("Empowered",enable);
        if(enable){
            int originalRank=rank(player),startup=stack.getOrCreateTag().contains("StartupCost")?Math.max(0,stack.getTag().getInt("StartupCost")):1000;
            long[] added={0};
            player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT).ifPresent(r -> {
                // FIX charges in units of the legacy 100-point rank, not modern raw points (300).
                long missing=Math.max(0,r.getUnitCapacity()*5-r.getRankPoint(player.level().getGameTime()));
                added[0]=Math.min(energy(stack),(missing*100+r.getUnitCapacity()-1)/r.getUnitCapacity());
                r.addRankPoint(player,Math.min(missing,added[0]*r.getUnitCapacity()/100));
            });
            extract(stack,(int)Math.min(Integer.MAX_VALUE,(long)startup*(1L<<originalRank)+added[0]*2),false);
        }
        player.level().playSound(null,player.blockPosition(),enable?SoundEvents.LIGHTNING_BOLT_THUNDER:SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.PLAYERS,enable?0.4F:0.2F,enable?1:0.6F);
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        if(!attacker.level().isClientSide&&attacker instanceof Player player&&empowered(stack)){
            int rank=rank(attacker),cost=100*(1<<rank);
            if(extract(stack,cost,false)==cost){target.invulnerableTime=0;target.hurt(new net.minecraft.world.damagesource.DamageSource(attacker.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new net.minecraft.resources.ResourceLocation("heartwork:flux"))),player),rank);}
        }
        return super.hurtEnemy(stack,target,attacker);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
        super.inventoryTick(stack,level,holder,slot,selected);
        if(empowered(stack)&&level.getGameTime()%10==0&&(!selected||holder instanceof Player p&&!p.isUsingItem())){
            if(!level.isClientSide)extract(stack,stack.getOrCreateTag().contains("RunningCost")?Math.max(0,stack.getTag().getInt("RunningCost")):100,false);
            else level.addParticle(net.minecraft.core.particles.ParticleTypes.WITCH,holder.getX(),holder.getY(),holder.getZ(),0,0.02,0);
        }
    }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack,CompoundTag nbt){return new FluxCapabilities(stack);}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,level,lines,flag);lines.add(Component.translatable("heartwork.flux.energy",energy(stack),CAPACITY));
        lines.add(Component.translatable(empowered(stack)?"heartwork.flux.on":"heartwork.flux.off"));
    }
    private static class FluxCapabilities extends NamedBladeStateCapabilityProvider {
        private final LazyOptional<IEnergyStorage> energy;
        FluxCapabilities(ItemStack stack){super(stack);energy=LazyOptional.of(() -> new IEnergyStorage(){
            public int receiveEnergy(int amount,boolean simulate){int value=Math.min(Math.max(0,amount),Math.min(TRANSFER,CAPACITY-energy(stack)));if(!simulate)stack.getOrCreateTag().putInt("Energy",energy(stack)+value);return value;}
            public int extractEnergy(int amount,boolean simulate){return extract(stack,amount,simulate);}
            public int getEnergyStored(){return energy(stack);}
            public int getMaxEnergyStored(){return CAPACITY;}
            public boolean canExtract(){return true;}
            public boolean canReceive(){return true;}
        });}
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ENERGY?energy.cast():super.getCapability(cap,side);}
    }
}
