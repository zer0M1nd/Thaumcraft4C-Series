package thmodern.blood;

import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thmodern.Horizons;

public final class EmptySyringeItem extends Item {
    public EmptySyringeItem(){super(new Properties());}
    private InteractionResult sample(ItemStack stack,Player player,LivingEntity target){
        if(target!=null&&(!BloodSampleItem.eligible(target)||player.distanceToSqr(target)>2.25||!player.hasLineOfSight(target)))return InteractionResult.FAIL;
        if(player.level().isClientSide)return InteractionResult.SUCCESS;
        var result=new ItemStack(target==null?Horizons.HUMAN_BLOOD.get():Horizons.BLOOD_SAMPLE.get());
        if(target!=null){var genes=BloodSampleItem.genetics(target);if(genes.sizeInBytes()>65536)return InteractionResult.FAIL;result.getOrCreateTag().put("critter",genes);result.getOrCreateTag().putString("critterName",target.getName().getString());}
        if(!player.getInventory().add(result))return InteractionResult.FAIL;
        stack.shrink(1);return InteractionResult.CONSUME;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){return sample(stack,player,target);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(1.5));
        var block=level.clip(new net.minecraft.world.level.ClipContext(start,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        if(block.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)end=block.getLocation();
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,player,start,end,player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),e->e instanceof LivingEntity&&e.isPickable());
        var target=hit!=null&&hit.getEntity() instanceof LivingEntity living?living:null;
        var stack=player.getItemInHand(hand);return new InteractionResultHolder<>(sample(stack,player,target),stack);
    }
}
