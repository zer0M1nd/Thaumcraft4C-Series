package thaumicfutures.devices;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

public class CrossbowPlacer extends Item {
    public CrossbowPlacer() { super(new Properties().stacksTo(16)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();var player=context.getPlayer();var support=context.getClickedPos();var pos=support.above();
        if(player==null || context.getClickedFace()!=Direction.UP || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,Direction.UP,context.getItemInHand())
                || !level.getBlockState(support).isFaceSturdy(level,support,Direction.UP))return InteractionResult.FAIL;
        var bounds=new AABB(pos.getX()+.025,pos.getY(),pos.getZ()+.025,pos.getX()+.975,pos.getY()+1.25,pos.getZ()+.975);
        if(!level.noCollision(bounds) || !level.getEntities(null,bounds).isEmpty())return InteractionResult.FAIL;
        if(!level.isClientSide) {
            var turret=Devices.CROSSBOW.get().create(level);if(turret==null)return InteractionResult.FAIL;
            turret.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,player.getYRot()+180,0);turret.setYHeadRot(turret.getYRot());turret.setOwnerUUID(player.getUUID());
            if(context.getItemInHand().hasCustomHoverName())turret.setCustomName(context.getItemInHand().getHoverName());
            if(!level.addFreshEntity(turret))return InteractionResult.FAIL;
            if(!player.getAbilities().instabuild)context.getItemInHand().shrink(1);
            turret.playSound(net.minecraft.sounds.SoundEvents.IRON_GOLEM_REPAIR,1,1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
