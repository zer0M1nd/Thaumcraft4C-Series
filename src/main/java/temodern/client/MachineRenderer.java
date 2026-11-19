package temodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import temodern.TE;
import temodern.machine.*;

@Mod.EventBusSubscriber(modid=TE.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class MachineRenderer implements BlockEntityRenderer<NetworkMachine> {
    private final ModelPart shaft,spoke;
    private final net.minecraft.client.renderer.entity.ItemRenderer items;
    public MachineRenderer(BlockEntityRendererProvider.Context context){
        var mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("shaft",CubeListBuilder.create().texOffs(1,15).addBox(-1.5f,-.5f,-.5f,3,1,1),PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("spoke",CubeListBuilder.create().texOffs(1,18).addBox(-.5f,-3.5f,-.5f,1,7,1),PartPose.ZERO);
        var root=LayerDefinition.create(mesh,32,32).bakeRoot();shaft=root.getChild("shaft");spoke=root.getChild("spoke");items=context.getItemRenderer();
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(Machines.TYPE.get(),MachineRenderer::new);}
    @Override public void render(NetworkMachine machine,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(machine.kind().endsWith("gearbox")){
            if(temodern.ClientConfig.DISABLE_GEARBOX_MODEL.get())return;
            var texture=ResourceLocation.fromNamespaceAndPath(TE.ID,"textures/modern/block/"+(machine.kind().startsWith("iron")?"iron":"thaumium")+"_gear.png");
            var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
            float angle=machine.getLevel()!=null&&machine.clientCrankUntil>machine.getLevel().getGameTime()?(machine.getLevel().getGameTime()+partial)*.4f:0;
            pose.pushPose();pose.translate(.5,.5,.5);pose.scale(1.92f,1.92f,1.92f);
            for(var side:Direction.values()){
                pose.pushPose();
                if(side.getAxis()==Direction.Axis.Y)pose.mulPose(Axis.ZP.rotationDegrees(90));
                if(side.getAxis()==Direction.Axis.Z)pose.mulPose(Axis.YP.rotationDegrees(90));
                int sign=side==Direction.WEST||side==Direction.DOWN||side==Direction.SOUTH?-1:1;
                pose.translate(sign*3.12/16,0,0);pose.mulPose(Axis.XP.rotation(angle));shaft.render(pose,vertices,light,overlay);
                var level=machine.getLevel();boolean connected=level!=null&&level.hasChunkAt(machine.getBlockPos().relative(side))&&appeng.api.implementations.blockentities.ICrankable.get(level,machine.getBlockPos().relative(side),side.getOpposite())!=null;
                if(!connected){pose.translate(sign*.624/16,0,0);for(int i=0;i<4;i++){pose.mulPose(Axis.XP.rotationDegrees(45));spoke.render(pose,vertices,light,overlay);}}
                pose.popPose();
            }
            pose.popPose();
        }
    }
}
