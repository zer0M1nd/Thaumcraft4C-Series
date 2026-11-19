package emtmodern.client;

import emtmodern.item.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid="emt",value=Dist.CLIENT)
public class QuantumChestClient {
    public static KeyMapping DETACH;
    protected static ic2.core.sound.Sound sound;
    protected static Player soundOwner;
    public static void stopSound(){if(sound!=null&&soundOwner!=null)ic2.core.IC2.soundManager.removeSound(soundOwner,sound);sound=null;soundOwner=null;}
    public static void jetpackSound(Player player,boolean active){
        if(player!=Minecraft.getInstance().player)return;
        if(!active||soundOwner!=null&&soundOwner!=player)stopSound();
        if(active&&sound==null){soundOwner=player;sound=ic2.core.IC2.soundManager.createSound(player,ic2.core.ref.Ic2SoundEvents.ITEM_JETPACK_LOOP,net.minecraft.sounds.SoundSource.PLAYERS,player,1,1);if(sound!=null)sound.play();}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var client=Minecraft.getInstance();
        if(client.player==null||!(client.player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof InfusedQuantumChestplate))stopSound();
        while(DETACH.consumeClick())if(client.screen==null&&client.player!=null&&client.player.isShiftKeyDown()&&client.player.getMainHandItem().isEmpty()&&client.player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof InfusedQuantumChestplate)QuantumChestNetwork.CHANNEL.sendToServer(new QuantumChestNetwork.Detach());
    }
    @Mod.EventBusSubscriber(modid="emt",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event){event.register(DETACH = new KeyMapping("gui.EMT.key.unequip",com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,org.lwjgl.glfw.GLFW.GLFW_KEY_Z,"Electro-Magic Tools"));}
        @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->ItemProperties.register(emtmodern.ElectroMagicTools.CONTENT.get("infused_quantum_chestplate").get(),new ResourceLocation("emt","attachment"),(stack,level,entity,seed)->{var part=InfusedQuantumChestplate.attachment(stack);return part==null?0:part.number();}));}
    }
}
