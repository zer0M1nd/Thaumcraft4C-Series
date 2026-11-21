package arcanearteries;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import wayoftime.bloodmagic.altar.IBloodAltar;
import wayoftime.bloodmagic.common.registries.BloodMagicDamageTypes;
import wayoftime.bloodmagic.util.helper.PlayerHelper;
import wayoftime.bloodmagic.util.helper.PlayerSacrificeHelper;
import arcanearteries.SacrificeValues.School;
import arcanearteries.SacrificeValues.Victim;

public class MobSacrificeKnife extends Item {
    public final School school;
    public MobSacrificeKnife(School school) { super(new Properties().stacksTo(1)); this.school = school; }
    @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        return entity instanceof LivingEntity target && sacrifice(target, player);
    }
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { return sacrifice(target, attacker); }
    public boolean sacrifice(LivingEntity target, LivingEntity attacker) {
        if (target == null || attacker == null || attacker.level().isClientSide || !ArcaneArteries.available(school, true)
                || attacker instanceof Player player && PlayerHelper.isFakePlayer(player)) return false;
        if (target instanceof Player || !target.canChangeDimensions() || target.isBaby() || !target.isAlive() || target.getHealth() < 0.5f) return false;
        IBloodAltar altar = PlayerSacrificeHelper.getAltar(target.level(), target.blockPosition());
        if (altar == null) return false;
        Victim kind = target instanceof Villager ? Victim.VILLAGER : target instanceof Slime ? Victim.SLIME
                : target instanceof EnderMan ? Victim.ENDERMAN : target instanceof Animal ? Victim.ANIMAL : Victim.DEFAULT;
        int lp = school == School.CUSTOM ? switch (kind) {
            case VILLAGER -> ArteriesConfig.value(ArteriesConfig.VILLAGER_LP);
            case SLIME -> ArteriesConfig.value(ArteriesConfig.SLIME_LP);
            case ENDERMAN -> ArteriesConfig.value(ArteriesConfig.ENDERMAN_LP);
            case ANIMAL -> ArteriesConfig.value(ArteriesConfig.ANIMAL_LP);
            default -> ArteriesConfig.value(ArteriesConfig.MOB_LP);
        } : SacrificeValues.mob(school, kind);
        if (lp <= 0) return false;
        // Commit LP only after a real death. Cancellation, invulnerability and totems cannot duplicate LP.
        target.invulnerableTime = 0;
        target.hurt(new net.minecraft.world.damagesource.DamageSource(target.level().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(BloodMagicDamageTypes.SACRIFICE)), Float.MAX_VALUE);
        if (target.isAlive()) return false;
        altar.sacrificialDaggerCall(lp, true);
        altar.startCycle();
        SelfSacrificeKnife.effects(target);
        return true;
    }
    @Override public Component getName(ItemStack stack) { return school == School.CUSTOM ? Component.literal(ArteriesConfig.value(ArteriesConfig.MOB_NAME)) : super.getName(stack); }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(school == School.CUSTOM ? Component.literal(ArteriesConfig.value(ArteriesConfig.MOB_DESCRIPTION)) : Component.translatable("tooltip.arcanearteries.mob"));
    }
}
