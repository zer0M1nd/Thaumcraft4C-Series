package thmodern.lens;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class LensCaseItem extends Item implements top.theillusivec4.curios.api.type.capability.ICurioItem {
    public LensCaseItem(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
    public static ItemStack get(ItemStack bag,int slot){if(slot<0||slot>=18||!bag.hasTag())return ItemStack.EMPTY;var stack=ItemStack.of(bag.getTag().getCompound("Lenses").getCompound(Integer.toString(slot)));return stack.getItem() instanceof LensItem?stack.copyWithCount(1):ItemStack.EMPTY;}
    public static void set(ItemStack bag,int slot,ItemStack item){if(slot<0||slot>=18||!(bag.getItem() instanceof LensCaseItem)||!item.isEmpty()&&!(item.getItem() instanceof LensItem))throw new IllegalArgumentException("Invalid lens case slot");var data=bag.getOrCreateTag().getCompound("Lenses");if(item.isEmpty())data.remove(Integer.toString(slot));else data.put(Integer.toString(slot),item.copyWithCount(1).save(new CompoundTag()));bag.getOrCreateTag().put("Lenses",data);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){if(player instanceof net.minecraft.server.level.ServerPlayer server)net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,p)->new LensCaseMenu(id,inventory,hand),net.minecraft.network.chat.Component.translatable("item.thaumichorizons.lens_case")),b->b.writeEnum(hand));return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
}
