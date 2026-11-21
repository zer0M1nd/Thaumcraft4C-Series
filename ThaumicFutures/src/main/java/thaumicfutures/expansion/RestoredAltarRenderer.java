package thaumicfutures.expansion;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import tc4revival.client.GroupedObjMesh;

public final class RestoredAltarRenderer {
    private static GroupedObjMesh pillar;
    private static BlockEntityWithoutLevelRenderer item;
    public static void reload(){pillar=null;}
    private static ResourceLocation texture(String family,String kind){
        return ResourceLocation.parse("thaumicfutures:textures/block/tc6/"+kind+"_"+family+".png");
    }
    public static class Matrix extends tc4revival.client.InfusionMatrixRenderer {
        private ResourceLocation selected=RestoredAltarRenderer.texture("ancient","infuser");
        public Matrix(BlockEntityRendererProvider.Context context){super(context);}
        @Override protected ResourceLocation texture(){return selected;}
        @Override public void render(tc4revival.content.InfusionMatrixBlockEntity matrix,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            var family=RestoredInfusion.family(matrix.getBlockState().getBlock());
            selected=RestoredAltarRenderer.texture(family==null?"ancient":family.name,"infuser");
            super.render(matrix,partial,pose,buffers,light,overlay);
        }
    }
    public static class Pillar implements BlockEntityRenderer<RestoredInfusion.Pillar> {
        public Pillar(BlockEntityRendererProvider.Context context){}
        @Override public void render(RestoredInfusion.Pillar tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            if(pillar==null)try(var reader=new InputStreamReader(Minecraft.getInstance().getResourceManager()
                    .getResourceOrThrow(ResourceLocation.parse("thaumicfutures:models/block/restored/pillar.obj")).open(),StandardCharsets.UTF_8)){
                pillar=GroupedObjMesh.read(reader);
            }catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
            var family=RestoredInfusion.family(tile.getBlockState().getBlock());
            pose.pushPose();pose.translate(.5,0,.5);
            pose.mulPose(Axis.YP.rotationDegrees(switch(tile.orientation()){case 3->90;case 4->270;case 5->180;default->0;}));
            pose.translate(-.5,0,-.5);
            var frame=pose.last();var out=buffers.getBuffer(RenderType.entityCutoutNoCull(texture(family.name,"pillar")));
            for(var group:pillar.groups().values())for(var vertex:group)
                // TC6's Forge OBJ asset uses image-space V; the TC4 grouped mesh
                // reader normally flips conventional OBJ coordinates.
                out.vertex(frame.pose(),vertex.x(),vertex.y(),vertex.z()).color(255,255,255,255).uv(vertex.u(),1-vertex.v())
                        .overlayCoords(overlay).uv2(light).normal(frame.normal(),vertex.nx(),vertex.ny(),vertex.nz()).endVertex();
            pose.popPose();
        }
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer(){
        if(item==null)item=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
            private final Matrix renderer=new Matrix(null);
            @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                if(stack.getItem() instanceof BlockItem block)renderer.render(new RestoredInfusion.Matrix(BlockPos.ZERO,block.getBlock().defaultBlockState()),0,pose,buffers,light,overlay);
            }
        };
        return item;
    }
    private RestoredAltarRenderer(){}
}
