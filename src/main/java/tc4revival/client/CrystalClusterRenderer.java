package tc4revival.client;

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
import tc4revival.content.CrystalClusterBlockEntity;
import tc4revival.content.CrystalClusterItem;
import tc4revival.content.ShardType;

public  class CrystalClusterRenderer implements BlockEntityRenderer<CrystalClusterBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/models/crystal.png");
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    private final ModelPart model;
    public CrystalClusterRenderer(BlockEntityRendererProvider.Context context) {
        var mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("crystal",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-16,-16,0,16,16,16),PartPose.offsetAndRotation(0,32,0,.7071f,0,.7071f));
        model=LayerDefinition.create(mesh,64,32).bakeRoot();
    }
    @Override public void render(CrystalClusterBlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var block=(CrystalClusterBlock)entity.getBlockState().getBlock(); var type=block.type(); var pos=entity.getBlockPos();
        var random=new Random(type.metadata()+pos.getX()+pos.getY()*pos.getZ());
        var face=entity.getBlockState().getValue(CrystalClusterBlock.FACING);
        float tick=Minecraft.getInstance().player==null ? 0 : Minecraft.getInstance().player.tickCount;
        draw(pose,buffers,face,(random.nextFloat()-random.nextFloat())*5,(random.nextFloat()-random.nextFloat())*5,
                random,type==ShardType.BALANCED ? ShardType.ORDER : type,1.1f,tick,overlay);
        for (int index=1;index<6;index++) draw(pose,buffers,face,random.nextInt(36)+72*index,15+random.nextInt(15),
                random,type==ShardType.BALANCED ? ShardType.values()[index==5 ? 5 : index-1] : type,.8f,tick,overlay);
    }
    private void draw(PoseStack pose,MultiBufferSource buffers,Direction face,float yaw,float tilt,Random random,ShardType type,float size,float tick,int overlay) {
        float shade=(float)Math.sin((tick+random.nextInt(10))/(5+random.nextFloat()))*.075f+.925f;
        pose.pushPose();
        switch (face) {
            case DOWN -> { pose.translate(.5,1.3,.5); pose.mulPose(Axis.XP.rotationDegrees(180)); }
            case UP -> pose.translate(.5,-.3,.5);
            case NORTH -> { pose.translate(.5,.5,1.3); pose.mulPose(Axis.XP.rotationDegrees(-90)); }
            case SOUTH -> { pose.translate(.5,.5,-.3); pose.mulPose(Axis.XP.rotationDegrees(90)); }
            case WEST -> { pose.translate(1.3,.5,.5); pose.mulPose(Axis.ZP.rotationDegrees(90)); }
            case EAST -> { pose.translate(-.3,.5,.5); pose.mulPose(Axis.ZP.rotationDegrees(-90)); }
        }
        pose.mulPose(Axis.YP.rotationDegrees(yaw)); pose.mulPose(Axis.XP.rotationDegrees(tilt));
        pose.scale((.15f+random.nextFloat()*.075f)*size,(.5f+random.nextFloat()*.1f)*size,(.15f+random.nextFloat()*.05f)*size);
        int color=type.tint();
        model.render(pose,buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),(int)(210*shade),overlay,
                Math.min(1,((color>>16)&255)/220f),Math.min(1,((color>>8)&255)/220f),Math.min(1,(color&255)/220f),1);
        pose.popPose();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        if (itemRenderer==null) itemRenderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()) {
            final CrystalClusterRenderer renderer=new CrystalClusterRenderer(null);
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
                if (!(stack.getItem() instanceof CrystalClusterItem item)) return;
                pose.pushPose(); pose.translate(.5,.5,.5); pose.mulPose(Axis.YP.rotationDegrees(90)); pose.translate(-.5,-.5,-.5);
                renderer.render(new CrystalClusterBlockEntity(BlockPos.ZERO,item.getBlock().defaultBlockState()),0,pose,buffers,light,overlay); pose.popPose();
            }
        };
        return itemRenderer;
    }
}
