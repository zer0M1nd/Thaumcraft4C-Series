package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;
import tc4revival.network.*;

@Mod.EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public class InfusionRiskOverlay {
    private static BlockPos target;
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static Connection connection;
    private static InfusionRisk risk;
    private static Integer stability;
    private static java.util.List<InfusionPreview.Item> items=java.util.List.of();
    private static tc4revival.aspect.AspectAmounts required=new tc4revival.aspect.AspectAmounts(),available=new tc4revival.aspect.AspectAmounts();
    private static boolean repeat;
    private static long nextQuery,expires;
    private static void clear(){target=null;world=null;connection=null;risk=null;stability=null;items=java.util.List.of();repeat=false;nextQuery=expires=0;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();}
    public static void receive(InfusionRiskReply packet,Connection source) {
        var mc=Minecraft.getInstance();
        if(source!=connection || mc.level!=world || !packet.pos().equals(target))return;
        risk=packet.risk();stability=packet.environment();items=packet.items();repeat=packet.repeat();required=packet.required();available=packet.available();expires=world.getGameTime()+30;
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || mc.getConnection()==null || mc.options.hideGui || mc.screen!=null
                || !RevealingGear.visible(mc.player,false) || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType()!=HitResult.Type.BLOCK || !(mc.level.getBlockEntity(hit.getBlockPos()) instanceof InfusionMatrixBlockEntity)) {clear();return;}
        if(world!=mc.level || !hit.getBlockPos().equals(target)) {clear();world=mc.level;target=hit.getBlockPos();connection=mc.getConnection().getConnection();}
        long now=world.getGameTime();
        if(now>=nextQuery){nextQuery=now+10;TCNetwork.CHANNEL.sendToServer(new InfusionRiskRequest(target));}
        if(now>expires)return;
        var gui=event.getGuiGraphics();int x=gui.guiWidth()/2,y=gui.guiHeight()/2+18;
        if(repeat) {
            var hint=Component.translatable("thaumcraft.infusion.repeat");
            gui.drawString(mc.font,hint,x-mc.font.width(hint)/2,y,0xffffffff,true);y+=12;
        }
        if(stability==null)return;
        var environment=Component.translatable("thaumcraft.infusion.stability",Long.toString(-(long)stability));
        environment.withStyle(style->style.withColor(stability<=0?0x55ff55:0xff5555));
        var line=risk==null?environment:Component.translatable("thaumcraft.infusion.risk",risk.recipe())
                .withStyle(style->style.withColor(risk.color()&0xffffff))
                .append(Component.literal(" | ").withStyle(net.minecraft.ChatFormatting.WHITE)).append(environment);
        gui.drawString(mc.font,line,x-mc.font.width(line)/2,y,0xffffffff,true);
        y+=14;
        if(!items.isEmpty()) {
            // Keep icons readable on narrow windows; wrap long addon recipes before the aspect rows.
            int columns=Math.max(1,(gui.guiWidth()-24)/20),rows=(items.size()+columns-1)/columns;
            for(int row=0;row<rows;row++) {
                int count=Math.min(columns,items.size()-row*columns),left=x-count*20/2;
                for(int col=0;col<count;col++) {
                    var item=items.get(row*columns+col);int ix=left+col*20,iy=y+row*22;
                    gui.renderItem(item.stack(),ix,iy);
                    var mark=switch(item.mark()){case PRESENT->GuiStatusIcon.CHECK;case MISSING->GuiStatusIcon.QUESTION;case EXTRA->GuiStatusIcon.CROSS;};
                    gui.pose().pushPose();gui.pose().translate(0,0,200);
                    mark.draw(gui,ix+18-mark.width(),iy+10);gui.pose().popPose();
                }
            }
            y+=rows*22+2;
        }
        var aspects=required.aspects();if(aspects.isEmpty())return;
        int width=0;for(var aspect:aspects)width+=Math.max(22,mc.font.width(available.amount(aspect)+"/"+required.amount(aspect))+4);
        float scale=Math.min(1f,(gui.guiWidth()-16f)/width);
        gui.pose().pushPose();gui.pose().translate(x-width*scale/2,y,0);gui.pose().scale(scale,scale,1);
        int offset=0;com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        for(var aspect:aspects){
            String text=available.amount(aspect)+"/"+required.amount(aspect);int cell=Math.max(22,mc.font.width(text)+4);
            int color=thaumcraft.api.client.ClientAspectViews.view(aspect).color();gui.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
            LegacyGui.blit(gui,thaumcraft.api.client.ClientAspectViews.view(aspect).texture(),offset,0,0,0,16,16,16,16);gui.setColor(1,1,1,1);
            gui.pose().pushPose();gui.pose().translate(offset+cell-2,16,10);
            gui.drawString(mc.font,text,-mc.font.width(text),0,available.amount(aspect)>=required.amount(aspect)?0xff55ff55:0xffff5555,true);
            gui.pose().popPose();offset+=cell;
        }
        gui.pose().popPose();
    }
    private InfusionRiskOverlay(){}
}
