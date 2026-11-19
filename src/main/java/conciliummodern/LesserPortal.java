package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.*;

/** Thirteen reinforcement waves, optionally followed by the pontifex. */
public final class LesserPortal extends Monster {
    private int stage,counter=200,pulse;private boolean boss;
    public LesserPortal(EntityType<? extends LesserPortal> type,Level level){super(type,level);xpReward=30;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,500).add(Attributes.ATTACK_DAMAGE,0).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.ARMOR,5);}
    public int stage(){return stage;}public int pulse(){return pulse;}public boolean summonsBoss(){return boss;}public void summonsBoss(boolean value){boss=value;}
    public void activateImmediately(){counter=0;}
    @Override public boolean isEffectiveAi(){return false;}
    // Legacy updateEntityActionState suppressed distance despawning. Modern ServerLevel
    // checks it separately from AI; retain the portal until death or the Ars summon expiry.
    @Override public boolean removeWhenFarAway(double distanceSquared){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean isPickable(){return true;}
    @Override public boolean canBeCollidedWith(){return true;}
    @Override public boolean canPickUpLoot(){return false;}
    @Override public void move(MoverType type,Vec3 delta){}
    @Override public boolean canBeAffected(MobEffectInstance effect){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public void tick(){
        super.tick();if(pulse>0)pulse--;if(!(level() instanceof ServerLevel level)||!isAlive())return;
        if(counter>0)counter--;
        else if(level.getNearestPlayer(this,48)!=null){
            level.broadcastEntityEvent(this,(byte)16);
            boolean advanced;
            if(stage==13){advanced=!boss||spawn(level,CrimsonContent.PONTIFEX.get().create(level));if(advanced&&boss){boss=false;hurt(damageSources().fellOutOfWorld(),getHealth()*10);return;}counter=20;}
            else{
                advanced=spawnMinion(level);
                int timing=level.getEntitiesOfClass(CultistEntity.class,getBoundingBox().inflate(32),LivingEntity::isAlive).size()*20;
                counter=stage<=4?15+random.nextInt(10-stage)-stage:stage==12?50+timing*2+random.nextInt(50):timing+random.nextInt(5+timing/3);
                if(!advanced)counter=Math.max(counter,20);
                if(advanced&&stage>12)hurt(damageSources().fellOutOfWorld(),5+random.nextInt(5));
            }
            if(advanced&&stage<Integer.MAX_VALUE)stage++;
        }else counter=30+random.nextInt(30);
        if(stage<12)heal(1);
    }
    private boolean spawnMinion(ServerLevel level){
        int choice=random.nextInt(5);Mob mob=switch(choice){
            case 0->TCEntities.CULTIST_KNIGHT.get().create(level);case 1->TCEntities.CULTIST_CLERIC.get().create(level);
            case 2->CrimsonContent.PALADIN.get().create(level);case 3->CrimsonContent.dyes()?CrimsonContent.ARCHER.get().create(level):TCEntities.CULTIST_KNIGHT.get().create(level);
            default->CrimsonContent.dyes()?CrimsonContent.RANGER.get().create(level):TCEntities.CULTIST_KNIGHT.get().create(level);
        };return spawn(level,mob);
    }
    private boolean spawn(ServerLevel level,Mob mob){
        if(mob==null)return false;mob.setPos(getX()+random.nextFloat()-random.nextFloat(),getY()+.25,getZ()+random.nextFloat()-random.nextFloat());
        if(!level.hasChunkAt(mob.blockPosition())||!level.getWorldBorder().isWithinBounds(mob.blockPosition()))return false;
        mob.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.EVENT,null,null);mob.restrictTo(blockPosition(),32);
        if(!level.addFreshEntity(mob))return false;mob.spawnAnim();mob.playSound(TCSounds.WAND_FAIL.get(),1,1);return true;
    }
    @Override public void playerTouch(Player player){if(!level().isClientSide&&distanceToSqr(player)<3&&player.hurt(damageSources().indirectMagic(this,this),8))playSound(TCSounds.ZAP.get(),1,1+(random.nextFloat()-random.nextFloat())*.1f);}
    @Override public void handleEntityEvent(byte message){if(message==16){pulse=10;spawnAnim();}else super.handleEntityEvent(message);}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return TCSounds.MONOLITH.get();}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return TCSounds.ZAP.get();}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return TCSounds.SHOCK.get();}
    @Override protected float getSoundVolume(){return .75f;}
    @Override public int getAmbientSoundInterval(){return 540;}
    @Override public void die(DamageSource source){if(!level().isClientSide&&!dead)level().explode(this,getX(),getY(),getZ(),2,false,Level.ExplosionInteraction.NONE);super.die(source);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("stage",stage);tag.putInt("StageCounter",counter);tag.putBoolean("SummonsBoss",boss);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);stage=Math.max(0,tag.getInt("stage"));counter=tag.contains("StageCounter")?Math.max(0,tag.getInt("StageCounter")):200;boss=tag.getBoolean("SummonsBoss");}
}
