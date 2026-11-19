package thaumicomniverse.compat.create;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.transmission.sequencer.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import tc4revival.client.WandKeys;
import tc4revival.content.*;

/** Keeps the original Create program editor, with its edited program saved in the focus. */
public final class CreateFocusClient {
    private static boolean using,attackDown,areaDown;
    private static InteractionHand activeHand=InteractionHand.MAIN_HAND;
    public static void register(net.minecraftforge.eventbus.api.IEventBus modBus){
        var bus=net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.addListener(CreateFocusClient::keys);bus.addListener(CreateFocusClient::scroll);bus.addListener(CreateFocusClient::tick);
        bus.addListener(KineticFocusEffects::render);
        modBus.addListener((net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event)->
            event.register((stack,tint)->tint==0?0xFF000000|((WandFocusItem)stack.getItem()).color(stack):0xFFFFFFFF,
                    ArcaneCreateContent.KINETIC_FOCUS.get(),ArcaneCreateContent.RELOCATION_FOCUS.get()));
        modBus.addListener((net.minecraftforge.client.event.ModelEvent.RegisterAdditional event)->{
            event.register(net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:item/focus_kinetic_orn"));
            event.register(net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:item/focus_relocation_orn"));
        });
    }
    private static InteractionHand hand(){
        var p=Minecraft.getInstance().player;
        if(p==null)return null;
        for(var hand:InteractionHand.values()){
            var wand=p.getItemInHand(hand);var focus=WandFocusItem.attached(wand);
            if(wand.getItem() instanceof CastingWandItem casting&&!casting.isSceptre(wand)
                    &&(focus.getItem() instanceof KineticFocusItem||focus.getItem() instanceof RelocationFocusItem))return hand;
        }return null;
    }
    private static void keys(InputEvent.InteractionKeyMappingTriggered event){
        var mc=Minecraft.getInstance();var hand=hand();
        if(hand==null||mc.screen!=null||!ArcaneContraptions.enabled())return;
        var focus=WandFocusItem.attached(mc.player.getItemInHand(hand));
        if(event.isAttack()&&focus.getItem() instanceof RelocationFocusItem){
            event.setCanceled(true);event.setSwingHand(false);
            if(!attackDown){CreateFocusNetwork.send(hand,CreateFocusNetwork.ATTACK,0);attackDown=true;}
        }else if(event.isUseItem()){
            event.setCanceled(true);event.setSwingHand(false);
            if(!using){
                activeHand=hand;using=true;CreateFocusNetwork.send(hand,CreateFocusNetwork.USE,0);
                if(focus.getItem() instanceof RelocationFocusItem||!KineticFocusItem.programmed(focus))
                    mc.player.startUsingItem(hand);
            }
        }
    }
    private static void scroll(InputEvent.MouseScrollingEvent event){
        var mc=Minecraft.getInstance();var hand=hand();
        if(hand==null||mc.screen!=null)return;
        var focus=WandFocusItem.attached(mc.player.getItemInHand(hand));
        if(focus.getItem() instanceof RelocationFocusItem&&mc.options.keyUse.isDown()&&event.getScrollDelta()!=0){
            event.setCanceled(true);
            CreateFocusNetwork.send(hand,CreateFocusNetwork.SCROLL,event.getScrollDelta()>0?1:-1);
        }
    }
    private static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null){using=false;attackDown=false;areaDown=false;return;}
        if(!mc.options.keyAttack.isDown())attackDown=false;
        if(using&&(!mc.options.keyUse.isDown()||mc.screen!=null||hand()!=activeHand)){
            boolean released=!mc.options.keyUse.isDown()&&mc.screen==null&&hand()==activeHand;
            CreateFocusNetwork.send(activeHand,released?CreateFocusNetwork.RELEASE:CreateFocusNetwork.CANCEL,0);
            mc.player.stopUsingItem();using=false;
        }
        if(mc.screen!=null)return;
        boolean area=WandKeys.CHANGE_AREA.isDown();
        boolean pressed=area&&!areaDown;areaDown=area;
        var hand=hand();if(hand==null)return;
        var focus=WandFocusItem.attached(mc.player.getItemInHand(hand));
        if(focus.getItem() instanceof KineticFocusItem&&pressed){
            if(mc.player.isShiftKeyDown()&&KineticFocusItem.programmed(focus))
                CreateFocusNetwork.send(hand,CreateFocusNetwork.EDIT_PROGRAM,0);
            else mc.setScreen(new FocusSpeedScreen(hand,KineticFocusItem.rpm(focus)));
        }
        if(focus.getItem() instanceof RelocationFocusItem&&focus.hasTag()&&focus.getTag().contains("RelocationCorner")
                &&focus.getTag().getString("RelocationDimension").equals(mc.level.dimension().location().toString())){
            var a=BlockPos.of(focus.getTag().getLong("RelocationCorner"));
            var eye=mc.player.getEyePosition();
            var hit=mc.level.clip(new net.minecraft.world.level.ClipContext(eye,eye.add(mc.player.getLookAngle().scale(16)),
                    net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player));
            var b=hit.getType()==HitResult.Type.BLOCK?hit.getBlockPos():a;
            int size=RelocationFocusItem.boxSize(focus);
            boolean valid=Math.abs(a.getX()-b.getX())<size&&Math.abs(a.getY()-b.getY())<size&&Math.abs(a.getZ()-b.getZ())<size;
            net.createmod.catnip.outliner.Outliner.getInstance().showAABB("thaumicomniverse:relocation_selection",new AABB(a).minmax(new AABB(b)))
                    .colored(valid?0xA777E8:0xDD4444).lineWidth(.0625f);
        }
    }
    public static void program(CreateFocusNetwork.Program packet){
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        var focus=WandFocusItem.attached(mc.player.getItemInHand(packet.hand()));
        if(!KineticFocusItem.programmed(focus))return;
        mc.setScreen(new FocusProgramScreen(packet.hand(),packet.program(),KineticFocusItem.programSize(focus)));
    }

    private CreateFocusClient(){}
}
