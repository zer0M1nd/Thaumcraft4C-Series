package wgmodern;

import net.minecraft.client.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.*;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid="witchinggadgets",value=Dist.CLIENT)
public class WGAccessoryClient {
    public static KeyMapping CLOAK;
    public static KeyMapping KAMA;
    public static KeyMapping POUCH;
    private static boolean jumpDown,jumped;private static int airborneTicks;private static net.minecraft.client.multiplayer.ClientLevel lastWorld;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();var p=mc.player;if(p==null||mc.level==null){jumpDown=false;jumped=false;airborneTicks=0;return;}if(mc.level!=lastWorld){lastWorld=mc.level;jumpDown=false;jumped=false;airborneTicks=0;}if(p.onGround()||p.isInWater()||p.onClimbable()){jumped=false;airborneTicks=0;}else airborneTicks++;if(mc.screen==null){while(POUCH.consumeClick())WGNetwork.CHANNEL.sendToServer(new WGNetwork.OpenPouch());while(CLOAK.consumeClick())WGNetwork.CHANNEL.sendToServer(new WGNetwork.Activate(false));while(KAMA.consumeClick())WGNetwork.CHANNEL.sendToServer(new WGNetwork.Activate(true));boolean jump=mc.options.keyJump.isDown();if(jump&&!jumpDown&&airborneTicks>1&&!p.onGround()&&!jumped&&!p.isInWater()&&!p.getAbilities().flying&&!p.isPassenger()&&WGAccessories.wearing(p,"shoulders_doublejump")){jumped=true;var v=p.getDeltaMovement();var boost=p.getEffect(MobEffects.JUMP);p.setDeltaMovement(v.x,.42+(boost==null?0:(boost.getAmplifier()+1)*.1),v.z);p.fallDistance=0;WGNetwork.CHANNEL.sendToServer(new WGNetwork.DoubleJump());}jumpDown=jump;}else jumpDown=mc.options.keyJump.isDown();}
    @SubscribeEvent public static void fov(ComputeFovModifierEvent e){var p=e.getPlayer();if(p.isShiftKeyDown()&&p.isUsingItem()&&p.getUseItem().getUseAnimation()==UseAnim.BOW&&WGAccessories.wearing(p,"ring_sniper"))e.setNewFovModifier(.25f);}
    @SubscribeEvent public static void names(RenderNameTagEvent e){if(e.getEntity() instanceof net.minecraft.world.entity.player.Player player&&CloakItem.spectral(player)&&!SpectralBuffers.unveiling(Minecraft.getInstance().player))e.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);}
    @Mod.EventBusSubscriber(modid="witchinggadgets",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    // Forge's generated handler uses the simple class name: keep it distinct from PrimordialRegistration.
    public static class AccessoryRegistration {@SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(CLOAK = new KeyMapping("key.witchinggadgets.cloak",GLFW.GLFW_KEY_V,"key.categories.witchinggadgets"));e.register(KAMA = new KeyMapping("key.witchinggadgets.kama",GLFW.GLFW_KEY_B,"key.categories.witchinggadgets"));e.register(POUCH = new KeyMapping("key.witchinggadgets.pouch",GLFW.GLFW_KEY_UNKNOWN,"key.categories.witchinggadgets"));}}
}
