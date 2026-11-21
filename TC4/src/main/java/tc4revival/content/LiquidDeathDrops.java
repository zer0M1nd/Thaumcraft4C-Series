package tc4revival.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.aspect.AspectAmounts;

@Mod.EventBusSubscriber(modid="thaumcraft")
public  class LiquidDeathDrops {
    public static final ResourceKey<DamageType> DISSOLVE=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("thaumcraft:dissolve"));
    private LiquidDeathDrops() {}
    public static DamageSource source(Level level) { return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DISSOLVE)); }
    public static java.util.List<ItemStack> crystals(AspectAmounts aspects,RandomSource random) {
        var result=new java.util.ArrayList<ItemStack>();
        for (var aspect:aspects.aspects()) {
            int amount=aspects.amount(aspect); if (random.nextBoolean() || amount<=0) continue;
            result.add(TCItems.ESSENCE_CRYSTAL.get().of(aspect,Math.max(1,(1+random.nextInt(amount))/2)));
        }
        return result;
    }
    public static void drops(LivingDropsEvent event) {
        var entity=event.getEntity(); if (entity.level().isClientSide || !event.getSource().is(DISSOLVE)) return;
        LegacyEntityDrops.addTable(event,"legacy_mob_bonus/dissolution");
    }
}
