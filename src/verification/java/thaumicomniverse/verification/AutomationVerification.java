package thaumicomniverse.verification;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import static thaumicomniverse.verification.ServerVerification.*;

final class AutomationVerification {
    static void run(MinecraftServer server) {
        var level = server.overworld();
        var pos = new BlockPos(40, 100, 40);
        if (ModList.get().isLoaded("thaumicfutures") && ModList.get().isLoaded("immersiveengineering")) check("Futures IE heater conservation and chemthrower effects", () -> {
            var block = ForgeRegistries.BLOCKS.getValues().stream()
                    .filter(b -> b instanceof tc4revival.content.AlchemyFurnaceBlock).findFirst().orElseThrow();
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            var furnace = level.getBlockEntity(pos);
            var heat = furnace.getCapability(blusunrize.immersiveengineering.api.tool.ExternalHeaterHandler.CAPABILITY)
                    .orElseThrow(() -> new AssertionError("No external heater capability"));
            require(heat.doHeatTick(0, true) == 0, "Heat without energy");
            require(heat.doHeatTick(1000, false) == 0, "Empty furnace heated without redstone");
            int used = heat.doHeatTick(1000, true);
            require(used > 0 && used <= 1000, "Invalid energy accounting");
            for (int i = 0; i < 60; i++) heat.doHeatTick(1000, true);
            require(heat.doHeatTick(1000, true) == 0, "Unbounded furnace heat");
            for (var name : java.util.List.of("liquid_death", "purifying_water", "flux_goo")) {
                var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID, id(name));
                var fluid = ForgeRegistries.FLUIDS.getValues().stream().filter(f -> f.is(tag)).findFirst().orElseThrow();
                require(blusunrize.immersiveengineering.api.tool.ChemthrowerHandler.getEffect(fluid) != null, "Missing chemthrower effect " + name);
            }
            level.removeBlock(pos, false);
        });
        if (ModList.get().isLoaded("thaumicfutures") && ModList.get().isLoaded("industrialforegoing")) check("Futures IF crop maturity, protection and harvest", () -> {
            var rule = com.buuz135.industrial.registry.IFRegistries.PLANT_RECOLLECTABLES_REGISTRY.get().getValue(id("thaumicfutures:magical_plants"));
            require(rule != null, "No magical plant handler");
            var plant = ForgeRegistries.BLOCKS.getValue(id("thaumcraft:shimmerleaf")).defaultBlockState();
            level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(pos, plant);
            require(rule.canBeHarvested(level, pos, plant), "Flower not harvestable");
            java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> deny = event -> {
                if (event.getPos().equals(pos)) event.setCanceled(true);
            };
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(deny);
            try {
                require(rule.doHarvestOperation(level, pos, plant).isEmpty() && level.getBlockState(pos).equals(plant), "Protection bypassed");
            } finally { net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(deny); }
            require(!rule.doHarvestOperation(level, pos, plant).isEmpty() && level.isEmptyBlock(pos), "Harvest failed");
            var pod = tc4revival.content.TCArcane.MANA_POD.get().defaultBlockState();
            require(!rule.canBeHarvested(level, pos, pod.setValue(tc4revival.content.ManaPodBlock.AGE, 4))
                    && rule.canBeHarvested(level, pos, pod.setValue(tc4revival.content.ManaPodBlock.AGE, 5)), "Mana pod maturity boundary");
        });
        if (ModList.get().isLoaded("thaumicfutures") && ModList.get().isLoaded("pamhc2crops")) check("Pam golem harvesting and replant", () -> {
            var cropPos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    level.getSharedSpawnPos().offset(4, 0, 4));
            level.getChunkAt(cropPos);
            level.setDayTime(6000);
            var crop = (net.minecraft.world.level.block.CropBlock) ForgeRegistries.BLOCKS.getEntries().stream()
                    .filter(e -> e.getKey().location().getNamespace().equals("pamhc2crops") && e.getValue() instanceof net.minecraft.world.level.block.CropBlock)
                    .findFirst().orElseThrow().getValue();
            var mature = crop.getStateForAge(crop.getMaxAge());
            level.setBlockAndUpdate(cropPos.below(), Blocks.FARMLAND.defaultBlockState());
            require(crop.getStateForAge(0).canSurvive(level, cropPos), "Fixture crop position must support planting before harvest: "
                    + cropPos + " sky=" + level.canSeeSky(cropPos) + " light=" + level.getRawBrightness(cropPos, 0)
                    + " soil=" + level.getBlockState(cropPos.below()));
            level.setBlockAndUpdate(cropPos, mature);
            var rule = thaumcraft.api.golems.GolemCropRegistry.find(mature).orElseThrow();
            require(rule.mature(level, cropPos, mature) && !rule.mature(level, cropPos, crop.getStateForAge(0)), "Maturity mismatch");
            var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
            require(rule.harvest(new thaumcraft.api.golems.GolemCropRegistry.Context(null, level, player, cropPos, mature, true)), "Harvest failed");
            require(level.getBlockState(cropPos).equals(crop.getStateForAge(0)), "Crop not replanted: " + ForgeRegistries.BLOCKS.getKey(crop)
                    + " seed=" + crop.getCloneItemStack(level, cropPos, mature) + " canSurvive=" + crop.getStateForAge(0).canSurvive(level, cropPos)
                    + " soil=" + level.getBlockState(cropPos.below()) + " sky=" + level.canSeeSky(cropPos) + " light=" + level.getRawBrightness(cropPos, 0)
                    + " drops=" + level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(cropPos).inflate(2)).stream().map(e -> e.getItem().toString()).toList());
            level.removeBlock(cropPos, false);
        });
    }
}
