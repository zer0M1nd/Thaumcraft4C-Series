package conciliummodern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Random;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.CrystalClusterBlock;
import tc4revival.aspect.TCAspects;
import tc4revival.content.CrystalClusterItem;
import tc4revival.content.ShardType;

public  class DestabilizedCrystalRenderer implements BlockEntityRenderer<DestabilizedCrystalBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/crystal.png");
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    private final ModelPart model;
    public DestabilizedCrystalRenderer(BlockEntityRendererProvider.Context context) {
        var mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("crystal",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-16,-16,0,16,16,16),PartPose.offsetAndRotation(0,32,0,.7071f,0,.7071f));
        model=LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    @Override public void render(DestabilizedCrystalBlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var pos=entity.getBlockPos(); int color=TCAspects.catalog().find(entity.aspectKey()).map(a->a.color()).orElse(0xffffff);
        float size=Math.max(.6f,Math.min(1.6f,.6f+entity.amount()/512f));
        var random=new Random(pos.getX()+(long)pos.getY()*pos.getZ());
        var face=entity.getBlockState().getValue(CrystalClusterBlock.FACING);
        float tick=Minecraft.getInstance().player==null ? 0 : Minecraft.getInstance().player.tickCount;
        pose.pushPose();
        if(entity.draining() || (face==Direction.UP && entity.getLevel()!=null && entity.getLevel().getBlockEntity(pos.below()) instanceof VisCondenserBlockEntity))pose.translate(0,Math.sin((tick+partial)/14f)*.08+.03,0);
        draw(pose,buffers,face,(random.nextFloat()-random.nextFloat())*5,(random.nextFloat()-random.nextFloat())*5,
                random,color,size,tick,overlay);
        for (int index=0;index<6;index++) draw(pose,buffers,face,random.nextInt(36)+72*index,15+random.nextInt(15),
                random,color,size-.3f,tick,overlay);
        pose.popPose();
    }
    public void drawSingle(PoseStack pose,MultiBufferSource buffers,int light,int overlay,int color){
        model.render(pose,buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),light,overlay,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,.8f);
    }
    private void draw(PoseStack pose,MultiBufferSource buffers,Direction face,float yaw,float tilt,Random random,int color,float size,float tick,int overlay) {
        float shade=(float)Math.sin((tick+random.nextInt(10))/(5+random.nextFloat()))*.075f+.925f;
        pose.pushPose();
        switch (face) {
            case DOWN -> { pose.translate(.5,1.15,.5); pose.mulPose(Axis.XP.rotationDegrees(180)); }
            case UP -> pose.translate(.5,-.1,.5);
            case NORTH -> { pose.translate(.5,.5,1.1); pose.mulPose(Axis.XP.rotationDegrees(-90)); }
            case SOUTH -> { pose.translate(.5,.5,-.1); pose.mulPose(Axis.XP.rotationDegrees(90)); }
            case WEST -> { pose.translate(1.1,.5,.5); pose.mulPose(Axis.ZP.rotationDegrees(90)); }
            case EAST -> { pose.translate(-.1,.5,.5); pose.mulPose(Axis.ZP.rotationDegrees(-90)); }
        }
        pose.mulPose(Axis.YP.rotationDegrees(yaw)); pose.mulPose(Axis.XP.rotationDegrees(tilt));
        pose.scale((.15f+random.nextFloat()*.075f)*size,(.5f+random.nextFloat()*.1f)*size,(.15f+random.nextFloat()*.05f)*size);
        model.render(pose,buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),(int)(210*shade),overlay,
                Math.min(1,((color>>16)&255)/220f),Math.min(1,((color>>8)&255)/220f),Math.min(1,(color&255)/220f),1);
        pose.popPose();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer==null) itemRenderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            final DestabilizedCrystalRenderer renderer=new DestabilizedCrystalRenderer(null);
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
                if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem item) || item.getBlock()!=ConciliumBlocks.CRYSTAL.get()) return;
                pose.pushPose(); pose.translate(.5,.5,.5); pose.scale(3,3,3); pose.mulPose(Axis.YP.rotationDegrees(90)); pose.translate(-.5,-.2,-.5);
                var crystal=new DestabilizedCrystalBlockEntity(BlockPos.ZERO,item.getBlock().defaultBlockState());
                var saved=net.minecraft.world.item.BlockItem.getBlockEntityData(stack);
                if(saved!=null)crystal.load(saved);
                renderer.render(crystal,0,pose,buffers,light,overlay); pose.popPose();
            }
        };
        return itemRenderer;
    }
}
