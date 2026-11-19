package wgmodern;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class BagItem extends Item implements DyeableLeatherItem {
    public final String kind;
    public BagItem(String kind){super(new Properties().stacksTo(1).rarity(kind.equals("normal")?Rarity.RARE:Rarity.EPIC));this.kind=kind;}
    @Override public String getDescriptionId(){return "item.WG_Bag."+kind;}
    @Override public int getColor(ItemStack stack){return hasCustomColor(stack)?DyeableLeatherItem.super.getColor(stack):switch(kind){case "void"->0x484848;case "ender"->0x2d4741;case "hungry"->0x855172;default->0x8a74bd;};}
    public static NonNullList<ItemStack> contents(ItemStack bag){var result=NonNullList.withSize(18,ItemStack.EMPTY);if(bag.hasTag())ContainerHelper.loadAllItems(bag.getTag(),result);return result;}
    public static void store(ItemStack bag,NonNullList<ItemStack> items){ContainerHelper.saveAllItems(bag.getOrCreateTag(),items);}
    public static boolean permitted(ItemStack bag,Player p){var t=bag.getTag();return t==null||!t.hasUUID("Owner")||t.getUUID("Owner").equals(p.getUUID())||t.getBoolean("Unlocked")||p.getAbilities().instabuild;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){var stack=p.getItemInHand(hand);if(!(p instanceof ServerPlayer server))return InteractionResultHolder.success(stack);var tag=stack.getOrCreateTag();if(!tag.hasUUID("Owner")){tag.putUUID("Owner",p.getUUID());tag.putString("OwnerName",p.getGameProfile().getName());}
        if((kind.equals("normal")||kind.equals("hungry"))&&!permitted(stack,p)){p.hurt(p.damageSources().magic(),4);p.setItemInHand(hand,ItemStack.EMPTY);p.drop(stack,false);p.sendSystemMessage(Component.translatable("wg.chat.notyourbag",tag.getString("OwnerName")));return InteractionResultHolder.fail(stack);}
        if((kind.equals("normal")||kind.equals("hungry"))&&p.isShiftKeyDown()&&tag.getUUID("Owner").equals(p.getUUID())){boolean unlocked=!tag.getBoolean("Unlocked");tag.putBoolean("Unlocked",unlocked);p.sendSystemMessage(Component.translatable(unlocked?"wg.chat.bagunlocked":"wg.chat.baglocked"));return InteractionResultHolder.success(stack);}
        if(kind.equals("ender")){server.openMenu(new SimpleMenuProvider((id,inv,user)->ChestMenu.threeRows(id,inv,user.getEnderChestInventory()),Component.translatable("container.enderchest")));}
        else{boolean offhand=hand==InteractionHand.OFF_HAND;NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inv,user)->new BagMenu(id,inv,offhand),stack.getHoverName()),buf->buf.writeBoolean(offhand));}
        return InteractionResultHolder.success(stack);
    }
    @Override public boolean canFitInsideContainerItems(){return false;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){if(stack.hasTag()&&stack.getTag().contains("OwnerName"))lines.add(Component.literal(stack.getTag().getString("OwnerName")).withStyle(ChatFormatting.GRAY));if(kind.equals("void"))for(var item:contents(stack))if(!item.isEmpty())lines.add(item.getHoverName().copy().withStyle(ChatFormatting.DARK_GRAY));}
}
