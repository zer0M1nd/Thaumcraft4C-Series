package ttmodern;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Tinkerer.ID,value=Dist.CLIENT)
public final class SoulHeartsHud {
    private static float amount;
    private static net.minecraft.network.Connection connection;
    public static void receive(float value,net.minecraft.network.Connection source){amount=Float.isFinite(value)?Math.max(0,Math.min(20,value)):0;connection=source;}
    @SubscribeEvent public static void render(RenderGuiOverlayEvent.Post event){
        var mc=Minecraft.getInstance();if(event.getOverlay()!=VanillaGuiOverlay.PLAYER_HEALTH.type()||mc.player==null||mc.options.hideGui||amount<=0||mc.getConnection()==null||mc.getConnection().getConnection()!=connection)return;
        int x=event.getWindow().getGuiScaledWidth()/2+10,y=event.getWindow().getGuiScaledHeight()-49-(mc.player.getAirSupply()<mc.player.getMaxAirSupply()?10:0);
        var texture=ResourceLocation.parse(Tinkerer.ID+":textures/gui/soul_hearts.png");
        for(int i=0;i<(int)Math.ceil(amount/2);i++){boolean full=amount-i*2>=2;event.getGuiGraphics().blit(texture,x+i*8,y,full?0:9,0,full?9:7,9,16,16);}
    }
    private SoulHeartsHud(){}
}
