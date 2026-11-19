package tc4revival.content;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public  class HandMirrorItem extends Item {
    public HandMirrorItem() { super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)); }
    @Override public boolean isFoil(ItemStack stack) { return ItemMirrorItem.destination(stack)!=null; }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var level=context.getLevel(); var player=context.getPlayer(); var pos=context.getClickedPos();
        if (player==null || !(level.getBlockEntity(pos) instanceof ItemMirrorBlockEntity mirror)) return InteractionResult.PASS;
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),stack)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            ItemMirrorItem.bindDestination(stack,mirror);
            level.playSound(null,pos,TCSounds.JAR.get(),SoundSource.BLOCKS,1,2);
            message(player,"tc.handmirrorlinked"); player.containerMenu.broadcastChanges();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private static void message(Player player,String key) { player.displayClientMessage(Component.translatable(key).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC),false); }
    private static ItemMirrorBlockEntity resolve(ItemStack stack,Player player) {
        var destination=ItemMirrorItem.destination(stack);
        if (destination==null || !(player.level() instanceof ServerLevel current)) return null;
        if(stack.getTag().hasUUID("MirrorTargetIdentity")){
            var endpoints=thaumcraft.api.transport.MirrorEndpoints.get(current.getServer());var id=stack.getTag().getUUID("MirrorTargetIdentity");
            var target=endpoints.resolve(current.getServer(),id);if(target!=null)return target;
            if(!endpoints.destroyed(id))return null;
        }else{
            var world=current.getServer().getLevel(destination.dimension()); if (world==null) return null;
            if (world.getBlockEntity(destination.pos()) instanceof ItemMirrorBlockEntity mirror) return mirror;
        }
        for(var key:java.util.List.of("linkDimension","linkX","linkY","linkZ","dimname","MirrorTargetIdentity"))stack.removeTagKey(key);
        current.playSound(null,player.blockPosition(),TCSounds.ZAP.get(),SoundSource.PLAYERS,1,.8f);
        message(player,"tc.handmirrorerror"); player.getInventory().setChanged(); return null;
    }
    public static boolean transport(ItemStack mirror,ItemStack items,Player player) {
        if (!(mirror.getItem() instanceof HandMirrorItem) || items.isEmpty()) return false;
        var target=resolve(mirror,player); if (target==null || !target.spawnItem(items)) return false;
        // Hand mirrors bypass the output queue and do not accumulate instability.
        target.transferEvent(); player.level().playSound(null,player.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,.1f,1); return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if (player instanceof ServerPlayer server && resolve(stack,player)!=null)
            net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,owner)->new HandMirrorMenu(id,inventory,hand),Component.translatable("item.thaumcraft.hand_mirror")),buffer->buffer.writeEnum(hand));
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flags) { ItemMirrorItem.appendDestination(stack,lines,Component.translatable("tc.handmirrorlinkedto")); }
}
