package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import tc4revival.content.CrucibleBlockEntity;

public  class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity> {
    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(CrucibleBlockEntity crucible,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        thaumcraft.api.client.DeviceRendering.render(new thaumcraft.api.client.DeviceRendering.Context<>(crucible,partial,pose,buffers,light,overlay),()->renderDefault(crucible,partial,pose,buffers,light,overlay));
    }
    private void renderDefault(CrucibleBlockEntity crucible,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var contents=crucible.contents(); if (contents.water()<=0) return;
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("minecraft","block/water_still"));
        float tint=contents.aspects().total()/100f; if (tint>0) tint=.5f+tint/2;
        float red=Math.max(0,1-tint/3),green=Math.max(0,1-tint),blue=Math.max(0,1-tint/2);
        // Modern water sprites are grayscale; restore the water color before applying TC4's aspect tint.
        int waterColor=0x3f76e4; // Legacy crucible samples the blue sprite without a biome multiplier.
        red*=((waterColor>>16)&255)/255f; green*=((waterColor>>8)&255)/255f; blue*=(waterColor&255)/255f;
        var vertices=buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        for (float[] point : new float[][]{{0,0,0,0},{0,1,0,16},{1,1,16,16},{1,0,16,0}})
            vertices.vertex(pose.last().pose(),point[0],contents.fluidHeight(),point[1]).color(red,green,blue,1)
                    .uv(sprite.getU(point[2]),sprite.getV(point[3])).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,1,0).endVertex();
    }
}
