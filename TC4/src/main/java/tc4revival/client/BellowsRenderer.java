package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.BellowsBlockEntity;

public  class BellowsRenderer implements BlockEntityRenderer<BellowsBlockEntity> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("thaumcraft","textures/models/bellows.png");
    private final ModelPart wood,bag;
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public BellowsRenderer(BlockEntityRendererProvider.Context context) { this(); }
    private BellowsRenderer() {
        var mesh=new MeshDefinition(); var root=mesh.getRoot();
        root.addOrReplaceChild("lower",CubeListBuilder.create().texOffs(0,0).addBox(-6,0,-6,12,2,12),PartPose.offset(0,22,0));
        root.addOrReplaceChild("upper",CubeListBuilder.create().texOffs(0,0).addBox(-6,0,-6,12,2,12),PartPose.offset(0,8,0));
        root.addOrReplaceChild("middle",CubeListBuilder.create().texOffs(0,0).addBox(-6,-1,-6,12,2,12),PartPose.offset(0,16,0));
        root.addOrReplaceChild("nozzle",CubeListBuilder.create().texOffs(0,36).addBox(-2,-2,0,4,4,2),PartPose.offset(0,16,6));
        wood=LayerDefinition.create(mesh,128,64).bakeRoot();
        var sack=new MeshDefinition();
        sack.getRoot().addOrReplaceChild("bag",CubeListBuilder.create().texOffs(48,0).addBox(-10,-12.03333f,-10,20,24,20),PartPose.offset(0,.5f,0));
        // Old addBox captured 128x64 before the later setTextureSize call.
        bag=LayerDefinition.create(sack,128,64).bakeRoot();
    }
    @Override public void render(BellowsBlockEntity bellows,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        draw(bellows.facing(),bellows.inflation(),pose,buffers,light,overlay);
    }
    private void draw(Direction direction,float inflation,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var out=buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)); float separation=.125f+inflation*.875f;
        pose.pushPose(); pose.translate(.5,-.5,.5);
        switch (direction) {
            case DOWN -> { pose.translate(0,1,-1); pose.mulPose(Axis.XP.rotationDegrees(90)); }
            case UP -> { pose.translate(0,1,1); pose.mulPose(Axis.XP.rotationDegrees(270)); }
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(270));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            default -> {}
        }
        pose.pushPose(); pose.translate(0,1,0); pose.scale(.5f,(inflation+.1f)/2,.5f); bag.render(pose,out,light,overlay); pose.popPose();
        pose.pushPose(); pose.translate(0,.5-separation/2,0); wood.getChild("upper").render(pose,out,light,overlay); pose.popPose();
        pose.pushPose(); pose.translate(0,separation/2-.5,0); wood.getChild("lower").render(pose,out,light,overlay); pose.popPose();
        wood.getChild("middle").render(pose,out,light,overlay); wood.getChild("nozzle").render(pose,out,light,overlay); pose.popPose();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer==null) {
            var mc=Minecraft.getInstance(); var renderer=new BellowsRenderer();
            itemRenderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()) {
                @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
                    float ticks=mc.player==null ? 0 : mc.player.tickCount;
                    renderer.draw(Direction.NORTH,(float)Math.sin(ticks/8)*.3f+.7f,pose,buffers,light,overlay);
                }
            };
        }
        return itemRenderer;
    }
}
