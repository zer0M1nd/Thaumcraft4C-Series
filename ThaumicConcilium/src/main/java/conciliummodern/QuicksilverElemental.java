package conciliummodern;

import net.minecraft.core.Direction;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import tc4revival.content.*;

public class QuicksilverElemental extends Monster {
    private float bodyScale=1;
    public QuicksilverElemental(EntityType<? extends QuicksilverElemental> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE,32).add(Attributes.MOVEMENT_SPEED,.6).add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,5);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(2,new MeleeAttackGoal(this,.6,false));goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,.6));
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.6));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(9,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Thaumaturge.class,false));targetSelector.addGoal(4,new NearestAttackableTargetGoal<>(this,CultistEntity.class,false));
    }
    @Override public EntityDimensions getDimensions(Pose pose){return super.getDimensions(pose).scale(bodyScale<=0?1:bodyScale);}
    @Override public void tick(){
        super.tick();float scale=Math.max(.01f,getHealth()/30f);if(Math.abs(scale-bodyScale)>.0001f){bodyScale=scale;refreshDimensions();}
        if(!level().isClientSide && isAlive() && tickCount%40==0 && getTarget()!=null && getTarget().isAlive()){
            var target=getTarget();var effect=new MobEffectInstance(TCEffects.INFECTIOUS_VIS_EXHAUST.get(),1000,20);effect.setCurativeItems(java.util.List.of());target.addEffect(effect,this);
            for(var existing:java.util.List.copyOf(target.getActiveEffects()))if(existing.getEffect().isBeneficial()){target.removeEffect(existing.getEffect());break;}
        }
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(isInvulnerableTo(source) || !isAlive() || (!source.is(DamageTypeTags.IS_FIRE)&&!hasEffect(MobEffects.MOVEMENT_SLOWDOWN)))return false;
        if(level() instanceof ServerLevel level && level.getBlockEntity(blockPosition()) instanceof CrucibleBlockEntity && transform(level,source))return true;
        return super.hurt(source,amount);
    }
    private boolean transform(ServerLevel level,DamageSource source){
        var pos=blockPosition();var actor=source.getEntity() instanceof ServerPlayer player?player:this;
        if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos))return false;
        if(actor instanceof ServerPlayer player){if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,player.getMainHandItem())
                || net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)==-1)return false;}
        else if(!ForgeEventFactory.getMobGriefingEvent(level,this))return false;
        var snapshot=BlockSnapshot.create(level.dimension(),level,pos);boolean accepted=false;
        try{
            if(!level.setBlock(pos,ConciliumBlocks.QUICKSILVER_CRUCIBLE.get().defaultBlockState(),2|16))return false;
            if(ForgeEventFactory.onBlockPlace(actor,snapshot,Direction.UP))return false;
            if(!(level.getBlockEntity(pos) instanceof ConciliumCrucibleBlockEntity tile))return false;
            tile.initialize(20);accepted=true;
        }finally{if(!accepted){boolean restoring=level.restoringBlockSnapshots;level.restoringBlockSnapshots=true;try{snapshot.restore(true,false);}finally{level.restoringBlockSnapshots=restoring;}}}
        level.updateNeighborsAt(pos,level.getBlockState(pos).getBlock());
        for(var player:level.getEntitiesOfClass(ServerPlayer.class,getBoundingBox().inflate(16)))if(!thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"thaumicconcilium:QUICKSILVERCRUCIBLE")){
            thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,"thaumicconcilium:QUICKSILVERCRUCIBLE");level.playSound(null,player.blockPosition(),TCSounds.HEARTBEAT.get(),SoundSource.PLAYERS,1,1);
        }
        discard();return true;
    }
    @Override public boolean doHurtTarget(Entity target){
        float amount=(float)getAttributeValue(Attributes.ATTACK_DAMAGE);
        if(target instanceof LivingEntity living && living.getAttribute(Attributes.ATTACK_DAMAGE)!=null)amount=(float)living.getAttributeValue(Attributes.ATTACK_DAMAGE);
        swing(InteractionHand.MAIN_HAND);
        return target.hurt(new DamageSource(level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.FALLING_ANVIL),this),amount);
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){super.dropCustomDeathLoot(source,looting,recentlyHit);spawnAtLocation(new ItemStack(TCItems.material(MaterialType.QUICKSILVER),random.nextInt(8)+4+looting));}
    @Override public boolean canPickUpLoot(){return false;}
    @Override protected SoundEvent getAmbientSound(){return ConciliumSounds.MELTED.get();}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.ITEM_BREAK;}
    @Override protected SoundEvent getDeathSound(){return ConciliumSounds.MELTED.get();}
    @Override protected float getSoundVolume(){return 2;}
    @Override public int getAmbientSoundInterval(){return 65;}
}
