package forgottenrelics;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import tc4revival.content.*;

import top.theillusivec4.curios.api.CuriosApi;

import java.nio.file.*;
import java.util.*;

/** Isolated test source set only. Never included in either release archive. */
@Mod.EventBusSubscriber(modid = ForgottenRelics.ID)
public final class RelicsValidationCommands {
    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher()
                .register(
                        Commands.literal("relictest")
                                .requires(s -> s.hasPermission(2))
                                .then(
                                        Commands.literal("setup")
                                                .executes(
                                                        c ->
                                                                setup(
                                                                        c.getSource()
                                                                                .getPlayerOrException())))
                                .then(
                                        Commands.literal("gallery")
                                                .executes(
                                                        c ->
                                                                gallery(
                                                                        c.getSource()
                                                                                .getPlayerOrException())))
                                .then(
                                        Commands.literal("equip")
                                                .then(
                                                        Commands.argument(
                                                                        "relic",
                                                                        StringArgumentType.word())
                                                                .executes(
                                                                        c ->
                                                                                equip(
                                                                                        c.getSource()
                                                                                                .getPlayerOrException(),
                                                                                        StringArgumentType
                                                                                                .getString(
                                                                                                        c,
                                                                                                        "relic")))))
                                .then(
                                        Commands.literal("targets")
                                                .executes(
                                                        c ->
                                                                targets(
                                                                        c.getSource()
                                                                                .getPlayerOrException())))
                                .then(
                                        Commands.literal("snapshot")
                                                .then(
                                                        Commands.argument(
                                                                        "label",
                                                                        StringArgumentType.word())
                                                                .executes(
                                                                        c ->
                                                                                snapshot(
                                                                                        c.getSource()
                                                                                                .getPlayerOrException(),
                                                                                        StringArgumentType
                                                                                                .getString(
                                                                                                        c,
                                                                                                        "label"))))));
    }

    private static int setup(ServerPlayer p) {
        var level = p.serverLevel();
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, p.server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, p.server);
        level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, p.server);
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, p.server);
        level.setDayTime(6000);
        level.setWeatherParameters(100000, 0, false, false);
        for (var pos : BlockPos.betweenClosed(-24, 79, -24, 24, 79, 24))
            level.setBlockAndUpdate(pos, Blocks.SMOOTH_STONE.defaultBlockState());
        p.teleportTo(level, .5, 80, .5, 0, 0);
        p.setGameMode(GameType.SURVIVAL);
        p.getFoodData().eat(20, 1);
        p.sendSystemMessage(Component.literal("FR setup: isolated platform ready"));
        return 1;
    }

    private static void clear(ServerPlayer p) {
        p.closeContainer();
        p.stopUsingItem();
        RelicUse.clear(p);
        p.getInventory().clearContent();
        p.removeAllEffects();
        p.clearFire();
        p.getPersistentData().remove("forgottenrelics:cooldown");
        p.getPersistentData().remove("forgottenrelics:grab_cooldown");
        CuriosApi.getCuriosInventory(p)
                .ifPresent(
                        inv ->
                                inv.getCurios()
                                        .values()
                                        .forEach(
                                                handler -> {
                                                    for (int i = 0; i < handler.getSlots(); i++)
                                                        handler.getStacks()
                                                                .setStackInSlot(i, ItemStack.EMPTY);
                                                }));
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
        p.setHealth(20);
        p.invulnerableTime = 0;
        p.getInventory()
                .setItem(
                        8,
                        TCItems.CASTING_WAND
                                .get()
                                .create(
                                        WandParts.Rod.SILVERWOOD,
                                        WandParts.Cap.THAUMIUM,
                                        false,
                                        true));
        p.getInventory().setItem(7, new ItemStack(TCItems.THAUMONOMICON.get()));
        p.getInventory().selected = 0;
    }

    private static int equip(ServerPlayer p, String name) {
        var relic =
                Arrays.stream(Relic.values())
                        .filter(r -> r.id.equals(name))
                        .findFirst()
                        .orElseThrow();
        clear(p);
        var item = new ItemStack(ForgottenRelics.item(relic));
        p.getInventory().setItem(0, item);
        if (relic == Relic.XP_TOME) {
            p.experienceLevel = 17;
            p.experienceProgress = 3f / p.getXpNeededForNextLevel();
            p.totalExperience = 397;
            p.giveExperiencePoints(0);
        }
        if (relic == Relic.PARADOX) {
            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            p.setHealth(1000);
        }
        p.containerMenu.broadcastChanges();
        p.sendSystemMessage(Component.literal("FR equipped " + name));
        return 1;
    }

    private static int gallery(ServerPlayer p) {
        clear(p);
        var level = p.serverLevel();
        var left = new BlockPos(-1, 80, 3);
        var right = left.east();
        level.setBlockAndUpdate(
                left,
                Blocks.CHEST
                        .defaultBlockState()
                        .setValue(ChestBlock.FACING, Direction.NORTH)
                        .setValue(ChestBlock.TYPE, ChestType.LEFT));
        level.setBlockAndUpdate(
                right,
                Blocks.CHEST
                        .defaultBlockState()
                        .setValue(ChestBlock.FACING, Direction.NORTH)
                        .setValue(ChestBlock.TYPE, ChestType.RIGHT));
        var a = (ChestBlockEntity) level.getBlockEntity(left);
        var b = (ChestBlockEntity) level.getBlockEntity(right);
        a.clearContent();
        b.clearContent();
        for (int i = 0; i < Relic.values().length; i++) {
            var chest = i < 27 ? a : b;
            chest.setItem(i % 27, new ItemStack(ForgottenRelics.item(Relic.values()[i])));
        }
        b.setItem(25, new ItemStack(Items.DIAMOND, 64));
        a.setChanged();
        b.setChanged();
        p.teleportTo(level, .5, 80, .5, 0, 10);
        return 1;
    }

    private static int targets(ServerPlayer p) {
        var old = new ArrayList<Entity>();
        for (var e : p.serverLevel().getAllEntities())
            if (e.getTags().contains("fr_test_target") || e instanceof RelicProjectile) old.add(e);
        old.forEach(Entity::discard);
        p.teleportTo(p.serverLevel(), .5, 80, .5, 0, 0);
        for (int i = 0; i < 3; i++) {
            var cow = EntityType.COW.create(p.level());
            cow.moveTo(.5 + (i - 1) * 3, 80, 8.5);
            cow.setNoAi(true);
            cow.setNoGravity(true);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            cow.setHealth(1000);
            cow.addTag("fr_test_target");
            p.serverLevel().addFreshEntity(cow);
        }
        return 1;
    }

    private static int snapshot(ServerPlayer p, String label) {
        if (!label.matches("[a-zA-Z0-9_-]{1,80}")) throw new IllegalArgumentException("label");
        var root = new JsonObject();
        root.addProperty("label", label);
        root.addProperty("gameTime", p.level().getGameTime());
        root.addProperty("dimension", p.level().dimension().location().toString());
        root.addProperty("health", p.getHealth());
        root.addProperty("position", p.position().toString());
        root.addProperty("using", p.isUsingItem());
        root.addProperty("xpLevel", p.experienceLevel);
        root.addProperty("xpProgress", p.experienceProgress);
        root.addProperty("raining", p.level().isRaining());
        root.addProperty(
                "reach", p.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get()));
        var inventory = new JsonArray();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            var stack = p.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                var item = new JsonObject();
                item.addProperty("slot", i);
                item.addProperty(
                        "stack", stack.save(new net.minecraft.nbt.CompoundTag()).toString());
                inventory.add(item);
            }
        }
        root.add("inventory", inventory);
        var curios = new JsonArray();
        CuriosApi.getCuriosInventory(p)
                .ifPresent(
                        inv ->
                                inv.getCurios()
                                        .forEach(
                                                (slot, handler) -> {
                                                    for (int i = 0; i < handler.getSlots(); i++) {
                                                        var item =
                                                                handler.getStacks()
                                                                        .getStackInSlot(i);
                                                        if (!item.isEmpty())
                                                            curios.add(
                                                                    slot
                                                                            + ":"
                                                                            + i
                                                                            + "="
                                                                            + item.save(
                                                                                    new net
                                                                                            .minecraft
                                                                                            .nbt
                                                                                            .CompoundTag()));
                                                    }
                                                }));
        root.add("curios", curios);
        var entities = new JsonArray();
        for (var e : p.serverLevel().getAllEntities())
            if (e.getTags().contains("fr_test_target") || e instanceof RelicProjectile) {
                var obj = new JsonObject();
                obj.addProperty("id", e.getUUID().toString());
                obj.addProperty("position", e.position().toString());
                if (e instanceof LivingEntity living) obj.addProperty("health", living.getHealth());
                if (e instanceof RelicProjectile spell)
                    obj.addProperty("spell", spell.kind().toString());
                entities.add(obj);
            }
        root.add("entities", entities);
        try {
            var path = Path.of("../server-observations");
            Files.createDirectories(path);
            Files.writeString(
                    path.resolve(label + ".json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        com.mojang.logging.LogUtils.getLogger().info("FR observation {}", root);
        p.sendSystemMessage(Component.literal("FR observation saved: " + label));
        return 1;
    }
}
