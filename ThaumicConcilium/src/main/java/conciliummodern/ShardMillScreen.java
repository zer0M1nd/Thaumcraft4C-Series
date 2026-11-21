package conciliummodern;

import java.util.List;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/** Hold the original J binding, hover a carried shard, release to select it. */
final class ShardMillScreen extends Screen {
    private final InteractionHand hand;
    private final List<PowderType> types;
    private PowderType hovered;
    private int ticks;
    ShardMillScreen(InteractionHand hand,List<PowderType> types){super(Component.translatable("key.thaumicconcilium.choose_shard"));this.hand=hand;this.types=List.copyOf(types);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){ticks++;if(minecraft.player==null || !(minecraft.player.getItemInHand(hand).getItem() instanceof ShardMill))onClose();}
    private void choose(){if(hovered!=null)ShardSelection.send(hand,hovered);onClose();}
    @Override public boolean keyReleased(int key,int scan,int modifiers){if(ShardMillClient.KEY.matches(key,scan)){choose();return true;}return super.keyReleased(key,scan,modifiers);}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0){choose();return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean mouseReleased(double x,double y,int button){if(ShardMillClient.KEY.matchesMouse(button)){choose();return true;}return super.mouseReleased(x,y,button);}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
        if(minecraft.player==null)return;var stack=minecraft.player.getItemInHand(hand);if(!(stack.getItem() instanceof ShardMill mill))return;
        var pose=graphics.pose();pose.pushPose();pose.translate(width/2d,height/2d,0);
        float radius=16+types.size()*2.5f,growth=Math.min(1,(ticks+partial)/5);RenderSystem.enableBlend();
        for(int i=0;i<2;i++){pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees((i==0?1:-1)*(minecraft.player.tickCount+partial)/2));int size=(int)(radius*(i==0?2.75:2.55)*growth);
            graphics.setColor(.5f,.5f,.5f,.5f);graphics.blit(ResourceLocation.parse("thaumcraft:textures/misc/radial"+(i==0?"":"2")+".png"),-size/2,-size/2,size,size,0,0,256,256,256,256);graphics.setColor(1,1,1,1);pose.popPose();}
        graphics.renderItem(mill.selected(stack).shard(),-8,-8);hovered=null;
        for(int i=0;i<types.size();i++){double angle=-Math.PI/2+i*2*Math.PI/types.size();int x=(int)(Math.cos(angle)*radius*growth),y=(int)(Math.sin(angle)*radius*growth);
            boolean over=Math.abs(mouseX-width/2d-x)<=10 && Math.abs(mouseY-height/2d-y)<=10;
            pose.pushPose();pose.translate(x,y,100);if(over){pose.scale(1.3f,1.3f,1);hovered=types.get(i);}graphics.renderItem(types.get(i).shard(),-8,-8);pose.popPose();
        }
        pose.popPose();RenderSystem.disableBlend();if(hovered!=null)graphics.renderTooltip(font,hovered.shard(),mouseX,mouseY);
    }
}
