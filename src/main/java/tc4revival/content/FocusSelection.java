package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public  class FocusSelection {
    public record Source(Supplier<ItemStack> get, Consumer<ItemStack> set) {}
    public record Available(TreeMap<String, Source> foci, List<ItemStack> pouches) {}
    private FocusSelection() {}
    public static Available available(Player player) {
        var foci = new TreeMap<String, Source>(); var pouches = new ArrayList<ItemStack>();
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            for (var handler : inventory.getCurios().values()) {
                var stacks = handler.getStacks();
                for (int slot = 0; slot < stacks.getSlots(); slot++) {
                    var stack = stacks.getStackInSlot(slot);
                    if (thaumcraft.api.wands.FocusStorageRegistry.find(stack).isPresent()) collectPouch(stack, foci, pouches);
                }
            }
        });
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            int index = slot; var stack = player.getInventory().items.get(slot);
            if (stack.getItem() instanceof WandFocusItem focus) foci.put(focus.sortingKey(stack),
                    new Source(() -> player.getInventory().items.get(index), next -> player.getInventory().items.set(index, next)));
            if (thaumcraft.api.wands.FocusStorageRegistry.find(stack).isPresent()) collectPouch(stack, foci, pouches);
        }
        return new Available(foci, List.copyOf(pouches));
    }
    private static void collectPouch(ItemStack pouch, TreeMap<String, Source> foci, List<ItemStack> pouches) {
        var storage=thaumcraft.api.wands.FocusStorageRegistry.find(pouch).orElseThrow();
        pouches.add(pouch);
        for(int slot=0;slot<storage.slots();slot++) {
            int index=slot;var stack=storage.get(slot);
            if(stack.getItem() instanceof WandFocusItem focus && stack.getCount()==1
                    && storage.accepts(slot,ItemStack.EMPTY))
                foci.put(focus.sortingKey(stack),new Source(()->storage.get(index),next->storage.set(index,next.copy())));
        }
    }
    private static boolean store(Player player, List<ItemStack> pouches, ItemStack focus) {
        if (focus.isEmpty()) return true;
        var storages=pouches.stream().map(thaumcraft.api.wands.FocusStorageRegistry::find).flatMap(java.util.Optional::stream).toList();
        if(thaumcraft.api.wands.FocusStorageTransactions.returnFocus(storages,focus))return true;
        return player.getInventory().add(focus.copy());
    }
    public static void change(ServerPlayer player, InteractionHand hand, String key) {
        if (player == null || !player.isAlive() || player.isSpectator() || player.containerMenu != player.inventoryMenu) return;
        var wand = player.getItemInHand(hand);
        if (!(wand.getItem() instanceof CastingWandItem casting) || casting.isSceptre(wand)) return;
        var available = available(player); var old = WandFocusItem.attached(wand);
        var storages=available.pouches.stream().map(thaumcraft.api.wands.FocusStorageRegistry::find).flatMap(java.util.Optional::stream).toList();
        try(var transaction=new thaumcraft.api.wands.FocusStorageTransactions(player.getInventory(),storages)){
        if (key.equals("REMOVE") || available.foci.isEmpty()) {
            if (old.isEmpty() || !store(player, available.pouches, old)) return;
            player.stopUsingItem(); WandFocusItem.attach(wand, ItemStack.EMPTY);
            player.level().playSound(null, player, TCSounds.CAMERA_TICKS.get(), net.minecraft.sounds.SoundSource.PLAYERS, .3f, .9f);
        } else {
            var selected = available.foci.containsKey(key) ? key : available.foci.higherKey(key);
            if (selected == null) selected = available.foci.firstKey();
            var source = available.foci.get(selected); var incoming = source.get.get().copy();
            if (!(incoming.getItem() instanceof WandFocusItem) || incoming.getCount() != 1) return;
            source.set.accept(ItemStack.EMPTY);
            if(!source.get.get().isEmpty())return;
            if (!store(player, available.pouches, old)) return;
            player.stopUsingItem(); WandFocusItem.attach(wand, incoming);
            player.level().playSound(null, player, TCSounds.CAMERA_TICKS.get(), net.minecraft.sounds.SoundSource.PLAYERS, .3f, 1);
        }
        transaction.commit();
        }
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
    }
}
