package temodern.test;

import appeng.api.client.StorageCellModels;
import appeng.api.storage.cells.CellState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import temodern.TE;
import temodern.client.StorageCellRendering;
import temodern.storage.EssentiaCell;

/** Opt-in real-client resource-loader regression checks, not visual acceptance. */
@Mod.EventBusSubscriber(modid=TE.ID, value=Dist.CLIENT)
public class ResourceClientAudit {
    private static boolean started;
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("temodern.resourceAudit") || started || event.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        if (!(client.screen instanceof TitleScreen) || client.getOverlay() != null) return;
        started = true;
        try {
            verify(client);
            ResourceReviewClientAudit.verify(client);
            var selected = java.util.List.copyOf(client.getResourcePackRepository().getSelectedIds());
            var pack = client.gameDirectory.toPath().resolve("resourcepacks/te-audit-override.zip");
            java.nio.file.Files.createDirectories(pack.getParent());
            try (var zip = new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(pack))) {
                zip.putNextEntry(new java.util.zip.ZipEntry("pack.mcmeta"));
                zip.write("{\"pack\":{\"pack_format\":15,\"description\":\"TE resource override regression fixture\"}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
                zip.putNextEntry(new java.util.zip.ZipEntry("assets/thaumicenergistics/textures/modern/item/essentia_cell_1k.png"));
                try (var input = client.getResourceManager().open(ResourceLocation.fromNamespaceAndPath(TE.ID, "textures/modern/item/storage_casing.png"))) { input.transferTo(zip); }
                zip.closeEntry();
            }
            client.getResourcePackRepository().reload();
            var withOverride = new java.util.ArrayList<>(selected); withOverride.add("file/te-audit-override.zip");
            client.getResourcePackRepository().setSelected(withOverride);
            client.reloadResourcePacks().whenComplete((unused, error) -> client.execute(() -> {
                try {
                    if (error != null) throw new IllegalStateException("Resource reload failed", error);
                    verify(client);
                    var texture = ResourceLocation.fromNamespaceAndPath(TE.ID, "textures/modern/item/essentia_cell_1k.png");
                    require(client.getResourceManager().getResourceOrThrow(texture).sourcePackId().equals("file/te-audit-override.zip"), "Player resource pack overrides bundled art");
                    client.getResourcePackRepository().setSelected(selected);
                    client.reloadResourcePacks().whenComplete((restored, restoreError) -> client.execute(() -> {
                        try {
                            if (restoreError != null) throw new IllegalStateException("Restoring resource packs failed", restoreError);
                            verify(client);
                            require(!client.getResourceManager().getResourceOrThrow(texture).sourcePackId().equals("file/te-audit-override.zip"), "Removing resource pack restores mod art");
                            result(client, "PASS: actual item, part, drive models, LED colors, resource pack override and two reloads", null);
                        } catch (Throwable failure) { result(client, "FAIL", failure); }
                    }));
                } catch (Throwable failure) { result(client, "FAIL", failure); }
            }));
        } catch (Throwable failure) { result(client, "FAIL", failure); }
    }

    private static void verify(Minecraft client) throws Exception {
        var manager = client.getModelManager();
        var source = client.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(TE.ID,
                "textures/modern/gui/arcane_terminal.png"));
        require(source.isPresent(), "Terminal GUI resource");
        for (var entry : TE.ITEMS.getEntries()) {
            var stack = new ItemStack(entry.get());
            model(client, client.getItemRenderer().getModel(stack, null, null, 0), entry.getId().toString());
        }
        for (var entry : TE.CELLS.entrySet()) {
            var item = (EssentiaCell)entry.getValue().get();
            var drive = StorageCellRendering.driveModel(entry.getKey());
            require(drive.equals(StorageCellModels.model(item)), "Drive registration: " + entry.getKey());
            model(client, manager.getModel(drive), drive.toString());
            var stack = new ItemStack(item);
            require(client.getItemColors().getColor(stack, 0) == 0xffffff, "Untinted shell");
            require(client.getItemColors().getColor(stack, 1) == (item.tier().creative() ? CellState.FULL : CellState.EMPTY).getStateColor(), "Empty cell LED");
            var values = new net.minecraft.nbt.CompoundTag();
            values.putLong("aer", 1); stack.getOrCreateTag().put("essentia", values);
            var partial = item.tier().creative() ? CellState.FULL : item.tier().types() == 1 ? CellState.TYPES_FULL : CellState.NOT_EMPTY;
            require(client.getItemColors().getColor(stack, 1) == partial.getStateColor(), "Part-filled cell LED");
            values.putLong("aer", item.tier().capacity());
            require(client.getItemColors().getColor(stack, 1) == CellState.FULL.getStateColor(), "Full cell LED");
            if (!item.tier().creative()) {
                values = new net.minecraft.nbt.CompoundTag();
                for (int i=0; i<item.tier().types(); i++) values.putLong("audit_" + i, 1);
                stack.getOrCreateTag().put("essentia", values);
                require(client.getItemColors().getColor(stack, 1) == CellState.TYPES_FULL.getStateColor(), "Type-full cell LED");
            }
        }
        for (String name : new String[]{"arcane_terminal", "arcane_terminal_off", "arcane_terminal_base", "vis_input", "vis_output", "vis_creative"}) {
            var location = ResourceLocation.fromNamespaceAndPath(TE.ID, "part/" + name);
            model(client, manager.getModel(location), location.toString());
        }
    }

    private static void model(Minecraft client, BakedModel model, String name) {
        require(model != client.getModelManager().getMissingModel(), "Missing model: " + name);
        var quads = new java.util.ArrayList<>(model.getQuads(null, null, RandomSource.create(0)));
        for (var direction : net.minecraft.core.Direction.values()) quads.addAll(model.getQuads(null, direction, RandomSource.create(0)));
        for (var quad : quads) require(!quad.getSprite().contents().name().getPath().equals("missingno"), "Missing sprite: " + name);
        if (name.contains(":part/") || name.contains(":block/drive/"))
            require(!model.getParticleIcon().contents().name().getPath().equals("missingno"), "Missing particle: " + name);
    }

    private static void require(boolean valid, String message) { if (!valid) throw new IllegalStateException(message); }
    private static void result(Minecraft client, String message, Throwable failure) {
        String text = message + (failure == null ? "" : ": " + failure);
        if (failure == null) LOG.info("TE_RESOURCE_AUDIT {}", text); else LOG.error("TE_RESOURCE_AUDIT {}", text, failure);
        try { java.nio.file.Files.writeString(client.gameDirectory.toPath().resolve("resource-audit-result.txt"), text); }
        catch (java.io.IOException error) { LOG.error("Cannot write audit result", error); }
        client.stop();
    }
}
