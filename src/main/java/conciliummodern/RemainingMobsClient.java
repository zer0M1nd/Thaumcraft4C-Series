package conciliummodern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.*;
import tc4revival.content.*;
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class RemainingMobsClient {
 public static void arc(ConciliumArcNetwork.Arc packet,net.minecraft.network.Connection connection){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null||mc.getConnection()==null||mc.getConnection().getConnection()!=connection||!connection.isConnected()||!mc.level.dimension().location().equals(packet.dimension()))return;double distance=packet.from().distanceToSqr(packet.to());if(!Double.isFinite(distance)||distance>65536||!Float.isFinite(packet.height())||packet.height()<0||packet.height()>64)return;EldritchArcEffects.emit(mc.level,packet.from(),packet.to(),(packet.color()>>16&255)/255f,(packet.color()>>8&255)/255f,(packet.color()&255)/255f,packet.height());}
 public static void blood(ConciliumArcNetwork.Blood packet,net.minecraft.network.Connection connection){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null||mc.getConnection()==null||mc.getConnection().getConnection()!=connection||!connection.isConnected()||!mc.level.dimension().location().equals(packet.dimension())||!Double.isFinite(packet.at().lengthSqr()))return;for(int i=0;i<50;i++)mc.particleEngine.add(new BloodParticle(mc.level,packet.at()));}
 private static final class BloodParticle extends net.minecraft.client.particle.BreakingItemParticle {
  private float originalAlpha;
  BloodParticle(net.minecraft.client.multiplayer.ClientLevel level,net.minecraft.world.phys.Vec3 at){super(level,at.x,at.y+level.random.nextFloat()*2,at.z,new ItemStack(Items.SLIME_BALL));boolean bright=random.nextBoolean();setColor(bright?.8f:.6f,0,0);originalAlpha=bright?.4f:.6f;setAlpha(originalAlpha);var velocity=new net.minecraft.world.phys.Vec3(random.nextDouble()*2-1,random.nextDouble()*2-1,random.nextDouble()*2-1).normalize().scale((random.nextDouble()+random.nextDouble()+1)*.15*.9640000000596046);xd=velocity.x;yd=velocity.y+.10000000149;zd=velocity.z;lifetime=(int)(66/(random.nextFloat()*.9f+.1f));}
  @Override public float getQuadSize(float partial){return super.getQuadSize(partial)*Math.max(0,1-age/(float)lifetime);}
  @Override public void move(double mx,double my,double mz){var moved=net.minecraft.world.entity.Entity.collideBoundingBox(null,new net.minecraft.world.phys.Vec3(mx,my,mz),getBoundingBox(),level,java.util.List.of());setBoundingBox(getBoundingBox().move(moved));setLocationFromBoundingbox();onGround=my!=moved.y&&my<0;if(mx!=moved.x)xd=0;if(my!=moved.y)yd=0;if(mz!=moved.z)zd=0;}
  @Override public void render(VertexConsumer out,net.minecraft.client.Camera camera,float partial){alpha=originalAlpha*Math.max(0,1-age/(float)lifetime);super.render(out,camera,partial);}
 }
 private static ResourceLocation skin(String name){return ResourceLocation.parse("thaumicconcilium:textures/entity/"+name+".png");}
 private static <T extends LivingEntity> HumanoidModel<T> biped(){return new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32).bakeRoot());}
 @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(RemainingMobs.DISSOLVED.get(),DissolvedRenderer::new);event.registerEntityRenderer(RemainingMobs.SAMURAI.get(),SamuraiRenderer::new);event.registerEntityRenderer(RemainingMobs.OVERANIMATED.get(),c->new BipedRenderer<>(c,"overanimated"));event.registerEntityRenderer(RemainingMobs.THAUM_GIB.get(),GibRenderer::new);event.registerEntityRenderer(RemainingMobs.DOPE_SQUID.get(),SquidRenderer::new);event.registerEntityRenderer(RemainingMobs.WITHERED_BOTANIST.get(),BotanistRenderer::new);event.registerEntityRenderer(RemainingMobs.UPCOMING_HOLE.get(),NoopRenderer::new);}
 private static class BipedRenderer<T extends Mob> extends HumanoidMobRenderer<T,HumanoidModel<T>> {
  private final ResourceLocation texture;BipedRenderer(EntityRendererProvider.Context c,String skin){super(c,biped(),.5f);texture=skin(skin);}
  @Override public ResourceLocation getTextureLocation(T entity){return texture;}
 }
 private static final class DissolvedRenderer extends BipedRenderer<Dissolved> {
  DissolvedRenderer(EntityRendererProvider.Context c){super(c,"thaumaturge");addLayer(new RenderLayer<Dissolved,HumanoidModel<Dissolved>>(this){
   final VoidRobeModel robe=new VoidRobeModel(true);
   @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,Dissolved entity,float swing,float amount,float partial,float age,float yaw,float pitch){
    var viewer=net.minecraft.client.Minecraft.getInstance().player;boolean ghost=entity.isInvisible();if(ghost&&(viewer==null||entity.isInvisibleTo(viewer)))return;
    robe.prepare(entity,EquipmentSlot.CHEST,getParentModel());robe.setAllVisible(true);
    robe.renderToBuffer(pose,ConciliumShaders.model(buffers.getBuffer(ghost?ConciliumShaders.endTranslucent():ConciliumShaders.end())),light,OverlayTexture.NO_OVERLAY,1,1,1,ghost?.15f:1);
   }
  });}
  @Override protected RenderType getRenderType(Dissolved entity,boolean body,boolean translucent,boolean outline){return body?ConciliumShaders.end():translucent?ConciliumShaders.endTranslucent():null;}
  @Override public void render(Dissolved entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){super.render(entity,yaw,partial,pose,type->type==ConciliumShaders.end()||type==ConciliumShaders.endTranslucent()?ConciliumShaders.model(buffers.getBuffer(type)):buffers.getBuffer(type),light);}
 }
 private static final class SamuraiRenderer extends BipedRenderer<Samurai> {
  SamuraiRenderer(EntityRendererProvider.Context c){super(c,"thaumaturge");addLayer(new SamuraiLayer(this,c));}
 }
 private static final class SamuraiLayer extends RenderLayer<Samurai,HumanoidModel<Samurai>> {
  private final FortressArmorModel armor=new FortressArmorModel(true),waist=new FortressArmorModel(false);private final ModelPart saya=ConciliumSayaGeometry.createBodyLayer().bakeRoot();private ModelPart katana;private final ItemRenderer items;
  SamuraiLayer(SamuraiRenderer parent,EntityRendererProvider.Context context){super(parent);items=context.getItemRenderer();}
  private ModelPart katana(){if(katana==null&&net.minecraftforge.fml.ModList.get().isLoaded("taintedmagic"))try{var type=Class.forName("tmmodern.TMClient$ItemRenderer");var renderer=type.getConstructor().newInstance();var field=type.getDeclaredField("katana");field.setAccessible(true);katana=(ModelPart)field.get(renderer);}catch(ReflectiveOperationException e){throw new IllegalStateException("Tainted Magic katana model contract changed",e);}return katana;}
  @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,Samurai entity,float swing,float amount,float partial,float age,float yaw,float pitch){if(entity.isInvisible())return;armor.prepare(entity,EquipmentSlot.CHEST,getParentModel());armor.setAllVisible(true);armor.head.getAllParts().forEach(part->part.visible=true);for(int i=0;i<3;i++)armor.head.getChild("mask"+i).visible=false;armor.body.getAllParts().forEach(part->part.visible=true);for(var part:new ModelPart[]{armor.leftArm,armor.rightArm,armor.leftLeg,armor.rightLeg})part.getAllParts().forEach(child->child.visible=true);
   String texture=entity.samuraiType()==0?"thaumcraft:textures/models/fortress_armor.png":"taintedmagic:textures/models/model"+(entity.samuraiType()==1?"void":"shadow")+"fortressarmor.png";armor.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(texture))),light,LivingEntityRenderer.getOverlayCoords(entity,0),1,1,1,1);
   // FakeFortress attaches BOTH original branches. Reuse the identical inner model waist.
   waist.prepare(entity,EquipmentSlot.LEGS,getParentModel());waist.body.visible=true;waist.body.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(texture))),light,LivingEntityRenderer.getOverlayCoords(entity,0),1,1,1,1);
   var swordTexture=ResourceLocation.parse("taintedmagic:textures/models/modelkatana"+new String[]{"thaumium","voidmetal","shadowmetal"}[entity.samuraiType()]+".png");pose.pushPose();pose.scale(.5f,.5f,.5f);pose.mulPose(Axis.XP.rotationDegrees(55));pose.mulPose(Axis.YP.rotationDegrees(180));pose.translate(-.6,2.25,1.25);saya.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(swordTexture)),light,OverlayTexture.NO_OVERLAY);if(entity.anger()==0&&katana()!=null)katana.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(swordTexture)),light,OverlayTexture.NO_OVERLAY);pose.popPose();
   boolean low=entity.getHealth()<entity.getMaxHealth()/3;if(!low&&entity.anger()==0)return;ItemStack item;if(low){item=new ItemStack(TCItems.CASTING_WAND.get());item.getOrCreateTag().putString("rod","forbidden_magic:profane");item.getOrCreateTag().putString("cap","taintedmagic:crimsoncloth");WandFocusItem.attach(item,new ItemStack(TCItems.SHOCK_FOCUS.get()));}else item=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("taintedmagic:thaumium_katana")));if(item.isEmpty())return;
   pose.pushPose();getParentModel().rightArm.translateAndRotate(pose);pose.translate(0,.35,-.32);pose.scale(.625f,-.625f,.625f);pose.mulPose(Axis.YP.rotationDegrees(45));if(low){pose.mulPose(Axis.ZP.rotationDegrees(-90));pose.mulPose(Axis.XP.rotationDegrees(-45));pose.translate(.1,-.5,-.1);}else{pose.scale(.5f,.5f,.5f);pose.mulPose(Axis.YP.rotationDegrees(-90));}items.renderStatic(entity,item,ItemDisplayContext.NONE,false,pose,buffers,entity.level(),light,OverlayTexture.NO_OVERLAY,entity.getId());pose.popPose();
  }
 }
 private static final class GibRenderer extends EntityRenderer<ThaumGib>{
  private final ModelPart root=ConciliumGibsGeometry.createBodyLayer().bakeRoot();private final String[] names={"head","left_arm","right_arm","body","left_leg","right_leg"};GibRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.2f;}
  @Override public ResourceLocation getTextureLocation(ThaumGib entity){return skin("thaumaturge");}
  @Override public void render(ThaumGib entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){pose.pushPose();pose.translate(0,entity.gibType()==0?.25:.125,0);pose.mulPose(Axis.YP.rotationDegrees(yaw));pose.mulPose(Axis.XP.rotationDegrees(-90));if(entity.gibType()==0)pose.mulPose(Axis.XP.rotationDegrees(180));root.getChild(names[entity.gibType()]).render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))),light,OverlayTexture.NO_OVERLAY);pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);}
 }
 private static final class BotanistRenderer extends EntityRenderer<WitheredBotanist>{
  private final ModelPart root=ConciliumGibsGeometry.createBodyLayer().bakeRoot();BotanistRenderer(EntityRendererProvider.Context c){super(c);root.getChild("right_arm").setPos(-5,2,0);root.getChild("left_arm").setPos(5,2,0);root.getChild("right_leg").setPos(-1.9f,12,0);root.getChild("left_leg").setPos(1.9f,12,0);}
  @Override public ResourceLocation getTextureLocation(WitheredBotanist e){return skin("withered_botanist");}
  @Override public void render(WitheredBotanist e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){pose.pushPose();pose.translate(0,.1,0);pose.mulPose(Axis.YP.rotationDegrees(yaw));pose.mulPose(Axis.XP.rotationDegrees(90));root.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(e))),light,OverlayTexture.NO_OVERLAY);pose.popPose();super.render(e,yaw,partial,pose,buffers,light);}
 }
 private static final class SquidRenderer extends MobRenderer<DopeSquid,SquidModel<DopeSquid>>{
  SquidRenderer(EntityRendererProvider.Context c){super(c,new SquidModel<>(c.bakeLayer(ModelLayers.SQUID)),.5f);}
  @Override public ResourceLocation getTextureLocation(DopeSquid e){return skin("dope_squid");}
  @Override protected void setupRotations(DopeSquid e,PoseStack pose,float age,float yaw,float partial){pose.translate(0,.5,0);pose.mulPose(Axis.YP.rotationDegrees(180-yaw));pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial,e.previousPitch,e.squidPitch)));pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial,e.previousYaw,e.squidYaw)));pose.translate(0,-1.2,0);}
  @Override protected float getBob(DopeSquid e,float partial){return Mth.lerp(partial,e.previousTentacle,e.tentacleAngle);}
 }
}
