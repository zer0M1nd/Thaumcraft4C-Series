package fmmodern;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.api.IScribeTools;
import thaumcraft.api.IRepairable;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import tc4revival.content.TCEffects;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class SpecialItems {
    public static class TaintFruit extends Item {
        public TaintFruit() { super(new Properties().food(new FoodProperties.Builder().nutrition(4).saturationMod(.8f).build())); }
        @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
            var result = super.finishUsingItem(stack, level, user);
            if (user instanceof ServerPlayer player) {
                ThaumcraftApiHelper.addStickyWarpToPlayer(player, 1);
                uncurable(player, TCEffects.FLUX_TAINT.get()); uncurable(player, MobEffects.HUNGER);
                if (player.getRandom().nextFloat() < .4f) { uncurable(player, TCEffects.THAUMARHIA.get()); player.displayClientMessage(Component.translatable("warp.text.15").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), false); }
            }
            return result;
        }
        private static void uncurable(Player player, MobEffect type) { var effect = new MobEffectInstance(type, 600, 0, true, true); effect.setCurativeItems(List.of()); player.addEffect(effect); }
    }
    public static class MobCrystal extends Item {
        public MobCrystal() { super(new Properties().stacksTo(1)); }
        public static ResourceLocation mob(ItemStack stack) { return stack.hasTag() ? ResourceLocation.tryParse(stack.getTag().getString("mob")) : null; }
        @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){var id=mob(stack);if(id!=null&&CageMobs.allowed(id))lines.add(Component.literal(thaumcraft.api.aspects.AspectViews.display(CageMobs.aspect(id)).displayName()));}
        @Override public Component getName(ItemStack stack) {
            var id = mob(stack); var type = id == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(id);
            return type == null ? Component.translatable("item.forbidden_magic.mob_crystal_empty") : Component.translatable("item.forbidden_magic.mob_crystal", type.getDescription());
        }
    }
    public static class RitualWeapon extends SwordItem implements IRepairable {
        private final boolean fork;
        public RitualWeapon(boolean fork) { super(fork?tc4revival.content.ThaumiumToolTier.INSTANCE:tc4revival.content.ElementalToolTier.INSTANCE, 3, -2.4f, new Properties().rarity(Rarity.UNCOMMON)); this.fork = fork; }
        @Override public Component getName(ItemStack stack){return fork&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.SPORK)?Component.translatable("item.forbidden_magic.diabolist_spork"):super.getName(stack);}
        @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return fork && ingredient.is(Items.QUARTZ) || !fork && ingredient.is(tc4revival.content.TCItems.material(tc4revival.content.MaterialType.THAUMIUM)) || super.isValidRepairItem(stack, ingredient); }
    }
    public static class RidingCrop extends SwordItem {
        public RidingCrop() { super(Tiers.WOOD, 3, -2.4f, new Properties()); }
        @Override public boolean hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity owner) {
            super.hurtEnemy(stack, victim, owner);
            if (!owner.level().isClientSide) {
                if (!ForbiddenConfig.value(ForbiddenConfig.NO_LUST) && owner.level().dimension() == Level.NETHER
                        && victim instanceof net.minecraft.world.entity.monster.Enemy
                        && SinRules.cropLust(victim.getRandom().nextInt(SinRules.LUST_ROLL_BOUND)))
                    victim.spawnAtLocation(Content.item("lust_shard"));
                if (victim instanceof AbstractHorse || victim instanceof Pig) victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 5));
                else if (victim instanceof Player || victim instanceof AbstractGolem || victim instanceof tc4revival.content.GolemEntity) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1)); victim.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1)); victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                }
            } return true;
        }
        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (!(player.getVehicle() instanceof LivingEntity mount)) return InteractionResultHolder.pass(stack);
            if (!level.isClientSide) { stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand)); mount.hurt(player.damageSources().generic(), 1); mount.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 5)); }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return ingredient.is(Items.LEATHER) || super.isValidRepairItem(stack, ingredient); }
    }
    public static class Inkwell extends Item implements IScribeTools {
        private final boolean infinite;
        public Inkwell(boolean infinite) { super(new Properties().durability(100).rarity(infinite ? Rarity.EPIC : Rarity.COMMON)); this.infinite = infinite; }
        @Override public int getInk(ItemStack stack) { return infinite ? 100 : IScribeTools.super.getInk(stack); }
        @Override public void setInk(ItemStack stack, int ink) { if (!infinite) IScribeTools.super.setInk(stack, ink); }
        @Override public boolean isFoil(ItemStack stack) { return !infinite && getInk(stack) == 0; }
        @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (infinite || getInk(stack) > 0 || easyResearch()) return InteractionResultHolder.pass(stack);
            if (player instanceof ServerPlayer serverPlayer) {
                for (Aspect aspect : Aspect.getPrimalAspects()) ThaumcraftApiHelper.addResearchPoints(serverPlayer, aspect, (short)(4 + player.getRandom().nextInt(4)));
                var replacement = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumcraft", "scribing_tools"))); replacement.setDamageValue(replacement.getMaxDamage());
                return InteractionResultHolder.success(replacement);
            } return InteractionResultHolder.success(stack);
        }
        @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            if (infinite || !easyResearch() || !(entity instanceof ServerPlayer player) || player.tickCount % 200 != 0) return;
            var aspects = Aspect.getPrimalAspects(); ThaumcraftApiHelper.addResearchPoints(player, aspects.get(player.getRandom().nextInt(aspects.size())), (short)(1 + player.getRandom().nextInt(4)));
            stack.setDamageValue(stack.getDamageValue() + 1); if (stack.getDamageValue() >= 50) stack.shrink(1);
        }
        private boolean easyResearch() { return tc4revival.config.TCConfig.SERVER.researchDifficulty.get() == -1; }
    }
    public static class NutritionRing extends Item implements ICurioItem, thaumcraft.api.IRunicArmor {
        public NutritionRing() { super(new Properties().stacksTo(1)); }
        @Override public int getRunicCharge(ItemStack stack) { return 0; }
    }
}
