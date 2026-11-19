package ttmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public final class PlacementMirrorItem extends Item {
    public PlacementMirrorItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    public static int size(ItemStack mirror){return Math.max(3,Math.min(11,(mirror.hasTag()?mirror.getTag().getInt("Size"):3)|1));}
    public static BlockState template(ItemStack mirror){return mirror.hasTag()?NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),mirror.getTag().getCompound("Template")):net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();}
    static boolean bind(ItemStack mirror,Level level,BlockPos pos){var state=level.getBlockState(pos);if(state.isAir()||state.hasBlockEntity()||!(state.getBlock().asItem() instanceof BlockItem)||!state.isCollisionShapeFullBlock(level,pos))return false;mirror.getOrCreateTag().put("Template",NbtUtils.writeBlockState(state));return true;}
    public static java.util.List<BlockPos> targets(ItemStack mirror,Player player,BlockHitResult hit){var origin=player.level().getBlockState(hit.getBlockPos()).canBeReplaced()?hit.getBlockPos():hit.getBlockPos().relative(hit.getDirection());return PlacementArea.positions(origin,hit.getDirection(),player.getDirection(),player.getXRot(),size(mirror)).stream().filter(pos->player.level().hasChunkAt(pos)&&!player.level().isOutsideBuildHeight(pos)&&player.level().getWorldBorder().isWithinBounds(pos)&&player.level().getBlockState(pos).canBeReplaced()).toList();}
    private static java.util.List<ItemStack> supplies(Player player){var result=new java.util.ArrayList<ItemStack>();for(int i=0;i<player.getInventory().getContainerSize();i++)result.add(player.getInventory().getItem(i));top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler->handler.getCurios().values().forEach(slots->{var stacks=slots.getStacks();for(int i=0;i<stacks.getSlots();i++){var stack=stacks.getStackInSlot(i);if(stack.is(Tinkerer.BLOCK_TALISMAN.get()))result.add(stack);}}));return result;}
    static long available(Player player,ItemStack exemplar){if(player.getAbilities().instabuild)return Long.MAX_VALUE;long count=0;for(var stack:supplies(player))if(ItemStack.isSameItemSameTags(stack,exemplar))count+=stack.getCount();else if(stack.is(Tinkerer.BLOCK_TALISMAN.get())&&ItemStack.isSameItemSameTags(BlockTalismanItem.template(stack),exemplar))count+=BlockTalismanItem.count(stack);return count;}
    private static boolean consume(Player player,ItemStack exemplar){if(player.getAbilities().instabuild)return true;var stacks=supplies(player);for(var stack:stacks)if(!stack.isEmpty()&&ItemStack.isSameItemSameTags(stack,exemplar)){stack.shrink(1);player.getInventory().setChanged();return true;}for(var stack:stacks)if(stack.is(Tinkerer.BLOCK_TALISMAN.get())&&ItemStack.isSameItemSameTags(BlockTalismanItem.template(stack),exemplar)&&BlockTalismanItem.remove(stack,1)==1)return true;return false;}
    @Override public InteractionResult useOn(UseOnContext context){var player=context.getPlayer();if(player==null||!TinkererConfig.KAMI.get())return InteractionResult.FAIL;var world=context.getLevel();if(world.isClientSide)return InteractionResult.SUCCESS;if(player.isShiftKeyDown())return bind(context.getItemInHand(),world,context.getClickedPos())?InteractionResult.CONSUME:InteractionResult.FAIL;return place(context)>0?InteractionResult.CONSUME:InteractionResult.FAIL;}
    static int place(UseOnContext context){
        var player=context.getPlayer();var world=context.getLevel();var state=template(context.getItemInHand());if(player==null||world.isClientSide||state.isAir()||state.hasBlockEntity()||!(state.getBlock().asItem() instanceof BlockItem))return 0;
        var exemplar=new ItemStack(state.getBlock());var hit=new BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),context.isInside());var targets=targets(context.getItemInHand(),player,hit);if(available(player,exemplar)<targets.size())return 0;int count=0;
        for(var pos:targets){if(!world.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),exemplar)||available(player,exemplar)<1)continue;
            var block=exemplar.copy();var properties=new CompoundTag();for(var property:state.getProperties())properties.putString(property.getName(),propertyValue(state,property));block.getOrCreateTag().put("BlockStateTag",properties);
            var virtual=new UseOnContext(player,context.getHand(),new BlockHitResult(pos.getCenter(),context.getClickedFace(),pos,false)){@Override public ItemStack getItemInHand(){return block;}};
            // BlockItem's native Forge path checks entity collision, survival and canceled placement.
            var before=world.getBlockState(pos);var result=block.useOn(virtual);if(result.consumesAction()&&!world.getBlockState(pos).equals(before)){if(consume(player,exemplar))count++;}
        }
        if(count>0)world.playSound(null,player.blockPosition(),tc4revival.content.TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);return count;
    }
    private static <T extends Comparable<T>> String propertyValue(BlockState state,net.minecraft.world.level.block.state.properties.Property<T> property){return property.getName(state.getValue(property));}
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(player.isShiftKeyDown()){if(!world.isClientSide){int next=size(stack)==11?3:size(stack)+2;stack.getOrCreateTag().putInt("Size",next);player.displayClientMessage(net.minecraft.network.chat.Component.literal(next+" x "+next),true);world.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.3f,.1f);}return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);}return InteractionResultHolder.pass(stack);}
    @Override public void appendHoverText(ItemStack stack,Level world,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.literal(size(stack)+" x "+size(stack)));var state=template(stack);if(!state.isAir())lines.add(state.getBlock().getName());}
}
