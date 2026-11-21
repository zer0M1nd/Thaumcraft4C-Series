package thaumicfutures.autocaster;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import java.util.List;

public class AutocasterItem extends Item {
    public AutocasterItem() { super(new Properties().stacksTo(16).rarity(Rarity.RARE)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();var level=context.getLevel();var support=context.getClickedPos();var face=context.getClickedFace();
        var pos=support.relative(face);
        if(player==null || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,face,context.getItemInHand())
                || !level.getBlockState(support).isFaceSturdy(level,support,face))return InteractionResult.FAIL;
        var bounds=new AABB(pos.getX()+.05,pos.getY(),pos.getZ()+.05,pos.getX()+.95,pos.getY()+.9,pos.getZ()+.95);
        if(!level.noCollision(bounds) || !level.getEntities(null,bounds).isEmpty())return InteractionResult.FAIL;
        if(!level.isClientSide) {
            var turret=Autocasters.ENTITY.get().create(level);if(turret==null)return InteractionResult.FAIL;
            turret.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,player.getYRot()+180,0);
            turret.setFacing(face);turret.setOwnerUUID(player.getUUID());
            if(context.getItemInHand().hasCustomHoverName())turret.setCustomName(context.getItemInHand().getHoverName());
            if(!level.addFreshEntity(turret))return InteractionResult.FAIL;
            if(!player.getAbilities().instabuild)context.getItemInHand().shrink(1);
            turret.playSound(net.minecraft.sounds.SoundEvents.IRON_GOLEM_REPAIR,1,1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags) {
        lines.add(Component.translatable("thaumicfutures.autocaster.tooltip"));
    }
}
