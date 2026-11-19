package dev.tc4.updater;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

public final class UpdateReview {
    public record Offer(String modid, String name, String currentVersion, String version, String changelog,
                        String original, String oldHash, ReleaseApi.Release release, List<UpdateTransaction.Original> duplicates) {}
    private static final Gson JSON = new Gson();
    private final Path game;
    private final ReleaseApi api;
    private final ReviewStore store;
    private final List<Offer> offers;
    private final UpdateEngine.Context context;
    private final Map<String,String> choices = new LinkedHashMap<>();
    public UpdateReview(Path game, ReleaseApi api, ReviewStore store, List<Offer> offers, UpdateEngine.Context context) {
        this.game=game; this.api=api; this.store=store; this.offers=List.copyOf(offers); this.context=context;
    }
    public synchronized String handle(String request) {
        String error = "";
        try {
            JsonObject input = JsonParser.parseString(request).getAsJsonObject();
            String action = input.get("action").getAsString();
            if (!action.equals("snapshot")) {
                if (!Set.of("update","skip","ignore").contains(action)) throw new IllegalArgumentException("Unknown update action");
                String id = input.get("modid").getAsString();
                Offer offer = offers.stream().filter(row -> row.modid.equals(id)).findFirst().orElseThrow();
                if (!choices.containsKey(id)) {
                    if (action.equals("update")) {
                        var approval = new ReviewStore.Approval(id,offer.original,offer.oldHash,offer.release,offer.duplicates);
                        for (var old : approval.originals()) {
                            Path original = game.resolve("mods").resolve(FilesSupport.localFilename(old.filename()));
                            FilesSupport.regularFile(original);
                            if (!FilesSupport.sha256(original).equals(old.hash())) throw new java.io.IOException("Installed JAR changed; restart to check again");
                        }
                        store.approve(approval,api);
                        if (id.equals(UpdateEngine.SELF)) {
                            UpdateEngine.checkSelfCandidate(game, approval.release(), store.staged(approval), context);
                            SelfUpdate.stage(game, approval.release().version(), UpdateEngine.transactionEntry(approval.originals(),approval.release()),store.staged(approval));
                            SelfUpdate.launch(game, ignored -> {});
                        }
                    } else if (action.equals("ignore")) store.ignore(id,offer.version);
                    choices.put(id,action);
                }
            }
        } catch (Exception failure) { error=String.valueOf(failure.getMessage()); }
        return JSON.toJson(Map.of("offers",offers,"choices",choices,"error",error));
    }
}
