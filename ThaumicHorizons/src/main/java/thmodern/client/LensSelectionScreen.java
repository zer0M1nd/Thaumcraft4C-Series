package thmodern.client;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import thmodern.Horizons;
import thmodern.lens.*;
public final class LensSelectionScreen extends Screen {
    private List<String> choices=List.of();
    private boolean held;private long openedAt=net.minecraft.Util.getMillis();
    public LensSelectionScreen(){super(Component.translatable("key.thaumichorizons.lenses"));}
    @Override protected void init(){if(minecraft.player==null)return;held=LensClient.SELECT.isDown();var keys=new ArrayList<>(LensEquipment.available(minecraft.player));if(!LensEquipment.active(minecraft.player).isEmpty())keys.add("");choices=List.copyOf(keys);}
    @Override public void tick(){if(minecraft.player==null||LensEquipment.goggles(minecraft.player).isEmpty()){onClose();return;}if(held&&!LensClient.SELECT.isDown()){double x=minecraft.mouseHandler.xpos()*width/minecraft.getWindow().getScreenWidth(),y=minecraft.mouseHandler.ypos()*height/minecraft.getWindow().getScreenHeight();int i=hovered(x,y);if(i>=0)LensNetwork.CHANNEL.sendToServer(new LensNetwork.Change(choices.get(i)));onClose();}}
    private double radius(){return 16+choices.size()*2.5;}
    private int x(int i){return width/2+(int)(Math.sin(i*Math.PI*2/choices.size())*radius());}
    private int y(int i){return height/2-(int)(Math.cos(i*Math.PI*2/choices.size())*radius());}
    private int hovered(double x,double y){for(int i=0;i<choices.size();i++)if(Math.abs(x-x(i))<16&&Math.abs(y-y(i))<16)return i;return -1;}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        int hovered=hovered(mouseX,mouseY);float opening=Math.min(1,(net.minecraft.Util.getMillis()-openedAt)/250f);float angle=(minecraft.player.tickCount+partial)*.5f;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        for(int layer=0;layer<2;layer++){float size=(float)(radius()*(layer==0?2.75:2.55))*opening;
            g.pose().pushPose();g.pose().translate(width/2f,height/2f,0);g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(layer==0?angle:-angle));g.setColor(.5f,.5f,.5f,.5f);
            tc4revival.client.LegacyGui.blit(g,net.minecraft.resources.ResourceLocation.parse("thaumcraft:textures/misc/radial"+(layer==0?"":"2")+".png"),-(int)size/2,-(int)size/2,0,0,(int)size,(int)size,(int)size,(int)size);g.setColor(1,1,1,1);g.pose().popPose();}
        String active=LensEquipment.active(minecraft.player);var current=Horizons.LENSES.get(active);if(current!=null)g.renderItem(new ItemStack(current.get()),width/2-8,height/2-8);
        for(int i=0;i<choices.size();i++){String key=choices.get(i);g.pose().pushPose();g.pose().translate(width/2f+(x(i)-width/2f)*opening,height/2f+(y(i)-height/2f)*opening,0);float scale=i==hovered?1.3f:1;g.pose().scale(scale,scale,1);
            if(key.isEmpty())g.drawCenteredString(font,Component.literal("×"),0,-4,0xffffff);else {var item=Horizons.LENSES.get(key);if(item!=null)g.renderItem(new ItemStack(item.get()),-8,-8);}g.pose().popPose();}
        if(hovered>=0){String key=choices.get(hovered);g.renderTooltip(font,Component.translatable(key.isEmpty()?"thaumichorizons.lens.remove":"item.thaumichorizons.lens_"+key),mouseX,mouseY);}
        if(choices.isEmpty())g.drawCenteredString(font,Component.translatable("thaumichorizons.lens.none"),width/2,height/2+15,0xcccccc);
        super.render(g,mouseX,mouseY,partial);
    }
    @Override public boolean mouseClicked(double x,double y,int button){int index=hovered(x,y);if(button==0&&index>=0){LensNetwork.CHANNEL.sendToServer(new LensNetwork.Change(choices.get(index)));onClose();return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean isPauseScreen(){return false;}
}
