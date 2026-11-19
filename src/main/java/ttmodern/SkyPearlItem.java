package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class SkyPearlItem extends Item {
    public SkyPearlItem(){super(new Properties().stacksTo(2).rarity(Rarity.EPIC));}
    public static boolean bound(ItemStack stack){return stack.hasTag()&&stack.getTag().contains("Destination",net.minecraft.nbt.Tag.TAG_LONG)&&ResourceLocation.tryParse(stack.getTag().getString("Dimension"))!=null;}
    public static BlockPos position(ItemStack stack){return bound(stack)?BlockPos.of(stack.getTag().getLong("Destination")):BlockPos.ZERO;}
    public static ResourceLocation dimension(ItemStack stack){return bound(stack)?ResourceLocation.parse(stack.getTag().getString("Dimension")):null;}
    public static void bind(ItemStack stack,Level world,BlockPos pos){stack.getOrCreateTag().putLong("Destination",pos.asLong());stack.getOrCreateTag().putString("Dimension",world.dimension().location().toString());}
    @Override public boolean isFoil(ItemStack stack){return bound(stack);}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        if(!bound(stack)&&context.getLevel().getBlockState(context.getClickedPos()).is(Tinkerer.WARP_GATE.get())){if(!context.getLevel().isClientSide)bind(stack,context.getLevel(),context.getClickedPos());return InteractionResult.sidedSuccess(context.getLevel().isClientSide);}return InteractionResult.PASS;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(player.isShiftKeyDown()&&bound(stack)){if(!world.isClientSide){stack.getTag().remove("Destination");stack.getTag().remove("Dimension");}return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);}return InteractionResultHolder.pass(stack);}
    @Override public void appendHoverText(ItemStack stack,Level world,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){if(bound(stack)){var pos=position(stack);lines.add(net.minecraft.network.chat.Component.literal("X: "+pos.getX()+"  Y: "+pos.getY()+"  Z: "+pos.getZ()));lines.add(net.minecraft.network.chat.Component.literal(dimension(stack).toString()));}}
}
