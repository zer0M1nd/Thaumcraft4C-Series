package thaumicomniverse.compat.maid;

import java.util.*;
import com.mojang.authlib.GameProfile;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.Mth;
import net.minecraftforge.common.util.FakePlayer;
import tc4revival.content.*;
import thaumcraft.api.wands.WandActors;

/** Uses the real focus dispatcher and vis payment; never reimplements a focus's damage formula. */
public final class MaidWandCombat {
    private static final Map<EntityMaid,Session> SESSIONS=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static final ThreadLocal<EntityMaid> CASTING=new ThreadLocal<>();
    private static final class Session {
        private final FakePlayer player;
        private int useTicks;
        Session(FakePlayer player) { this.player=player; }
        FakePlayer player() { return player; }
    }
    /** FakePlayer.tick is empty; advance only the vanilla melee cooldown, never a second entity tick. */
    private static final class Caster extends FakePlayer {
        private long lastMeleeTick;
        Caster(ServerLevel level,GameProfile profile) { super(level,profile);lastMeleeTick=level.getGameTime(); }
        void advanceMeleeClock() {
            long now=level().getGameTime();
            attackStrengthTicker=(int)Math.min(10000L,attackStrengthTicker+Math.max(0,now-lastMeleeTick));
            lastMeleeTick=now;
        }
    }
    private MaidWandCombat() {}
    public static EntityMaid casting() { return CASTING.get(); }
    static FakePlayer actor(EntityMaid maid) {
        var session=SESSIONS.get(maid);
        if (session==null || session.player().level()!=maid.level()) {
            var id=UUID.nameUUIDFromBytes(("OmniverseMaid:"+maid.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            session=new Session(new Caster((ServerLevel)maid.level(),new GameProfile(id,"ThaumicMaid")));
            SESSIONS.put(maid,session);
        }
        var player=session.player();WandActors.bind(player,maid);player.tickCount=maid.tickCount;
        ((Caster)player).advanceMeleeClock();
        for(var slot:net.minecraft.world.entity.EquipmentSlot.values())player.setItemSlot(slot,maid.getItemBySlot(slot));
        for (var attribute:net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES) {
            var id=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (id==null || !id.getNamespace().equals("thaumcraft")) continue;
            var target=player.getAttribute(attribute);var original=maid.getAttribute(attribute);
            if(target!=null && original!=null) { target.removeModifiers();target.setBaseValue(original.getValue()); }
        }
        return player;
    }
    private static String id(ItemStack stack) { return net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString(); }
    private static double range(ItemStack focus) {
        return switch(id(focus)) {
            case "thaumcraft:focus_fire" -> ((WandFocusItem)focus.getItem()).upgradeLevel(focus,FocusUpgrade.FIREBALL.id)>0?20:10;
            case "taintedmagic:focus_mage_mace" -> 3;
            case "taintedmagic:focus_shockwave" -> 6;
            case "thaumichorizons:focus_disintegration", "thaumichorizons:focus_liquefaction" -> 9;
            case "thaumicbases:focus_experience" -> 1.2;
            default -> 20;
        };
    }
    private static boolean offensive(ItemStack focus) {
        if(id(focus).equals("thaumicbases:focus_experience"))return focus.getItem() instanceof WandFocusItem item
                && item.upgradeLevel(focus,FocusUpgrade.require("thaumicbases:decomposing").id)>0;
        return focus.getItem() instanceof WandFocusItem && focus.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:maid_attack_foci")));
    }
    private static boolean usable(ItemStack wand,ItemStack focus,FakePlayer player,LivingEntity target) {
        if (!offensive(focus)) return false;
        if ((id(focus).equals("thaumichorizons:focus_disintegration") || id(focus).equals("thaumichorizons:focus_liquefaction"))
                && !(target instanceof net.minecraft.world.entity.Mob)) return false;
        if ((id(focus).equals("thaumcraft:focus_fire") || id(focus).equals("thaumichorizons:focus_liquefaction")) && target.fireImmune()) return false;
        if(id(focus).equals("emt:energy_ball_focus") && !EnergyReserve.available(player))return false;
        if(id(focus).equals("thaumicbases:focus_experience")) {
            if(target instanceof net.minecraft.world.entity.player.Player || target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                    || target instanceof net.minecraft.world.entity.boss.wither.WitherBoss || target.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,net.minecraft.resources.ResourceLocation.parse("forge:bosses")))
                    || target.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,net.minecraft.resources.ResourceLocation.parse("thaumicbases:decomposition_blacklist"))))return false;
        }
        var candidate=wand.copy();WandFocusItem.attach(candidate,focus);
        var costs=((WandFocusItem)focus.getItem()).costs(focus);
        // EU-only and target-dependent foci pay inside cast(). An empty vis map is not
        // an attempted zero-cost vis transaction (which the wand correctly rejects).
        return costs.isEmpty()?thaumcraft.api.wands.WandAutomation.allowed(candidate,player)
                :((CastingWandItem)wand.getItem()).consumeForPlayer(candidate,player,costs,false,false,false);
    }
    /** Loaded only for the IC2-dependent EMT focus, never on a host-absent installation. */
    private static final class EnergyReserve {
        static boolean available(FakePlayer player) {
            if(!net.minecraftforge.fml.ModList.get().isLoaded("ic2"))return false;
            var chest=player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            return !chest.isEmpty() && ic2.api.item.ElectricItem.manager.discharge(chest,5120,4,true,false,true)>=5120;
        }
    }
    private static boolean select(EntityMaid maid,ItemStack wand,FakePlayer player,LivingEntity target) {
        var current=WandFocusItem.attached(wand);
        double distance=maid.distanceTo(target);
        double best=usable(wand,current,player,target)?score(current,distance)+.25:Double.NEGATIVE_INFINITY;
        int chosenSlot=-1,chosenPouch=-1;ItemStack chosen=ItemStack.EMPTY;
        var inventory=maid.getAvailableInv(false);
        for (int slot=0;slot<inventory.getSlots();slot++) {
            var stack=inventory.getStackInSlot(slot);
            if (stack.getCount()==1 && usable(wand,stack,player,target) && score(stack,distance)>best) {
                best=score(stack,distance);chosen=stack;chosenSlot=slot;chosenPouch=-1;
            }
            if (stack.getItem() instanceof FocusPouchItem) {
                var contents=FocusPouchItem.contents(stack);
                for(int index=0;index<contents.size();index++) {
                    var candidate=contents.get(index);
                    if(candidate.getCount()==1 && usable(wand,candidate,player,target) && score(candidate,distance)>best) {
                        best=score(candidate,distance);chosen=candidate;chosenSlot=slot;chosenPouch=index;
                    }
                }
            }
        }
        if(chosenSlot<0)return best>Double.NEGATIVE_INFINITY;
        stop(maid);
        // release() may mutate focus state; return the actual post-release core to its slot.
        current=WandFocusItem.attached(wand);
        var replacement=chosen.copy();
        if(chosenPouch<0)inventory.setStackInSlot(chosenSlot,current.copy());
        else {
            var pouch=inventory.getStackInSlot(chosenSlot).copy();var contents=FocusPouchItem.contents(pouch);
            contents.set(chosenPouch,current.copy());FocusPouchItem.save(pouch,contents);inventory.setStackInSlot(chosenSlot,pouch);
        }
        WandFocusItem.attach(wand,replacement);return true;
    }
    /** Prefer a core that can reach now; keep the current core on near ties to avoid churn. */
    private static double score(ItemStack focus,double distance) {
        double reach=range(focus);
        if(distance>reach)return -10-(distance-reach);
        if(id(focus).equals("taintedmagic:focus_mage_mace"))return 3;
        if(id(focus).equals("taintedmagic:focus_shockwave"))return 2;
        return 1;
    }
    public static void tick(EntityMaid maid,LivingEntity target) {
        if (!thaumicomniverse.RevivalFeatures.Group.MAID.enabled() || !(maid.level() instanceof ServerLevel level) || !target.isAlive() || !maid.canAttack(target)) { stop(maid);return; }
        var stack=maid.getMainHandItem();
        if (!(stack.getItem() instanceof CastingWandItem wand) || wand.isSceptre(stack)) { stop(maid);return; }
        var player=actor(maid);
        if (!select(maid,stack,player,target)) { stop(maid);return; }
        var focus=WandFocusItem.attached(stack);double reach=range(focus);
        maid.getLookControl().setLookAt(target,90,90);
        if (!maid.hasLineOfSight(target) || maid.distanceToSqr(target)>reach*reach) {
            stop(maid);maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(new EntityTracker(target,false),.6f,(int)Math.max(1,reach-2)));return;
        }
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        // EMT's ball activates on block contact, not entity contact. Land it at the
        // target's feet instead of firing horizontally past the target indefinitely.
        var aim=id(focus).equals("emt:energy_ball_focus")?target.position().add(0,-.25,0):target.getEyePosition();
        var delta=aim.subtract(maid.getEyePosition());
        float yaw=(float)(Mth.atan2(delta.z,delta.x)*180/Math.PI)-90;
        float pitch=(float)(-Mth.atan2(delta.y,delta.horizontalDistance())*180/Math.PI);
        player.moveTo(maid.getX(),maid.getEyeY()-player.getEyeHeight(),maid.getZ(),yaw,pitch);
        player.setYHeadRot(yaw);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        CASTING.set(maid);
        try (var identity=thaumcraft.api.wands.WandAutomation.open(stack,player)) {
            if (id(focus).equals("taintedmagic:focus_mage_mace")) {
                // Vanilla normally refreshes equipment attributes during LivingEntity.tick.
                // The proxy does not tick, so use the item's real (Forge-modified) melee values.
                for(var attribute:List.of(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)) {
                    var value=player.getAttribute(attribute);value.removeModifiers();
                    value.setBaseValue(attribute==net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE?1:4);
                    for(var modifier:stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND).get(attribute))value.addTransientModifier(modifier);
                }
                // TM's PlayerTick adapter adds this potency bonus; virtual players never receive it.
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(1+FocusPotency.bonus(player));
                if(player.getAttackStrengthScale(0)>=1 && WandCooldowns.begin(player,500)){player.attack(target);maid.swing(InteractionHand.MAIN_HAND);}
            } else if (player.isUsingItem()) {
                var session=SESSIONS.get(maid);
                int remaining=Math.max(0,stack.getUseDuration()-++session.useTicks);
                wand.onUseTick(level,player,stack,remaining);
                if(remaining==0)stop(maid);
            }
            else if (WandCooldowns.begin(player,Math.max(id(focus).equals("emt:energy_ball_focus")?2000:50,wand.focusCooldownMillis(stack,focus,((WandFocusItem)focus.getItem()).cooldownMillis(focus))))) {
                var hit=BlockHitResult.miss(target.getEyePosition(),Direction.UP,target.blockPosition());
                ((WandFocusItem)focus.getItem()).cast(stack,level,player,InteractionHand.MAIN_HAND,hit);
                SESSIONS.get(maid).useTicks=0;
            }
        } finally { CASTING.remove(); }
    }
    public static void stop(EntityMaid maid) {
        var session=SESSIONS.get(maid);if(session==null)return;
        var player=session.player();
        if(player.isUsingItem()) {
            var previous=CASTING.get();CASTING.set(maid);
            try (var identity=thaumcraft.api.wands.WandAutomation.open(player.getUseItem(),player)) {
                var stack=player.getUseItem();
                stack.releaseUsing(player.level(),player,Math.max(0,stack.getUseDuration()-session.useTicks));
            } finally {
                player.stopUsingItem();
                if(previous==null)CASTING.remove();else CASTING.set(previous);
            }
        }
    }
}
