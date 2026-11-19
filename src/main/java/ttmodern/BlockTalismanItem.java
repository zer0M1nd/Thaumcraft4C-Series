package ttmodern;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class BlockTalismanItem extends Item implements ICurioItem {
    public BlockTalismanItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    public static int count(ItemStack stack){return stack.hasTag()?Math.max(0,stack.getTag().getInt("BlockCount")):0;}
    public static ItemStack template(ItemStack stack){return stack.hasTag()?ItemStack.of(stack.getTag().getCompound("BlockTemplate")):ItemStack.EMPTY;}
    public static boolean active(ItemStack stack){return stack.hasTag()&&stack.getTag().getBoolean("enabled");}
    static int add(ItemStack stack,int amount){int accepted=(int)Math.min(Integer.MAX_VALUE-(long)count(stack),Math.max(0,amount));stack.getOrCreateTag().putInt("BlockCount",count(stack)+accepted);return accepted;}
    static int remove(ItemStack stack,int amount){int removed=Math.min(count(stack),Math.max(0,amount));stack.getOrCreateTag().putInt("BlockCount",count(stack)-removed);return removed;}
    static boolean bind(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){
        var item=state.getBlock().asItem();if(!(item instanceof BlockItem)||count(stack)>0)return false;
        stack.getOrCreateTag().put("BlockTemplate",new ItemStack(item).save(new net.minecraft.nbt.CompoundTag()));return true;
    }
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack){return false;}
    private static InteractionResult toggle(Level level,Player player,ItemStack stack){
        if(template(stack).isEmpty())return InteractionResult.FAIL;
        if(!level.isClientSide){stack.getOrCreateTag().putBoolean("enabled",!active(stack));level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.3f,.1f);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        return player.isShiftKeyDown()?new InteractionResultHolder<>(toggle(level,player,stack),stack):InteractionResultHolder.pass(stack);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();
        if(player!=null&&player.isShiftKeyDown())return toggle(context.getLevel(),player,context.getItemInHand());
        if(player==null||!player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand())||!context.getLevel().mayInteract(player,context.getClickedPos()))return InteractionResult.FAIL;
        var stack=context.getItemInHand();if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(count(stack)==0)return bind(stack,context.getLevel().getBlockState(context.getClickedPos()))?InteractionResult.CONSUME:InteractionResult.FAIL;
        var exemplar=template(stack);if(!(exemplar.getItem() instanceof BlockItem))return InteractionResult.FAIL;
        var blockEntity=context.getLevel().getBlockEntity(context.getClickedPos());
        if(blockEntity!=null){var handler=blockEntity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,context.getClickedFace()).orElse(null);if(handler!=null)return deposit(stack,handler)>0?InteractionResult.CONSUME:InteractionResult.FAIL;}
        var placed=exemplar.copyWithCount(1);var hit=new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside());
        var virtual=new UseOnContext(player,context.getHand(),hit){@Override public ItemStack getItemInHand(){return placed;}};
        var result=placed.useOn(virtual);if(result.consumesAction())remove(stack,1);return result;
    }
    static int deposit(ItemStack stack,net.minecraftforge.items.IItemHandler target){
        var exemplar=template(stack);if(exemplar.isEmpty())return 0;int moved=0;
        for(int slot=0;slot<target.getSlots()&&count(stack)>0;slot++){
            var offer=exemplar.copyWithCount(Math.min(count(stack),exemplar.getMaxStackSize()));var remainder=target.insertItem(slot,offer,false);int accepted=offer.getCount()-(remainder.isEmpty()?0:remainder.getCount());
            if(accepted>0&&accepted<=offer.getCount()){remove(stack,accepted);moved+=accepted;}
        }return moved;
    }
    @Override public void curioTick(SlotContext context,ItemStack stack){if(context.entity() instanceof Player player&&!player.level().isClientSide&&active(stack)&&TinkererConfig.KAMI.get())replenish(player,stack);}
    static void replenish(Player player,ItemStack stack){
        var exemplar=template(stack);if(exemplar.isEmpty())return;var inv=player.getInventory();int keep=-1,free=-1;
        for(int i=0;i<inv.items.size();i++){var candidate=inv.getItem(i);if(candidate.isEmpty()){if(free<0)free=i;continue;}if(ItemStack.isSameItemSameTags(candidate,exemplar)&&(keep<0||keep>=9&&candidate.getCount()>inv.getItem(keep).getCount()))keep=i;}
        if(keep<0){if(free>=0&&player.containerMenu.getCarried().isEmpty()){int take=remove(stack,exemplar.getMaxStackSize());if(take>0)inv.setItem(free,exemplar.copyWithCount(take));}return;}
        for(int i=0;i<inv.items.size();i++)if(i!=keep){var candidate=inv.getItem(i);if(ItemStack.isSameItemSameTags(candidate,exemplar)){int stored=add(stack,candidate.getCount());candidate.shrink(stored);if(candidate.isEmpty())inv.setItem(i,ItemStack.EMPTY);}}
        var retained=inv.getItem(keep);retained.grow(remove(stack,Math.max(0,retained.getMaxStackSize()-retained.getCount())));inv.setChanged();
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){var exemplar=template(stack);if(!exemplar.isEmpty())lines.add(exemplar.getHoverName().copy().append(" (x"+count(stack)+")"));lines.add(net.minecraft.network.chat.Component.translatable(active(stack)?"ttmisc.active":"ttmisc.inactive"));}
}
