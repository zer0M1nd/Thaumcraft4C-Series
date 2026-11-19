package thmodern.lens;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.RevealingGear;

/** Plan the whole swap in copies before committing, including nested case inventories. */
public final class LensEquipment {
    public static final String DATA="thaumichorizons:lens";
    public static ItemStack goggles(Player player){
        var helmet=player.getItemBySlot(EquipmentSlot.HEAD);if(usable(player,helmet))return helmet;
        var curios=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElse(null);
        if(curios!=null){var handler=curios.getStacksHandler("head").orElse(null);if(handler!=null)for(int i=0;i<handler.getStacks().getSlots();i++){var stack=handler.getStacks().getStackInSlot(i);if(usable(player,stack))return stack;}}
        return ItemStack.EMPTY;
    }
    private static boolean usable(Player p,ItemStack s){return s.getItem() instanceof RevealingGear gear&&gear.showNodes(s,p);}
    public static ItemStack installed(ItemStack goggles){if(!goggles.hasTag())return ItemStack.EMPTY;var item=ItemStack.of(goggles.getTag().getCompound(DATA));return item.getItem() instanceof LensItem?item.copyWithCount(1):ItemStack.EMPTY;}
    public static String active(Player player){var stack=installed(goggles(player));return stack.getItem() instanceof LensItem lens?lens.key:"";}
    private static final class Root {final ItemStack original;final Consumer<ItemStack> save;ItemStack value;Root(ItemStack stack,Consumer<ItemStack> save){original=stack;value=stack.copy();this.save=save;}}
    private record Slot(Root root,int inside){ItemStack get(){return inside<0?root.value:LensCaseItem.get(root.value,inside);}void set(ItemStack value){if(inside<0)root.value=value.copy();else LensCaseItem.set(root.value,inside,value);}}
    private static List<Root> roots(Player player){
        var roots=new ArrayList<Root>();for(int i=0;i<36;i++){int slot=i;roots.add(new Root(player.getInventory().getItem(i),s->player.getInventory().setItem(slot,s)));}
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inv->inv.getCurios().values().forEach(h->{var items=h.getStacks();for(int i=0;i<items.getSlots();i++){int slot=i;roots.add(new Root(items.getStackInSlot(i),s->items.setStackInSlot(slot,s)));}}));return roots;
    }
    private static List<Slot> slots(List<Root> roots){var result=new ArrayList<Slot>();for(var root:roots)if(root.value.getItem() instanceof LensCaseItem)for(int i=0;i<18;i++)result.add(new Slot(root,i));for(int i=0;i<Math.min(36,roots.size());i++)result.add(new Slot(roots.get(i),-1));return result;}
    public static SortedSet<String> available(Player player){var result=new TreeSet<String>();for(var slot:slots(roots(player)))if(slot.get().getItem() instanceof LensItem lens)result.add(lens.key);return result;}
    public static boolean change(Player player,String key){
        if(player.level().isClientSide||!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu)return false;
        var goggles=goggles(player);if(goggles.isEmpty())return false;var old=installed(goggles);var roots=roots(player);var slots=slots(roots);ItemStack next=ItemStack.EMPTY;
        if(!key.isEmpty()){
            var source=slots.stream().filter(s->s.get().getItem() instanceof LensItem lens&&lens.key.equals(key)).findFirst().orElse(null);if(source==null)return false;next=source.get().copyWithCount(1);var rest=source.get().copy();rest.shrink(1);source.set(rest);
        }else if(old.isEmpty())return false;
        if(!old.isEmpty()){var destination=slots.stream().filter(s->s.get().isEmpty()).findFirst().orElse(null);if(destination==null)return false;destination.set(old);}
        for(var root:roots)if(!ItemStack.matches(root.original,root.value))root.save.accept(root.value);
        if(next.isEmpty())goggles.getOrCreateTag().remove(DATA);else goggles.getOrCreateTag().put(DATA,next.save(new net.minecraft.nbt.CompoundTag()));
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();player.level().playSound(null,player.blockPosition(),tc4revival.content.TCSounds.CAMERA_TICKS.get(),net.minecraft.sounds.SoundSource.PLAYERS,.3f,1);return true;
    }
    private LensEquipment(){}
}
