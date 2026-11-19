package conciliummodern;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import tc4revival.content.*;

/** Horizons life-infusion colossus. Riding uses vanilla passenger input and vehicle synchronization. */
public final class GolemBydlo extends AbstractGolem {
    private BlockPos anchor;private int action;
    public GolemBydlo(EntityType<? extends GolemBydlo> type,Level level){super(type,level);setPersistenceRequired();setMaxUpStep(1);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,90).add(Attributes.MOVEMENT_SPEED,.2).add(Attributes.ATTACK_DAMAGE,10);}
    public int action(){return action;}public boolean inactive(){return getControllingPassenger()==null||getY()<level().getMinBuildHeight();}
    @Override public LivingEntity getControllingPassenger(){return getFirstPassenger() instanceof Player player?player:null;}
    @Override protected boolean canAddPassenger(Entity passenger){return !isVehicle()&&passenger instanceof Player;}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(!player.isShiftKeyDown()&&!player.getItemInHand(hand).is(Items.NAME_TAG)&&!isVehicle()){
            if(!level().isClientSide)player.startRiding(this);return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player,hand);
    }
    @Override protected Vec3 getRiddenInput(Player player,Vec3 input){return new Vec3(player.xxa*.5,0,player.zza);}
    @Override protected float getRiddenSpeed(Player player){return (float)getAttributeValue(Attributes.MOVEMENT_SPEED);}
    @Override protected void tickRidden(Player player,Vec3 input){super.tickRidden(player,input);setYRot(player.getYRot());yRotO=getYRot();setXRot(player.getXRot()*.5f);yHeadRot=yBodyRot=getYRot();}
    @Override protected void positionRider(Entity passenger,MoveFunction move){
        if(!hasPassenger(passenger))return;float yaw=yBodyRot*Mth.DEG_TO_RAD;
        move.accept(passenger,getX()+.5*Mth.sin(yaw),getY()+getPassengersRidingOffset()+passenger.getMyRidingOffset()+.15,getZ()-.5*Mth.cos(yaw));
        if(passenger instanceof LivingEntity living)living.yBodyRot=yBodyRot;
    }
    @Override public void aiStep(){
        super.aiStep();if(action>0)action--;if(level().isClientSide)return;if(anchor==null)anchor=blockPosition();
        if(tickCount%20==0&&(distanceToSqr(Vec3.atCenterOf(anchor))>=2304||isInWall()))returnHome();
        if(action==0&&getControllingPassenger() instanceof ServerPlayer player&&player.swinging){
            var wand=player.getMainHandItem();if(!(wand.getItem() instanceof CastingWandItem))return;var focus=WandFocusItem.attached(wand);
            if(focus.getItem() instanceof ExcavationFocusItem excavation)excavate(player,focus,excavation);
            else if(focus.is(ConciliumItems.WRATH_ATTRACTION.get())){
                animate();for(var target:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(2,1,2),e->e!=this&&e!=player&&e.isAlive()&&!player.isAlliedTo(e)&&!(e instanceof TamableAnimal tame&&tame.isOwnedBy(player)))){
                    if(target instanceof Player victim&&!player.canHarmPlayer(victim)||!getSensing().hasLineOfSight(target)||MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player,target)))continue;
                    doHurtTarget(target);
                }
            }
        }
    }
    private void animate(){action=6;level().broadcastEntityEvent(this,(byte)4);playSound(SoundEvents.IRON_GOLEM_ATTACK,1,1);}
    @Override public void handleEntityEvent(byte event){if(event==4)action=6;else super.handleEntityEvent(event);}
    @Override public boolean doHurtTarget(Entity entity){boolean hit=entity.hurt(damageSources().mobAttack(this),10+random.nextInt(15));if(hit){entity.setDeltaMovement(entity.getDeltaMovement().add(0,.4,0));entity.hurtMarked=true;}return hit;}
    private void returnHome(){
        for(int y=1;y>=-1;y--)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var pos=anchor.offset(x,y,z);if(!level().hasChunkAt(pos)||!level().getWorldBorder().isWithinBounds(pos)||!level().getBlockState(pos.below()).isFaceSturdy(level(),pos.below(),Direction.UP))continue;
            var box=getBoundingBox().move(pos.getX()+.5-getX(),pos.getY()-getY(),pos.getZ()+.5-getZ());
            if(level().noCollision(this,box)){teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);getNavigation().stop();return;}
        }
    }
    private void excavate(ServerPlayer player,ItemStack focus,ExcavationFocusItem item){
        var level=player.serverLevel();var eye=player.getEyePosition();var end=eye.add(player.getLookAngle().scale(5));
        // Clip only when every crossed sample is loaded, including chunk-boundary endpoints.
        for(int step=0;step<=10;step++)if(!level.hasChunkAt(BlockPos.containing(eye.lerp(end,step/10.0))))return;
        var hit=level.clip(new ClipContext(eye,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));if(hit.getType()!=HitResult.Type.BLOCK)return;
        var center=hit.getBlockPos();var tool=new ItemStack(TCItems.PRIMAL_CRUSHER.get());if(!effective(tool,level.getBlockState(center)))return;
        int fortune=item.upgradeLevel(focus,FocusUpgrade.TREASURE.id),silk=item.upgradeLevel(focus,FocusUpgrade.SILK_TOUCH.id);
        if(fortune>0)tool.enchant(Enchantments.BLOCK_FORTUNE,fortune);if(silk>0)tool.enchant(Enchantments.SILK_TOUCH,1);animate();
        for(int a=-2;a<=2;a++)for(int b=-2;b<=2;b++){
            var pos=switch(hit.getDirection().getAxis()){case Y->center.offset(a,0,b);case Z->center.offset(a,b,0);case X->center.offset(0,b,a);};
            if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(player,pos)||player.blockActionRestricted(level,pos,player.gameMode.getGameModeForPlayer()))continue;
            var state=level.getBlockState(pos);if(state.isAir()||state.getDestroySpeed(level,pos)<0||!effective(tool,state)||ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)==-1)continue;
            var drops=Block.getDrops(state,level,pos,level.getBlockEntity(pos),player,tool);int xp=silk>0?0:state.getExpDrop(level,random,pos,fortune,0);
            if(!level.removeBlock(pos,false))continue;for(var stack:drops)Block.popResource(level,pos,stack);if(xp>0)state.getBlock().popExperience(level,pos,xp);level.levelEvent(2001,pos,Block.getId(state));
        }
    }
    private static boolean effective(ItemStack tool,BlockState state){return tool.isCorrectToolForDrops(state)&&tool.getDestroySpeed(state)>1;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(anchor!=null)tag.putLong("ConciliumHome",anchor.asLong());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);anchor=tag.contains("ConciliumHome")?BlockPos.of(tag.getLong("ConciliumHome")):blockPosition();}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return SoundEvents.IRON_GOLEM_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.IRON_GOLEM_DEATH;}
    @Override protected void playStepSound(BlockPos pos,BlockState state){playSound(SoundEvents.IRON_GOLEM_STEP,1,1);}
}
