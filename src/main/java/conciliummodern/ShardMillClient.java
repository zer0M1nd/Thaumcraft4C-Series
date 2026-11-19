package conciliummodern;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,value=Dist.CLIENT)
public final class ShardMillClient {
    public static final KeyMapping KEY=new KeyMapping("key.thaumicconcilium.choose_shard",net.minecraftforge.client.settings.KeyConflictContext.IN_GAME,net.minecraftforge.client.settings.KeyModifier.ALT,com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_J),"key.categories.misc");
    private static final ResourceLocation HUD=ResourceLocation.parse("thaumcraft:textures/gui/hud.png");
    @Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event){event.register(KEY);}
        @SubscribeEvent public static void hud(RegisterGuiOverlaysEvent event){event.registerAboveAll("shard_mill",(gui,graphics,partial,width,height)->drawHud(graphics,height));}
    }
    static InteractionHand hand(){var player=Minecraft.getInstance().player;if(player==null)return null;return player.getMainHandItem().getItem() instanceof ShardMill?InteractionHand.MAIN_HAND:player.getOffhandItem().getItem() instanceof ShardMill?InteractionHand.OFF_HAND:null;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        while(KEY.consumeClick())if(mc.screen==null && mc.player!=null && !mc.player.isSpectator() && !mc.player.isUsingItem()){
            var hand=hand();if(hand!=null){var mill=(ShardMill)mc.player.getItemInHand(hand).getItem();if(!mill.available(mc.player).isEmpty())mc.setScreen(new ShardMillScreen(hand,mill.available(mc.player)));}
        }
    }
    private static void drawHud(GuiGraphics graphics,int height){
        var mc=Minecraft.getInstance();var hand=hand();if(hand==null || mc.options.hideGui || mc.player.isSpectator())return;
        var stack=mc.player.getItemInHand(hand);var mill=(ShardMill)stack.getItem();var pose=graphics.pose();
        pose.pushPose();pose.translate(0,tc4revival.config.TCConfig.CLIENT.wandDialBottom.get()?height-32:0,0);pose.scale(.5f,.5f,1);
        RenderSystem.enableBlend();graphics.blit(HUD,0,0,0,0,64,64);pose.popPose();
        pose.pushPose();pose.translate(16,(tc4revival.config.TCConfig.CLIENT.wandDialBottom.get()?height-32:0)+16,0);
        pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(90));pose.translate(0,-32,0);pose.scale(.5f,.5f,1);
        int amount=ShardMill.amount(stack);var type=mill.loaded(stack);
        if(type!=null && amount>0){int color=type.color(),fill=30*amount/20;graphics.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,.8f);graphics.blit(HUD,-4,35-fill,104,0,8,fill);graphics.setColor(1,1,1,1);}
        graphics.blit(HUD,-8,-3,72,0,16,42);pose.popPose();graphics.renderItem(mill.selected(stack).shard(),-8,-8);
        if(amount>0 && mc.player.isShiftKeyDown()){pose.pushPose();pose.translate(0,0,150);pose.scale(.5f,.5f,1);graphics.drawCenteredString(mc.font,Integer.toString(amount),0,-4,0xffffff);pose.popPose();}
        pose.popPose();RenderSystem.disableBlend();
    }
}
