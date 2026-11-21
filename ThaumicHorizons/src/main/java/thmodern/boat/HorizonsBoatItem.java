package thmodern.boat;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import thmodern.Horizons;
/** BoatItem's modern placement path, using the two persistent Horizons entity IDs. */
public final class HorizonsBoatItem extends Item {
    private final boolean metal;
    public HorizonsBoatItem(boolean metal){super(new Properties().stacksTo(1));this.metal=metal;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.ANY);
        if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);
        var sight=player.getViewVector(1);
        for(var entity:level.getEntities(player,player.getBoundingBox().expandTowards(sight.scale(5)).inflate(1),EntitySelector.NO_SPECTATORS.and(e->e.isPickable())))
            if(entity.getBoundingBox().inflate(entity.getPickRadius()).contains(player.getEyePosition()))return InteractionResultHolder.pass(stack);
        if(!level.mayInteract(player,hit.getBlockPos())||!player.mayUseItemAt(hit.getBlockPos(),hit.getDirection(),stack))return InteractionResultHolder.fail(stack);
        var boat=(metal?Horizons.THAUMIUM_BOAT:Horizons.GREATWOOD_BOAT).get().create(level);
        if(boat==null)return InteractionResultHolder.fail(stack);
        boat.setPos(hit.getLocation());boat.setYRot(player.getYRot());
        if(!level.noCollision(boat,boat.getBoundingBox()))return InteractionResultHolder.fail(stack);
        if(!level.isClientSide){
            if(!level.addFreshEntity(boat))return InteractionResultHolder.fail(stack);
            level.gameEvent(player,GameEvent.ENTITY_PLACE,hit.getLocation());
            if(!player.getAbilities().instabuild)stack.shrink(1);
        }
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
