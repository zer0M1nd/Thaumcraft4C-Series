package thaumicomniverse.verification;

import com.google.gson.JsonParser;
import com.reborn.modularmachinery.machine.*;
import com.reborn.modularmachinery.recipe.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import thaumicfutures.compat.machinery.*;
import static thaumicomniverse.verification.ServerVerification.*;

final class MachineryVerification {
    private static AspectPort place(ServerLevel level, String name, BlockPos pos) {
        level.setBlockAndUpdate(pos, ForgeRegistries.BLOCKS.getValue(id(name)).defaultBlockState());
        return (AspectPort) level.getBlockEntity(pos);
    }
    static void run(MinecraftServer server) {
        var level = server.overworld();
        var origin = new BlockPos(32, 100, 32);
        check("port world save and restart marker", () -> {
            var pos = new BlockPos(48, 100, 48);
            level.getChunkAt(pos);
            var air = tc4revival.aspect.TCAspects.catalog().require("aer");
            if (java.nio.file.Files.exists(java.nio.file.Path.of("port-restart-marker"))) {
                require(level.getBlockEntity(pos) instanceof AspectPort, "Saved port missing after restart");
                var port = (AspectPort) level.getBlockEntity(pos);
                require(port.amount(air) == 37 && !port.enabled(Direction.UP), "Stored quantity or face lost after restart");
            } else {
                var port = place(level, "machine_essentia_input_2", pos);
                port.insert(air, 37, false); port.toggleSide(Direction.UP);
                try { java.nio.file.Files.writeString(java.nio.file.Path.of("port-restart-marker"), "Verify 37 aer and disabled top after restart"); }
                catch (java.io.IOException error) { throw new AssertionError(error); }
            }
        });
        check("machinery registry and all craft chains", () -> {
            for (String resource : List.of("essentia", "vis")) for (String io : List.of("input", "output")) for (int tier = 1; tier <= 16; tier++) {
                String name = "machine_" + resource + "_" + io + "_" + tier;
                require(ForgeRegistries.BLOCKS.containsKey(id(name)), name);
                require(server.getRecipeManager().byKey(id("collab/machinery/" + name)).isPresent(), "Missing recipe: " + name);
                var port = place(level, name, origin);
                int expected = (resource.equals("vis") ? 100 : 64) << Math.min(10, tier - 1);
                require(port.capacity() == expected, "Capacity: " + name);
                level.removeBlock(origin, false);
            }
        });
        check("essentia conservation, faces and corrupt persistence", () -> {
            var port = place(level, "machine_essentia_input_1", origin);
            var air = tc4revival.aspect.TCAspects.catalog().require("aer");
            require(port.insert(air, 80, true) == 64 && port.amount(air) == 0, "Simulation changed storage");
            require(port.insert(air, 80, false) == 64, "Capacity overflow");
            require(port.extract(air, 4, false) == 4 && port.amount(air) == 60, "Drain conservation");
            port.toggleSide(Direction.NORTH);
            var saved = port.saveWithoutMetadata();
            var clone = new AspectPort.Essentia(origin, port.getBlockState()); clone.load(saved);
            require(clone.amount(air) == 60 && !clone.enabled(Direction.NORTH), "NBT roundtrip");
            saved.getCompound("Aspects").putInt("aer", Integer.MAX_VALUE);
            saved.getCompound("Aspects").putInt("ignis", -5);
            saved.getCompound("Aspects").putInt("absent:missing", 9);
            clone.load(saved);
            require(clone.amount(air) == 64 && clone.contents().size() == 1, "Corrupt NBT not bounded");
            var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
            player.setPos(origin.getX() + .5, origin.getY() + .5, origin.getZ() + .5);
            var menu = new AspectPortMenu(1, port);
            require(!menu.clickMenuButton(player, 6), "Invalid button accepted");
            require(menu.clickMenuButton(player, Direction.NORTH.ordinal()) && port.enabled(Direction.NORTH), "Face button not applied");
            player.setPos(300, 100, 300);
            require(!menu.clickMenuButton(player, 0), "Remote menu permitted mutation");
        });
        check("machine sum preflight and native collection hook", () -> {
            var port = place(level, "machine_essentia_input_1", origin);
            // Remove previous saved state if the same block survived the prior case.
            port.load(new CompoundTag());
            var air = tc4revival.aspect.TCAspects.catalog().require("aer");
            port.insert(air, 12, false);
            var pattern = MachinePattern.builder().add(BlockPos.ZERO, List.of(BlockMatcher.parse("thaumicfutures:machine_essentia_input_1"))).build();
            var definition = new MachineDefinition(id("verify_machine"), "Verify", pattern, MachineDefinition.DEFAULT_FAILURE_ACTION, false);
            HatchCollection ports;
            try {
                var method = com.reborn.modularmachinery.block.MachineControllerBlockEntity.class
                        .getDeclaredMethod("collectHatches", net.minecraft.world.level.Level.class, BlockPos.class, MachineDefinition.class);
                method.setAccessible(true);
                var controller = new com.reborn.modularmachinery.block.MachineControllerBlockEntity(origin.below(),
                        ForgeRegistries.BLOCKS.getValue(id("modular_machinery_reborn:machine_controller")).defaultBlockState());
                controller.setLevel(level);
                ports = (HatchCollection) method.invoke(controller, level, origin, definition);
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            require(MachineAspectPorts.available(ports, AspectPort.Kind.ESSENTIA_INPUT, air) == 12, "Collection injection missing");
            var a = new AspectRequirement(false, IOType.INPUT, "aer", 8, false, 1);
            var b = new AspectRequirement(false, IOType.INPUT, "aer", 8, false, 1);
            var recipe = new MachineRecipe(id("verify_recipe"), "verify_machine", "verify_recipe", 5, List.of(a, b));
            require(!recipe.canStart(ports) && !recipe.start(ports, level.random, RecipeModifiers.EMPTY), "Duplicate requirements allowed partial start");
            require(port.amount(air) == 12, "Failed start consumed storage");
            port.insert(air, 28, false);
            require(recipe.parallelism(ports, RecipeModifiers.EMPTY, 100) == 2, "Aggregate parallel limit");
            recipe.setParallelism(2);
            require(recipe.start(ports, level.random, RecipeModifiers.EMPTY) && port.amount(air) == 8, "Parallel consumption");
            level.removeBlock(origin, false);
            require(MachineAspectPorts.available(ports, AspectPort.Kind.ESSENTIA_INPUT, air) == 0, "Removed tile retained by machine");
        });
        check("machine native JSON and wire codec", () -> {
            var json = JsonParser.parseString("""
                {"machine":"thaumicomniverse:verify_machine","recipeTime":40,"requirements":[
                 {"type":"thaumicomniverse:vis","io-type":"input","aspect":"aer","amount":25,
                  "chance":0.5,"per-tick":true,"parallelize-unaffected":true}]}
                """).getAsJsonObject();
            var serializer = (RecipeSerializer<MachineRecipe>) ForgeRegistries.RECIPE_SERIALIZERS.getValue(id("modular_machinery_reborn:machine"));
            var recipe = serializer.fromJson(id("verify_wire"), json);
            var buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                serializer.toNetwork(buffer, recipe);
                var copy = serializer.fromNetwork(id("verify_wire"), buffer);
                var requirement = (AspectRequirement) copy.requirements().get(0);
                require(requirement.vis && requirement.amount == 25 && requirement.chance == .5f && requirement.perTick()
                        && requirement.isParallelizeUnaffected(), "Wire fields changed");
                require(buffer.readableBytes() == 0, "Trailing bytes");
            } finally { buffer.release(); }
            json.getAsJsonArray("requirements").get(0).getAsJsonObject().addProperty("amount", 1.5);
            try { serializer.fromJson(id("invalid"), json); throw new AssertionError("Fractional cost accepted"); }
            catch (com.google.gson.JsonParseException expected) { }
            require(new AspectRequirement(true, IOType.INPUT, "praecantatio", 1, false, 1).aspect() == null, "Compound vis accepted");
        });
        check("vis bridge transfer and source lifecycle", () -> {
            var output = place(level, "machine_vis_output_16", origin);
            var bridge = place(level, "machine_vis_bridge", origin.east());
            output.load(new CompoundTag()); bridge.load(new CompoundTag());
            var air = tc4revival.aspect.TCAspects.catalog().require("aer");
            output.insert(air, 1500, false);
            bridge.selectAbsorption(Direction.WEST);
            long time = level.getGameTime(); server.getWorldData().overworldData().setGameTime(100);
            bridge.tick();
            require(output.amount(air) == 500 && bridge.amount(air) == 1000, "Bridge transfer amount");
            require(bridge.drain(air, 400) == 400 && bridge.amount(air) == 600, "Bridge source extraction");
            var saved = bridge.saveWithoutMetadata();
            bridge.load(saved);
            require(bridge.absorption() == Direction.WEST && bridge.amount(air) == 600, "Bridge persistence");
            server.getWorldData().overworldData().setGameTime(time);
            level.removeBlock(origin, false); level.removeBlock(origin.east(), false);
        });
    }
}
