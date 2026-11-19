package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import tc4revival.content.*;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;
import thaumcraft.api.fx.LegacyEffects;

/** Natural and bound rift lifecycle. Boss transitions and dimensional transit remain separate systems. */
public class RiftEntity extends Entity {
    private static final EntityDataAccessor<Integer> SIZE=SynchedEntityData.defineId(RiftEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEED=SynchedEntityData.defineId(RiftEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(RiftEntity.class,EntityDataSerializers.INT);
    private int targetSize=5,age,lastSize=-1,lastSeed;
    private double centerOffset;
    private BlockPos boundCenter;
    private boolean brightestPending;
    private boolean peelerPending;
    private boolean pontifexPending;
    public BlockPos boundCenter(){return boundCenter;}
    public void bind(BlockPos center){boundCenter=center.immutable();}
    public boolean isClosing(){return entityData.get(PHASE)==1 && targetSize==0;}
    private RiftGeometry geometry=RiftGeometry.create(5,0);
    public RiftEntity(EntityType<? extends RiftEntity> type,Level level){super(type,level);noPhysics=true;}
    @Override protected void defineSynchedData(){entityData.define(SIZE,5);entityData.define(SEED,0);entityData.define(PHASE,0);}
    public int riftSize(){return entityData.get(SIZE);}
    public RiftGeometry geometry(){updateGeometry();return geometry;}
    public double centerOffset(){return centerOffset;}
    public void initialize(int seed,int target){entityData.set(SEED,seed);targetSize=Mth.clamp(target,5,300);entityData.set(PHASE,-1);updateGeometry();}
    public void collapseTo(int size){targetSize=Mth.clamp(size,0,riftSize());entityData.set(PHASE,1);}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public net.minecraft.world.InteractionResult interact(Player player,net.minecraft.world.InteractionHand hand){
        if(player.getItemInHand(hand).is(ConciliumItems.POLISHMENT_DEVICE.get())){
            if(!level().isClientSide)discard();return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.interact(player,hand);
    }
    @Override public boolean isPushable(){return false;}
    @Override public boolean fireImmune(){return true;}
    @Override public void move(MoverType type,Vec3 delta){}
    private void size(int value){entityData.set(SIZE,Mth.clamp(value,0,4096));updateGeometry();}
    private void updateGeometry(){
        if(lastSize==riftSize() && lastSeed==entityData.get(SEED))return;
        lastSize=riftSize();lastSeed=entityData.get(SEED);geometry=RiftGeometry.create(lastSize,lastSeed);
        double minX=0,minY=0,minZ=0,maxX=0,maxY=0,maxZ=0;
        for(var point:geometry.points()) {minX=Math.min(minX,point.x);minY=Math.min(minY,point.y);minZ=Math.min(minZ,point.z);maxX=Math.max(maxX,point.x);maxY=Math.max(maxY,point.y);maxZ=Math.max(maxZ,point.z);}
        double offset=(maxY-minY)/2;centerOffset=offset;
        setBoundingBox(new AABB(getX()+minX,getY()+minY+offset,getZ()+minZ,getX()+maxX,getY()+maxY+offset,getZ()+maxZ).inflate(.1));
    }
    @Override public void setPos(double x,double y,double z){super.setPos(x,y,z);lastSize=-1;if(geometry!=null)updateGeometry();}
    @Override public void tick(){
        super.tick();updateGeometry();if(!(level() instanceof ServerLevel level))return;age++;RiftTransit.tick(this,age);
        if(!level.hasChunkAt(blockPosition()))return;
        if(boundCenter!=null && level.hasChunkAt(boundCenter)
                && (!(level.getBlockEntity(boundCenter) instanceof PredictabilityBlockEntity hex) || !hex.isMaster()))collapseTo(0);
        if(age%300==0)playSound(TCSounds.EVIL_PORTAL.get(),Math.max(.01f,(float)(.15+random.nextGaussian()*.066)),(float)(.75+random.nextGaussian()*.1));
        var points=geometry.points();int index=random.nextInt(points.size()-1);
        var start=points.get(index).add(position()).add(0,centerOffset(),0);var end=points.get(index+1).add(position()).add(0,centerOffset(),0);
        if(level.hasChunkAt(BlockPos.containing(start)) && level.hasChunkAt(BlockPos.containing(end))) {
            if(!RiftTransit.active(this)&&net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level,this)) {
                var hit=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
                if(hit.getType()!=HitResult.Type.MISS) {
                    var pos=hit.getBlockPos();var state=level.getBlockState(pos);
                    if(state.is(BlockTags.ENDERMAN_HOLDABLE) && state.getDestroySpeed(level,pos)>=0
                            && level.getWorldBorder().isWithinBounds(pos)
                            && !net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.BlockEvent.BreakEvent(
                                    level,pos,state,net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level))))level.removeBlock(pos,false);
                }
            }
            for(var target:level.getEntities(this,new AABB(start,start).inflate(.5))) {
                if(boundCenter!=null && !brightestPending && !peelerPending && !pontifexPending && !isClosing() && target instanceof ChargedWisp wisp && wisp.getOwner() instanceof net.minecraft.server.level.ServerPlayer player
                        && thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"thaumicconcilium:BRIGHTESTONE")){
                    brightestPending=true;collapseTo(0);wisp.discard();continue;
                }
                if(boundCenter!=null && !brightestPending && !peelerPending && !pontifexPending && !isClosing() && target instanceof ItemEntity item && CrimsonContent.available()&&item.getItem().is(TCItems.PRIMORDIAL_PEARL.get())){
                    var saved=new CompoundTag();item.saveWithoutId(saved);
                    if(saved.hasUUID("Thrower")&&KnowledgeSavedData.get(level.getServer()).snapshot(saved.getUUID("Thrower"))
                            .map(knowledge->tc4revival.research.ResearchCatalogs.rules().completed(knowledge,"thaumicconcilium:CRIMSONPONTIFEX")).orElse(false)){
                        pontifexPending=true;collapseTo(0);var remainder=item.getItem().copy();remainder.shrink(1);if(remainder.isEmpty())item.discard();else item.setItem(remainder);continue;
                    }
                }
                if(boundCenter!=null && !brightestPending && !peelerPending && !pontifexPending && !isClosing() && target instanceof ItemEntity item && item.getItem().is(ConciliumItems.PRIMORDIAL_LIFE.get())){
                    var saved=new CompoundTag();item.saveWithoutId(saved);
                    if(saved.hasUUID("Thrower") && KnowledgeSavedData.get(level.getServer()).snapshot(saved.getUUID("Thrower"))
                            .map(knowledge->tc4revival.research.ResearchCatalogs.rules().completed(knowledge,"thaumicconcilium:MATERIALPEELER")).orElse(false)){
                        peelerPending=true;collapseTo(0);var remainder=item.getItem().copy();remainder.shrink(1);if(remainder.isEmpty())item.discard();else item.setItem(remainder);continue;
                    }
                }
                if(RiftTransit.contact(this,target))continue;
                if(!target.isAlive() || target.isSpectator() || target instanceof Player player && player.getAbilities().instabuild)continue;
                if(boundCenter==null){
                    if(target instanceof ItemEntity) {if(age>100)target.discard();}
                    else target.hurt(level.damageSources().fellOutOfWorld(),2);
                }
            }
            if(boundCenter!=null && level.hasChunkAt(boundCenter) && level.getBlockEntity(boundCenter) instanceof PredictabilityBlockEntity hex && hex.heat()>0 && !hex.revealedAspects().isEmpty()){
                var aspects=hex.revealedAspects().aspects();int shown=0;
                for(var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(start,start).inflate(1.4))){
                    if(shown++>=6)break;ConciliumArcNetwork.emit(level,start,item.position(),aspects.get(random.nextInt(aspects.size())).color(),hex.heat()/1200f);
                }
                if(shown>0)playSound(TCSounds.JACOBS.get(),.5f,1+(random.nextFloat()-random.nextFloat())*.2f);
            }
        }
        if(boundCenter==null && !RiftTransit.active(this) && age%300==0)fireEvent(level);
        if(entityData.get(PHASE)==1) {
            if(riftSize()>targetSize) {size(riftSize()-1);if(targetSize==0 && random.nextInt(3)==0)taintBurst(level);}
            else entityData.set(PHASE,0);
        } else if(entityData.get(PHASE)==-1) {
            if(riftSize()<targetSize){if(random.nextInt(3)==0)size(riftSize()+1);}else entityData.set(PHASE,0);
        }
        if(riftSize()>300)collapseTo(riftSize()/2);
        if(riftSize()==0){
            if(pontifexPending){var portal=CrimsonContent.PORTAL.get().create(level);if(portal==null)return;portal.setPos(position());portal.summonsBoss(true);if(!level.addFreshEntity(portal))return;pontifexPending=false;playSound(TCSounds.CRAFT_FAIL.get(),1,1);}
            if(peelerPending){var peeler=ConciliumEntities.PEELER.get().create(level);if(peeler==null || boundCenter==null)return;peeler.setPos(position());peeler.bind(boundCenter);if(!level.addFreshEntity(peeler))return;peelerPending=false;tc4revival.network.FocusEffectPacket.burst(level,position(),2);playSound(TCSounds.CRAFT_FAIL.get(),1,1);}
            if(brightestPending){var boss=ConciliumEntities.BRIGHTEST.get().create(level);if(boss==null)return;boss.setPos(position());if(!level.addFreshEntity(boss))return;brightestPending=false;}
            if(boundCenter==null && !RiftTransit.active(this)){int count=random.nextInt(12);if(count>0)spawnAtLocation(new ItemStack(TCItems.material(MaterialType.VOID_SEED),count),1.5f);}discard();
        }
    }
    private int warpGrowth(ServerLevel level,Player player){return KnowledgeSavedData.get(level.getServer()).snapshot(player.getUUID()).map(k->Mth.clamp(k.totalWarp(),2,10)).orElse(2);}
    private void fireEvent(ServerLevel level){
        var area=getBoundingBox().inflate(7);var players=level.getEntitiesOfClass(Player.class,area);var living=level.getEntitiesOfClass(LivingEntity.class,area);
        switch(random.nextInt(10)) {
            case 0 -> {for(var player:players)targetSize=Math.min(4096,targetSize+warpGrowth(level,player));entityData.set(PHASE,-1);}
            case 1 -> {if(!living.isEmpty())for(int i=0;i<Mth.clamp(riftSize()%11,3,10);i++) {
                var target=living.get(random.nextInt(living.size()));var orb=new PrimalOrbEntity(TCEntities.PRIMAL_ORB.get(),level);
                orb.setPos(position().add(random.nextInt(6)-3,random.nextInt(6)-3,random.nextInt(6)-3));var direction=target.position().subtract(position());
                orb.shoot(direction.x,direction.y,direction.z,.66f,.1f);level.addFreshEntity(orb);
            }}
            case 2 -> {for(var target:living)zap(target,level);}
            case 3 -> {for(var player:players){size(riftSize()+warpGrowth(level,player));zap(player,level);}}
            case 4 -> spawnSpiders(level,players);
            case 5 -> {if(!living.isEmpty())for(int i=0;i<Mth.clamp(riftSize()%11,3,10);i++) {
                var target=living.get(random.nextInt(living.size()));var wisp=new ChargedWisp(ConciliumEntities.CHARGED_WISP.get(),level);
                var aspects=TCAspects.catalog().all();wisp.setPos(position());wisp.initialize(this,aspects.get(random.nextInt(aspects.size())).tag(),0,0);
                wisp.setDeltaMovement(target.position().add(target.getDeltaMovement()).subtract(position()).normalize());level.addFreshEntity(wisp);
            }}
            case 6 -> {if(random.nextInt(100)>60)collapseTo(0);}
            default -> {}
        }
    }
    private void zap(LivingEntity target,ServerLevel level){
        tc4revival.network.FocusEffectPacket.nodeZap(level,position().add(.5,.5,.5),target.position().add(0,target.getBbHeight()/2,0));
        target.hurt(level.damageSources().magic(),4+random.nextInt(Mth.clamp(riftSize(),5,30)));
    }
    private void spawnSpiders(ServerLevel level,java.util.List<Player> players){
        for(int i=0;i<Math.min(50,riftSize());i++) {
            var spider=TCEntities.MIND_SPIDER.get().create(level);if(spider==null)return;
            for(int attempt=0;attempt<50;attempt++) {
                var pos=blockPosition().offset((7+random.nextInt(18))*(random.nextInt(3)-1),(7+random.nextInt(18))*(random.nextInt(3)-1),(7+random.nextInt(18))*(random.nextInt(3)-1));
                if(!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP))continue;
                spider.setPos(Vec3.atBottomCenterOf(pos));if(!level.noCollision(spider) || level.containsAnyLiquid(spider.getBoundingBox()))continue;
                if(!players.isEmpty())spider.setTarget(players.get(random.nextInt(players.size())));level.addFreshEntity(spider);break;
            }
        }
    }
    private void taintBurst(ServerLevel level){
        if(!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level,this))return;
        var origin=BlockPos.containing(getX()+random.nextGaussian()*1.5,getY(),getZ()+random.nextGaussian()*1.5);
        for(int i=0;i<10;i++) {
            var pos=origin.offset((int)((random.nextFloat()-random.nextFloat())*5),0,(int)((random.nextFloat()-random.nextFloat())*5));
            if(!random.nextBoolean() || !canChange(level,pos) || level.getBiome(pos).is(TCBiomes.TAINTED_LAND))continue;
            if(!WorldBiomeChanges.changeColumn(level,pos,TCBiomes.TAINTED_LAND))continue;
            if(level.getBlockState(pos.below()).isRedstoneConductor(level,pos.below()) && level.getBlockState(pos).canBeReplaced())
                level.setBlock(pos,TaintFibreBlock.faces(TCArcane.TAINT_FIBRES.get().defaultBlockState(),level,pos),3);
            if(canChange(level,pos.above()) && level.isEmptyBlock(pos.above()))level.setBlock(pos.above(),(random.nextBoolean()?TCArcane.FLUX_GAS.get():TCArcane.FLUX_GOO.get()).defaultBlockState(),3);
        }
    }
    private boolean canChange(ServerLevel level,BlockPos pos){
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
                && !net.minecraftforge.event.ForgeEventFactory.onBlockPlace(this,net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos),net.minecraft.core.Direction.UP);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putBoolean("Interdimensional",RiftTransit.active(this));tag.putBoolean("PontifexPending",pontifexPending);tag.putBoolean("PeelerPending",peelerPending);tag.putBoolean("BrightestPending",brightestPending);tag.putInt("RiftSize",riftSize());tag.putInt("RiftSeed",entityData.get(SEED));tag.putInt("Phase",entityData.get(PHASE));tag.putInt("MaxSize",targetSize);tag.putInt("Age",age);if(boundCenter!=null)tag.putLong("BoundCenter",boundCenter.asLong());}
    @Override protected void readAdditionalSaveData(CompoundTag tag){if(tag.getBoolean("Interdimensional"))RiftTransit.initialize(this);peelerPending=tag.getBoolean("PeelerPending");brightestPending=!peelerPending && tag.getBoolean("BrightestPending");pontifexPending=!peelerPending&&!brightestPending&&tag.getBoolean("PontifexPending");entityData.set(SEED,tag.getInt("RiftSeed"));entityData.set(PHASE,Mth.clamp(tag.getInt("Phase"),-1,1));targetSize=Mth.clamp(tag.getInt("MaxSize"),0,4096);age=Math.max(0,tag.getInt("Age"));boundCenter=tag.contains("BoundCenter")?BlockPos.of(tag.getLong("BoundCenter")):null;size(tag.getInt("RiftSize"));}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
