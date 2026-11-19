package ttmodern;

import java.util.Map;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.content.CastingWandItem;
import tc4revival.aspect.Aspect;

public final class EnderChestFocus extends TinkererFocus {
    public EnderChestFocus(){super("focus_ender_chest","TTEC",0x132223);}
    @Override public String ornamentTexture(){return null;}
    @Override public String depthTexture(){return Tinkerer.ID+":item/focus_ender_chest_depth";}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return cost("ordo",100,"perditio",100);}
    public static boolean bound(ItemStack focus){return focus.hasTag()&&focus.getTag().getBoolean("EnderStorage");}
    public static void reset(ItemStack focus){if(focus.hasTag()){focus.getTag().remove("EnderStorage");focus.getTag().remove("Frequency");}}
    @Override public void appendHoverText(ItemStack focus,Level level,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag){super.appendHoverText(focus,level,lines,flag);if(bound(focus))EnderStorageBridge.tooltip(focus,lines);}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        if(!(player instanceof net.minecraft.server.level.ServerPlayer server))return InteractionResultHolder.fail(stack);
        var wand=(CastingWandItem)stack.getItem();var focus=attached(stack);
        if(player.isShiftKeyDown()&&hit!=null&&hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK){
            var pos=hit.getBlockPos();if(!level.hasChunkAt(pos)||!level.mayInteract(player,pos))return InteractionResultHolder.fail(stack);
            if(level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.OBSIDIAN)){reset(focus);attach(stack,focus);player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ttmodern.enderfocus.reset"),true);return InteractionResultHolder.success(stack);}
            var tile=level.getBlockEntity(pos);if(EnderStorageBridge.isChest(tile)){boolean bound=EnderStorageBridge.bind(focus,tile,server);if(bound){attach(stack,focus);player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ttmodern.enderfocus.bound"),true);}return bound?InteractionResultHolder.success(stack):InteractionResultHolder.fail(stack);}
        }
        if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,false))return InteractionResultHolder.fail(stack);
        boolean opened;
        if(bound(focus)){opened=EnderStorageBridge.open(focus,server);if(!opened)player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ttmodern.enderfocus.unavailable"),true);}
        else {player.getEnderChestInventory().setActiveChest(null);opened=player.openMenu(new SimpleMenuProvider((id,inventory,p)->ChestMenu.threeRows(id,inventory,player.getEnderChestInventory()),net.minecraft.network.chat.Component.translatable("container.enderchest"))).isPresent();}
        if(opened){wand.consumeForPlayer(stack,player,costs(focus),false,false,true);level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,net.minecraft.sounds.SoundSource.PLAYERS,1,1);}
        return opened?InteractionResultHolder.success(stack):InteractionResultHolder.fail(stack);
    }
}
