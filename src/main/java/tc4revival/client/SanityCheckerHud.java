package tc4revival.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.*;
import tc4revival.research.PlayerKnowledge.WarpKind;
public  class SanityCheckerHud {
    private static final ResourceLocation HUD=ResourceLocation.parse("thaumcraft:textures/gui/hud.png");
    private SanityCheckerHud() {}
    public static void render(GuiGraphics graphics) {
        var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||mc.options.hideGui||!(mc.player.getMainHandItem().is(TCItems.SANITY_CHECKER.get()) || mc.player.getOffhandItem().is(TCItems.SANITY_CHECKER.get())))return;
        var knowledge=ClientKnowledge.displaySnapshot().orElse(null);if(knowledge==null)return;
        int permanent=knowledge.warp(WarpKind.PERMANENT),sticky=knowledge.warp(WarpKind.STICKY),temporary=knowledge.warp(WarpKind.TEMPORARY);var gauge=SanityGauge.of(permanent,sticky,temporary);
        LegacyGui.blit(graphics,HUD,1,1,152,0,20,76);
        if(temporary>0) { graphics.setColor(1,.5f,1,1);LegacyGui.blit(graphics,HUD,7,21+gauge.gap(),200,gauge.gap(),8,gauge.temporary()); }
        if(sticky>0) { graphics.setColor(.75f,0,.75f,1);LegacyGui.blit(graphics,HUD,7,21+gauge.temporary()+gauge.gap(),200,gauge.temporary()+gauge.gap(),8,gauge.sticky()); }
        if(permanent>0) { graphics.setColor(.5f,0,.5f,1);LegacyGui.blit(graphics,HUD,7,21+gauge.temporary()+gauge.sticky()+gauge.gap(),200,gauge.temporary()+gauge.sticky()+gauge.gap(),8,48-gauge.gap()-gauge.temporary()-gauge.sticky()); }
        graphics.setColor(1,1,1,1);LegacyGui.blit(graphics,HUD,1,1,176,0,20,76);if(gauge.saturated())LegacyGui.blit(graphics,HUD,1,1,216,0,20,16);
    }
}
