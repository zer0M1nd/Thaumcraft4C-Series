package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.content.*;

public final class AstralMonitor extends Item {
    public AstralMonitor(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
    public static ItemStack stored(ItemStack monitor){return monitor.hasTag()?ItemStack.of(monitor.getTag().getCompound("wand")):ItemStack.EMPTY;}
    public static boolean accepts(ItemStack stack){return !MonitorSource.from(stack).isEmpty();}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()){
            if(player instanceof ServerPlayer server){int slot=hand==InteractionHand.OFF_HAND?40:player.getInventory().selected;
                net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,owner)->new AstralMonitorMenu(id,inventory,slot),stack.getHoverName()),buffer->buffer.writeVarInt(slot));}
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
        if(!(holder instanceof ServerPlayer viewer) || viewer.tickCount%10!=0 || (!selected && viewer.getOffhandItem()!=stack))return;
        var source=MonitorSource.from(stored(stack));var tag=stack.getOrCreateTag();
        tag.putIntArray("vals",MonitorSource.playerValues(viewer,source));
        tag.putInt("mindPower",MonitorSource.power(viewer,source,false));
    }
    public static int[] sample(ServerPlayer target){
        var shield=RunicShieldEvents.state(target);
        int health=Math.max(0,Math.min(10,Math.round(10*target.getHealth()/Math.max(1,target.getMaxHealth()))));
        int runes=shield.maximum()>0?Math.max(0,Math.min(9,Math.round(10*shield.exactCharge()/shield.maximum())-1)):-1;
        return new int[]{health,MonitorIntegrations.blood(target),MonitorIntegrations.mana(target),runes};
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flags){
        var wand=stored(stack);if(wand.hasTag() && !wand.getTag().getString("Xylography").isBlank())
            lines.add(Component.translatable("tc.tooltip.monitor").append(" "+wand.getTag().getString("Xylography")));
        var source=MonitorSource.from(wand);if(source.contains("pos")){var pos=net.minecraft.core.BlockPos.of(source.getLong("pos"));lines.add(Component.literal(source.getString("dimension")+" ["+pos.getX()+", "+pos.getY()+", "+pos.getZ()+"]"));}
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return AstralMonitorRenderer.instance();}
        });
    }
}
