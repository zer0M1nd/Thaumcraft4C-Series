package thmodern.client;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;
import thmodern.lens.*;
@Mod.EventBusSubscriber(modid=Horizons.ID,value=Dist.CLIENT)
public final class ScholarLensOverlay {
    private static LensNetwork.View view;private static long received;
    public static void receive(LensNetwork.View data){var mc=Minecraft.getInstance();view=data;received=mc.level==null?0:mc.level.getGameTime();}
    @SubscribeEvent public static void render(net.minecraftforge.client.event.RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.options.hideGui||view==null||!LensEquipment.active(mc.player).equals("order_entropy")||mc.level.getGameTime()-received>15||mc.level.getGameTime()<received)return;
        var g=event.getGuiGraphics();int x=g.guiWidth()/2,y=g.guiHeight()/2;g.drawCenteredString(mc.font,view.name(),x,y-16,0xffffff);int count=0;
        for(var entry:view.aspects().entrySet()){
            var aspect=tc4revival.aspect.TCAspects.catalog().find(entry.getKey()).orElse(null);if(aspect==null)continue;int row=count/5,col=count%5,rowSize=Math.min(5,view.aspects().size()-row*5);int ax=x-rowSize*9+col*18,ay=y+16+row*18;int color=aspect.color();g.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,.5f);tc4revival.client.LegacyGui.blit(g,aspect.texture(),ax,ay,0,0,16,16,16,16);g.setColor(1,1,1,1);
            g.pose().pushPose();g.pose().scale(.5f,.5f,1);g.drawString(mc.font,Integer.toString(entry.getValue()),ax*2+24,ay*2+32-mc.font.lineHeight,0xffffff,false);g.pose().popPose();count++;
        }
        if(view.progress()>0){g.fill(x-30,y+5,x+30,y+8,0x99000000);g.fill(x-30,y+5,x-30+Math.min(60,view.progress()*60/LensScanProgress.DURATION),y+8,0xffaaaaee);}
    }
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){view=null;}
}
