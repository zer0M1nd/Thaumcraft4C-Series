package tc4revival.content;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;

public  class EssentiaMirrorItem extends BlockItem {
    public EssentiaMirrorItem() { super(TCArcane.ESSENTIA_MIRROR.get(),new Properties().rarity(Rarity.UNCOMMON)); }
    public static int legacyMetadata(ItemStack stack) {
        return destination(stack)!=null || stack.hasTag() && stack.getTag().getInt("tc4_metadata")==7 ? 7 : 6;
    }
    public static ItemStack linkedStack(GlobalPos target,String dimensionName) {
        var stack=new ItemStack(TCItems.ESSENTIA_MIRROR.get()); var tag=stack.getOrCreateTag();
        tag.putString("linkDimension",target.dimension().location().toString()); tag.putString("dimname",dimensionName);
        tag.putInt("linkX",target.pos().getX()); tag.putInt("linkY",target.pos().getY()); tag.putInt("linkZ",target.pos().getZ());
        tag.putInt("tc4_metadata",7);
        return stack;
    }
    public static GlobalPos destination(ItemStack stack) {
        if (!stack.hasTag()) return null;
        var tag=stack.getTag(); var dimension=ResourceLocation.tryParse(tag.getString("linkDimension"));
        if (dimension==null || !tag.contains("linkX",3) || !tag.contains("linkY",3) || !tag.contains("linkZ",3)) return null;
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION,dimension),new net.minecraft.core.BlockPos(tag.getInt("linkX"),tag.getInt("linkY"),tag.getInt("linkZ")));
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var level=context.getLevel(); var player=context.getPlayer(); var pos=context.getClickedPos();
        if (player==null || !(level.getBlockEntity(pos) instanceof EssentiaMirrorBlockEntity mirror)) return InteractionResult.PASS;
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),stack)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (mirror.valid(true)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("thaumcraft.mirror.already_linked")
                    .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC),false);
            return InteractionResult.CONSUME;
        }
        var prepared=linkedStack(GlobalPos.of(level.dimension(),pos),level.dimension().location().toString());
        if (!player.getAbilities().instabuild) stack.shrink(1);
        if (!player.getInventory().add(prepared)) player.drop(prepared,false);
        level.playSound(null,pos,TCSounds.JAR.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,2);
        player.containerMenu.broadcastChanges(); return InteractionResult.CONSUME;
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        var target=destination(context.getItemInHand()); var pos=context.getClickedPos();
        var result=super.place(context);
        if (result.consumesAction() && target!=null && !context.getLevel().isClientSide
                && context.getLevel().getBlockEntity(pos) instanceof EssentiaMirrorBlockEntity mirror) {
            mirror.remember(target); mirror.restoreLink();
        }
        return result;
    }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flags) {
        var target=destination(stack); if (target==null) return;
        var pos=target.pos(); String name=stack.getTag().getString("dimname");
        if (name.isEmpty()) name=target.dimension().location().toString();
        lines.add(net.minecraft.network.chat.Component.translatable("thaumcraft.mirror.linked_to").append(" ").append(net.minecraft.network.chat.Component.translatable("thaumcraft.mirror.destination",pos.getX(),pos.getY(),pos.getZ(),name)));
    }
}
