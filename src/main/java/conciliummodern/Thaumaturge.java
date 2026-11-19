package conciliummodern;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import tc4revival.content.*;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;

public final class Thaumaturge extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(Thaumaturge.class,EntityDataSerializers.INT);
    Player customer;
    public Thaumaturge(EntityType<? extends Thaumaturge> type,Level level) {
        super(type,level);setCanPickUpLoot(false);
        if(getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground)ground.setCanOpenDoors(true);
    }
    public static AttributeSupplier.Builder attributes() {return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.MOVEMENT_SPEED,.4);}
    @Override protected void defineSynchedData() {super.defineSynchedData();entityData.define(ANGER,0);}
    public int anger() {return entityData.get(ANGER);}
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new RangedAttackGoal(this,1,20,40,24) {
            @Override public boolean canUse(){return customer==null && getMainHandItem().getItem() instanceof CastingWandItem && super.canUse();}
            @Override public boolean canContinueToUse(){return canUse();}
        });
        goalSelector.addGoal(2,new MeleeAttackGoal(this,.6,false) {
            @Override public boolean canUse(){return customer==null && !(getMainHandItem().getItem() instanceof CastingWandItem) && super.canUse();}
            @Override public boolean canContinueToUse(){return canUse();}
        });
        goalSelector.addGoal(0,new Goal() {
            {setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
            @Override public boolean canUse(){return customer!=null;}
            @Override public void tick(){getNavigation().stop();getLookControl().setLookAt(customer,30,30);}
        });
        goalSelector.addGoal(5,new OpenDoorGoal(this,true));
        goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.5));
        goalSelector.addGoal(6,new MoveThroughVillageGoal(this,1,false,4,()->false));
        goalSelector.addGoal(9,new RandomStrollGoal(this,.6));goalSelector.addGoal(9,new LookAtPlayerGoal(this,Player.class,3,1));
        goalSelector.addGoal(10,new LookAtPlayerGoal(this,Mob.class,8));goalSelector.addGoal(11,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,target->
                target instanceof CultistEntity || target instanceof Zombie || target instanceof AbstractSkeleton || target instanceof BrainyZombieEntity
                || target instanceof thaumcraft.api.entities.IEldritchMob || target instanceof thaumcraft.api.entities.ITaintedMob));
        targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,Player.class,10,true,false,target->anger()>0));
    }
    @Override public int getArmorValue(){return Math.min(20,super.getArmorValue()+2);}
    @Override public boolean removeWhenFarAway(double distance){return customer==null && super.removeWhenFarAway(distance);}
    @Override public void tick() {
        super.tick();
        if(level().isClientSide) {
            if(anger()>0 && random.nextInt(15)==0)level().addParticle(ParticleTypes.ANGRY_VILLAGER,getRandomX(1),getRandomY()+.5,getRandomZ(1),0,0,0);
            return;
        }
        if(anger()>0) {
            entityData.set(ANGER,anger()-1);
            if(anger()==0 && getTarget() instanceof Player){setTarget(null);setLastHurtByMob(null);}
        }
        if(tickCount%40==0)heal(1);
        if(customer!=null && (!customer.isAlive() || !(customer.containerMenu instanceof ThaumaturgeMenu menu) || !menu.stillValid(customer)))customer=null;
    }
    private void angerAt(Player player){entityData.set(ANGER,400+random.nextInt(400));setTarget(player);if(customer!=null)customer.closeContainer();}
    @Override public boolean hurt(DamageSource source,float amount) {
        boolean hurt=super.hurt(source,amount);
        if(hurt && source.getEntity() instanceof Player player && !level().isClientSide) {
            for(var friend:level().getEntitiesOfClass(Thaumaturge.class,getBoundingBox().inflate(32,16,32)))friend.angerAt(player);
            angerAt(player);
        }
        return hurt;
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand) {
        if(player.isShiftKeyDown() || player.getItemInHand(hand).is(Items.NAME_TAG))return super.mobInteract(player,hand);
        if(anger()>0 || customer!=null)return InteractionResult.PASS;
        if(player instanceof ServerPlayer server) {
            var knowledge=KnowledgeSavedData.get(server.server).snapshot(player.getUUID()).orElse(null);
            if(knowledge!=null && ResearchCatalogs.rules().completed(knowledge,"thaumicconcilium:THAUMATURGES")) {
                customer=player;
                net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,p)->new ThaumaturgeMenu(id,inventory,this),getDisplayName()),buffer->buffer.writeInt(getId()));
                if(server.containerMenu instanceof ThaumaturgeMenu && allTeachingKnown(server))
                    player.sendSystemMessage(Component.translatable("tc.thaumaturge.no_more").withStyle(ChatFormatting.DARK_PURPLE));
            } else player.sendSystemMessage(Component.translatable("tc.thaumaturge.taunt."+random.nextInt(4)).withStyle(ChatFormatting.DARK_PURPLE));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    private static boolean allTeachingKnown(ServerPlayer player){
        for(String shortKey:java.util.List.of("VISCONDUCTOR","POSITIVEBURSTFOCI","IMPULSEFOCI")){
            String key=ThaumicConcilium.ID+":"+shortKey;
            if(thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,key))continue;
            boolean found=false;
            for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
                var stack=player.getInventory().getItem(slot);if(!(stack.getItem() instanceof ResearchNotesItem))continue;
                var note=ResearchNoteNbt.read(stack.getTag(),TCAspects.catalog()).note();
                if(note.map(value->key.equals(value.key())).orElse(false)){found=true;break;}
            }
            if(!found)return false;
        }
        return true;
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag) {
        var result=super.finalizeSpawn(level,difficulty,reason,group,tag);
        var wand=new ItemStack(TCItems.CASTING_WAND.get());WandFocusItem.attach(wand,new ItemStack(TCItems.FIRE_FOCUS.get()));
        var item=TCItems.CASTING_WAND.get();
        for(var aspect:TCAspects.catalog().primals()) {
            int vis=switch(aspect.tag()){case "ignis"->25;case "aer","ordo"->random.nextInt(4);default->2+random.nextInt(6);};
            item.addVis(wand,aspect,vis*100,false,true);
        }
        setItemSlot(EquipmentSlot.MAINHAND,wand);setDropChance(EquipmentSlot.MAINHAND,.1f);return result;
    }
    @Override public void performRangedAttack(LivingEntity target,float distance) {
        for(int i=0;i<5;i++) {
            var ember=new EmberEntity(level(),this,20,3,0,20);
            var direction=target.position().add(target.getDeltaMovement().x,0,target.getDeltaMovement().z).subtract(position());
            ember.shoot(direction.x,direction.y,direction.z,1.5f,4.5f);ember.setPos(ember.position().add(ember.getDeltaMovement()));level().addFreshEntity(ember);
        }
        playSound(TCSounds.FIRE_LOOP.get(),.4f,1+random.nextFloat()*.1f);swing(InteractionHand.MAIN_HAND);
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return ConciliumSounds.THINK.get();}
    @Override public int getAmbientSoundInterval(){return 100;}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) {
        int count=random.nextInt(2)+looting;if(count>0)spawnAtLocation(new ItemStack(TCItems.KNOWLEDGE_FRAGMENT.get(),count),1.5f);
        super.dropCustomDeathLoot(source,looting,recentlyHit);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Anger",anger());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(ANGER,Math.max(0,Math.min(800,tag.getInt("Anger"))));}
}
