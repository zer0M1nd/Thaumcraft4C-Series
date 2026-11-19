package tc4revival.content;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tc4revival.aspect.TCAspects;
import tc4revival.research.*;

/** Inventory half of a validated book request; the future book session supplies server-owned knowledge. */
public  class ResearchBookInventory {
    public enum Status { CREATED, EXISTING, UNKNOWN, INELIGIBLE, PURCHASE_ROUTE, PAPER, INK, INVALID_NOTE }
    public record Result(Status status, Optional<ItemStack> note) {}
    private ResearchBookInventory() {}
    public static Result requestNote(ServerPlayer player, PlayerKnowledge knowledge, String key, int difficulty) {
        var catalog = ResearchCatalogs.catalog();
        var definition = catalog.find(key).filter(ResearchCatalogs::enabled);
        if (definition.isEmpty()) return result(Status.UNKNOWN);
        var entry = definition.orElseThrow();
        var rules = ResearchCatalogs.rules();
        var book = new ResearchBookActions(TCAspects.catalog(), catalog, ResearchCatalogs::enabled);
        if (rules.completed(knowledge, key) || !book.visible(knowledge, entry) || !rules.prerequisites(knowledge, key)) return result(Status.INELIGIBLE);
        if (ResearchBookActions.route(entry, difficulty) != ResearchBookActions.Route.NOTE) return result(Status.PURCHASE_ROUTE);
        var inventory=new java.util.ArrayList<>(player.getInventory().items);inventory.addAll(player.getInventory().offhand);
        for (var stack : inventory) if (stack.getItem() instanceof ResearchNotesItem) {
            var existing = ResearchNoteNbt.read(stack.getTag(), TCAspects.catalog()).note();
            if (existing.filter(note -> note.key().equals(key)).isPresent()) return new Result(Status.EXISTING, Optional.of(stack.copy()));
        }
        ItemStack ink = ItemStack.EMPTY, paper = ItemStack.EMPTY;
        for (var stack : inventory) {
            if (ink.isEmpty() && stack.getItem() instanceof thaumcraft.api.IScribeTools && ScribingToolsItem.ink(stack) > 0) ink = stack;
            if (paper.isEmpty() && stack.is(Items.PAPER)) paper = stack;
        }
        if (ink.isEmpty()) return result(Status.INK);
        if (paper.isEmpty()) return result(Status.PAPER);
        // Generate before debiting physical resources, so invalid definitions cannot lose paper or ink.
        var generated = new ResearchNotes(TCAspects.catalog()).generate(entry, player.serverLevel().random::nextInt, tc4revival.config.TCConfig.SERVER.randomizedResearchNotes.get());
        if (generated.isEmpty()) return result(Status.INVALID_NOTE);
        var output = ResearchNotesItem.create(generated.orElseThrow()); var evidence = output.copy();
        if(!player.getAbilities().instabuild)paper.shrink(1);
        // Ink is a refillable resource: an exhausted scribing tool must remain available for refilling.
        if (!player.getAbilities().instabuild) {
            if(ink.getItem() instanceof ScribingToolsItem)ink.hurt(1,player.getRandom(),player);
            else ((thaumcraft.api.IScribeTools)ink.getItem()).setInk(ink,ScribingToolsItem.ink(ink)-1);
        }
        var remainder=net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(
                new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(player.getInventory()),output,false);
        if(!remainder.isEmpty())player.drop(remainder,false);
        player.getInventory().setChanged(); player.inventoryMenu.broadcastChanges();
        return new Result(Status.CREATED, Optional.of(evidence));
    }
    private static Result result(Status status) { return new Result(status, Optional.empty()); }
}
