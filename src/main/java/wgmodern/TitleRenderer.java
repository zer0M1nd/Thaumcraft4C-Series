package wgmodern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

/** Traveller Gear's title line: 64 block range, 3/4 font scale, below the normal player name. */
@Mod.EventBusSubscriber(modid="witchinggadgets",value=Dist.CLIENT)
public class TitleRenderer {
    @SubscribeEvent public static void render(RenderPlayerEvent.Pre event) {
        var mc=Minecraft.getInstance();var player=event.getEntity();var camera=mc.getCameraEntity();
        if(camera==null||camera==player||player.isShiftKeyDown()||mc.options.hideGui||camera.distanceToSqr(player)>4096)return;
        var title=CuriosApi.getCuriosInventory(player).resolve().flatMap(inv->inv.findFirstCurio(stack->stack.getItem() instanceof WGAccessories.Gear gear&&gear.kind.equals("title_crimson_cult"))).map(result->result.stack()).orElse(net.minecraft.world.item.ItemStack.EMPTY);
        if(title.isEmpty()||!title.hasTag()||!title.getTag().contains("title"))return;
        var key=title.getTag().getString("title");if(key.isBlank())return;
        var text=Component.translatable(key);var pose=event.getPoseStack();pose.pushPose();
        try {
            pose.translate(0,player.getBbHeight()+.2625,0);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.scale(-.0125f,-.0125f,.0125f);
            float x=-mc.font.width(text)/2f;int y=player.getGameProfile().getName().equalsIgnoreCase("deadmau5")?-10:0;
            mc.font.drawInBatch(text,x,y,0x20ffffff,false,pose.last().pose(),event.getMultiBufferSource(),Font.DisplayMode.SEE_THROUGH,0x40000000,event.getPackedLight());
            mc.font.drawInBatch(text,x,y,0xffffffff,false,pose.last().pose(),event.getMultiBufferSource(),Font.DisplayMode.NORMAL,0,event.getPackedLight());
        } finally { pose.popPose(); }
    }
}
