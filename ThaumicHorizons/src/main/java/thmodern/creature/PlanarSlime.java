package thmodern.creature;
import net.minecraft.core.particles.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Modern Slime already splits using getType(), so no duplicate custom split loop is needed. */
public final class PlanarSlime extends Slime {
 private final String kind;
 public PlanarSlime(EntityType<? extends Slime> type,Level level,String kind){super(type,level);this.kind=kind;}
 public String kind(){return kind;}
 @Override protected ResourceLocation getDefaultLootTable(){return BuiltInLootTables.EMPTY;}
 @Override protected ParticleOptions getParticleType(){return switch(kind){case "meat"->DustParticleOptions.REDSTONE;case "volt"->ParticleTypes.ENCHANTED_HIT;default->ParticleTypes.ITEM_SNOWBALL;};}
 @Override public void playerTouch(Player player){
  if(!kind.equals("med")){super.playerTouch(player);return;}
  if(level().isClientSide||isRemoved()||!isAlive()||player.getHealth()>=player.getMaxHealth())return;
  double reach=.6*getSize();if(!hasLineOfSight(player)||distanceToSqr(player)>=reach*reach)return;
  player.heal(getAttackDamage()+1);playSound(SoundEvents.PLAYER_ATTACK_STRONG,1,(random.nextFloat()-random.nextFloat())*.2f+1);
  if(level() instanceof ServerLevel server)server.sendParticles(ParticleTypes.EXPLOSION,getX(),getY()+getBbHeight()/2,getZ(),1,0,0,0,0);
  discard(); // Health stays positive: healing consumes the whole slime, without splitting.
 }
}
