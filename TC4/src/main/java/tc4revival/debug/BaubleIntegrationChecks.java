package tc4revival.debug;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import tc4revival.content.MundaneBaubleType;
import tc4revival.content.TCItems;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

/** One integration check for Curios data, capabilities and persistence; never touches a real player. */
 class BaubleIntegrationChecks {
    private BaubleIntegrationChecks() {}
    static int run(CommandSourceStack source) {
        try {
            var player = new FakePlayer(source.getServer().overworld(), new GameProfile(
                    UUID.fromString("e6cb4638-0bf2-448c-8f6c-3d34a0d196e8"), "TC4BaubleProbe"));
            var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            inventory.reset();
            for (var type : MundaneBaubleType.values()) {
                var stack = new ItemStack(TCItems.bauble(type));
                var slotTypes = CuriosApi.getItemStackSlots(stack, player);
                require(slotTypes.containsKey(type.slot()), "Missing matching slot: " + type);
                for (var other : MundaneBaubleType.values()) if (other != type)
                    require(!slotTypes.containsKey(other.slot()), "Cross-slot match: " + type + "/" + other);
                var handler = inventory.getStacksHandler(type.slot()).orElseThrow();
                require(handler.getSlots() == type.slots(), "Wrong slot count: " + type);
                var capability = CuriosApi.getCurio(stack).resolve().orElseThrow();
                var context = new SlotContext(type.slot(), player, 0, false, true);
                require(capability.canEquip(context) && capability.canUnequip(context), "Wear/remove rejected: " + type);
                require(!capability.canEquipFromUse(context), "Unexpected right-click equip: " + type);
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    require(handler.getStacks().insertItem(slot, stack.copy(), false).isEmpty(), "Insert rejected: " + type);
                    // A full slot must return the entire input instead of duplicating or replacing its existing item.
                    require(handler.getStacks().insertItem(slot, stack.copy(), false).getCount() == 1, "Occupied slot consumed input");
                }
            }
            var saved = inventory.writeTag().copy();
            var reloadedPlayer = new FakePlayer(source.getServer().overworld(), new GameProfile(
                    UUID.fromString("7a97b332-7da6-4695-b0e8-0bcbf0162903"), "TC4BaubleReload"));
            var reloaded = CuriosApi.getCuriosInventory(reloadedPlayer).resolve().orElseThrow();
            reloaded.readTag(saved);
            for (var type : MundaneBaubleType.values()) {
                var stacks = reloaded.getStacksHandler(type.slot()).orElseThrow().getStacks();
                for (int slot = 0; slot < type.slots(); slot++) {
                    require(stacks.getStackInSlot(slot).is(TCItems.bauble(type)), "Lost equipped item after load");
                    var extracted = stacks.extractItem(slot, 1, false);
                    require(extracted.is(TCItems.bauble(type)) && stacks.getStackInSlot(slot).isEmpty(), "Unequip failed");
                }
            }
            source.sendSuccess(() -> Component.literal("baubles PASS: Curios 1 necklace/2 rings/1 belt, slot matching, capabilities, occupied-slot debit, save/load and removal; isolated players, no client test"), false);
            return 1;
        } catch (RuntimeException failure) {
            tc4revival.TC4Revival.LOGGER.error("Bauble integration validation failed", failure);
            source.sendFailure(Component.literal("baubles FAILED: " + failure.getMessage()));
            return 0;
        }
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
