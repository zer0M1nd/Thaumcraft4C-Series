package thaumicfutures.compat;

import blusunrize.immersiveengineering.api.tool.ChemthrowerHandler;
import blusunrize.immersiveengineering.api.tool.ExternalHeaterHandler;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import tc4revival.content.AlchemyFurnaceBlockEntity;
import tc4revival.content.TCEffects;

public final class ImmersiveEngineeringCompat {
    private ImmersiveEngineeringCompat() {}
    public static void register() {
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, ImmersiveEngineeringCompat::attach);
    }
    private static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof AlchemyFurnaceBlockEntity furnace)) return;
        LazyOptional<ExternalHeaterHandler.IExternalHeatable> heat = LazyOptional.of(() ->
                (available, redstone) -> thaumicfutures.FutureFeatures.Group.EXTERNAL_HEAT.enabled()
                        ? furnace.receiveHeat(available, ExternalHeaterHandler.defaultFurnaceEnergyCost, redstone, false) : 0);
        event.addCapability(new ResourceLocation("thaumicfutures", "external_heater"), new ICapabilityProvider() {
            @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
                return cap == ExternalHeaterHandler.CAPABILITY ? heat.cast() : LazyOptional.empty();
            }
        });
        event.addListener(heat::invalidate);
    }
    public static void setup() {
        ChemthrowerHandler.registerEffect(tag("liquid_death"), new ChemthrowerHandler.ChemthrowerEffect() {
            @Override public void applyToEntity(LivingEntity target, Player shooter, ItemStack thrower, Fluid fluid) {
                if (target.level().isClientSide || !thaumicfutures.FutureFeatures.Group.CHEMTHROWER.enabled()) return;
                var key = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("thaumcraft", "dissolve"));
                target.hurt(new net.minecraft.world.damagesource.DamageSource(
                        target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), shooter), 4);
            }
            @Override public void applyToBlock(Level level, HitResult hit, Player shooter, ItemStack thrower, Fluid fluid) {}
        });
        ChemthrowerHandler.registerEffect(tag("purifying_water"), new ChemthrowerHandler.ChemthrowerEffect_Potion(
                null, 0, new MobEffectInstance(TCEffects.WARP_WARD.get(), 100)) {
            @Override public void applyToEntity(LivingEntity target, Player shooter, ItemStack thrower, Fluid fluid) {
                if(thaumicfutures.FutureFeatures.Group.CHEMTHROWER.enabled()) super.applyToEntity(target,shooter,thrower,fluid);
            }
        });
        ChemthrowerHandler.registerEffect(tag("flux_goo"), new ChemthrowerHandler.ChemthrowerEffect_Potion(
                null, 0, new MobEffectInstance(TCEffects.VIS_EXHAUST.get(), 100)) {
            @Override public void applyToEntity(LivingEntity target, Player shooter, ItemStack thrower, Fluid fluid) {
                if(thaumicfutures.FutureFeatures.Group.CHEMTHROWER.enabled()) super.applyToEntity(target,shooter,thrower,fluid);
            }
        });
    }
    private static TagKey<Fluid> tag(String path) {
        return TagKey.create(Registries.FLUID, new ResourceLocation("thaumicfutures", path));
    }
}
