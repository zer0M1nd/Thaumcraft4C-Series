package temodern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.client.GolemModel;
import tc4revival.client.GolemRenderer;
import tc4revival.content.GolemEntity;
import temodern.TE;
import temodern.golem.WirelessBackpack;

@Mod.EventBusSubscriber(modid=TE.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BackpackLayer extends RenderLayer<GolemEntity,GolemModel> {
    private final ModelPart backpack;
    private static ResourceLocation texture(GolemEntity entity){
        String skin=entity.configuration().material().name().toLowerCase(java.util.Locale.ROOT);
        if(skin.equals("thaumium"))skin="thaum";
        return ResourceLocation.fromNamespaceAndPath(TE.ID,"textures/models/backpack_"+skin+".png");
    }
    public BackpackLayer(GolemRenderer renderer){
        super(renderer);var mesh=new MeshDefinition();
        var pose=PartPose.offsetAndRotation(0,4,7,0,(float)Math.PI/2,0);
        mesh.getRoot().addOrReplaceChild("pack",CubeListBuilder.create().texOffs(0,0).addBox(-1,-3,-3,2,6,6).texOffs(2,0).addBox(-1.5f,-1,-2,1,2,4).texOffs(10,0).addBox(-.5f,-6,-.5f,1,3,1),pose);
        mesh.getRoot().addOrReplaceChild("pearl",CubeListBuilder.create().texOffs(8,6).addBox(-.65f,-.65f,-.65f,1.3f,1.3f,1.3f),PartPose.offset(0,-2.5f,7));
        backpack=LayerDefinition.create(mesh,16,16).bakeRoot();
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event){
        var renderer=event.getRenderer(tc4revival.content.TCEntities.GOLEM.get());if(renderer instanceof GolemRenderer golem)golem.addLayer(new BackpackLayer(golem));
    }
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,GolemEntity entity,float walk,float amount,float partial,float age,float yaw,float pitch){
        if(entity.isInvisible()||WirelessBackpack.data(entity).isEmpty())return;
        var item=ItemStack.of(WirelessBackpack.data(entity));var facade=item.hasTag()?ItemStack.of(item.getTag().getCompound("facade")):ItemStack.EMPTY;
        pose.pushPose();pose.scale(.4f,.4f,.4f);getParentModel().root().getChild("body").translateAndRotate(pose);
        if(facade.getItem() instanceof appeng.items.parts.FacadeItem facadeItem){
            pose.translate(-.3,0,.3);pose.scale(.6f,.6f,.25f);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(facadeItem.getTextureBlockState(facade),pose,buffers,light,OverlayTexture.NO_OVERLAY);
        }else {
            var pearl=backpack.getChild("pearl");pearl.yRot=age*.08f;pearl.visible=false;
            var vertices=buffers.getBuffer(RenderType.entityCutoutNoCull(texture(entity)));
            backpack.render(pose,vertices,light,OverlayTexture.NO_OVERLAY);pearl.visible=true;
            boolean online=WirelessBackpack.data(entity).getBoolean("connected");pearl.render(pose,vertices,light,OverlayTexture.NO_OVERLAY,1,online?1:.15f,online?1:.15f,1);
        }
        pose.popPose();
    }
}
