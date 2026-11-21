package forgottenrelics;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

import tc4revival.aspect.TCAspects;
import tc4revival.content.*;
import tc4revival.research.*;

import top.theillusivec4.curios.api.*;

import java.util.*;

@GameTestHolder(ForgottenRelics.ID)
@PrefixGameTestTemplate(false)
public class RelicsServerTests {
    static ServerPlayer player(GameTestHelper h) {
        var profile = new GameProfile(UUID.randomUUID(), "RelicTest");
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile);
        // Only the transport is stubbed: preserve real damage, death and teleport logic.
        p.connection =
                new net.minecraft.server.network.ServerGamePacketListenerImpl(
                        h.getLevel().getServer(),
                        new net.minecraft.network.Connection(
                                net.minecraft.network.protocol.PacketFlow.SERVERBOUND),
                        p) {
                    @Override
                    public void send(net.minecraft.network.protocol.Packet<?> packet) {}

                    @Override
                    public void send(
                            net.minecraft.network.protocol.Packet<?> packet,
                            net.minecraft.network.PacketSendListener listener) {}
                };
        p.setGameMode(GameType.SURVIVAL);
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 3))));
        for (int i = 0; i < 61; i++) p.tick();
        return p;
    }

    static ItemStack stack(Relic r) {
        return new ItemStack(ForgottenRelics.item(r));
    }

    static void equip(ServerPlayer p, Relic r, int slot) {
        var inv = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        inv.reset();
        inv.getStacksHandler(r.slot).orElseThrow().getStacks().setStackInSlot(slot, stack(r));
    }

    static JsonObject json(String path) {
        try (var in = RelicsServerTests.class.getResourceAsStream(path);
                var reader =
                        new java.io.InputStreamReader(
                                Objects.requireNonNull(in),
                                java.nio.charset.StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GameTest(template = "empty")
    public static void allOriginalRecipesAndResearchResolve(GameTestHelper h) {
        var resources =
                h.getLevel()
                        .getServer()
                        .getResourceManager()
                        .listResources(
                                "recipes",
                                id ->
                                        id.getNamespace().equals(ForgottenRelics.ID)
                                                && id.getPath().endsWith(".json"));
        int infusions = 0;
        for (var id : resources.keySet()) {
            var key = ForgottenRelics.id(id.getPath().substring(8, id.getPath().length() - 5));
            var recipe = h.getLevel().getRecipeManager().byKey(key).orElse(null);
            h.assertTrue(recipe != null, "Real recipe manager loaded " + key);
            if (!(recipe instanceof InfusionRecipe infusion)) continue;
            infusions++;
            var definition = json("/data/forgottenrelics/" + id.getPath());
            ItemStack center = ingredient(definition.getAsJsonObject("center"));
            var components = new ArrayList<ItemStack>();
            for (var element : definition.getAsJsonArray("components"))
                components.add(ingredient(element.getAsJsonObject()));
            var input = new InfusionRecipe.Input(center, components, k -> true);
            h.assertTrue(
                    infusion.matches(input, h.getLevel()), "Original ingredients match " + key);
            h.assertTrue(
                    !infusion.matches(
                            new InfusionRecipe.Input(center, components, k -> false), h.getLevel()),
                    "Research gate enforced " + key);
            h.assertTrue(
                    !infusion.assemble(input, h.getLevel().registryAccess()).isEmpty(),
                    "Actual infusion output exists " + key);
        }
        h.assertTrue(infusions == 33, "All 33 original infusions load; got " + infusions);
        var catalog =
                json("/data/forgottenrelics/research_catalog/main.json")
                        .getAsJsonArray("categories")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonArray("research");
        h.assertTrue(catalog.size() == 35, "35 research definitions");
        for (var value : catalog) {
            var entry = value.getAsJsonObject();
            checkItem(h, entry.getAsJsonObject("icon_item").get("item").getAsString());
            for (String group : List.of("parents", "parentsHidden"))
                if (entry.has(group))
                    for (var parent : entry.getAsJsonArray(group))
                        h.assertTrue(
                                ResearchCatalogs.catalog().find(parent.getAsString()).isPresent(),
                                "Valid prerequisite " + parent);
        }
        var triggers = json("/data/forgottenrelics/forgotten_knowledge.json");
        for (var entry : triggers.entrySet())
            for (var item : entry.getValue().getAsJsonArray())
                checkItem(h, item.getAsJsonObject().get("item").getAsString());
        h.succeed();
    }

    static void checkItem(GameTestHelper h, String id) {
        h.assertTrue(
                ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(id)),
                "Registered item " + id);
    }

    static ItemStack ingredient(JsonObject json) {
        var stack = ShapedRecipe.itemStackFromJson(json.getAsJsonObject("ingredient"));
        if (json.has("nbt"))
            try {
                stack.setTag(TagParser.parseTag(json.get("nbt").getAsString()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        return stack;
    }

    @GameTest(template = "empty")
    public static void xpTransferConservesActualPointsAcrossLevelBoundary(GameTestHelper h) {
        var p = player(h);
        p.experienceLevel = 17;
        p.experienceProgress = 3f / p.getXpNeededForNextLevel();
        p.totalExperience = 99999;
        var tome = stack(Relic.XP_TOME);
        tome.getOrCreateTag().putBoolean("active", true);
        for (int i = 0; i < 80; i++) RelicPassives.inventory(Relic.XP_TOME, tome, p);
        h.assertTrue(
                tome.getOrCreateTag().getLong("experience") == 397
                        && p.experienceLevel == 0
                        && p.experienceProgress == 0,
                "Stores current 397 XP rather than lifetime counter");
        tome.getOrCreateTag().putBoolean("extract", true);
        for (int i = 0; i < 80; i++) RelicPassives.inventory(Relic.XP_TOME, tome, p);
        h.assertTrue(
                tome.getOrCreateTag().getLong("experience") == 0
                        && p.experienceLevel == 17
                        && Math.round(p.experienceProgress * p.getXpNeededForNextLevel()) == 3,
                "All XP returned across multiple level boundaries");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oblivionBindingAndThreeModesPreserveOtherItems(GameTestHelper h) {
        var menu =
                new AbstractContainerMenu(null, 0) {
                    public boolean stillValid(Player p) {
                        return true;
                    }

                    public ItemStack quickMoveStack(Player p, int i) {
                        return ItemStack.EMPTY;
                    }
                };
        var grid = new TransientCraftingContainer(menu, 2, 2);
        grid.setItem(0, stack(Relic.OBLIVION_STONE));
        grid.setItem(1, new ItemStack(Items.COBBLESTONE));
        var recipe =
                new OblivionRecipe(
                        ForgottenRelics.id("test"),
                        net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        var stone = recipe.assemble(grid, h.getLevel().registryAccess());
        h.assertTrue(
                OblivionRecipe.filters(stone).getString(0).equals("minecraft:cobblestone"),
                "Binding stores stable ID");
        h.assertTrue(
                recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty),
                "No sample duplication");
        grid.setItem(0, stone);
        h.assertTrue(!recipe.matches(grid, h.getLevel()), "Duplicate binding rejected");
        grid.setItem(1, ItemStack.EMPTY);
        h.assertTrue(
                OblivionRecipe.filters(recipe.assemble(grid, h.getLevel().registryAccess()))
                        .isEmpty(),
                "Solo stone clears bindings");
        var p = player(h);
        p.tickCount = 10;
        p.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        p.getInventory().setItem(1, new ItemStack(Items.COBBLESTONE, 64));
        p.getInventory().setItem(2, new ItemStack(Items.DIAMOND));
        stone.getOrCreateTag().putInt("mode", 1);
        OblivionRecipe.tick(stone, p);
        h.assertTrue(
                p.getInventory().getItem(0).isEmpty()
                        && p.getInventory().getItem(1).getCount() == 64
                        && p.getInventory().getItem(2).is(Items.DIAMOND),
                "Keep largest stack, preserve unrelated items");
        stone.getOrCreateTag().putInt("mode", 2);
        OblivionRecipe.tick(stone, p);
        h.assertTrue(p.getInventory().getItem(1).getCount() == 64, "Not full does not delete");
        for (int i = 0; i < 36; i++)
            if (p.getInventory().getItem(i).isEmpty())
                p.getInventory().setItem(i, new ItemStack(Items.DIRT));
        OblivionRecipe.tick(stone, p);
        h.assertTrue(p.getInventory().getItem(1).isEmpty(), "Full mode frees one matching slot");
        p.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE));
        stone.getOrCreateTag().putInt("mode", 0);
        OblivionRecipe.tick(stone, p);
        h.assertTrue(
                p.getInventory().getItem(0).isEmpty()
                        && p.getInventory().getItem(2).is(Items.DIAMOND),
                "Delete-all only affects bound type");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void wandPaymentUsesCentivisAndRejectsPartialPayment(GameTestHelper h) {
        var p = player(h);
        var wand =
                TCItems.CASTING_WAND
                        .get()
                        .create(WandParts.Rod.SILVERWOOD, WandParts.Cap.THAUMIUM, false, true);
        p.getInventory().setItem(1, wand);
        var fire = TCAspects.catalog().require("ignis");
        int before = TCItems.CASTING_WAND.get().vis(wand, fire);
        h.assertTrue(Relics.pay(p, 1, 0, 0, 100, 0, 0, 0), "Pay one whole vis as 100 centivis");
        int spent = before - TCItems.CASTING_WAND.get().vis(wand, fire);
        h.assertTrue(spent > 0 && spent <= 100, "Discounted one-vis payment, not 100 vis");
        var copy = wand.copy();
        h.assertTrue(
                !Relics.pay(p, 1, 999999, 0, 100, 0, 0, 0) && ItemStack.matches(copy, wand),
                "Insufficient mixed payment does not drain any aspect");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void trueDamageAndChaosTransfersCannotRecurse(GameTestHelper h) {
        var p = player(h);
        p.getInventory().setItem(0, stack(Relic.FALSE_JUSTICE));
        p.setHealth(20);
        p.hurt(p.damageSources().generic(), 3);
        h.assertTrue(
                Math.abs(p.getHealth() - 14) < .01,
                "Ordinary incoming 3 becomes exactly 6 true damage");
        p.getInventory().setItem(0, ItemStack.EMPTY);
        p.getInventory().setItem(1, stack(Relic.CHAOS_CORE));
        p.invulnerableTime = 0;
        float before = p.getHealth();
        RelicsEvents.transfer(p, p.damageSources().generic(), 2);
        h.assertTrue(
                Math.abs(p.getHealth() - (before - 2)) < .01 && !RelicsEvents.redirected(),
                "Transferred hit is not rerolled or recursively redirected; guard restored");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void omegaPreventsDeathAndFateRequiresPaidVis(GameTestHelper h) {
        var p = player(h);
        p.getInventory().setItem(0, stack(Relic.OMEGA_CORE));
        p.hurt(Relics.damage(p.level(), "fate", null), 40000);
        h.assertTrue(p.isAlive() && p.getHealth() == 1, "Omega revives at one health");
        var unpaid = player(h);
        unpaid.getInventory().setItem(0, stack(Relic.FATE_TOME));
        unpaid.hurt(unpaid.damageSources().genericKill(), 1000);
        h.assertTrue(unpaid.isDeadOrDying(), "Fate cannot revive without vis");
        var paid = player(h);
        var tome = stack(Relic.FATE_TOME);
        paid.getInventory().setItem(0, tome);
        paid.getInventory()
                .setItem(
                        1,
                        TCItems.CASTING_WAND
                                .get()
                                .create(
                                        WandParts.Rod.SILVERWOOD,
                                        WandParts.Cap.THAUMIUM,
                                        false,
                                        true));
        paid.hurt(paid.damageSources().genericKill(), 1000);
        h.assertTrue(
                paid.isAlive()
                        && paid.getHealth() == paid.getMaxHealth()
                        && tome.getOrCreateTag().getInt("fate_cooldown") >= 600,
                "Paid Fate revives and starts cooldown");
        paid.invulnerableTime = 0;
        paid.hurt(paid.damageSources().genericKill(), 1000);
        h.assertTrue(paid.isDeadOrDying(), "Fate cannot revive again during cooldown");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void curioSlotRevivalAndFireHealing(GameTestHelper h) {
        var p = player(h);
        equip(p, Relic.DORMANT_ARCANUM, 0);
        var dormant = Relics.worn(p, Relic.DORMANT_ARCANUM);
        dormant.getOrCreateTag().putInt("lifetime", 0);
        CuriosApi.getCurio(dormant)
                .resolve()
                .orElseThrow()
                .curioTick(new SlotContext("necklace", p, 0, false, true));
        h.assertTrue(Relics.wearing(p, Relic.ARCANUM), "Dormant core wakes in actual Curios slot");
        equip(p, Relic.DARK_SUN_RING, 0);
        p.setHealth(8);
        p.hurt(p.damageSources().inFire(), 3);
        h.assertTrue(p.getHealth() == 11, "Fire is canceled and heals");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mirrorUsesFlooredCoordinatesAndFailsClosed(GameTestHelper h) {
        var p = player(h);
        p.moveTo(-3.25, 70, -5.75);
        var mirror = stack(Relic.DIMENSIONAL_MIRROR);
        p.setItemInHand(InteractionHand.MAIN_HAND, mirror);
        p.setShiftKeyDown(true);
        RelicUse.begin(Relic.DIMENSIONAL_MIRROR, p.level(), p, InteractionHand.MAIN_HAND);
        var dest = mirror.getOrCreateTag().getCompound("destination");
        h.assertTrue(
                dest.getInt("x") == -4 && dest.getInt("z") == -6, "Negative coordinates use floor");
        var before = p.position();
        dest.putString("dimension", "absent:dimension");
        p.setShiftKeyDown(false);
        var result =
                RelicUse.begin(Relic.DIMENSIONAL_MIRROR, p.level(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(
                result.getResult() == InteractionResult.FAIL && p.position().equals(before),
                "Missing dimension does not move player");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void missileCrossesWallAndCrimsonStopsAtWall(GameTestHelper h) {
        var p = player(h);
        p.moveTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(3, 2, 3))));
        p.setYRot(0);
        p.setXRot(0);
        for (int x = 0; x < 7; x++)
            for (int y = 1; y < 6; y++) h.setBlock(new BlockPos(x, y, 6), Blocks.STONE);
        var victim = h.spawn(EntityType.COW, new BlockPos(3, 2, 10));
        victim.setNoAi(true);
        victim.setNoGravity(true);
        victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        victim.setHealth(200);
        var missile = RelicProjectiles.fire(p, Relic.MISSILE_TOME, null);
        var crimson = RelicProjectiles.fire(p, Relic.CRIMSON_SPELL, null);
        crimson.setDeltaMovement(0, 0, 1.5);
        h.runAfterDelay(
                40,
                () -> {
                    h.assertTrue(
                            victim.getHealth() < 200, "Phasing missile damages target beyond wall");
                    h.assertTrue(
                            crimson.isRemoved(), "Crimson projectile is stopped by solid wall");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void damageArmorAndParadoxUseOriginalSemantics(GameTestHelper h) {
        boolean pvp = h.getLevel().getServer().isPvpAllowed();
        h.getLevel().getServer().setPvpAllowed(true);
        try {
            var cow = h.spawn(EntityType.COW, new BlockPos(3, 2, 6));
            cow.setNoAi(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            cow.setHealth(1000);
            cow.getAttribute(Attributes.ARMOR).setBaseValue(20);
            cow.hurt(Relics.damage(cow.level(), "magic", null), 20);
            h.assertTrue(cow.getHealth() > 980, "Ordinary relic magic respects armor");
            cow.setHealth(1000);
            cow.invulnerableTime = 0;
            cow.hurt(Relics.damage(cow.level(), "dark_matter", null), 20);
            h.assertTrue(cow.getHealth() == 980, "Dark matter bypasses armor");
            cow.getAttribute(Attributes.ARMOR).setBaseValue(0);
            cow.invulnerableTime = 0;
            cow.setHealth(1000);
            var p = player(h);
            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            p.setHealth(1000);
            boolean canceled =
                    ForgottenRelics.item(Relic.PARADOX)
                            .onLeftClickEntity(stack(Relic.PARADOX), p, cow);
            h.assertTrue(
                    canceled
                            && Math.abs((1000 - p.getHealth()) + (1000 - cow.getHealth()) - 200)
                                    < .01,
                    "Paradox splits 200 pre-armor damage: player="
                            + p.getHealth()
                            + ", victim="
                            + cow.getHealth());
            h.assertTrue(
                    stack(Relic.PARADOX).getMaxDamage() == 3000
                            && stack(Relic.TERROR_CROWN).getMaxDamage() == 440,
                    "Original tool and crown material durability");
            h.succeed();
        } finally {
            h.getLevel().getServer().setPvpAllowed(pvp);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void lunarPassesVictimBeforeBurstAtLockedGround(GameTestHelper h) {
        var p = player(h);
        var ground = h.absolutePos(new BlockPos(3, 1, 8));
        h.setBlock(new BlockPos(3, 1, 8), Blocks.STONE);
        var cow = h.spawn(EntityType.COW, new BlockPos(3, 6, 8));
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);
        cow.setHealth(500);
        var flare = RelicProjectiles.fire(p, Relic.LUNAR_FLARES, ground);
        flare.setPos(ground.getX() + .5, ground.getY() + 9, ground.getZ() + .5);
        flare.setDeltaMovement(0, -1, 0);
        h.runAfterDelay(
                3,
                () -> {
                    h.assertTrue(
                            cow.getHealth() == 400 && !flare.isRemoved(),
                            "Lunar direct hit deals 100 and keeps descending");
                });
        h.runAfterDelay(
                12,
                () -> {
                    h.assertTrue(flare.isRemoved(), "Lunar ends on its locked ground");
                    h.succeed();
                });
    }

    @GameTest(template = "empty")
    public static void researchOverridesRejectBadIdsAndPreserveOtherTriggers(GameTestHelper h) {
        var original = ForgottenKnowledge.currentTriggers();
        var changed =
                ForgottenKnowledge.withOverrides(
                        original,
                        List.of(
                                "AncientAegis[]",
                                "FateTome[minecraft:diamond,minecraft:ender_eye]"));
        h.assertTrue(
                changed.get("forgottenrelics:AncientAegis").isEmpty()
                        && changed.get("forgottenrelics:FateTome").size() == 2
                        && changed.size() == original.size(),
                "Config overrides replace only named research");
        boolean rejected = false;
        try {
            ForgottenKnowledge.withOverrides(original, List.of("AncientAegis[absent:item]"));
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        h.assertTrue(
                rejected && !original.get("forgottenrelics:AncientAegis").isEmpty(),
                "Invalid registry IDs rejected without changing defaults");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oblivionPreservesPotionSubtypesAndSettings(GameTestHelper h) {
        var healing =
                net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                        new ItemStack(Items.POTION),
                        net.minecraft.world.item.alchemy.Potions.HEALING);
        var speed =
                net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                        new ItemStack(Items.POTION),
                        net.minecraft.world.item.alchemy.Potions.SWIFTNESS);
        h.assertTrue(
                !OblivionRecipe.identity(healing).equals(OblivionRecipe.identity(speed)),
                "Old potion metadata remains distinct after modern NBT migration");
        var damaged = new ItemStack(Items.DIAMOND_SWORD);
        damaged.setDamageValue(20);
        h.assertTrue(
                OblivionRecipe.identity(damaged)
                        .equals(OblivionRecipe.identity(new ItemStack(Items.DIAMOND_SWORD))),
                "Damageable items ignore durability as in the original");
        var menu =
                new AbstractContainerMenu(null, 0) {
                    public boolean stillValid(Player p) {
                        return true;
                    }

                    public ItemStack quickMoveStack(Player p, int i) {
                        return ItemStack.EMPTY;
                    }
                };
        var grid = new TransientCraftingContainer(menu, 2, 2);
        var stone = stack(Relic.OBLIVION_STONE);
        stone.getOrCreateTag().putInt("mode", 2);
        stone.getOrCreateTag().putBoolean("disabled", true);
        grid.setItem(0, stone);
        var result =
                new OblivionRecipe(
                                ForgottenRelics.id("test"),
                                net.minecraft.world.item.crafting.CraftingBookCategory.MISC)
                        .assemble(grid, h.getLevel().registryAccess());
        h.assertTrue(
                result.getOrCreateTag().getInt("mode") == 2
                        && result.getOrCreateTag().getBoolean("disabled"),
                "Clearing filters preserves mode and enable state");
        grid.setItem(1, new ItemStack(Items.LAVA_BUCKET));
        var recipe =
                new OblivionRecipe(
                        ForgottenRelics.id("test"),
                        net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        h.assertTrue(
                recipe.getRemainingItems(grid).get(1).is(Items.BUCKET)
                        && recipe.getRemainingItems(grid).get(1).getCount() == 1,
                "Binding consumes the sample and returns exactly its normal container");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void mirrorCompletesAndInterruptedCastCannotFinish(GameTestHelper h) {
        var p = player(h);
        var mirror = stack(Relic.DIMENSIONAL_MIRROR);
        p.setItemInHand(InteractionHand.MAIN_HAND, mirror);
        p.setShiftKeyDown(true);
        RelicUse.begin(Relic.DIMENSIONAL_MIRROR, p.level(), p, InteractionHand.MAIN_HAND);
        var start = p.blockPosition();
        p.moveTo(p.position().add(0, 0, 5));
        p.setShiftKeyDown(false);
        RelicUse.begin(Relic.DIMENSIONAL_MIRROR, p.level(), p, InteractionHand.MAIN_HAND);
        RelicUse.tick(Relic.DIMENSIONAL_MIRROR, mirror, p, 1);
        h.assertTrue(
                p.blockPosition().equals(start), "Fully charged mirror returns to bound block");
        p.stopUsingItem();
        p.moveTo(p.position().add(0, 0, 5));
        var away = p.position();
        RelicUse.begin(Relic.DIMENSIONAL_MIRROR, p.level(), p, InteractionHand.MAIN_HAND);
        p.stopUsingItem();
        RelicUse.validate(p);
        RelicUse.tick(Relic.DIMENSIONAL_MIRROR, mirror, p, 1);
        h.assertTrue(p.position().equals(away), "An interrupted use cannot finish a stale cast");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void kingKeyAdvancementRevealsApotheosisImmediately(GameTestHelper h) {
        var p = player(h);
        var advancement =
                p.server
                        .getAdvancements()
                        .getAdvancement(ResourceLocation.parse("botania:challenge/king_key"));
        h.assertTrue(advancement != null, "Modern Botania King Key advancement exists");
        for (var criterion : advancement.getCriteria().keySet())
            p.getAdvancements().award(advancement, criterion);
        var knowledge = KnowledgeSavedData.get(p.server).snapshot(p.getUUID()).orElseThrow();
        h.assertTrue(
                knowledge.researchMarkers().contains("@forgottenrelics:Apotheosis"),
                "Advancement event reveals clue immediately without waiting for random scans");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void scannedItemsRevealAllOriginalCluesWithoutCompletingResearch(
            GameTestHelper h) {
        var p = player(h);
        var data = KnowledgeSavedData.get(p.server);
        data.update(p.getUUID(), p.getRandom()::nextInt, knowledge -> null);
        p.tickCount = 600;
        p.getRandom().setSeed(2166L);
        for (int i = 0; i < 1000; i++) ForgottenKnowledge.tick(p);
        var before = data.snapshot(p.getUUID()).orElseThrow();
        h.assertTrue(
                before.researchMarkers().stream().noneMatch(k -> k.startsWith("@forgottenrelics:")),
                "Unscanned items do not reveal forgotten clues");
        var original = json("/data/forgottenrelics/forgotten_knowledge.json");
        data.update(
                p.getUUID(),
                p.getRandom()::nextInt,
                knowledge -> {
                    for (var entry : original.entrySet())
                        for (var value : entry.getValue().getAsJsonArray()) {
                            var item = value.getAsJsonObject();
                            var sample =
                                    new ItemStack(
                                            ForgeRegistries.ITEMS.getValue(
                                                    ResourceLocation.parse(
                                                            item.get("item").getAsString())));
                            if (item.has("nbt"))
                                try {
                                    sample.setTag(
                                            TagParser.parseTag(item.get("nbt").getAsString()));
                                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                                    throw new IllegalArgumentException(e);
                                }
                            knowledge.rememberScan(
                                    PlayerKnowledge.ScanKind.OBJECT,
                                        PlayerKnowledge.ScanMode.THAUMOMETER.key(ScanIdentity.item(sample)));
                        }
                    return null;
                });
        for (int i = 0; i < 1000; i++) ForgottenKnowledge.tick(p);
        var after = data.snapshot(p.getUUID()).orElseThrow();
        h.assertTrue(original.size() == 22, "All 22 original scan groups are covered");
        for (var key : original.keySet()) {
            h.assertTrue(
                    after.researchMarkers().contains("@" + key),
                    "Real scan identities reveal " + key);
            h.assertTrue(
                    !after.researchMarkers().contains(key),
                    "A clue does not grant completed research " + key);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void botaniaCosmeticsInkAndRunicHardeningRemainCompatible(GameTestHelper h) {
        var menu =
                new AbstractContainerMenu(null, 0) {
                    public boolean stillValid(Player p) {
                        return true;
                    }

                    public ItemStack quickMoveStack(Player p, int i) {
                        return ItemStack.EMPTY;
                    }
                };
        var grid = new TransientCraftingContainer(menu, 2, 2);
        var ring = stack(Relic.MINING_CHARM);
        var cosmetic =
                new ItemStack(
                        ForgeRegistries.ITEMS.getValues().stream()
                                .filter(i -> i instanceof vazkii.botania.api.item.CosmeticBauble)
                                .findFirst()
                                .orElseThrow());
        grid.setItem(0, ring);
        grid.setItem(1, cosmetic);
        var attach =
                new vazkii.botania.common.crafting.recipe.CosmeticAttachRecipe(
                        ForgottenRelics.id("test"));
        h.assertTrue(
                attach.matches(grid, h.getLevel()),
                "Native Botania cosmetic attachment accepts relic baubles");
        var decorated = attach.assemble(grid, h.getLevel().registryAccess());
        var item = (RelicBaubleItem) decorated.getItem();
        h.assertTrue(
                ItemStack.isSameItemSameTags(item.getCosmeticItem(decorated), cosmetic),
                "Cosmetic saved on bauble");
        grid.setItem(0, decorated);
        grid.setItem(1, ItemStack.EMPTY);
        var remove =
                new vazkii.botania.common.crafting.recipe.CosmeticRemoveRecipe(
                        ForgottenRelics.id("test"));
        h.assertTrue(
                remove.matches(grid, h.getLevel())
                        && item.getCosmeticItem(
                                        remove.assemble(grid, h.getLevel().registryAccess()))
                                .isEmpty()
                        && ItemStack.isSameItemSameTags(
                                remove.getRemainingItems(grid).get(0), cosmetic),
                "Native removal returns one bauble and its cosmetic");
        grid.setItem(1, new ItemStack(vazkii.botania.common.item.BotaniaItems.phantomInk));
        var ink =
                new vazkii.botania.common.crafting.recipe.PhantomInkRecipe(
                        ForgottenRelics.id("test"));
        h.assertTrue(
                ink.matches(grid, h.getLevel())
                        && item.hasPhantomInk(ink.assemble(grid, h.getLevel().registryAccess())),
                "Native phantom ink recipe works");
        h.assertTrue(
                ink.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty),
                "Ink application does not duplicate the attached cosmetic");
        ring.getOrCreateTag().putByte("RS.HARDEN", (byte) 2);
        h.assertTrue(
                RunicGear.charge(ring) == 2,
                "Relic baubles accept runic hardening through the public API");
        var p = player(h);
        h.assertTrue(
                CuriosApi.getCurio(ring)
                        .resolve()
                        .orElseThrow()
                        .canEquipFromUse(new SlotContext("ring", p, 0, false, true)),
                "Curios right-click equip is enabled");
        h.succeed();
    }
}
