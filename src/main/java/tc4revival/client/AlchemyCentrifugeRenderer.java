package tc4revival.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.AlchemyCentrifugeBlockEntity;
public  class AlchemyCentrifugeRenderer implements BlockEntityRenderer<AlchemyCentrifugeBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/centrifuge.png");
    private final ModelPart model;private static BlockEntityWithoutLevelRenderer item;
    public AlchemyCentrifugeRenderer(BlockEntityRendererProvider.Context context){this();}
    private AlchemyCentrifugeRenderer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("top",CubeListBuilder.create().texOffs(20,16).mirror().addBox(-4,-8,-4,8,4,8),PartPose.ZERO);
        root.addOrReplaceChild("bottom",CubeListBuilder.create().texOffs(20,16).mirror().addBox(-4,4,-4,8,4,8),PartPose.ZERO);
        var rotor=root.addOrReplaceChild("rotor",CubeListBuilder.create(),PartPose.ZERO);
        rotor.addOrReplaceChild("crossbar",CubeListBuilder.create().texOffs(16,0).mirror().addBox(-4,-1,-1,8,2,2),PartPose.ZERO);
        rotor.addOrReplaceChild("right",CubeListBuilder.create().texOffs(0,16).mirror().addBox(4,-3,-2,4,6,4),PartPose.ZERO);
        rotor.addOrReplaceChild("left",CubeListBuilder.create().texOffs(0,16).mirror().addBox(-8,-3,-2,4,6,4),PartPose.ZERO);
        rotor.addOrReplaceChild("core",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-1.5f,-4,-1.5f,3,8,3),PartPose.ZERO);
        model=LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    private void draw(float rotation,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));pose.pushPose();pose.translate(.5,.5,.5);
        model.getChild("top").render(pose,out,light,overlay);model.getChild("bottom").render(pose,out,light,overlay);pose.mulPose(Axis.YP.rotationDegrees(rotation));model.getChild("rotor").render(pose,out,light,overlay);pose.popPose();
    }
    @Override public void render(AlchemyCentrifugeBlockEntity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(tile.rotation,pose,buffers,light,overlay);}
    public static BlockEntityWithoutLevelRenderer itemRenderer(){if(item==null){var renderer=new AlchemyCentrifugeRenderer();item=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
        @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){renderer.draw(0,pose,buffers,light,overlay);}
    };}return item;}
}
