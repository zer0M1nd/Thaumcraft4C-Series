package thaumicfutures.devices.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import tc4revival.client.TCRenderTypes;
import thaumicfutures.devices.NodeReading;

/** Real node texture layers: each aspect, node type, brightness and the animated TC4 atlas. */
final class NodeGlyph {
    static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/misc/nodes.png");
    private record Layer(int row,int color,float size,float alpha,float angle,boolean alphaBlend) {}
    private static java.util.List<Layer> layers(NodeReading node,float time) {
        var layers=new java.util.ArrayList<Layer>();
        var aspects=node.capacity().aspects();int count=Math.max(1,aspects.size()),index=0;
        float opacity=node.modifier()==null?.85f:switch(node.modifier()){case BRIGHT->1;case PALE->.57f;case FADING->.35f+.18f*Mth.sin(time/3);};
        for(var aspect:aspects) {
            int amount=node.charge().amount(aspect);
            float size=.6f+(float)Math.log1p(Math.max(0,amount))*.18f;
            size*=.95f+Mth.sin(time/(14f-index%14))*.08f;
            layers.add(new Layer(0,aspect.color(),size,opacity/Math.max(1,count/2f),time/(100+10f*index++),aspect.blendFactor()==771));
        }
        int row=switch(node.type()){case NORMAL->1;case DARK->2;case HUNGRY->3;case PURE->4;case TAINTED->5;case UNSTABLE->6;};
        layers.add(new Layer(row,0xffffff,.65f,opacity,node.type()==tc4revival.content.AuraNodeType.UNSTABLE?0:time/150,node.type()==tc4revival.content.AuraNodeType.DARK || node.type()==tc4revival.content.AuraNodeType.TAINTED));
        return layers;
    }
    static void world(NodeReading node,float time,PoseStack pose,MultiBufferSource buffers,float radius) {
        int frame=Math.floorMod((int)(time*1.25f)+node.pos().getX(),32);
        pose.pushPose();pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        for(var layer:layers(node,time)) {
            pose.pushPose();pose.mulPose(Axis.ZP.rotation(layer.angle));var out=buffers.getBuffer(TCRenderTypes.node(TEXTURE,layer.alphaBlend));var p=pose.last().pose();
            float s=radius*layer.size,u=frame/32f,v=layer.row/32f,unit=1/32f,r=(layer.color>>16&255)/255f,g=(layer.color>>8&255)/255f,b=(layer.color&255)/255f;
            out.vertex(p,-s,-s,0).color(r,g,b,layer.alpha).uv(u+unit,v+unit).uv2(0xf000f0).endVertex();
            out.vertex(p,-s,s,0).color(r,g,b,layer.alpha).uv(u+unit,v).uv2(0xf000f0).endVertex();
            out.vertex(p,s,s,0).color(r,g,b,layer.alpha).uv(u,v).uv2(0xf000f0).endVertex();
            out.vertex(p,s,-s,0).color(r,g,b,layer.alpha).uv(u,v+unit).uv2(0xf000f0).endVertex();pose.popPose();
        }
        pose.popPose();
    }
    static void screen(NodeReading node,float time,GuiGraphics graphics,float x,float y,float size) {
        int frame=Math.floorMod((int)(time*1.25f)+node.pos().getX(),32);
        graphics.flush();
        graphics.pose().pushPose();graphics.pose().translate(x,y,0);
        for(var layer:layers(node,time)) {
            graphics.pose().pushPose();graphics.pose().mulPose(Axis.ZP.rotation(-layer.angle));
            var out=graphics.bufferSource().getBuffer(layer.alphaBlend?GuiLayers.ALPHA:GuiLayers.ADDITIVE);
            var p=graphics.pose().last().pose();float s=Math.max(2,size*layer.size),u=frame/32f,v=layer.row/32f,unit=1/32f;
            float r=(layer.color>>16&255)/255f,g=(layer.color>>8&255)/255f,b=(layer.color&255)/255f;
            out.vertex(p,-s,s,0).color(r,g,b,layer.alpha).uv(u,v+unit).endVertex();
            out.vertex(p,s,s,0).color(r,g,b,layer.alpha).uv(u+unit,v+unit).endVertex();
            out.vertex(p,s,-s,0).color(r,g,b,layer.alpha).uv(u+unit,v).endVertex();
            out.vertex(p,-s,-s,0).color(r,g,b,layer.alpha).uv(u,v).endVertex();graphics.pose().popPose();
        }
        graphics.flush();graphics.pose().popPose();
    }
    /** The node atlas contains black backgrounds that only disappear with additive blending. */
    private static class GuiLayers extends net.minecraft.client.renderer.RenderType {
        private GuiLayers() { super("unused",com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX,com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,256,false,false,()->{},()->{}); }
        static final net.minecraft.client.renderer.RenderType ALPHA=layer(true),ADDITIVE=layer(false);
        static net.minecraft.client.renderer.RenderType layer(boolean alpha) {
            return create("thaumicfutures_node_gui",com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX,com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,256,false,false,
                    CompositeState.builder().setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionColorTexShader))
                            .setTextureState(new TextureStateShard(TEXTURE,true,false)).setTransparencyState(alpha?TRANSLUCENT_TRANSPARENCY:LIGHTNING_TRANSPARENCY)
                            .setCullState(NO_CULL).setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        }
    }
    private NodeGlyph() {}
}
