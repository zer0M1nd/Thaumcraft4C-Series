package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

/** The use core visits a departure-time snapshot of matching marked faces. */
public  class GolemUseItemGoal extends Goal {
    private static final GameProfile PROFILE=new GameProfile(UUID.nameUUIDFromBytes("FakeThaumcraftGolem".getBytes(StandardCharsets.UTF_8)),"FakeThaumcraftGolem");
    private final GolemWorkHost host;
    private final GolemTaskRoute route;
    private final java.util.Map<BlockPos,Byte> routeColors=new java.util.HashMap<>();
    private BlockPos target;
    private BlockState expected;
    private byte color;
    private int ticks,nextTick;
    public GolemUseItemGoal(GolemWorkHost host) {
        this.host=host; route=new GolemTaskRoute(host); nextTick=host.mob().tickCount+host.mob().getRandom().nextInt(6);
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public boolean canUse() {
        if (!host.workEnabled() || !(host.mob().level() instanceof ServerLevel level)
                || host.mob().tickCount<nextTick || !host.mob().getNavigation().isDone()
                || host.carried().isEmpty() && GolemInventoryViews.exists(level,host.homeInventory())) return false;
        nextTick=host.mob().tickCount+Math.max(1,host.workDelay());
        routeColors.clear();
        for (byte matching:host.filters().matchingColors(host.carried(),host.configuration())) {
            for (var marker:host.markers().inDimension(level.dimension().location())) {
                if (matching!=-1 && marker.color()!=matching) continue;
                if (!level.hasChunkAt(marker.pos()) || level.isEmptyBlock(marker.pos())!=host.configuration().toggle(0)) continue;
                var adjacent=marker.pos().relative(marker.face());
                if (!level.hasChunkAt(adjacent) || !level.isEmptyBlock(adjacent)) continue;
                routeColors.putIfAbsent(marker.pos(),matching);
            }
        }
        if(!route.plan(routeColors.keySet()))return false;
        select();return true;
    }
    private void select() { target=route.target();if(target!=null){color=routeColors.getOrDefault(target,(byte)-1);expected=host.mob().level().getBlockState(target);} }
    private void next() { route.advance();select(); }
    @Override public void start() {
        ticks=1200;route.start();
    }
    @Override public boolean canContinueToUse() {
        return host.workEnabled() && !route.empty() && ticks>0;
    }
    @Override public void tick() {
        ticks--;if(target==null)return;
        if(!host.mob().level().hasChunkAt(target) || !host.mob().level().getBlockState(target).equals(expected)){next();return;}
        host.mob().getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5,30,30);
        if (GolemWorkRange.reaches(host,target)) click();
        else if(host.mob().getNavigation().isDone())next();
    }
    private void click() {
        if (!(host.mob().level() instanceof ServerLevel level)) return;
        var faces=host.markers().faces(target,level.dimension().location(),color);
        if (faces.isEmpty()) { next();return; }
        if (host.carried().isEmpty() && GolemInventoryViews.exists(level,host.homeInventory())) { next(); return; }
        var face=faces.get(0);
        var clicked=level.isEmptyBlock(target)?target.relative(face.getOpposite()):target;
        if (!level.hasChunkAt(clicked) || !level.mayInteract(FakePlayerFactory.get(level,PROFILE),clicked)) { next(); return; }
        FakePlayer player=FakePlayerFactory.get(level,PROFILE);
        var mob=host.mob();
        player.moveTo(mob.getX(),mob.getY(),mob.getZ(),mob.getYRot(),mob.getXRot());
        player.getInventory().selected=0;
        player.setItemInHand(InteractionHand.MAIN_HAND,host.carried().copy());
        player.setShiftKeyDown(host.configuration().toggle(2));
        try (var interaction=thaumcraft.api.golems.GolemInteraction.enter(host)) {
            if (host.configuration().toggle(1)) {
                host.leftArm();
                player.gameMode.handleBlockBreakAction(clicked,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,face,level.getMaxBuildHeight(),0);
            } else {
                var hit=new BlockHitResult(Vec3.atCenterOf(clicked),face,clicked,false);
                var result=host.configuration().toggle(0)
                        ? player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND)
                        : player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
                if (result.consumesAction()) { host.rightArm(); host.transport().progress(); }
            }
        } catch (RuntimeException exception) {
            com.mojang.logging.LogUtils.getLogger().warn("Golem item interaction failed at {}",clicked,exception);
        } finally {
            // Empty hand is valid: the legacy stackSize dereference aborted cleanup after item consumption.
            host.carried(player.getMainHandItem().copy());
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            for (int slot=1;slot<player.getInventory().items.size();slot++) {
                var extra=player.getInventory().getItem(slot);
                if (extra.isEmpty()) continue;
                if (host.carried().isEmpty()) host.carried(extra.copy());
                else player.drop(extra.copy(),false);
                player.getInventory().setItem(slot,ItemStack.EMPTY);
            }
            next();
        }
    }
    @Override public void stop() { ticks=0; route.clear();host.mob().getNavigation().stop(); }
}
