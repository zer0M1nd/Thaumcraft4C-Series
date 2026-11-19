package gadomancymodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gadomancymodern.claw.InfusionClawEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Original eight UV sections, four redstone plates, four orbiting plates and suspended short wand. */
public class InfusionClawRenderer implements BlockEntityRenderer<InfusionClawEntity> {
    protected static final ResourceLocation TEXTURE=ResourceLocation.parse("gadomancy:textures/models/infusion_claw.png");
    protected final ModelPart[] parts=new ModelPart[8];
    protected final ClawWandRenderer wandRenderer=new ClawWandRenderer();
    protected final float[] idle=new float[12];
    public InfusionClawRenderer(BlockEntityRendererProvider.Context context){for(int i=0;i<8;i++){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("plate",CubeListBuilder.create().texOffs(0,8*i).addBox(-3,-1,-3,6,2,6),PartPose.ZERO);parts[i]=LayerDefinition.create(mesh,32,64).bakeRoot();}}
    @Override public void render(InfusionClawEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(entity,partial,pose,buffers,light,overlay);}
    protected void plate(int index,PoseStack pose,MultiBufferSource buffers,int light,int overlay){parts[index].render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,overlay);}
    public void draw(InfusionClawEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();float ticks=mc.getCameraEntity()==null?0:mc.getCameraEntity().tickCount+partial;
        float elapsed=entity==null?0:entity.lastRenderTick<0?0:Math.max(0,Math.min(10,ticks-entity.lastRenderTick));if(entity!=null)entity.lastRenderTick=ticks;
        float[] state=entity==null?idle:entity.animation;pose.pushPose();pose.translate(.5,.5,.5);
        float speed=(float)Math.pow(1.1,Math.abs(state[8]));
        if(state[8]>0){state[8]+=elapsed;if(state[8]>50){state[8]=-state[8];state[9]=1;if(entity!=null)zap(entity);}}else if(state[8]<0)state[8]=Math.min(0,state[8]+elapsed*.5f);
        for(int i=0;i<4;i++){
            var side=Direction.from3DDataValue(i+2);state[i]+=Math.random()*2*elapsed*speed;state[i+4]+=Math.random()*2*elapsed*speed;
            float height=(float)(.0625*Math.sin(state[i]/30)),width=(float)(.0625*Math.sin(state[i+4]/30));
            boolean powered=entity!=null&&entity.getLevel()!=null&&entity.getLevel().getSignal(entity.getBlockPos().relative(side),side)>0;
            pose.pushPose();pose.translate(-side.getStepZ()*width,height,-side.getStepX()*width);pose.translate(.453125*side.getStepX(),0,.453125*side.getStepZ());pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)Math.PI/2,-side.getStepZ(),0,side.getStepX()));pose.scale(.75f,.75f,.75f);plate(powered?6:5,pose,buffers,light,overlay);pose.popPose();
        }
        speed=Math.min(50,(float)Math.pow(1.1,Math.abs(state[9])));
        if(state[9]>0){state[9]+=elapsed*.5f;if(state[9]>100){state[9]=-state[9];if(entity!=null&&entity.getItem(0).getItem() instanceof tc4revival.content.CastingWandItem wand)state[11]=wand.isStaff(entity.getItem(0))?.5f:.1f;}}else if(state[9]<0)state[9]=Math.min(0,state[9]+elapsed*.5f);
        for(int i=0;i<4;i++){state[10]+=speed*elapsed*.001f*(float)Math.PI;float angle=state[10]+i*.5f*(float)Math.PI;pose.pushPose();pose.translate(.25*Math.sin(angle),.0625*Math.sin(angle/1.4)+.0625-.27,.25*Math.cos(angle));pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)Math.PI/4,(float)Math.cos(angle),0,(float)-Math.sin(angle)));pose.scale(.75f,.75f,.75f);plate(i,pose,buffers,light,overlay);pose.popPose();}
        pose.mulPose(Axis.YP.rotation(ticks/60));pose.pushPose();pose.translate(0,.453125,0);pose.scale(.75f,.75f,.75f);plate(4,pose,buffers,light,overlay);pose.popPose();
        if(entity!=null&&!entity.getItem(0).isEmpty()){pose.pushPose();pose.mulPose(Axis.XP.rotationDegrees(180));pose.scale(.5f,.5f,.5f);pose.translate(0,Math.sin(ticks/30)*.08+.08-1.4924,0);if(((tc4revival.content.CastingWandItem)entity.getItem(0).getItem()).isStaff(entity.getItem(0)))pose.translate(0,-.5,0);wandRenderer.render(entity.getItem(0),ticks,pose,buffers,light,overlay);pose.popPose();}
        pose.popPose();
        if(entity!=null&&state[11]>0&&mc.player!=null){state[11]+=elapsed/70;pose.pushPose();pose.translate(.5,.75-state[11],.5);pose.scale(.35f,.35f,.35f);var orb=new tc4revival.content.PrimalOrbEntity(entity.getLevel(),mc.player,true);orb.tickCount=(int)ticks;mc.getEntityRenderDispatcher().getRenderer(orb).render(orb,0,partial,pose,buffers,light);pose.popPose();if(.75-state[11]<-.5)state[11]=0;}
    }
    protected void zap(InfusionClawEntity entity){var level=entity.getLevel();if(level==null)return;var pos=entity.getBlockPos();for(var side:Direction.Plane.HORIZONTAL){var from=Vec3.atLowerCornerOf(pos).add(.5+.5*side.getStepX(),.6,.5+.5*side.getStepZ());var to=Vec3.atLowerCornerOf(pos).add(.5+.2*side.getStepX(),.2,.5+.2*side.getStepZ());FamiliarVisuals.nodeBolt(from,to);level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),tc4revival.content.TCSounds.ZAP.get(),net.minecraft.sounds.SoundSource.BLOCKS,.1f,1+level.random.nextFloat()*.2f,false);}}
}
