package thmodern.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import com.mojang.math.Axis;
import thaumcraft.api.client.ResearchPageRenderers;

/** A page-local preview; it never constructs a world or changes blocks. */
public final class VatStructurePage implements ResearchPageRenderers.Renderer {
    private float yaw=35;
    private int layer=4;
    private boolean dragging;
    @Override public void render(ResearchPageRenderers.Context context,GuiGraphics graphics,int mx,int my,float partial){
        var bounds=context.bounds();var minecraft=Minecraft.getInstance();
        graphics.drawWordWrap(minecraft.font,Component.translatable("thaumichorizons.vat.preview"),bounds.x(),bounds.y(),bounds.width(),0x493522);
        graphics.flush();var pose=graphics.pose();pose.pushPose();
        pose.translate(bounds.x()+bounds.width()/2f,bounds.y()+bounds.height()/2f+8,100);
        float scale=Math.min(bounds.width()/6f,(bounds.height()-55)/6f);pose.scale(scale,-scale,scale);
        pose.mulPose(Axis.XP.rotationDegrees(25));pose.mulPose(Axis.YP.rotationDegrees(yaw));
        for(int part=0;part<36;part++){
            int y=part/9;if(layer<4&&y!=layer)continue;
            var offset=thmodern.vat.VatStructure.offset(part);var state=thmodern.vat.VatStructure.original(part);
            // Water has no baked block model. A translucent blue cube marks each source.
            if(state.is(Blocks.WATER))state=Blocks.BLUE_STAINED_GLASS.defaultBlockState();
            pose.pushPose();pose.translate(offset.getX()-.5,offset.getY()+1,offset.getZ()-.5);
            minecraft.getBlockRenderer().renderSingleBlock(state,pose,graphics.bufferSource(),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);pose.popPose();
        }
        pose.popPose();graphics.flush();
        graphics.drawCenteredString(minecraft.font,Component.translatable(layer==4?"thaumichorizons.vat.all_layers":"thaumichorizons.vat.layer",layer+1),bounds.x()+bounds.width()/2,bounds.y()+bounds.height()-23,0x493522);
        graphics.drawCenteredString(minecraft.font,Component.literal("<                 >"),bounds.x()+bounds.width()/2,bounds.y()+bounds.height()-11,0x493522);
    }
    @Override public boolean mouseClicked(ResearchPageRenderers.Context context,double x,double y,int button){if(button!=0||!context.bounds().contains(x,y))return false;var b=context.bounds();if(y>=b.y()+b.height()-25)layer=Math.floorMod(layer+(x<b.x()+b.width()/2d?-1:1),5);else dragging=true;return true;}
    @Override public boolean mouseDragged(ResearchPageRenderers.Context context,double x,double y,int button,double dx,double dy){if(!dragging||button!=0)return false;yaw=(float)((yaw+dx*2)%360);return true;}
    @Override public boolean mouseReleased(ResearchPageRenderers.Context context,double x,double y,int button){boolean handled=dragging;dragging=false;return handled;}
    @Override public boolean mouseScrolled(ResearchPageRenderers.Context context,double x,double y,double delta){if(!context.bounds().contains(x,y)||delta==0)return false;layer=Math.floorMod(layer+(delta>0?1:-1),5);return true;}
    @Override public void close(){dragging=false;}
}
