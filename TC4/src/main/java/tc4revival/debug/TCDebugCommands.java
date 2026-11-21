package tc4revival.debug;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;
import tc4revival.config.TCConfig;

public  class TCDebugCommands {
    private TCDebugCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcdebug")
                .requires(source -> source.hasPermission(2))
                .then(RecordingDebugCommands.tree())
                .then(Commands.literal("verify_recording").executes(context -> RecordingDebugCommands.verify(context.getSource())))
                .then(WarpDebugCommands.tree())
                .then(OreSurveyCommand.tree())
                .then(OuterMazeDebugCommands.tree(false))
                .then(OuterMazeDebugCommands.tree(true))
                .then(AspectDebugCommands.tree())
                .then(AspectTableExportCommand.tree())
                .then(KnowledgeDebugCommands.tree())
                .then(KnowledgeSyncDebugCommands.tree())
                .then(EntityAspectDebugCommands.tree())
                .then(ItemAspectDebugCommands.tree(event.getBuildContext()))
                .then(ItemBaseAspectDebugCommands.tree(event.getBuildContext()))
                .then(StackAspectDebugCommands.tree(event.getBuildContext()))
                .then(PotionAspectDebugCommands.tree())
                .then(BlockScanDebugCommands.tree())
                .then(ScanSessionDebugCommands.tree())
                .then(ScanAimingDebugCommands.tree())
                .then(ShardDebugCommands.tree())
                .then(ThaumometerDebugCommands.tree())
                .then(ResearchDebugCommands.tree())
                .then(RecipeAspectDebugCommands.tree())
                .then(Commands.literal("environment").executes(context -> environment(context.getSource())))
                .then(Commands.literal("verify_crab_vent").executes(context -> CrabVentChecks.verify(context.getSource())))
                .then(Commands.literal("verify_report0919_1").executes(context -> Report0919Checks.verify(context.getSource())))
                .then(Commands.literal("verify_report0919_2").executes(context -> Report0919TwoChecks.verify(context.getSource())))
                .then(Commands.literal("verify_jei_aspects").executes(context -> JeiAspectChecks.verify(context.getSource())))
                .then(Commands.literal("config").executes(context -> config(context.getSource())))
                .then(Commands.literal("verify_bucket_tooltips").executes(context -> verifyBucketTooltips(context.getSource())))
                .then(Commands.literal("verify_0918_1").executes(context -> Report0918Checks.verify(context.getSource())))
                .then(Commands.literal("verify_0918_2").executes(context -> Report0918TwoChecks.verify(context.getSource())))
                .then(Commands.literal("verify_addon_assets").executes(context -> AddonAssetDebugChecks.verify(context.getSource())))
                .then(Commands.literal("verify_iron").executes(context -> verifyIron(context.getSource()))));
    }

    private static int verifyBucketTooltips(CommandSourceStack source) {
        int count = 0;
        var missing = new java.util.ArrayList<String>();
        if (tc4revival.compat.BucketTooltipFluids.forTooltip(null) != net.minecraft.world.level.material.Fluids.EMPTY)
            throw new IllegalStateException("Null tooltip fluid must become EMPTY");
        for (var item : ForgeRegistries.ITEMS) {
            if (!(item instanceof net.minecraft.world.item.BucketItem bucket)) continue;
            count++;
            var raw = bucket.getFluid();
            var safe = tc4revival.compat.BucketTooltipFluids.forTooltip(raw);
            if (raw == null) missing.add(String.valueOf(ForgeRegistries.ITEMS.getKey(item)));
            else if (raw != safe) throw new IllegalStateException("Valid bucket fluid changed");
            if (!(safe instanceof net.minecraft.world.level.material.EmptyFluid))
                new net.minecraftforge.fluids.FluidStack(safe, 1000);
        }
        String report = "Bucket tooltip validation passed: buckets=" + count + ", null fluids=" + missing;
        source.sendSuccess(() -> Component.literal(report), false);
        return count;
    }

    private static int config(CommandSourceStack source) {
        var settings = TCConfig.SERVER;
        source.sendSuccess(() -> Component.literal("research=" + settings.researchDifficulty.get()
                + "; nodes=" + settings.nodeRarity.get() + "; special=" + settings.specialNodeRarity()
                + "; golem=" + settings.golemDelayTicks() + " ticks/" + settings.golemIgnoreMilliseconds() + " ms"
                + "; runic=" + settings.shieldRechargeMilliseconds() + " ms/" + settings.shieldWaitTicks()
                + " ticks/" + settings.shieldCostCentiVis() + " cVis"), false);
        return 1;
    }

    private static int environment(CommandSourceStack source) {
        var version = ModList.get().getModContainerById(TC4Revival.MOD_ID).orElseThrow()
                .getModInfo().getVersion().toString();
        source.sendSuccess(() -> Component.translatable("commands.thaumcraft.debug.environment", version,
                source.getLevel().dimension().location().toString(), source.getServer().getPlayerCount(),
                ModList.get().getModContainerById("curios").orElseThrow().getModInfo().getVersion().toString()), false);
        return 1;
    }

    private static int verifyIron(CommandSourceStack source) {
        String problem = null;
        if (!ResourceLocation.fromNamespaceAndPath("minecraft", "iron_nugget").equals(ForgeRegistries.ITEMS.getKey(Items.IRON_NUGGET))) {
            problem = "unexpected canonical iron nugget";
        } else if (!new ItemStack(Items.IRON_NUGGET).is(Tags.Items.NUGGETS_IRON)) {
            problem = "missing forge:nuggets/iron";
        } else if (ForgeRegistries.ITEMS.containsKey(ResourceLocation.fromNamespaceAndPath(TC4Revival.MOD_ID, "iron_nugget"))) {
            problem = "duplicate thaumcraft:iron_nugget";
        }
        if (problem != null) {
            source.sendFailure(Component.translatable("commands.thaumcraft.debug.iron.fail", problem));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.thaumcraft.debug.iron.pass"), false);
        return 1;
    }
}
