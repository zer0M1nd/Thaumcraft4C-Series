package automagy.content;

import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import tc4revival.content.ItemMirrorItem;
import tc4revival.content.ItemMirrorBlockEntity;

public final class AutoHandMirrorItem extends Item {
    public AutoHandMirrorItem(){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    public static List<ItemStack> filters(ItemStack mirror){
        var result=new ArrayList<ItemStack>(List.of(ItemStack.EMPTY,ItemStack.EMPTY));
        if(mirror.hasTag())for(Tag raw:mirror.getTag().getList("AutomagyFilters",Tag.TAG_COMPOUND)){
            var saved=(CompoundTag)raw;int slot=saved.getInt("Slot");var stack=ItemStack.of(saved.getCompound("Item"));
            if(slot>=0&&slot<2&&stack.getItem() instanceof FilterItem)result.set(slot,stack.copyWithCount(1));
        }return result;
    }
    public static void filters(ItemStack mirror,List<ItemStack> stacks){
        var list=new ListTag();for(int i=0;i<Math.min(2,stacks.size());i++)if(stacks.get(i).getItem() instanceof FilterItem){
            var tag=new CompoundTag();tag.putInt("Slot",i);tag.put("Item",stacks.get(i).copyWithCount(1).save(new CompoundTag()));list.add(tag);
        }mirror.getOrCreateTag().put("AutomagyFilters",list);
    }
    public static List<HandMirrorFilters.Rule> rules(ItemStack mirror){return filters(mirror).stream().filter(s->s.getItem() instanceof FilterItem).map(s->new HandMirrorFilters.Rule(ItemFilter.load(s),((FilterItem)s.getItem()).blacklist)).toList();}
    @Override public boolean isFoil(ItemStack stack){return ItemMirrorItem.destination(stack)!=null;}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        var level=context.getLevel();var player=context.getPlayer();var pos=context.getClickedPos();
        if(player==null||!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof ItemMirrorBlockEntity))return InteractionResult.PASS;
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.FAIL;
        if(!level.isClientSide){ItemMirrorItem.writeDestination(stack.getOrCreateTag(),GlobalPos.of(level.dimension(),pos),level.dimension().location().toString());player.getInventory().setChanged();player.displayClientMessage(Component.translatable("tc.handmirrorlinked"),true);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private static ItemMirrorBlockEntity resolve(ItemStack mirror,Player player){
        var destination=ItemMirrorItem.destination(mirror);if(destination==null||!(player.level() instanceof ServerLevel server))return null;
        var target=server.getServer().getLevel(destination.dimension());
        // An absent dimension or unloaded chunk does not establish that a link is broken.
        if(target==null||!target.hasChunkAt(destination.pos()))return null;
        if(target.getBlockEntity(destination.pos()) instanceof ItemMirrorBlockEntity tile)return tile;
        var contents=filters(mirror);
        if(player.containerMenu instanceof AutoHandMirrorMenu menu&&menu.mirror()==mirror)player.closeContainer();
        mirror.setTag(null);for(var filter:contents)if(!filter.isEmpty())player.drop(filter,false);
        player.getInventory().setChanged();player.displayClientMessage(Component.translatable("tc.handmirrorerror"),true);return null;
    }
    public static boolean transport(ItemStack mirror,ItemStack items,Player player){
        if(!(mirror.getItem() instanceof AutoHandMirrorItem)||items.isEmpty()||items==mirror)return false;
        var target=resolve(mirror,player);if(target==null||!target.spawnItem(items.copy()))return false;
        target.transferEvent();player.level().playSound(null,player.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,.1f,1);return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);int slot=hand==InteractionHand.MAIN_HAND?player.getInventory().selected:40;
        if(player instanceof ServerPlayer server&&resolve(stack,player)!=null)net.minecraftforge.network.NetworkHooks.openScreen(server,
            new SimpleMenuProvider((id,inventory,p)->new AutoHandMirrorMenu(id,inventory,slot),stack.getHoverName()),buf->buf.writeVarInt(slot));
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){var target=ItemMirrorItem.destination(stack);if(target!=null)lines.add(Component.translatable("tooltip.automagy.link",target.pos().getX(),target.pos().getY(),target.pos().getZ(),target.dimension().location().toString()));}
}
