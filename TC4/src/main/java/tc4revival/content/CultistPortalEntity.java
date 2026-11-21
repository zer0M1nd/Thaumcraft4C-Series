package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Stationary encounter controller: opening decorations, escalating waves, then self-consuming reinforcements. */
public  class CultistPortalEntity extends Monster {
    private final ServerBossEvent bossBar=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    private int stage,counter=200,pulse;
    public CultistPortalEntity(EntityType<? extends CultistPortalEntity> type,Level level) { super(type,level); xpReward=30; }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,500).add(Attributes.ATTACK_DAMAGE,0).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.ARMOR,5);
    }
    @Override public boolean isEffectiveAi() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return true; }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public void move(MoverType type,Vec3 movement) { }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance<4096; }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return false; }
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) { return false; }
    public int pulse() { return pulse; }
    public int stage() { return stage; }
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && isAlive()) {
            if (counter>0) {
                counter--;
                if (stage==0 && counter==160) createBanners(server);
                if (stage==0 && counter>20 && counter<150 && counter%13==0) createCrate(server);
            } else if (server.getNearestPlayer(this,48)!=null) {
                server.broadcastEntityEvent(this,(byte)16);
                if (stage<=4) { counter=15+random.nextInt(10-stage)-stage; spawnMinion(server); }
                else if (stage==12) { counter=50+timing()*2+random.nextInt(50); spawnLeader(server); }
                else { int timing=timing(); counter=timing+random.nextInt(5+timing/3); spawnMinion(server); }
                if (stage<Integer.MAX_VALUE) stage++;
            } else counter=30+random.nextInt(30);
            if (stage<12) heal(1);
            bossBar.setName(getDisplayName()); bossBar.setProgress(getHealth()/getMaxHealth());
        }
        if (pulse>0) pulse--;
    }
    private BlockPos origin() { return new BlockPos((int)getX(),(int)getY(),(int)getZ()); }
    private void createBanners(ServerLevel server) {
        server.broadcastEntityEvent(this,(byte)16);
        for (var direction:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}) {
            var pos=origin().offset(-direction.getStepX()*6,0,direction.getStepZ()*6);
            int rotation=switch(direction) { case NORTH->8; case SOUTH->0; case WEST->12; case EAST->4; default->0; };
            CrimsonBanner.place(server,pos,rotation);
            if (server.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BannerBlockEntity) decorationEffect(server,pos);
        }
    }
    private void createCrate(ServerLevel server) {
        var center=origin(); var pos=center.offset(random.nextInt(5)-random.nextInt(5),0,random.nextInt(5)-random.nextInt(5));
        if (pos.getX()==center.getX() || pos.getZ()==center.getZ() || !server.isEmptyBlock(pos)) return;
        server.broadcastEntityEvent(this,(byte)16); float roll=server.random.nextFloat();
        server.setBlock(pos,TCEldritch.loot(true,roll<.05 ? 2 : roll<.2 ? 1 : 0).defaultBlockState(),3); decorationEffect(server,pos);
    }
    private void decorationEffect(ServerLevel server,BlockPos pos) {
        tc4revival.network.FocusEffectPacket.blockArc(server,this,pos,true); playSound(TCSounds.WAND_FAIL.get(),1,1);
    }
    private int timing() { return level().getEntitiesOfClass(CultistEntity.class,new AABB(position(),position()).inflate(32)).size()*20; }
    private void spawnMinion(ServerLevel server) {
        CultistEntity cultist=random.nextFloat()>.33 ? TCEntities.CULTIST_KNIGHT.get().create(server) : TCEntities.CULTIST_CLERIC.get().create(server);
        if (cultist!=null) spawn(server,cultist);
        if (stage>12) hurt(damageSources().fellOutOfWorld(),5+random.nextInt(5));
    }
    private void spawnLeader(ServerLevel server) { var leader=TCEntities.CULTIST_LEADER.get().create(server); if (leader!=null) spawn(server,leader); }
    private void spawn(ServerLevel server,Mob cultist) {
        cultist.setPos(getX()+random.nextFloat()-random.nextFloat(),getY()+.25,getZ()+random.nextFloat()-random.nextFloat());
        cultist.finalizeSpawn(server,server.getCurrentDifficultyAt(cultist.blockPosition()),MobSpawnType.EVENT,null,null);
        cultist.restrictTo(origin(),32); server.addFreshEntity(cultist); cultist.spawnAnim(); cultist.playSound(TCSounds.WAND_FAIL.get(),1,1);
    }
    @Override public void playerTouch(Player player) {
        if (!level().isClientSide && distanceToSqr(player)<3 && player.hurt(damageSources().indirectMagic(this,this),8)) playSound(TCSounds.ZAP.get(),1,1+(random.nextFloat()-random.nextFloat())*.1f);
    }
    @Override public void handleEntityEvent(byte event) { if (event==16) { pulse=10; spawnAnim(); } else super.handleEntityEvent(event); }
    @Override protected float getSoundVolume() { return .75f; }
    @Override public int getAmbientSoundInterval() { return 540; }
    @Override protected SoundEvent getAmbientSound() { return TCSounds.MONOLITH.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return TCSounds.ZAP.get(); }
    @Override protected SoundEvent getDeathSound() { return TCSounds.SHOCK.get(); }
    @Override public void die(DamageSource source) {
        if (!level().isClientSide && !dead) level().explode(this,getX(),getY(),getZ(),2,false,Level.ExplosionInteraction.NONE);
        super.die(source);
    }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) { }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossBar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossBar.removePlayer(player); }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("stage",stage); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); stage=Math.max(0,tag.getInt("stage")); }
    @Override public net.minecraft.world.entity.item.ItemEntity spawnAtLocation(net.minecraft.world.item.ItemStack stack,float offset) {
        if(!stack.is(TCItems.PRIMORDIAL_PEARL.get()))return super.spawnAtLocation(stack,offset);
        var item=TCEntities.SPECIAL_ITEM.get().create(level());if(item==null)return null;
        item.setPos(getX(),getY()+getBbHeight()/2,getZ());item.setItem(stack);item.setDefaultPickUpDelay();
        if(captureDrops()!=null)captureDrops().add(item);else level().addFreshEntity(item);return item;
    }

}
