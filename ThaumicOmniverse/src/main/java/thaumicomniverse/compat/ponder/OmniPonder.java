package thaumicomniverse.compat.ponder;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import net.createmod.ponder.api.registration.*;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import thaumicomniverse.RevivalFeatures;

/** Loaded only from the client entry point, after both Create and Ponder are present. */
public final class OmniPonder implements PonderPlugin {
    public static final String MOD = "thaumicomniverse";
    public static final ResourceLocation THAUMCRAFT = id("thaumcraft"), OMNIVERSE = id("omniverse"), FUTURES = id("futures");
    public record Lesson(String id, String title, int size, String group, Consumer<PonderStage> story, String... items) {
        boolean enabled() {
            return switch (group) {
                case "create" -> RevivalFeatures.Group.CREATE.enabled();
                case "tubes" -> RevivalFeatures.Group.TUBES.enabled();
                case "restoration" -> RevivalFeatures.Group.RESTORATION.enabled();
                case "futures" -> net.minecraftforge.fml.ModList.get().isLoaded("thaumicfutures")
                    && thaumcraft.api.config.FeatureRecipes.enabled(ResourceLocation.parse("thaumicfutures:restored/ancient_matrix"));
                default -> true;
            };
        }
        ResourceLocation tag() { return switch(group){case "tc"->THAUMCRAFT;case "futures"->FUTURES;default->OMNIVERSE;}; }
        ResourceLocation[] components(){
            if(id.equals("infernal"))return new ResourceLocation[]{ResourceLocation.parse("thaumcraft:infernal_furnace")};
            return java.util.Arrays.stream(items).map(ResourceLocation::parse).filter(ForgeRegistries.ITEMS::containsKey).toArray(ResourceLocation[]::new);
        }
    }
    public static final List<Lesson> LESSONS = List.of(
        new Lesson("restored_altar", "Improving an Infusion Altar", 7, "futures", RestoredScenes::restoredAltar,
            "thaumicfutures:ancient_matrix", "thaumicfutures:eldritch_matrix", "thaumicfutures:ancient_pedestal", "thaumicfutures:eldritch_pedestal", "thaumicfutures:infusion_speed_stone", "thaumicfutures:infusion_efficiency_stone"),
        new Lesson("wireless", "Transferring Essentia without pipes", 7, "futures", RestoredScenes::wireless,
            "thaumicfutures:essentia_extractor", "thaumicfutures:essentia_injector"),
        new Lesson("tables", "Preparing a magical workshop", 5, "tc", ThaumcraftScenes::tables,
            "thaumcraft:table", "thaumcraft:research_table", "thaumcraft:scribing_tools", "thaumcraft:arcane_workbench"),
        new Lesson("crucible", "Alchemy in a Crucible", 5, "tc", ThaumcraftScenes::crucible,
            "thaumcraft:crucible"),
        new Lesson("distillation", "Distilling and transporting Essentia", 7, "tc", ThaumcraftScenes::distillation,
            "thaumcraft:alchemy_furnace", "thaumcraft:alembic", "thaumcraft:essentia_tube", "thaumcraft:essentia_jar", "thaumcraft:filled_essentia_jar", "thaumcraft:void_jar", "thaumcraft:filled_void_jar"),
        new Lesson("suction", "Controlling Essentia suction", 7, "tc", ThaumcraftScenes::suction,
            "thaumcraft:essentia_tube", "thaumcraft:essentia_valve", "thaumcraft:essentia_filter", "thaumcraft:essentia_restrictor", "thaumcraft:essentia_oneway", "thaumcraft:essentia_buffer"),
        new Lesson("infusion_build", "Building an Infusion Altar", 7, "tc", ThaumcraftScenes::infusionBuild,
            "thaumcraft:infusion_matrix", "thaumcraft:arcane_pedestal", "thaumcraft:arcane_stone", "thaumcraft:arcane_stone_bricks"),
        new Lesson("infusion", "Performing Infusion", 9, "tc", ThaumcraftScenes::infusion,
            "thaumcraft:infusion_matrix", "thaumcraft:arcane_pedestal"),
        new Lesson("thaumatorium", "Automating Crucible recipes", 5, "tc", ThaumcraftScenes::thaumatorium,
            "thaumcraft:alchemical_construct", "thaumcraft:thaumatorium", "thaumcraft:mnemonic_matrix"),
        new Lesson("advanced_alchemy", "Building an Advanced Alchemical Furnace", 7, "tc", ThaumcraftScenes::advancedAlchemy,
            "thaumcraft:advanced_alchemical_construct", "thaumcraft:advanced_alchemy_furnace"),
        new Lesson("infernal", "Building an Infernal Furnace", 7, "tc", ThaumcraftScenes::infernal),
        new Lesson("node_jar", "Capturing an Aura Node", 5, "tc", ThaumcraftScenes::nodeJar,
            "thaumcraft:node_jar", "thaumcraft:aura_node"),
        new Lesson("energize", "Energizing an Aura Node", 5, "tc", ThaumcraftScenes::energize,
            "thaumcraft:node_converter", "thaumcraft:node_stabilizer", "thaumcraft:advanced_node_stabilizer"),
        new Lesson("vis_relay", "Distributing Vis with Relays", 9, "tc", ThaumcraftScenes::visRelay,
            "thaumcraft:vis_relay", "thaumcraft:workbench_charger"),
        new Lesson("bore", "Operating the Arcane Bore", 7, "tc", ThaumcraftScenes::bore,
            "thaumcraft:arcane_bore", "thaumcraft:arcane_bore_base"),
        new Lesson("eldritch", "Opening an Eldritch Portal", 7, "tc", ThaumcraftScenes::eldritch,
            "thaumcraft:eldritch_eye", "thaumcraft:eldritch_altar", "thaumcraft:eldritch_obelisk"),
        new Lesson("convenient", "Moving Essentia with Servos", 7, "tubes", OmniverseScenes::convenient,
            "thaumicomniverse:convenient_essentia_tube", "thaumicomniverse:convenient_essentia_filter", "thaumicomniverse:convenient_essentia_restrictor", "thaumicomniverse:essentia_servo"),
        new Lesson("routing", "Sorting and prioritizing Essentia", 7, "tubes", OmniverseScenes::routing,
            "thaumicomniverse:convenient_essentia_tube", "thaumicomniverse:convenient_essentia_filter", "thaumicomniverse:convenient_essentia_restrictor"),
        new Lesson("crushing_wheel", "Crystallizing aspects with Crushing Wheels", 7, "create", OmniverseScenes::crushingWheel,
            "thaumicomniverse:thaumic_crushing_wheel"),
        new Lesson("motor", "Generating rotation with Vis", 7, "create", OmniverseScenes::motor,
            "thaumicomniverse:arcane_motor"),
        new Lesson("burner", "Heating with Essentia", 5, "create", OmniverseScenes::burner,
            "thaumicomniverse:arcane_blaze_burner", "thaumicomniverse:empty_arcane_blaze_burner"),
        new Lesson("interface", "Exchanging Essentia with Contraptions", 7, "create", OmniverseScenes::portableInterface,
            "thaumicomniverse:portable_essentia_interface"),
        new Lesson("charger", "Charging tools and Deployers", 7, "create", OmniverseScenes::charger,
            "thaumicomniverse:wand_charger"),
        new Lesson("charger_crafting", "Arcane recipes in Mechanical Crafters", 9, "create", OmniverseScenes::chargerCrafting,
            "thaumicomniverse:wand_charger"),
        new Lesson("switch", "Monitoring Essentia stocks", 5, "create", OmniverseScenes::stockpile,
            "create:stockpile_switch"),
        new Lesson("coupler", "Connecting separated structures", 5, "create", OmniverseScenes::coupler,
            "thaumicomniverse:arcane_suspension_coupler"),
        new Lesson("magnet", "Attracting magnetic structures", 9, "create", OmniverseScenes::magnet,
            "thaumicomniverse:arcane_kinetic_magnet", "thaumicomniverse:thaumic_magnetic_casing"),
        new Lesson("depot", "Delivering items with Floating Depots", 9, "create", OmniverseScenes::depot,
            "thaumicomniverse:arcane_floating_depot"),
        new Lesson("spout", "Processing items with Essentia", 7, "create", OmniverseScenes::spout,
            "thaumicomniverse:essentia_spout")
    );
    public static ResourceLocation id(String path) { return new ResourceLocation(MOD, path); }
    public static void register() { PonderIndex.addPlugin(new OmniPonder()); BookPonderBridge.register(); }
    @Override public String getModId() { return MOD; }
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        for (var lesson : LESSONS) if (lesson.enabled()) {
            var items = lesson.components();
            if (items.length > 0) helper.forComponents(items).addStoryBoard(lesson.id(),
                (scene, util) -> { var stage = new PonderStage(scene, util, lesson); lesson.story().accept(stage); stage.finish(); }, lesson.tag());
        }
    }
    @Override public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(THAUMCRAFT).addToIndex().item(PonderStage.item("thaumcraft:infusion_matrix").getItem(), true, false)
            .title("Thaumcraft").description("Magical structures, Essentia and Vis networks").register();
        helper.registerTag(OMNIVERSE).addToIndex().item(PonderStage.item("thaumicomniverse:arcane_motor").getItem(), true, false)
            .title("Thaumic Omniverse").description("Arcane machinery and expanded magical devices").register();
        if(net.minecraftforge.fml.ModList.get().isLoaded("thaumicfutures"))
            helper.registerTag(FUTURES).addToIndex().item(PonderStage.item("thaumicfutures:ancient_matrix").getItem(),true,false)
                .title("Thaumic Futures").description("Restored structures and devices from Thaumic Futures").register();
        for (var lesson : LESSONS) if (lesson.enabled()) for (String item : lesson.items()) {
            var location = new ResourceLocation(item);
            if (ForgeRegistries.ITEMS.containsKey(location)) helper.addToTag(lesson.tag()).add(location);
        }
        for (String item : thaumicomniverse.client.OmniTutorialTooltips.ITEMS) {
            var location = id(item);
            if (ForgeRegistries.ITEMS.containsKey(location)) helper.addToTag(OMNIVERSE).add(location);
        }
    }
    @Override public void registerSharedText(SharedTextRegistrationHelper helper) {
        // Explicit shared keys survive inserting or reordering captions. JSON also supplies normal playback.
        try (var stream = OmniPonder.class.getResourceAsStream("/assets/thaumicomniverse/lang/en_us.json")) {
            if (stream == null) throw new IllegalStateException("Missing Omniverse language resources");
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            String prefix = MOD + ".ponder.shared.";
            json.entrySet().stream().filter(e -> e.getKey().startsWith(prefix))
                .forEach(e -> helper.registerSharedText(e.getKey().substring(prefix.length()), e.getValue().getAsString()));
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot read Ponder captions", error); }
    }
}
