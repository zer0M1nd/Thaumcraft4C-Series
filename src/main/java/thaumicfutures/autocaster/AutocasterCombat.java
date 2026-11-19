package thaumicfutures.autocaster;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.FakePlayer;
import tc4revival.content.*;
import thaumcraft.api.wands.*;

/** Drives the same cast/use/release hooks as a player and the Omniverse maid. */
public class AutocasterCombat {
    private final AutocasterEntity turret;
    private final Caster actor;
    private int useTicks;
    private static final ThreadLocal<AutocasterEntity> ACTIVE=new ThreadLocal<>();
    public static AutocasterEntity casting() { return ACTIVE.get(); }
    private static class Caster extends FakePlayer {
        Caster(ServerLevel level,GameProfile profile) { super(level,profile); }
        @Override public float getStandingEyeHeight(Pose pose,EntityDimensions dimensions) { return .45f; }
        void advance() { attackStrengthTicker=Math.min(10000,attackStrengthTicker+1); }
    }
    public AutocasterCombat(AutocasterEntity turret) {
        this.turret=turret;
        var uuid=UUID.nameUUIDFromBytes(("ThaumicAutocaster:"+turret.getUUID()).getBytes(StandardCharsets.UTF_8));
        actor=new Caster((ServerLevel)turret.level(),new GameProfile(uuid,"Autocaster"));
        WandActors.bind(actor,turret);
    }
    public void tick(LivingEntity target) {
        var stack=turret.getItem(0);var focus=turret.focus();
        if(!(stack.getItem() instanceof CastingWandItem wand) || !(focus.getItem() instanceof WandFocusItem item)
                || !turret.validTarget(target) || !AutocasterFoci.hasPower(focus,turret.getItem(1)) || !WandAutomation.allowed(stack,actor)) { stop();return; }
        var aim=switch(AutocasterFoci.id(focus)) {
            case "emt:energy_ball_focus"->target.position().add(0,-.25,0);
            // Decomposition checks a small box one block along the look ray. Aim at
            // the nearest height within the body so the short turret can reach it.
            case "thaumicbases:focus_experience"->new net.minecraft.world.phys.Vec3(target.getX(),
                    Mth.clamp(turret.getEyeY(),target.getBoundingBox().minY+.1,target.getBoundingBox().maxY-.1),target.getZ());
            default->target.getEyePosition();
        };
        var delta=aim.subtract(turret.getEyePosition());
        float yaw=(float)(Mth.atan2(delta.z,delta.x)*180/Math.PI)-90;
        float pitch=(float)(-Mth.atan2(delta.y,delta.horizontalDistance())*180/Math.PI);
        turret.setYRot(turret.getYRot()+Mth.clamp(Mth.wrapDegrees(yaw-turret.getYRot()),-20,20));
        turret.setYHeadRot(turret.getYRot());turret.setXRot(turret.getXRot()+Mth.clamp(pitch-turret.getXRot(),-20,20));
        if(Math.abs(Mth.wrapDegrees(yaw-turret.getYRot()))>5 || Math.abs(pitch-turret.getXRot())>5) { stop();return; }
        actor.moveTo(turret.getX(),turret.getY(),turret.getZ(),yaw,pitch);actor.setYHeadRot(yaw);
        actor.tickCount=turret.tickCount;actor.advance();
        actor.setItemInHand(InteractionHand.MAIN_HAND,stack);actor.setItemSlot(EquipmentSlot.CHEST,turret.getItem(1));
        var previous=ACTIVE.get();ACTIVE.set(turret);
        try(var identity=WandAutomation.open(stack,actor)) {
            if(AutocasterFoci.id(focus).equals("taintedmagic:focus_mage_mace")) {
                for(var attribute:java.util.List.of(Attributes.ATTACK_DAMAGE,Attributes.ATTACK_SPEED)) {
                    var value=actor.getAttribute(attribute);value.removeModifiers();value.setBaseValue(attribute==Attributes.ATTACK_DAMAGE?1+FocusPotency.bonus(actor):4);
                    for(var modifier:stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute))value.addTransientModifier(modifier);
                }
                if(actor.getAttackStrengthScale(0)>=1 && WandCooldowns.begin(actor,500))actor.attack(target);
            } else if(actor.isUsingItem()) {
                if(actor.getUseItem()!=stack) { stop();return; }
                int remaining=Math.max(0,stack.getUseDuration()-++useTicks);
                wand.onUseTick(turret.level(),actor,stack,remaining);
                if(remaining==0)stop();
            } else if(WandCooldowns.begin(actor,Math.max(AutocasterFoci.id(focus).equals("emt:energy_ball_focus")?2000:50,
                    wand.focusCooldownMillis(stack,focus,item.cooldownMillis(focus))))) {
                // A miss deliberately avoids accidentally operating blocks behind an enemy.
                // Only eligible attack foci are called; they perform their own payment/permission checks.
                item.cast(stack,turret.level(),actor,InteractionHand.MAIN_HAND,BlockHitResult.miss(aim,Direction.UP,target.blockPosition()));
                useTicks=0;
            }
        } finally { if(previous==null)ACTIVE.remove();else ACTIVE.set(previous); }
    }
    public void stop() {
        if(!actor.isUsingItem())return;
        var previous=ACTIVE.get();ACTIVE.set(turret);var stack=actor.getUseItem();
        try(var identity=WandAutomation.open(stack,actor)) {
            stack.releaseUsing(turret.level(),actor,Math.max(0,stack.getUseDuration()-useTicks));
        } finally {
            actor.stopUsingItem();useTicks=0;
            if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);
        }
    }
}
