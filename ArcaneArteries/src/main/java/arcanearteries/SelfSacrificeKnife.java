package arcanearteries;

import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import wayoftime.bloodmagic.ConfigManager;
import wayoftime.bloodmagic.common.item.ItemSacrificialDagger;
import wayoftime.bloodmagic.common.registries.BloodMagicDamageTypes;
import wayoftime.bloodmagic.event.SacrificeKnifeUsedEvent;
import wayoftime.bloodmagic.util.helper.IncenseHelper;
import wayoftime.bloodmagic.util.helper.PlayerHelper;
import wayoftime.bloodmagic.util.helper.PlayerSacrificeHelper;
import arcanearteries.SacrificeValues.School;

public class SelfSacrificeKnife extends ItemSacrificialDagger {
    public final School school;
    public SelfSacrificeKnife(School school) { this.school = school; }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (PlayerHelper.isFakePlayer(player) || !ArcaneArteries.available(school, false)) return InteractionResultHolder.fail(stack);
        // The server owns health, LP, mana and event dispatch. A client may only begin the animation.
        if (canUseForSacrifice(stack)) {
            if (!level.isClientSide && !mana(stack, player, false)) return InteractionResultHolder.fail(stack);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        var fray = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("bloodmagic", "soulfray"));
        int lp = SacrificeValues.self(school, ArteriesConfig.value(ArteriesConfig.HARD_DAGGERS), fray != null && player.hasEffect(fray),
                ArteriesConfig.value(ArteriesConfig.SELF_LP), ArteriesConfig.value(ArteriesConfig.FRAY_LP));
        int hp = school == School.CUSTOM ? ArteriesConfig.value(ArteriesConfig.HEALTH) : 2;
        return sacrifice(stack, player, hp, lp) ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (level.isClientSide || !(user instanceof Player player) || PlayerHelper.isFakePlayer(player)
                || !ArcaneArteries.available(school, false)) return;
        double incense = PlayerSacrificeHelper.getPlayerIncense(player);
        float floor = player.getMaxHealth() / 10.0f;
        if (incense <= 0 || player.getHealth() <= floor || PlayerSacrificeHelper.getAltar(level, player.blockPosition()) == null) return;
        float hp = player.getHealth() - floor;
        int lp = (int)(hp * ConfigManager.COMMON.sacrificialDaggerConversion.get() * PlayerSacrificeHelper.getModifier(incense));
        if (sacrifice(stack, player, hp, lp)) {
            PlayerSacrificeHelper.setPlayerIncense(player, 0);
            IncenseHelper.setHasMaxIncense(stack, player, false);
            setUseForSacrifice(stack, false);
        }
    }

    private boolean sacrifice(ItemStack stack, Player player, float hp, int lp) {
        if (!mana(stack, player, false)) return false;
        SacrificeKnifeUsedEvent event = new SacrificeKnifeUsedEvent(player, !player.isCreative(), true, (int)hp, lp);
        if (MinecraftForge.EVENT_BUS.post(event)) return false;
        if (!event.shouldDrainHealth && !event.shouldFillAltar) return false;
        if (!mana(stack, player, true)) return false;
        if (event.shouldDrainHealth) {
            // Match the legacy direct health payment (no armor/absorption discount), using BM's death source.
            float remaining = player.getHealth() - hp;
            player.setHealth(Math.max(0.0001f, remaining));
            if (remaining <= 0) {
                player.invulnerableTime = 0;
                player.hurt(new net.minecraft.world.damagesource.DamageSource(player.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(BloodMagicDamageTypes.SACRIFICE)), Float.MAX_VALUE);
            }
        }
        if (event.shouldFillAltar && event.lpAdded > 0)
            PlayerSacrificeHelper.findAndFillAltar(player.level(), player, event.lpAdded, false);
        effects(player);
        return true;
    }
    private boolean mana(ItemStack stack, Player player, boolean consume) {
        return school != School.BOTANICAL || !ArteriesConfig.value(ArteriesConfig.REQUIRE_MANA) || ArteriesConfig.value(ArteriesConfig.MANA_COST) == 0
                || BotaniaBridge.request(stack, player, consume);
    }
    static void effects(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel server) {
            server.playSound(null, entity.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 2.6f);
            server.sendParticles(DustParticleOptions.REDSTONE, entity.getX(), entity.getY() + 0.5, entity.getZ(), 8, 0.4, 0.4, 0.4, 0);
        }
    }
    @Override public boolean isFoil(ItemStack stack) { return canUseForSacrifice(stack) || super.isFoil(stack); }
    @Override public Component getName(ItemStack stack) {
        if (net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient() && ArteriesConfig.ORBS.get())
            return school == School.CUSTOM ? Component.literal(ArteriesConfig.value(ArteriesConfig.ORB_NAME))
                    : Component.translatable("item.arcanearteries." + school.name().toLowerCase(java.util.Locale.ROOT) + "_orb");
        if (school == School.CUSTOM) return Component.literal(ArteriesConfig.value(ArteriesConfig.SELF_NAME));
        return super.getName(stack);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (school == School.CUSTOM) tooltip.add(Component.literal(ArteriesConfig.value(ArteriesConfig.SELF_DESCRIPTION)));
        else tooltip.add(Component.translatable("tooltip.arcanearteries.self"));
        if (school == School.BOTANICAL) tooltip.add(Component.translatable("tooltip.arcanearteries.mana", ArteriesConfig.value(ArteriesConfig.MANA_COST)));
    }
}
