package conciliummodern;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import tc4revival.content.*;

/** Registered upstream creative prototype: the original provides no survival recipe or texture. */
public final class ElementalSurferShorts extends ArmorItem {
    public ElementalSurferShorts(){super(SpecialArmorMaterial.INSTANCE,Type.LEGGINGS,new Properties());}
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String layer){return "minecraft:textures/models/armor/leather_layer_2.png";}
    @Override public void onArmorTick(ItemStack armor,Level level,Player player){
        if(level.isClientSide || !player.isShiftKeyDown())return;
        if(WandInventory.consume(player,ConciliumFocus.vis("ordo",200,"perditio",200))){
            var point=player.position();for(var projectile:level.getEntitiesOfClass(ThrowableProjectile.class,new AABB(point.add(-4,-4,-4),point.add(3,3,3)),e->e.getOwner()==player)){
                var movement=projectile.getDeltaMovement().normalize().scale(player.distanceTo(projectile));var target=projectile.position().add(movement);
                if(level.hasChunkAt(net.minecraft.core.BlockPos.containing(target)) && level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(target)))projectile.setPos(target);
            }
        }
        if(player.tickCount%7==0){
            if(WandInventory.consume(player,ConciliumFocus.vis("ignis",200)))for(int i=0;i<8;i++){
                var orb=new EmberEntity(level,player,15,5,0,20);var motion=orb.getDeltaMovement().multiply(level.random.nextDouble()-.5,level.random.nextDouble(),level.random.nextDouble()-.5);
                orb.setDeltaMovement(motion);orb.setPos(orb.position().add(motion.multiply(5+level.random.nextDouble(),5+level.random.nextDouble(),5+level.random.nextDouble())));level.addFreshEntity(orb);
            }
            if(WandInventory.consume(player,ConciliumFocus.vis("aqua",200)))for(int i=0;i<8;i++){
                var shard=new FrostShardEntity(level,player,8,1,4,true,false);var motion=shard.getDeltaMovement().multiply(level.random.nextDouble()-.5,2*level.random.nextDouble(),level.random.nextDouble()-.5);
                shard.setDeltaMovement(motion);shard.setPos(shard.position().add(motion.x*(2+level.random.nextDouble()),1,motion.z*(2+level.random.nextDouble())));level.addFreshEntity(shard);
            }return;
        }
        if(player.tickCount%13==0 && WandInventory.consume(player,ConciliumFocus.vis("aer",200)))for(var target:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(7),e->e!=player && e.isAlive() && !e.isSpectator())){
            if(target instanceof Player other && (!level.getServer().isPvpAllowed() || !player.canHarmPlayer(other)))continue;
            var orb=new GolemOrbEntity(level,player,target,false);orb.setPos(orb.position().add(player.getLookAngle().x,.5,player.getLookAngle().z));
            var delta=target.position().add(target.getDeltaMovement()).subtract(player.position()).add(0,-target.getBbHeight()/2,0);orb.shoot(delta.x,delta.y,delta.z,.66f,5);
            if(level.addFreshEntity(orb))player.playSound(TCSounds.ELDRITCH_ATTACK.get(),1,1+level.random.nextFloat()*.1f);
        }
    }
}
