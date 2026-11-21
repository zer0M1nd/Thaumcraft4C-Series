package ttmodern;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class WarpGateScreen extends AbstractContainerScreen<WarpGateMenu> {
    private double centerX,centerZ,scale=1;
    private Button lock;
    public WarpGateScreen(WarpGateMenu menu,Inventory inventory,Component title){super(menu,inventory,title);centerX=menu.gate.getBlockPos().getX();centerZ=menu.gate.getBlockPos().getZ();}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(Tinkerer.WARP_GATE_MENU.get(),WarpGateScreen::new));}
    @Override protected void init(){super.init();if(!menu.destinations)lock=addRenderableWidget(new LegacyToggle(leftPos+5,topPos+5,"warp_gate",lockText(),()->!menu.gate.locked(),()->send(0)));}
    private Component lockText(){return Component.translatable(menu.gate.locked()?"ttmodern.gate.locked":"ttmodern.gate.unlocked");}
    private void send(int id){if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private boolean available(int slot){var pearl=menu.gate.items.getStackInSlot(slot);return pearl.is(Tinkerer.SKY_PEARL.get())&&SkyPearlItem.bound(pearl)&&minecraft.level.dimension().location().equals(SkyPearlItem.dimension(pearl));}
    private int px(double x){return width/2+(int)Math.round((x-centerX)*scale);}
    private int pz(double z){return height/2+(int)Math.round((z-centerZ)*scale);}
    private boolean inMap(double x,double y){return x>=0&&x<width&&y>=28&&y<height;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        if(!menu.destinations){tc4revival.client.LegacyGui.blit(g,ResourceLocation.parse(Tinkerer.ID+":textures/gui/warp_gate.png"),leftPos,topPos,0,0,176,166);lock.setMessage(lockText());return;}
        float time=minecraft.player.tickCount+partial;int tint=java.awt.Color.HSBtoRGB((float)(Math.sin(time/150)+.5),.5f,.4f);
        g.setColor((tint>>16&255)/255f,(tint>>8&255)/255f,(tint&255)/255f,1);tc4revival.client.LegacyGui.blit(g,ResourceLocation.parse("minecraft:textures/entity/end_portal.png"),0,0,0,0,width,height,256,256);g.setColor(1,1,1,1);
        int gx=px(menu.gate.getBlockPos().getX()),gy=pz(menu.gate.getBlockPos().getZ()),alpha=(int)(((Math.sin(time/10)+1)/4+.25)*255);
        for(int i=0;i<10;i++)if(available(i)){var pos=SkyPearlItem.position(menu.gate.items.getStackInSlot(i));line(g,gx,gy,px(pos.getX()),pz(pos.getZ()),alpha<<24|0xffffff);}
        pearl(g,new net.minecraft.world.item.ItemStack(Tinkerer.SKY_PEARL.get()),gx,gy);
        g.drawString(font,Component.translatable("ttmisc.destinations"),3,40,0xffffff,true);
        for(int i=0;i<10;i++)if(available(i)){var pearl=menu.gate.items.getStackInSlot(i);var pos=SkyPearlItem.position(pearl);pearl(g,pearl,px(pos.getX()),pz(pos.getZ()));g.drawString(font,(i+1)+": "+pearl.getHoverName().getString(),5,54+i*11,0xffffff,true);}
        g.drawCenteredString(font,Component.translatable("ttmisc.numberKeys"),width/2,5,0xffffff);g.drawCenteredString(font,Component.translatable("ttmisc.spaceToReset"),width/2,16,0xffffff);
    }
    private void pearl(GuiGraphics g,net.minecraft.world.item.ItemStack pearl,int x,int y){g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(.5f,.5f,1);g.renderItem(pearl,-8,-8);g.pose().popPose();}
    private void line(GuiGraphics g,int x,int y,int xx,int yy,int color){
        // Clip before rasterizing: linked destinations may be millions of blocks away.
        double dx=xx-(double)x,dy=yy-(double)y,t0=0,t1=1;double[] p={-dx,dx,-dy,dy},q={x,width-x,y,height-y};
        for(int i=0;i<4;i++){if(p[i]==0){if(q[i]<0)return;}else{double t=q[i]/p[i];if(p[i]<0)t0=Math.max(t0,t);else t1=Math.min(t1,t);}}if(t0>t1)return;
        double sx=x+t0*dx,sy=y+t0*dy,ex=x+t1*dx,ey=y+t1*dy;int steps=(int)Math.ceil(Math.max(Math.abs(ex-sx),Math.abs(ey-sy)));
        for(int i=0;i<=steps;i++){double t=steps==0?0:(double)i/steps;int px=(int)(sx+(ex-sx)*t),py=(int)(sy+(ey-sy)*t);g.fill(px,py,px+2,py+2,color);}
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){if(!menu.destinations)g.drawString(font,Component.translatable("ttmisc.lockedGate"),20,7,0x999999,true);}
    @Override public boolean mouseClicked(double x,double y,int button){if(menu.destinations&&button==0&&hasShiftDown()){for(int i=0;i<10;i++)if(available(i)){var pos=SkyPearlItem.position(menu.gate.items.getStackInSlot(i));if(Math.abs(x-px(pos.getX()))<=5&&Math.abs(y-pz(pos.getZ()))<=5){send(100+i);onClose();return true;}}}return super.mouseClicked(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(menu.destinations&&button==0&&inMap(x,y)){centerX-=dx/scale;centerZ-=dy/scale;return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseScrolled(double x,double y,double amount){if(menu.destinations&&inMap(x,y)){scale=Math.max(1.0/1024,Math.min(8,scale*Math.pow(1.3,amount)));return true;}return super.mouseScrolled(x,y,amount);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(menu.destinations){if(key==32){centerX=menu.gate.getBlockPos().getX();centerZ=menu.gate.getBlockPos().getZ();return true;}int slot=key==48?9:key-49;if(slot>=0&&slot<10&&available(slot)){var pos=SkyPearlItem.position(menu.gate.items.getStackInSlot(slot));centerX=pos.getX();centerZ=pos.getZ();return true;}}return super.keyPressed(key,scan,modifiers);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){if(!menu.destinations)renderBackground(g);super.render(g,x,y,partial);if(!menu.destinations)renderTooltip(g,x,y);else for(int i=0;i<10;i++)if(available(i)){var pearl=menu.gate.items.getStackInSlot(i);var pos=SkyPearlItem.position(pearl);if(Math.abs(x-px(pos.getX()))<=6&&Math.abs(y-pz(pos.getZ()))<=6)g.renderTooltip(font,java.util.List.of(pearl.getHoverName(),Component.literal("X: "+pos.getX()+"  Z: "+pos.getZ()),Component.translatable("ttmisc.clickToTeleport")),java.util.Optional.empty(),x,y);}}
}
