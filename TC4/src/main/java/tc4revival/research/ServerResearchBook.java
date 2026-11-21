package tc4revival.research;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.TCAspects;
import tc4revival.content.ResearchBookInventory;

/** One authenticated opening, operating only on current server knowledge and current server settings. */
public  class ServerResearchBook {
    public record Settings(int difficulty, boolean allowCheatBook, boolean disableWarp) {
        public Settings { if (difficulty < -1 || difficulty > 1) throw new IllegalArgumentException("Invalid research difficulty"); }
    }
    public sealed interface Outcome permits Denied, Purchase, Note {}
    public record Denied(ServerResearchBookSession.Rejection reason) implements Outcome {}
    public record Purchase(ResearchBookActions.Result result) implements Outcome {}
    public record Note(ResearchBookInventory.Result result) implements Outcome {}
    private final ServerPlayer player;
    private final KnowledgeSavedData data;
    private final Supplier<Settings> settings;
    private final ServerResearchBookSession session;
    private final ResearchBookActions.Change opening;

    /** Dependencies must be supplied by server item/session code, never decoded from a client packet. */
    public ServerResearchBook(ServerPlayer player, InteractionHand hand, Predicate<ItemStack> allowedBook,
            Predicate<ItemStack> cheatBook, KnowledgeSavedData data, Supplier<Settings> settings) {
        this(player,new ServerResearchBookSession(player,hand,allowedBook),cheatBook.test(player.getItemInHand(hand)),data,settings);
    }
    public ServerResearchBook(ServerPlayer player,int slot,Predicate<ItemStack> allowedBook,
            KnowledgeSavedData data,Supplier<Settings> settings) {
        this(player,new ServerResearchBookSession(player,slot,allowedBook),false,data,settings);
    }
    private ServerResearchBook(ServerPlayer player,ServerResearchBookSession session,boolean cheat,
            KnowledgeSavedData data,Supplier<Settings> settings) {
        this.player = player; this.data = data; this.settings = settings;
        this.session = session;
        var config = settings.get();
        data.ensure(player.getUUID(), player.getRandom()::nextInt);
        long revision = data.revision(player.getUUID());
        opening = actions().open(data.snapshot(player.getUUID()).orElseThrow(), cheat,
                config.allowCheatBook() || player.getAbilities().instabuild, config.disableWarp());
        boolean migrated = ResearchGuidance.migrate(opening.knowledge(), ResearchCatalogs.catalog(), ResearchRequirementData::server);
        if (opening.changed() || migrated) commit(revision, opening);
    }
    public UUID token() { return session.token(); }
    public boolean ownedBy(ServerPlayer candidate) { return player == candidate; }
    public ResearchBookActions.Change opening() { return opening; }
    public boolean valid() { return session.valid(); }
    public void close() { session.close(); }
    public Outcome request(ServerPlayer sender, UUID token, long sequence, String key) {
        return request(sender,token,sequence,key,false);
    }
    public Outcome request(ServerPlayer sender, UUID token, long sequence, String key, boolean creative) {
        return request(sender, token, sequence, key, creative, false);
    }
    public Outcome request(ServerPlayer sender, UUID token, long sequence, String key, boolean creative, boolean listen) {
        var authorization = session.authorize(sender, token, sequence);
        if (authorization != ServerResearchBookSession.Rejection.NONE) return new Denied(authorization);
        var config = settings.get();
        long revision = data.revision(player.getUUID());
        var knowledge = data.snapshot(player.getUUID()).orElseThrow();
        if(creative) {
            if(!sender.getAbilities().instabuild)return new Denied(ServerResearchBookSession.Rejection.PLAYER);
            var change=actions().completeCreative(knowledge,key);
            if(change.changed())commit(revision,change);
            return new Purchase(new ResearchBookActions.Result(java.util.Optional.of(change),java.util.Optional.empty()));
        }
        var definition = ResearchCatalogs.catalog().find(key);
        if (listen) {
            if (definition.isEmpty())
                return new Purchase(new ResearchBookActions.Result(java.util.Optional.empty(), java.util.Optional.of(ResearchBookActions.Rejection.HIDDEN)));
            var result = ResearchGuidance.listen(definition.get(), knowledge, ResearchCatalogs.catalog(), TCAspects.catalog(),
                    ResearchRequirementData::server,player.getRandom()::nextInt);
            result.change().ifPresent(change -> commit(revision, change));
            return new Purchase(result);
        }
        // Clients choose a research key, never a difficulty, cost, completion list or purchase-vs-note path.
        if (definition.isPresent() && definition.get().tutorial()) {
            var result = actions().readTutorial(knowledge, key);
            result.change().ifPresent(change -> commit(revision, change));
            return new Purchase(result);
        }
        if (definition.isPresent() && ResearchBookActions.route(definition.get(), config.difficulty()) == ResearchBookActions.Route.NOTE) {
            return new Note(ResearchBookInventory.requestNote(player, knowledge, key, config.difficulty()));
        }
        var purchase = actions().purchase(knowledge, key, config.difficulty(), config.disableWarp());
        purchase.change().ifPresent(change -> commit(revision, change));
        return new Purchase(purchase);
    }
    private ResearchBookActions actions() { return new ResearchBookActions(TCAspects.catalog(), ResearchCatalogs.catalog(), ResearchCatalogs::enabled); }
    private void commit(long revision, ResearchBookActions.Change change) {
        if (!data.replace(player.getUUID(), revision, change.knowledge())) {
            session.close();
            throw new IllegalStateException("Research book knowledge changed before commit");
        }
    }
}
