package advancedthaumaturgy;
import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tc4revival.content.*;
import tc4revival.aspect.*;
public class VoidCageFocusItem extends WandFocusItem {
    @Override public int color(ItemStack s){return 0xff00ff;}
    @Override public String sortingPrefix(){return "ADV_VOIDCAGE";}
    @Override public int cooldownMillis(ItemStack s){return 500;}
    @Override public Map<Aspect,Integer> costs(ItemStack s){return Map.of(TCAspects.catalog().require("ordo"),5000,TCAspects.catalog().require("perditio"),5000);}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack s,int rank){return List.of(FocusUpgrade.FRUGAL);}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(!ATConfig.enabled("focus_void_cage"))return InteractionResultHolder.fail(stack);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        var focus=attached(stack);var data=focus.getOrCreateTag();var wand=(CastingWandItem)stack.getItem();
        if(data.contains("CagedEntity")){
            if(hit==null||hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);
            var p=hit.getBlockPos().relative(hit.getDirection());if(!level.hasChunkAt(p)||!level.mayInteract(player,p)||!player.mayUseItemAt(p,hit.getDirection(),stack))return InteractionResultHolder.fail(stack);
            var saved=data.getCompound("CagedEntity").copy();saved.remove("UUID");
            var restored=EntityType.create(saved,level).orElse(null);if(!(restored instanceof LivingEntity)||restored instanceof Player)return InteractionResultHolder.fail(stack);
            restored.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,player.getYRot(),0);restored.setDeltaMovement(Vec3.ZERO);
            if(!level.noCollision(restored)||!level.addFreshEntity(restored))return InteractionResultHolder.fail(stack);data.remove("CagedEntity");
        }else{
            Vec3 from=player.getEyePosition(),end=from.add(player.getLookAngle().scale(32));
            var wall=level.clip(new net.minecraft.world.level.ClipContext(from,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player));double limit=from.distanceToSqr(wall.getLocation());
            var target=ProjectileUtil.getEntityHitResult(player,from,end,player.getBoundingBox().expandTowards(end.subtract(from)).inflate(1),e->e instanceof LivingEntity&&!(e instanceof Player)&&e.isAlive()&&!e.isPassenger()&&!e.isVehicle(),limit);
            if(target==null||!level.mayInteract(player,target.getEntity().blockPosition()))return InteractionResultHolder.pass(stack);
            var entity=target.getEntity();var saved=new net.minecraft.nbt.CompoundTag();if(!entity.save(saved)||!wand.consumeForPlayer(stack,player,costs(focus),false,false,true))return InteractionResultHolder.fail(stack);
            data.put("CagedEntity",saved);entity.discard();
        }
        WandFocusItem.attach(stack,focus);player.swing(hand);return InteractionResultHolder.success(stack);
    }
}
