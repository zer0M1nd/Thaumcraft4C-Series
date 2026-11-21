package automagy.client;

import automagy.content.FilterNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="automagy",value=Dist.CLIENT)
public final class CraftingKeyBinding {
 public static KeyMapping CRAFT;
 @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
  if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
  while(CRAFT.consumeClick())if(Minecraft.getInstance().screen==null&&Minecraft.getInstance().player!=null)FilterNetwork.craftKey();
 }
 @Mod.EventBusSubscriber(modid="automagy",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
 public static final class Registration{
  @SubscribeEvent public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event){event.register(CRAFT = new KeyMapping("key.automagy.craft",net.minecraftforge.client.settings.KeyConflictContext.IN_GAME,com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,org.lwjgl.glfw.GLFW.GLFW_KEY_C,"key.categories.automagy"));}
 }
 private CraftingKeyBinding(){}
}
