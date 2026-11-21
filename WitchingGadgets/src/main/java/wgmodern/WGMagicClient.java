package wgmodern;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
@Mod.EventBusSubscriber(modid="witchinggadgets",value=Dist.CLIENT)
public class WGMagicClient {
    public static boolean hidden(ItemStack stack,LivingEntity wearer){if(!(wearer instanceof Player))return false;int level=WGMagic.level(stack,"invisible_gear");return (level>1||level>0&&wearer.isInvisible())&&(wearer==Minecraft.getInstance().player||!SpectralBuffers.unveiling(Minecraft.getInstance().player));}
    @SubscribeEvent public static void reveal(RenderLivingEvent.Pre<?,?> event){var viewer=Minecraft.getInstance().player;if(viewer!=null&&event.getEntity() instanceof Player player&&player!=viewer&&player.isInvisibleTo(viewer)&&SpectralBuffers.unveiling(viewer))WGEffects.reveal(player);}
    public static class EffectIcon implements IClientMobEffectExtensions {
        private final java.util.function.IntSupplier index;private static final ResourceLocation IMAGE=new ResourceLocation("witchinggadgets","textures/gui/potioneffects.png");
        public EffectIcon(int index){this(()->index);}
        public EffectIcon(java.util.function.IntSupplier index){this.index=index;}
        public boolean renderInventoryIcon(MobEffectInstance effect,EffectRenderingInventoryScreen<?> screen,GuiGraphics g,int x,int y,int z){tc4revival.client.LegacyGui.blit(g,IMAGE,x,y+7,index.getAsInt()*18,198,18,18);return true;}
        public boolean renderGuiIcon(MobEffectInstance effect,Gui gui,GuiGraphics g,int x,int y,float z,float alpha){g.setColor(1,1,1,alpha);tc4revival.client.LegacyGui.blit(g,IMAGE,x+3,y+3,index.getAsInt()*18,198,18,18);g.setColor(1,1,1,1);return true;}
    }
}
