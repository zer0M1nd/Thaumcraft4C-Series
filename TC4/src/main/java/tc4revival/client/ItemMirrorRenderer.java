package tc4revival.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.ItemMirrorBlockEntity;

public  class ItemMirrorRenderer implements BlockEntityRenderer<ItemMirrorBlockEntity> {
    private static final ResourceLocation FRAME=ResourceLocation.parse("thaumcraft:textures/block/mirrorframe.png");
    public ItemMirrorRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(ItemMirrorBlockEntity mirror,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        float jitter=mirror.instability()>0 && mirror.getLevel()!=null ? mirror.getLevel().random.nextFloat()*mirror.instability()/10000f : 0;
        EssentiaMirrorRenderer.renderMirror(mirror,mirror.facing(),mirror.linked(),jitter,FRAME,pose,buffers,light,overlay);
    }
}
