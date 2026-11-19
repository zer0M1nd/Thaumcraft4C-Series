package ttmodern;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class IchorPouchItem extends Item implements ICurioItem {
    public static final int SIZE=117;
    public IchorPouchItem(){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));}
    @Override public boolean canFitInsideContainerItems(){return false;}
    @Override public boolean canEquipFromUse(top.theillusivec4.curios.api.SlotContext context,ItemStack stack){return false;}
    public static boolean accepts(ItemStack stack){return !stack.isEmpty()&&!(stack.getItem() instanceof IchorPouchItem)&&!(stack.getItem() instanceof tc4revival.content.FocusPouchItem)&&!stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":pouch_blacklist")))&&stack.getItem().canFitInsideContainerItems();}
    public static NonNullList<ItemStack> contents(ItemStack pouch){
        var result=NonNullList.withSize(SIZE,ItemStack.EMPTY);if(!pouch.hasTag())return result;
        var entries=pouch.getTag().getList("Inventory",Tag.TAG_COMPOUND);
        for(int i=0;i<entries.size();i++){var entry=entries.getCompound(i);int slot=entry.getInt("Slot");if(slot>=0&&slot<SIZE)result.set(slot,ItemStack.of(entry));}
        return result;
    }
    public static void save(ItemStack pouch,java.util.List<ItemStack> items){
        if(items.size()!=SIZE)throw new IllegalArgumentException("Ichor pouch requires 117 slots");var entries=new ListTag();
        for(int i=0;i<SIZE;i++)if(!items.get(i).isEmpty()){var entry=items.get(i).save(new CompoundTag());entry.putInt("Slot",i);entries.add(entry);}
        if(entries.isEmpty()&&pouch.hasTag())pouch.getTag().remove("Inventory");else if(!entries.isEmpty())pouch.getOrCreateTag().put("Inventory",entries);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&!player.isSpectator())net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,owner)->new IchorPouchMenu(id,inventory,hand),getDescription()),buffer->buffer.writeEnum(hand));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
