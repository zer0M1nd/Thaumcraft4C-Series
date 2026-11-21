package tc4revival.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;

/** Vanilla GUI item transforms with alpha blending also enabled for opaque model layers. */
public class ResearchMirrorItems {
    private static final MirrorBuffers BUFFERS=new MirrorBuffers();
    private static class MirrorBuffers extends MultiBufferSource.BufferSource {
        MirrorBuffers(){super(new BufferBuilder(256),java.util.Map.of());}
        @Override public VertexConsumer getBuffer(RenderType type){return super.getBuffer(TCRenderTypes.researchMirrorItem(type));}
    }
    public static void draw(GuiGraphics graphics,ItemStack stack,int x,int y) {
        var color=RenderSystem.getShaderColor().clone();
        if(color[3]>=1){graphics.renderItem(stack,x,y);return;}
        graphics.flush();
        var mirror=new GuiGraphics(Minecraft.getInstance(),BUFFERS);
        mirror.pose().last().pose().set(graphics.pose().last().pose());
        mirror.pose().last().normal().set(graphics.pose().last().normal());
        try {mirror.setColor(color[0],color[1],color[2],color[3]);mirror.renderItem(stack,x,y);}
        finally {mirror.flush();graphics.setColor(color[0],color[1],color[2],color[3]);}
    }
}
