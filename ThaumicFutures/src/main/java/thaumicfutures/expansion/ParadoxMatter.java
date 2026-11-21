package thaumicfutures.expansion;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class ParadoxMatter extends Item {
    public ParadoxMatter(){super(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled())return InteractionResultHolder.pass(stack);
        if(!level.isClientSide){
            var entity=new Projectile(RestoredContent.PARADOX_ENTITY.get(),level);
            entity.setOwner(player);entity.setItem(stack.copyWithCount(1));entity.setPos(player.getX(),player.getEyeY()-.1,player.getZ());
            entity.shootFromRotation(player,player.getXRot(),player.getYRot(),0,.75f,1);
            if(!level.addFreshEntity(entity))return InteractionResultHolder.fail(stack);
            if(!player.getAbilities().instabuild)stack.shrink(1);player.getCooldowns().addCooldown(this,10);
            level.playSound(null,player,net.minecraft.sounds.SoundEvents.SNOWBALL_THROW,net.minecraft.sounds.SoundSource.PLAYERS,.5f,.8f);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    public static class Projectile extends ThrowableItemProjectile {
        public Projectile(EntityType<? extends Projectile> type,Level level){super(type,level);}
        @Override protected Item getDefaultItem(){return RestoredContent.PARADOX_MATTER.get();}
        @Override protected float getGravity(){return .03f;}
        @Override protected void onHit(HitResult hit){
            super.onHit(hit);
            if(!level().isClientSide&&!isRemoved()){
                level().explode(this,getX(),getY(),getZ(),2,false,Level.ExplosionInteraction.TNT);discard();
            }
        }
        @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
    }
}
