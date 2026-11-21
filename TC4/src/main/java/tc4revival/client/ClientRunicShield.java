package tc4revival.client;

/** The current player receives charge independently of vanilla health/absorption. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public  class ClientRunicShield {
    private static int charge,maximum,entityId=-1;
    private static net.minecraft.network.Connection connection;
    private ClientRunicShield() {}
    public static void receive(tc4revival.network.RunicChargePacket packet,net.minecraft.network.Connection source) {
        var mc=net.minecraft.client.Minecraft.getInstance();
        if (mc.getConnection()==null || mc.getConnection().getConnection()!=source) return;
        connection=source; entityId=packet.entityId(); maximum=Math.max(0,packet.maximum()); charge=Math.max(0,Math.min(maximum,packet.charge()));
    }
    private static boolean valid() {
        var mc=net.minecraft.client.Minecraft.getInstance(); return mc.player!=null && mc.player.getId()==entityId && mc.getConnection()!=null && mc.getConnection().getConnection()==connection;
    }
    public static int charge() { return valid() ? charge : 0; }
    public static int maximum() { return valid() ? maximum : 0; }
    public static void render(net.minecraft.client.gui.GuiGraphics graphics,int width,int height) {
        var mc=net.minecraft.client.Minecraft.getInstance();
        if (!valid() || maximum<=0 || mc.player.isCreative() || mc.player.isSpectator() || mc.options.hideGui) return;
        var texture=net.minecraft.resources.ResourceLocation.parse("thaumcraft:textures/misc/particles.png");
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        graphics.pose().pushPose(); graphics.pose().translate(width/2-91,height-39,0);
        for (int rune=0;rune<charge/(float)maximum*10;rune++) {
            graphics.setColor(1,1,1,1); LegacyGui.blit(graphics,texture,rune*8,0,160,16,9,9);
            graphics.pose().pushPose(); graphics.pose().scale(.5f,.5f,.5f);
            graphics.setColor(1,.75f,.24f,net.minecraft.util.Mth.sin(mc.player.tickCount/4f+rune)*.4f+.6f);
            LegacyGui.blit(graphics,texture,rune*16,0,rune*16,96,16,16); graphics.pose().popPose();
        }
        graphics.setColor(1,1,1,1);
        if (mc.player.isShiftKeyDown()) {
            String text=charge+"/"+maximum;
            int x=40-mc.font.width(text)/2, y=(9-mc.font.lineHeight)/2;
            graphics.pose().translate(0,0,10);
            // The experience-level HUD uses a four-sided dark outline and a bright centre.
            graphics.drawString(mc.font,text,x+1,y,0,false);
            graphics.drawString(mc.font,text,x-1,y,0,false);
            graphics.drawString(mc.font,text,x,y+1,0,false);
            graphics.drawString(mc.font,text,x,y-1,0,false);
            graphics.drawString(mc.font,text,x,y,0xFFFF55,false);
        }
        graphics.pose().popPose(); com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { charge=maximum=0; entityId=-1; connection=null; }
}
