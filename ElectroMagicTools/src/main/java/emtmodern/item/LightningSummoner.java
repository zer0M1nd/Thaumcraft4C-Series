package emtmodern.item;

import emtmodern.ElectroMagicTools;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="emt")
public class LightningSummoner extends LegacyMaterialItem {
    @Override public int getUseDuration(ItemStack stack){return 72000;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BOW;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);var hook=net.minecraftforge.event.ForgeEventFactory.onArrowNock(stack,level,player,hand,true);if(hook!=null)return hook;
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){
        if(entity instanceof Player player&&getUseDuration(stack)-remaining>=40){
            player.swing(player.getUsedItemHand());
            if(!level.isClientSide){var hit=trace(player);if(hit.getType()==HitResult.Type.BLOCK&&level.mayInteract(player,hit.getBlockPos())){
                var lightning=EntityType.LIGHTNING_BOLT.create(level);if(lightning!=null){var pos=hit.getBlockPos();lightning.moveTo(pos.getX(),pos.getY(),pos.getZ());level.addFreshEntity(lightning);
                    if(!player.getAbilities().instabuild)for(var inventory:player.getInventory().items)if(inventory.is(this)){inventory.shrink(1);break;}
                }
            }}player.stopUsingItem();
        }
    }
    /** Original ray endpoint is 5000 blocks away but World.rayTraceBlocks stops after 200 voxel steps. */
    public static BlockHitResult trace(Player player){
        var level=player.level();Vec3 from=player.getEyePosition(),to=from.add(player.getViewVector(1).scale(5000));
        var context=new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,player);int[] visits={0};
        return BlockGetter.traverseBlocks(from,to,context,(ctx,pos)->{
            if(visits[0]++>200||!level.hasChunkAt(pos))return miss(ctx);
            var state=level.getBlockState(pos);var block=level.clipWithInteractionOverride(from,to,pos,ctx.getBlockShape(state,level,pos),state);
            var fluid=ctx.getFluidShape(level.getFluidState(pos),level,pos).clip(from,to,pos);
            return (block==null?Double.MAX_VALUE:from.distanceToSqr(block.getLocation()))<=(fluid==null?Double.MAX_VALUE:from.distanceToSqr(fluid.getLocation()))?block:fluid;
        },LightningSummoner::miss);
    }
    public static BlockHitResult miss(ClipContext ctx){var direction=ctx.getFrom().subtract(ctx.getTo());return BlockHitResult.miss(ctx.getTo(),Direction.getNearest(direction.x,direction.y,direction.z),BlockPos.containing(ctx.getTo()));}
    @SubscribeEvent public static void chargedCreeperDrop(LivingDropsEvent event){
        if(event.getEntity() instanceof Creeper creeper&&creeper.isPowered()&&!creeper.level().isClientSide)
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(creeper.level(),creeper.getX(),creeper.getY()+1,creeper.getZ(),new ItemStack(ElectroMagicTools.CONTENT.get("lightning_summoner").get())));
    }
}
