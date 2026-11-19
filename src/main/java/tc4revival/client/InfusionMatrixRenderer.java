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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.InfusionMatrixBlockEntity;

public  class InfusionMatrixRenderer implements BlockEntityRenderer<InfusionMatrixBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/infuser.png");
    private final ModelPart cube=cube(0),runes=cube(32);
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public InfusionMatrixRenderer(BlockEntityRendererProvider.Context context) {}
    protected ResourceLocation texture(){return TEXTURE;}
    private static ModelPart cube(int row) {
        var mesh=new MeshDefinition();
        // The old model set mirror after addBox, so its already-created cube was not mirrored.
        mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(0,row).addBox(-8,-8,-8,16,16,16),PartPose.ZERO);
        return LayerDefinition.create(mesh,64,64).bakeRoot();
    }
    @Override public void render(InfusionMatrixBlockEntity matrix,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var viewer=Minecraft.getInstance().getCameraEntity(); float ticks=(viewer==null ? 0 : viewer.tickCount)+partial;
        draw(matrix.active(),matrix.startUp,matrix.instability(),matrix.craftCount,ticks,pose,buffers,light,overlay);
        if (matrix.crafting()) halo(matrix.craftCount,pose,buffers);
    }
    private void draw(boolean active,float start,int instability,int count,float ticks,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        float wobble=Math.min(6,1+instability*.66f*Math.min(count,50)/50f);
        pose.pushPose(); pose.translate(.5,.5,.5);
        pose.mulPose(Axis.YP.rotationDegrees(ticks%360*start)); pose.mulPose(Axis.XP.rotationDegrees(35*start)); pose.mulPose(Axis.ZP.rotationDegrees(45*start));
        for (int layer=0;layer<(active ? 2 : 1);layer++) {
            var output=TCRenderTypes.buffer(buffers,layer==0 ? RenderType.entitySolid(texture()) : TCRenderTypes.infusionRunes(texture()));
            for (int x=0;x<2;x++) for (int y=0;y<2;y++) for (int z=0;z<2;z++) {
                pose.pushPose();
                pose.translate(offset(x,15,ticks,start,wobble,active),offset(y,14,ticks,start,wobble,active),offset(z,13,ticks,start,wobble,active));
                if (x==1) pose.mulPose(Axis.XP.rotationDegrees(90));
                if (y==1) pose.mulPose(Axis.YP.rotationDegrees(90));
                if (z==1) pose.mulPose(Axis.ZP.rotationDegrees(90));
                pose.scale(.45f,.45f,.45f);
                if (layer==0) cube.render(pose,output,light,overlay);
                else runes.render(pose,output,0xf000f0,overlay,.8f,.1f,1,((float)Math.sin((ticks+x*2+y*3+z*4)/4)*.1f+.2f)*start);
                pose.popPose();
            }
        }
        pose.popPose();
    }
    private static double offset(int side,float period,float ticks,float start,float wobble,boolean active) {
        return (side==0 ? -.25 : .25)+(active ? Math.sin((ticks+side*10)/(period-wobble/2))*.01*start*wobble : 0);
    }
    private static void halo(int count,PoseStack pose,MultiBufferSource buffers) {
        var random=new java.util.Random(245); var out=TCRenderTypes.buffer(buffers,TCRenderTypes.PRIMAL_RAYS);
        pose.pushPose(); pose.translate(.5,.5,.5);
        for (int ray=0;ray<(Minecraft.useFancyGraphics() ? 20 : 10);ray++) {
            for (int rotation=0;rotation<6;rotation++) {
                var axis=switch (rotation%3) { case 0->Axis.XP; case 1->Axis.YP; default->Axis.ZP; };
                pose.mulPose(axis.rotationDegrees(random.nextFloat()*360+(rotation==5 ? count/500f*360 : 0)));
            }
            float length=(5+random.nextFloat()*20)*Math.min(count,50)/1000f,width=(1+random.nextFloat()*2)*Math.min(count,50)/1000f;
            float[][] tips={{-.866f*width,length,-.5f*width},{.866f*width,length,-.5f*width},{0,length,width}};
            for (int face=0;face<3;face++) {
                out.vertex(pose.last().pose(),0,0,0).color(255,255,255,255).endVertex();
                for (int index:new int[]{face,(face+1)%3}) {
                    var tip=tips[index]; out.vertex(pose.last().pose(),tip[0],tip[1],tip[2]).color(204,0,255,0).endVertex();
                }
            }
        }
        pose.popPose();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer==null) itemRenderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            private final InfusionMatrixRenderer renderer=new InfusionMatrixRenderer(null);
            @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
                renderer.draw(false,0,0,0,0,pose,buffers,light,overlay);
            }
        };
        return itemRenderer;
    }
}
