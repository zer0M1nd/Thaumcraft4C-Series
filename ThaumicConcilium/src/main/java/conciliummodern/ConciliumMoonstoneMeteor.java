package conciliummodern;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Original AM2 moonstone-meteor behavior, absent from AML 1.5.0; uses AML's actual moonstone ore. */
public final class ConciliumMoonstoneMeteor extends Entity {
    public static final net.minecraft.resources.ResourceLocation ID=net.minecraft.resources.ResourceLocation.parse("thaumicconcilium:moonstone_meteor");
    public static final RegistryObject<EntityType<ConciliumMoonstoneMeteor>> TYPE=RegistryObject.create(ID,ForgeRegistries.ENTITY_TYPES);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> STAR=net.minecraft.network.syncher.SynchedEntityData.defineId(ConciliumMoonstoneMeteor.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private BlockPos target=BlockPos.ZERO;private int age;
    public ConciliumMoonstoneMeteor(EntityType<? extends ConciliumMoonstoneMeteor> type,Level level){super(type,level);}
    public void target(BlockPos at){target=at.immutable();}
    public BlockPos target(){return target;}
    public boolean shootingStar(){return entityData.get(STAR);}
    public void shootingStar(boolean value){entityData.set(STAR,value);}
    @Override protected void defineSynchedData(){entityData.define(STAR,false);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){target=BlockPos.of(tag.getLong("Target"));age=Math.max(0,tag.getInt("Age"));shootingStar(tag.getBoolean("ShootingStar"));}
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.putLong("Target",target.asLong());tag.putInt("Age",age);tag.putBoolean("ShootingStar",shootingStar());}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;if(!(level() instanceof ServerLevel level))return;
        if(++age>400||getY()<level.getMinBuildHeight()-16||!level.hasChunkAt(blockPosition())||!level.getWorldBorder().isWithinBounds(blockPosition())){discard();return;}
        var offset=position().subtract(Vec3.atCenterOf(target));double horizontal=Math.sqrt(offset.x*offset.x+offset.z*offset.z);
        double angle=Math.atan2(offset.z,offset.x),vertical=Math.atan2(offset.y,horizontal);
        var movement=new Vec3(-Math.cos(angle)*.2,-Math.sin(vertical)*2.5,-Math.sin(angle)*.2);if(getY()<=target.getY())movement=new Vec3(0,-2.5,0);
        if(shootingStar())movement=new Vec3(0,Math.max(-2,getDeltaMovement().y-.1),0);
        setDeltaMovement(movement);var next=position().add(movement);if(!level.hasChunkAt(BlockPos.containing(next))){discard();return;}
        var hit=level.clip(new ClipContext(position(),next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if(shootingStar())level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.24f,.58f,.71f),1.5f),getX(),getY(),getZ(),5,.15,.15,.15,.02);
        else level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,getX(),getY(),getZ(),3,.1,.1,.1,.015);
        var entityHit=shootingStar()?net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,this,position(),hit.getType()==HitResult.Type.MISS?next:hit.getLocation(),getBoundingBox().expandTowards(movement).inflate(.3),e->e instanceof LivingEntity&&e.isAlive()):null;
        if(hit.getType()!=HitResult.Type.MISS||entityHit!=null){setPos(entityHit!=null?entityHit.getLocation():hit.getLocation());if(shootingStar())starImpact(level);else impact(level);discard();}else setPos(next);
    }
    private void starImpact(ServerLevel level){
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(5),LivingEntity::isAlive)){
            var eye=entity.getEyePosition();var visible=level.clip(new ClipContext(position().add(0,.1,0),eye,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            if(visible.getType()==HitResult.Type.MISS)entity.hurt(level.damageSources().magic(),50);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,getX(),getY()+.5,getZ(),1,0,0,0,0);
        level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.24f,.58f,.71f),2),getX(),getY()+.5,getZ(),80,2.5,.5,2.5,.1);
    }
    private void impact(ServerLevel level){
        if(!level.hasChunkAt(target)||!level.getWorldBorder().isWithinBounds(target)||!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level,this))return;
        var actor=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);if(!level.mayInteract(actor,target))return;
        level.explode(this,target.getX(),target.getY(),target.getZ(),.8f,false,Level.ExplosionInteraction.MOB);
        var ore=ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("arsmagicalegacy:moonstone_ore"));if(ore==null||ore==Blocks.AIR)return;
        for(int i=0,count=random.nextInt(6)+1;i<count;i++){
            var at=target.offset(random.nextInt(4)-2,0,random.nextInt(4)-2);if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at))continue;
            while(!level.isEmptyBlock(at)&&at.getY()<level.getMaxBuildHeight())at=at.above();
            if(level.isOutsideBuildHeight(at)||!level.mayInteract(actor,at))continue;
            var state=(i==0||random.nextInt(4)<2?ore:Blocks.STONE).defaultBlockState();
            if(!net.minecraftforge.event.ForgeEventFactory.onBlockPlace(actor,net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,at),Direction.UP))level.setBlockAndUpdate(at,state);
        }
    }
    @Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class MeteorLifecycle {
        @SubscribeEvent public static void register(RegisterEvent event){if(ArsLegacyIntegration.available())event.register(ForgeRegistries.Keys.ENTITY_TYPES,helper->helper.register(ID,EntityType.Builder.<ConciliumMoonstoneMeteor>of(ConciliumMoonstoneMeteor::new,MobCategory.MISC).sized(.5f,.5f).clientTrackingRange(12).updateInterval(1).build(ID.toString())));}
    }
    @Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent public static void render(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event){if(ArsLegacyIntegration.available())event.registerEntityRenderer(TYPE.get(),Renderer::new);}
        private static final class Renderer extends net.minecraft.client.renderer.entity.EntityRenderer<ConciliumMoonstoneMeteor> {
            Renderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context){super(context);}
            @Override public net.minecraft.resources.ResourceLocation getTextureLocation(ConciliumMoonstoneMeteor entity){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
            @Override public void render(ConciliumMoonstoneMeteor meteor,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){if(!meteor.shootingStar()){pose.pushPose();pose.scale(.5f,.5f,.5f);pose.translate(-.5,-.5,-.5);net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.STONE.defaultBlockState(),pose,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);pose.popPose();}super.render(meteor,yaw,partial,pose,buffers,light);}
        }
    }
}
