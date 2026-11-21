package tc4revival.debug;

import com.mojang.authlib.GameProfile;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;
import tc4revival.research.*;

 class ResearchBookDebugCommands {
    private ResearchBookDebugCommands() {}
    static int materials(CommandSourceStack source) {
        try { return checkMaterials(source); }
        catch (RuntimeException failure) {
            tc4revival.TC4Revival.LOGGER.error("Material validation failed", failure);
            source.sendFailure(Component.literal("materials FAILED: " + failure.getMessage()));
            return 0;
        }
    }
    private static int checkMaterials(CommandSourceStack source) {
        var aer = TCAspects.catalog().require("aer"); var ignis = TCAspects.catalog().require("ignis"); var aqua = TCAspects.catalog().require("aqua");
        for (var type : MaterialType.values()) {
            var stack = new ItemStack(TCItems.material(type));
            require(stack.getItem() instanceof MaterialItem && stack.getMaxStackSize() == 64 && !stack.isDamageableItem(), "Wrong material item properties");
            var identity = tc4revival.aspect.item.ItemStackAspects.identity(stack).orElseThrow();
            require(identity.item().equals("Thaumcraft:ItemResource") && identity.metadata() == type.metadata(), "Wrong material legacy identity");
            for (String tag : type.tags()) require(stack.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse(tag))), "Missing material tag " + tag);
            try { verifyStoredAspects(stack, aer, ignis, aqua); }
            catch (RuntimeException failure) { throw new IllegalStateException(type + ": " + failure.getMessage(), failure); }
        }
        // Fragments belonged to the same old essentia-container class and must receive the same composition behavior.
        verifyStoredAspects(new ItemStack(TCItems.KNOWLEDGE_FRAGMENT.get()), aer, ignis, aqua);
        require(!MaterialType.DEFUNCT_BRAIN.creativeVisible(), "Defunct brain leaked into creative listing policy");
        source.sendSuccess(() -> Component.literal("materials PASS: 12 real materials, legacy identities/tags, stored-aspect roundtrip and scan composition; no inventory changes or client test"), false);
        return 1;
    }
    private static void verifyStoredAspects(ItemStack stack, tc4revival.aspect.Aspect aer, tc4revival.aspect.Aspect ignis, tc4revival.aspect.Aspect aqua) {
        var container = (StoredAspectItem) stack.getItem();
        var base = tc4revival.aspect.item.ItemStackAspects.resolve(stack).orElseThrow();
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        var uncappedBase = tc4revival.aspect.item.ItemAspects.baseSession().base(id).orElseGet(tc4revival.aspect.AspectAmounts::new);
        stack.getOrCreateTag().putString("unrelated", "keep");
        container.setStoredAspects(stack, new tc4revival.aspect.AspectAmounts().add(aer, 100).add(ignis, 0).add(aqua, -2));
        var stored = container.storedAspects(stack);
        require(stored.size() == 3 && stored.amount(aer) == 100 && stored.amount(ignis) == 0 && stored.amount(aqua) == -2, "Stored aspects lost zero/negative entries");
        stored.add(aer, 1); require(container.storedAspects(stack).amount(aer) == 100, "Shared mutable stored aspects");
        var calculated = tc4revival.aspect.item.ItemStackAspects.resolve(stack).orElseThrow();
        var positiveOnly = stack.copy(); container.setStoredAspects(positiveOnly, new tc4revival.aspect.AspectAmounts().add(aer, 100));
        require(calculated.amount(aer) == uncappedBase.amount(aer) + 100 && calculated.size() <= 6
                && calculated.view().equals(tc4revival.aspect.item.ItemStackAspects.resolve(positiveOnly).orElseThrow().view()),
                "Container composition: uncapped base=" + uncappedBase.view() + ", result=" + calculated.view());
        container.setStoredAspects(stack, new tc4revival.aspect.AspectAmounts());
        require(container.storedAspects(stack).isEmpty() && stack.getTag().getString("unrelated").equals("keep"), "Replacing aspects removed unrelated NBT");
        require(base.view().equals(tc4revival.aspect.item.ItemStackAspects.resolve(stack).orElseThrow().view()), "Clearing contents did not restore baseline aspects");
    }
    static int items(CommandSourceStack source) {
        var data = ResearchDisplayItems.bundled(); int resolved = 0, pending = 0;
        for (var entry : data.entries().values()) {
            var identity = entry.identity();
            var original = new ResearchDefinition.Stack(new tc4revival.aspect.recipe.RecipeAspectData.Key(identity.item(), identity.metadata()),
                    9, entry.cycleDamage(), identity.nbt(), java.util.Optional.empty());
            if (entry.missing().isPresent()) { require(data.resolve(original, 0).isEmpty(), "Missing dependency became a fake item"); pending++; continue; }
            for (int i = 0; i < entry.variants().size(); i++) {
                var stack = data.resolve(original, i * 1000L).orElseThrow();
                require(stack.getCount() == (entry.cycleCount() ? 1 : 9), "Wrong display count");
                require(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals(entry.variants().get(i).item()), "Wrong display variant");
            }
            var first = data.resolve(original, 1230).orElseThrow();
            first.setCount(1); first.getOrCreateTag().putString("tc_test_mutation", "isolated");
            var second = data.resolve(original, 1230).orElseThrow();
            require(!second.getOrCreateTag().contains("tc_test_mutation"), "Display tags share mutable state");
            if (entry.cycleDamage()) require(second.getDamageValue() == 123 % second.getMaxDamage(), "Wrong damage animation");
            resolved++;
        }
        var nugget = new ResearchDefinition.Stack(new tc4revival.aspect.recipe.RecipeAspectData.Key("Thaumcraft:ItemNugget", 0), 9, false,
                java.util.Optional.empty(), java.util.Optional.empty());
        require(data.resolve(nugget, 0).orElseThrow().is(Items.IRON_NUGGET), "Iron nugget was not vanilla");
        String result = "book_items PASS: resolved=" + resolved + ", pending=" + pending + "; real registered display stacks, no inventory changes or GPU test";
        source.sendSuccess(() -> Component.literal(result), false); return 1;
    }
    static int nbt(CommandSourceStack source) {
        var tags = new java.util.ArrayList<String>();
        for (String directory : java.util.List.of("research_catalog", "research_pages", "research_page_recipes")) {
            var id = net.minecraft.resources.ResourceLocation.parse("thaumcraft:" + directory + "/legacy.json");
            try (var reader = source.getServer().getResourceManager().getResourceOrThrow(id).openAsReader()) {
                collectNbt(com.google.gson.JsonParser.parseReader(reader), tags);
            } catch (java.io.IOException failure) { throw new IllegalStateException("Missing loaded book data " + id, failure); }
        }
        for (String tag : tags) {
            var parsed = LegacyStackNbt.read(tag);
            require(parsed.equals(LegacyStackNbt.read(parsed.toString())), "NBT modern roundtrip changed a tag");
        }
        var jar = ResearchCatalogs.catalog().require("NODEJAR").display().iconItem().orElseThrow().tag().orElseThrow();
        var aspects = jar.getList("Aspects", net.minecraft.nbt.Tag.TAG_COMPOUND);
        require(aspects.size() == 4 && aspects.getCompound(0).getString("key").equals("aer")
                && aspects.getCompound(3).getString("key").equals("terra") && aspects.getCompound(3).getInt("amount") == 40,
                "Node jar display lost aspect order or amounts");
        source.sendSuccess(() -> Component.literal("book_nbt PASS: stackTags=" + tags.size() + ", unique=" + tags.stream().distinct().count()
                + "; NODEJAR aspects preserved; loaded resources, no player changes or item-render test"), false);
        return 1;
    }
    private static void collectNbt(com.google.gson.JsonElement element, java.util.List<String> tags) {
        if (element.isJsonArray()) element.getAsJsonArray().forEach(value -> collectNbt(value, tags));
        else if (element.isJsonObject()) element.getAsJsonObject().entrySet().forEach(entry -> {
            if (entry.getKey().equals("nbt") && entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString())
                tags.add(entry.getValue().getAsString());
            collectNbt(entry.getValue(), tags);
        });
    }
    static int browser(CommandSourceStack source) {
        var catalog = ResearchCatalogs.catalog();
        var model = new ResearchBrowser(catalog, ResearchCatalogs::enabled);
        var knowledge = new PlayerKnowledge(TCAspects.catalog());
        require(model.categories(knowledge).stream().noneMatch(c -> c.key().equals("ELDRITCH")), "Eldritch shown before gate");
        int initial = model.categories(knowledge).stream().mapToInt(c -> model.view(knowledge, c.key(), 0).orElseThrow().nodes().size()).sum();
        for (var entry : catalog.all()) knowledge.rememberResearch(entry.key());
        int nodes = 0, links = 0, lineVertices = 0;
        for (var category : model.categories(knowledge)) {
            var view = model.view(knowledge, category.key(), 0).orElseThrow();
            nodes += view.nodes().size(); links += view.links().size();
            require(view.nodes().stream().allMatch(n -> n.state() == ResearchBrowser.State.COMPLETE), "Completed node not complete");
            require(view.links().stream().allMatch(l -> l.state() == ResearchBrowser.LinkState.COMPLETE), "Completed link not complete");
            for (var link : view.links()) {
                require(catalog.require(link.target()).category().equals(category.key())
                        && !catalog.require(link.target()).has(ResearchDefinition.Flag.VIRTUAL), "Invalid link target");
                for (var state : ResearchBrowser.LinkState.values()) {
                    var sample = ResearchBrowserLines.sample(new ResearchBrowser.Link(link.source(), link.target(), link.x(), link.y(),
                            link.targetX(), link.targetY(), link.kind(), state), 13.5f);
                    require(sample.stream().allMatch(p -> Float.isFinite(p.x()) && Float.isFinite(p.y())
                            && p.alpha() >= 0 && p.alpha() <= .6f), "Invalid browser line geometry");
                    lineVertices += sample.size();
                }
            }
        }
        long expected = catalog.all().stream().filter(ResearchCatalogs::enabled).filter(e -> !e.has(ResearchDefinition.Flag.VIRTUAL)).count();
        require(nodes == expected, "Missing completed nodes: " + nodes + "/" + expected);
        String result = "book_browser PASS: initialNodes=" + initial + ", completeNodes=" + nodes + ", links=" + links + ", lineVertices=" + lineVertices
                + "; loaded server catalog, isolated knowledge, no GUI test";
        source.sendSuccess(() -> Component.literal(result), false);
        return 1;
    }
    static int lifecycle(CommandSourceStack source) {
        var player = new FakePlayer(source.getServer().overworld(), new GameProfile(UUID.randomUUID(), "TC4BookLifecycle"));
        var data = new KnowledgeSavedData(TCAspects.catalog());
        var notifications = new java.util.ArrayList<ResearchBookActions.Change>();
        var registry = new ResearchBookSessions.Registry((owner, change) -> notifications.add(change));
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        try {
            player.setItemInHand(hand, new ItemStack(Items.BOOK));
            data.ensure(player.getUUID(), bound -> 0);
            data.update(player.getUUID(), bound -> 0, knowledge -> {
                ResearchCatalogs.progression().unlockAutomatic(knowledge, false);
                for (var cost : ResearchCatalogs.catalog().require("HUNGRYCHEST").aspects().entrySet())
                    knowledge.setPool(TCAspects.catalog().require(cost.getKey()), cost.getValue().shortValue());
                return null;
            });
            java.util.function.Supplier<ServerResearchBook> open = () -> new ServerResearchBook(player, hand, stack -> stack.is(Items.BOOK),
                    stack -> false, data, () -> new ServerResearchBook.Settings(0, false, false));
            var first = registry.replace(player, open);
            var second = registry.replace(player, open);
            require(!first.valid() && second.valid(), "Replacing opening kept old session");
            registry.close(player, first.token()); require(second.valid(), "Old close packet closed new opening");
            require(registry.request(player, first.token(), 0, "HUNGRYCHEST") instanceof ServerResearchBook.Denied, "Old token accepted by registry");
            require(notifications.isEmpty(), "Opening emitted completion popup");
            var intent = new tc4revival.network.BookActionPacket(second.token(), 0, tc4revival.network.BookActionPacket.Action.REQUEST, "HUNGRYCHEST");
            var wire = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                intent.encode(wire);
                var reply = tc4revival.network.BookActionPacket.decode(wire).dispatch(player, registry);
                wire.clear(); reply.encode(wire);
                require(tc4revival.network.BookResultPacket.decode(wire).status() == tc4revival.network.BookResultPacket.Status.PURCHASED, "Wire purchase rejected");
            } finally { wire.release(); }
            require(notifications.size() == 1 && notifications.get(0).completions().stream().anyMatch(c -> c.key().equals("HUNGRYCHEST")), "Missing committed completion notification");
            registry.request(player, second.token(), 0, "HUNGRYCHEST"); require(notifications.size() == 1, "Replay repeated completion notification");
            player.setItemInHand(hand, ItemStack.EMPTY); registry.sweep(source.getServer());
            player.setItemInHand(hand, new ItemStack(Items.BOOK));
            require(!second.valid() && registry.request(player, second.token(), 1, "HUNGRYCHEST") instanceof ServerResearchBook.Denied, "Sweep failed to discard invalid opening");
            var third = registry.replace(player, open); registry.clear(source.getServer()); require(!third.valid(), "Server clear retained opening");
            source.sendSuccess(() -> Component.literal("Book lifecycle PASS; replace invalidates old opening; old close/request token cannot affect new opening; purchase notification once; invalid held state swept; server clear closes sessions; isolated knowledge/inventory, no real player changes"), false);
            return 1;
        } finally { registry.clear(source.getServer()); player.getInventory().clearContent(); }
    }
    static int transactions(CommandSourceStack source) {
        var player = new FakePlayer(source.getServer().overworld(), new GameProfile(UUID.randomUUID(), "TC4BookTransaction"));
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var data = new KnowledgeSavedData(TCAspects.catalog());
        var config = new ServerResearchBook.Settings[] {new ServerResearchBook.Settings(0, false, false)};
        try {
            player.setItemInHand(hand, new ItemStack(Items.BOOK));
            data.ensure(player.getUUID(), bound -> 0);
            data.update(player.getUUID(), bound -> 0, knowledge -> {
                ResearchCatalogs.progression().unlockAutomatic(knowledge, false);
                knowledge.rememberResearch("@THAUMIUM");
                for (var cost : ResearchCatalogs.catalog().require("HUNGRYCHEST").aspects().entrySet())
                    knowledge.setPool(TCAspects.catalog().require(cost.getKey()), (short) (cost.getValue() + 2));
                return null;
            });
            var book = new ServerResearchBook(player, hand, stack -> stack.is(Items.BOOK), stack -> false, data, () -> config[0]);
            long before = data.revision(player.getUUID());
            var purchase = (ServerResearchBook.Purchase) book.request(player, book.token(), 0, "HUNGRYCHEST");
            require(purchase.result().accepted(), "Purchase rejected");
            var saved = data.snapshot(player.getUUID()).orElseThrow();
            require(saved.researchMarkers().contains("HUNGRYCHEST") && data.revision(player.getUUID()) == before + 1, "Purchase not committed exactly once");
            for (var cost : ResearchCatalogs.catalog().require("HUNGRYCHEST").aspects().entrySet())
                require(saved.pool(TCAspects.catalog().require(cost.getKey())) == 2, "Purchase fee mismatch");
            require(book.request(player, book.token(), 0, "HUNGRYCHEST") instanceof ServerResearchBook.Denied, "Replayed purchase accepted");
            require(data.revision(player.getUUID()) == before + 1, "Replay changed knowledge");
            player.getInventory().setItem(1, new ItemStack(TCItems.SCRIBING_TOOLS.get()));
            player.getInventory().setItem(2, new ItemStack(Items.PAPER, 2));
            var note = (ServerResearchBook.Note) book.request(player, book.token(), 1, "THAUMIUM");
            require(note.result().status() == ResearchBookInventory.Status.CREATED, "Primary research did not produce note");
            require(player.getInventory().getItem(2).getCount() == 1 && player.getInventory().getItem(1).getDamageValue() == 1, "Note did not debit paper/ink");
            require(data.revision(player.getUUID()) == before + 1 && !data.snapshot(player.getUUID()).orElseThrow().researchMarkers().contains("THAUMIUM"), "Note granted completion");
            require(book.request(player, book.token(), 1, "THAUMIUM") instanceof ServerResearchBook.Denied, "Replayed note accepted");
            var duplicate = (ServerResearchBook.Note) book.request(player, book.token(), 2, "THAUMIUM");
            require(duplicate.result().status() == ResearchBookInventory.Status.EXISTING && player.getInventory().getItem(2).getCount() == 1, "Existing note charged materials");
            config[0] = new ServerResearchBook.Settings(-1, false, false);
            var currentMode = book.request(player, book.token(), 3, "THAUMIUM");
            require(currentMode instanceof ServerResearchBook.Purchase, "Session cached old difficulty");
            require(!((ServerResearchBook.Purchase) currentMode).result().accepted() && data.revision(player.getUUID()) == before + 1, "Insufficient points changed knowledge");
            data.update(player.getUUID(), bound -> 0, knowledge -> {
                for (var cost : ResearchCatalogs.catalog().require("THAUMIUM").aspects().entrySet())
                    knowledge.setPool(TCAspects.catalog().require(cost.getKey()), cost.getValue().shortValue());
                return null;
            });
            require(((ServerResearchBook.Purchase) book.request(player, book.token(), 4, "THAUMIUM")).result().accepted(), "Session reused stale knowledge");
            book.close(); require(book.request(player, book.token(), 5, "THAUMIUM") instanceof ServerResearchBook.Denied, "Closed book performed operation");
            source.sendSuccess(() -> Component.literal("Book transactions PASS; current server settings select route; knowledge purchase commits once; note creation debits physical paper/ink; duplicate/replay/close checks; isolated knowledge only, no real player records modified"), false);
            return 1;
        } finally { player.getInventory().clearContent(); }
    }
    static int session(CommandSourceStack source) {
        var player = new FakePlayer(source.getServer().overworld(), new GameProfile(UUID.randomUUID(), "TC4BookSession"));
        var other = new FakePlayer(source.getServer().overworld(), new GameProfile(UUID.randomUUID(), "TC4OtherSession"));
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        try {
            player.setItemInHand(hand, new ItemStack(Items.BOOK));
            var session = new ServerResearchBookSession(player, hand, stack -> stack.is(Items.BOOK));
            require(session.authorize(other, session.token(), 0) == ServerResearchBookSession.Rejection.PLAYER, "Foreign player accepted");
            require(session.authorize(player, UUID.randomUUID(), 0) == ServerResearchBookSession.Rejection.TOKEN, "Foreign token accepted");
            require(session.authorize(player, session.token(), 1) == ServerResearchBookSession.Rejection.SEQUENCE, "Skipped sequence accepted");
            require(session.authorize(player, session.token(), 0) == ServerResearchBookSession.Rejection.NONE, "Valid opening rejected");
            require(session.authorize(player, session.token(), 0) == ServerResearchBookSession.Rejection.SEQUENCE, "Replay accepted");
            require(session.nextSequence() == 1, "Rejected attempts consumed sequence");
            player.getItemInHand(hand).getOrCreateTag().putBoolean("changed", true);
            require(session.authorize(player, session.token(), 1) == ServerResearchBookSession.Rejection.CONTEXT, "Changed book accepted");
            player.getItemInHand(hand).setTag(null);
            require(!session.valid(), "Invalidated session revived");
            var reopened = new ServerResearchBookSession(player, hand, stack -> stack.is(Items.BOOK));
            require(reopened.authorize(player, session.token(), 0) == ServerResearchBookSession.Rejection.TOKEN, "Old opening token reused");
            reopened.close(); require(!reopened.valid(), "Closed session remained valid");
            var changedSlot = new ServerResearchBookSession(player, hand, stack -> stack.is(Items.BOOK));
            player.getInventory().setItem(1, new ItemStack(Items.BOOK)); player.getInventory().selected = 1;
            require(!changedSlot.valid(), "Slot change accepted identical book");
            var lostBook = new ServerResearchBookSession(player, hand, stack -> stack.is(Items.BOOK));
            player.setItemInHand(hand, ItemStack.EMPTY); require(!lostBook.valid(), "Missing book accepted");
            source.sendSuccess(() -> Component.literal("Book session PASS; actual temporary inventory ownership, token/sequence replay, changed NBT, slot change, lost book and close checks; no real player data changed; book registration/network integration pending"), false);
            return 1;
        } finally { player.getInventory().clearContent(); other.getInventory().clearContent(); }
    }
    static int pages(CommandSourceStack source) {
        var pages = ResearchPages.bundled(); pages.validate(ResearchCatalogs.catalog());
        var recipes = ResearchPageRecipes.bundled(); recipes.validate(pages);
        require(recipes.entries().size() == 347, "Named page recipe count");
        for (var parent : java.util.Map.of("CAP_copper", "BASICTHAUMATURGY", "PURECOPPER", "PUREIRON", "TRANSCOPPER", "TRANSIRON").entrySet()) {
            var research = ResearchCatalogs.catalog().require(parent.getKey());
            require(research.parents().equals(java.util.List.of(parent.getValue())) && research.complexity() == 1
                    && research.display().pageCount() == 2, "Copper research parent or page count");
            boolean secondary = !parent.getKey().equals("CAP_copper");
            require(research.has(ResearchDefinition.Flag.CONCEALED) == secondary
                    && research.has(ResearchDefinition.Flag.SECONDARY) == secondary, "Copper research visibility or purchase rule");
        }
        require(ResearchCatalogs.catalog().require("PURECOPPER").aspects().equals(java.util.Map.of("metallum", 3, "ordo", 2, "permutatio", 1))
                && ResearchCatalogs.catalog().require("TRANSCOPPER").aspects().equals(java.util.Map.of("metallum", 3, "permutatio", 3)), "Copper research costs");
        var goggles = (ResearchPageRecipes.Infusion) recipes.require("HelmGoggles").definition();
        require(goggles.output() instanceof ResearchPage.Sequence sequence && sequence.elements().get(1) instanceof ResearchPage.Nbt,
                "Infusion tag modification lost");
        var label = (ResearchPageRecipes.Crafting) recipes.require("JarLabel0").definition();
        require(label.matchesInputNbt(), "Label ingredient NBT comparison lost");
        var textParts = ResearchText.parse("text<LINE><IMG>thaumcraft:textures/items/amber.png:0:0:255:255:0.0625</IMG>");
        var picture = textParts.stream().filter(ResearchText.Picture.class::isInstance).map(ResearchText.Picture.class::cast).findFirst().orElseThrow();
        require(picture.xOffset(139) == 58.0625f && picture.nextY(100, 9) == 115, "Old image geometry changed");
        require(ResearchText.class.getResource("/assets/thaumcraft/textures/gui/gui_researchbook.png") != null, "Missing book illustration resource");
        int count = pages.entries().values().stream().mapToInt(java.util.List::size).sum();
        require(count == 576, "Static page count");
        int concealed = 0;
        for (var entry : pages.entries().entrySet()) {
            var shown = pages.visible(entry.getKey(), key -> false, java.util.Map.of());
            concealed += entry.getValue().size() - shown.size();
        }
        require(concealed == 15, "Concealed page filtering");
        var sceptres = (ResearchPage.Sequence) pages.require("SCEPTRE").get(1).recipe();
        require(sceptres.elements().size() == 3, "Sceptre example count");
        for (var example : sceptres.elements()) {
            var recipe = (ResearchPage.Arcane) example;
            require(recipe.output().nbt().orElseThrow().contains("sceptre:1b"), "Lost sceptre NBT");
            require(recipe.ingredients().elements().size() == 9, "Lost shaped cells");
        }
        var runic = (ResearchPage.Sequence) pages.require("RUNICAUGMENTATION").get(1).recipe();
        require(runic.elements().size() == 5, "Runic example count");
        for (int i = 0; i < 5; i++) {
            var recipe = (ResearchPage.RunicExample) runic.elements().get(i);
            require(recipe.aspects().get("potentia") == (32 << i), "Runic cost");
            require(recipe.output().nbt().orElseThrow().contains("RS.HARDEN:" + (i + 1) + "b"), "Runic output");
        }
        source.sendSuccess(() -> Component.literal("Research book pages PASS; 194 entries, 576 static pages, 347 named recipes resolved, 15 conditional pages, 3 sceptre and 5 runic display examples with NBT; infusion tag outputs and label NBT matching retained; display data only, recipe gameplay and client rendering remain pending"), false);
        return 1;
    }
    static int verify(CommandSourceStack source) {
        var catalog = ResearchCatalogs.catalog(); var aspects = TCAspects.catalog();
        var book = new ResearchBookActions(aspects, catalog, ResearchCatalogs::enabled);
        var knowledge = PlayerKnowledge.firstLogin(aspects, bound -> 0);
        ResearchCatalogs.progression().unlockAutomatic(knowledge, false);
        knowledge.rememberResearch("@THAUMIUM");
        var hungry = catalog.require("HUNGRYCHEST");
        for (var cost : hungry.aspects().entrySet()) knowledge.setPool(aspects.require(cost.getKey()), (short) (cost.getValue() + 2));
        var purchase = book.purchase(knowledge, "HUNGRYCHEST", 0, false).change().orElseThrow();
        require(purchase.knowledge().researchMarkers().contains("HUNGRYCHEST") && !knowledge.researchMarkers().contains("HUNGRYCHEST"), "Purchase mutated source or failed");
        for (var cost : hungry.aspects().entrySet()) require(purchase.knowledge().pool(aspects.require(cost.getKey())) == 2, "Exact purchase fee");
        require(book.purchase(knowledge, "HUNGRYCHEST", 1, false).rejection().orElseThrow() == ResearchBookActions.Rejection.NEEDS_NOTE, "Hard-mode bypass");
        require(book.purchase(knowledge, "THAUMIUM", 0, false).rejection().orElseThrow() == ResearchBookActions.Rejection.NEEDS_NOTE, "Normal primary bypass");
        var cheated = book.open(knowledge, true, true, true);
        require(cheated.knowledge().knownAspects().size() == 48 && cheated.knowledge().totalWarp() == 0, "Cheat known aspects/wuss");
        require(catalog.all().stream().filter(ResearchCatalogs::enabled).allMatch(e -> cheated.knowledge().researchMarkers().contains(e.key())), "Cheat missed enabled research");
        require(!book.open(cheated.knowledge(), true, true, true).changed(), "Repeated cheat opening changed points");
        inventory(source, knowledge);
        source.sendSuccess(() -> Component.literal("Research book actions PASS; real catalog purchase costs/difficulty/cheat rules; actual fake-player note creation, last ink, existing discovery, missing materials, creative paper/ink and full-inventory drop; no real player knowledge/inventory modified"), false);
        return 1;
    }
    private static void inventory(CommandSourceStack source, PlayerKnowledge knowledge) {
        var level = source.getServer().overworld(); var area = new AABB(-4, 196, -4, 5, 206, 5);
        require(level.players().stream().noneMatch(p -> p.getBoundingBox().intersects(area.inflate(12))), "Player near book inventory fixture");
        require(level.hasChunkAt(new net.minecraft.core.BlockPos(0, 200, 0)), "Book fixture chunk not loaded");
        Set<UUID> oldDrops = level.getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
        var player = new FakePlayer(level, new GameProfile(UUID.nameUUIDFromBytes("tc4revival:book-inventory-probe".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "TC4BookProbe"));
        player.setPos(0.5, 200, 0.5);
        try {
            require(ResearchBookInventory.requestNote(player, knowledge, "HUNGRYCHEST", 0).status() == ResearchBookInventory.Status.PURCHASE_ROUTE, "Secondary note bypass");
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(TCItems.SCRIBING_TOOLS.get()));
            require(ResearchBookInventory.requestNote(player, knowledge, "THAUMIUM", 0).status() == ResearchBookInventory.Status.INK, "Modern offhand entered old main-inventory search");
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
            require(ResearchBookInventory.requestNote(player, knowledge, "THAUMIUM", 0).status() == ResearchBookInventory.Status.INK, "Missing ink accepted");
            var ink = new ItemStack(TCItems.SCRIBING_TOOLS.get()); ink.setDamageValue(99); player.getInventory().setItem(0, ink);
            require(ResearchBookInventory.requestNote(player, knowledge, "THAUMIUM", 0).status() == ResearchBookInventory.Status.PAPER && ink.getDamageValue() == 99, "No paper charged ink");
            player.getInventory().setItem(1, new ItemStack(Items.PAPER, 2));
            var created = ResearchBookInventory.requestNote(player, knowledge, "THAUMIUM", 0);
            require(created.status() == ResearchBookInventory.Status.CREATED && ink.getDamageValue() == 100 && !ink.isEmpty()
                    && player.getInventory().getItem(1).getCount() == 1, "Survival paper/last ink");
            var note = ResearchNoteNbt.read(created.note().orElseThrow().getTag(), TCAspects.catalog()).note().orElseThrow();
            require(note.key().equals("THAUMIUM") && !note.complete() && note.grid().values().stream().filter(c -> c.type() == 1).count() == 2, "Generated actual research grid");
            require(ResearchBookInventory.requestNote(player, knowledge, "THAUMIUM", 0).status() == ResearchBookInventory.Status.EXISTING, "Repeated note charged twice");
            player.getInventory().clearContent();
            player.getInventory().setItem(5, ResearchNotesItem.create(new ResearchNote(note.key(), note.color(), true, note.copies(), note.grid(), note.layoutSize())));
            require(ResearchBookInventory.requestNote(player, knowledge, "THAUMIUM", 0).status() == ResearchBookInventory.Status.EXISTING, "Existing discovery not recognized");
            player.getInventory().clearContent(); player.getAbilities().instabuild = true;
            ink = new ItemStack(TCItems.SCRIBING_TOOLS.get()); ink.setDamageValue(99); player.getInventory().setItem(0, ink);
            player.getInventory().setItem(1, new ItemStack(Items.PAPER, 2));
            require(ResearchBookInventory.requestNote(player, knowledge, "THAUMIUM", 0).status() == ResearchBookInventory.Status.CREATED
                    && ink.getDamageValue() == 99 && player.getInventory().getItem(1).getCount() == 1, "Creative must spend paper and spare ink");
            player.getInventory().clearContent(); player.getAbilities().instabuild = false;
            for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
            player.getInventory().setItem(0, new ItemStack(TCItems.SCRIBING_TOOLS.get())); player.getInventory().setItem(1, new ItemStack(Items.PAPER, 64));
            require(ResearchBookInventory.requestNote(player, knowledge, "THAUMIUM", 0).status() == ResearchBookInventory.Status.CREATED, "Full inventory note failed");
            var drops = level.getEntitiesOfClass(ItemEntity.class, area).stream().filter(e -> !oldDrops.contains(e.getUUID())).toList();
            require(drops.size() == 1 && drops.get(0).getItem().is(TCItems.RESEARCH_NOTES.get()) && drops.get(0).getItem().getCount() == 1, "Full inventory must drop exactly one note");
        } finally {
            player.getInventory().clearContent();
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, area)) if (!oldDrops.contains(entity.getUUID())) entity.discard();
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
