package thaumicomniverse.verification;

import com.google.gson.*;
import java.nio.file.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Test-only source set, never packaged into the addon. Runs in isolated dedicated servers. */
@Mod("thaumicomniverse_verification")
public final class ServerVerification {
    private static final JsonArray RESULTS = new JsonArray();
    private MinecraftServer pending;
    private int warmup;
    public ServerVerification() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }
    public static ResourceLocation id(String path) { return ResourceLocation.parse(path.contains(":") ? path : (java.util.Set.of("mana_inkwell", "akashic_record", "arcane_crafting_upgrade", "veined_scribing_tools").contains(path) ? "thaumicfutures:" : "thaumicomniverse:") + path); }
    public static void require(boolean condition, String detail) { if (!condition) throw new AssertionError(detail); }
    public static void check(String name, Runnable test) {
        var result = new JsonObject(); result.addProperty("name", name);
        try { test.run(); result.addProperty("passed", true); }
        catch (Throwable failure) {
            result.addProperty("passed", false); result.addProperty("error", failure.toString());
            failure.printStackTrace();
        }
        RESULTS.add(result);
        System.out.println("OMNIVERSE_VERIFY " + result);
    }
    private void started(ServerStartedEvent event) { pending = event.getServer(); }
    private void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || pending == null || ++warmup < 100) return;
        var server = pending; pending = null;
        verify(server);
    }
    private void verify(MinecraftServer server) {
        check("core and compass registry", () -> {
            require(ForgeRegistries.ITEMS.containsKey(id("sinister_compass")), "Missing sinister compass");
            require(ForgeRegistries.ITEMS.containsKey(id("primal_compass")), "Missing primal compass");
            require(server.getRecipeManager().byKey(id("collab/sinister_compass")).isPresent(), "Missing compass infusion");
        });
        check("optional registration gates", () -> {
            String[][] entries = {{"avaritia", "akashic_record"}, {"botania", "mana_inkwell"},
                    {"modular_machinery_reborn", "machine_vis_bridge"}, {"allthemodium", "thaumaturge_certificate"}};
            for (var entry : entries) require(ForgeRegistries.ITEMS.containsKey(id(entry[1])) == (ModList.get().isLoaded(entry[0]) && (!java.util.Set.of("mana_inkwell", "akashic_record").contains(entry[1]) || ModList.get().isLoaded("thaumicfutures"))), "Wrong optional gate: " + entry[1]);
        });
        check("conquest save roundtrip", () -> {
            var data = new thaumicomniverse.content.ObeliskClaims(); data.conquer(-12, 23);
            var loaded = thaumicomniverse.content.ObeliskClaims.load(data.save(new net.minecraft.nbt.CompoundTag()));
            require(loaded.conquered(-12, 23) && !loaded.conquered(12, 23), "Conquest lost or aliased");
        });
        if (ModList.get().isLoaded("avaritia")) check("legacy infinity wand capacity and refill", () -> {
            var wand = (tc4revival.content.CastingWandItem) ForgeRegistries.ITEMS.getValue(id("thaumcraft:casting_wand"));
            var stack = wand.create(tc4revival.content.WandParts.Rod.require("thaumicomniverse:infinity"),
                    tc4revival.content.WandParts.Cap.require("thaumicomniverse:matrix"), false, false);
            require(wand.capacity(stack) == 999_999_900, "Legacy rod capacity not restored");
            var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(server.overworld());
            thaumcraft.api.wands.WandRod.tick(stack, player);
            for (var aspect : tc4revival.aspect.TCAspects.catalog().primals()) require(wand.vis(stack, aspect) == wand.capacity(stack), "Refill failed: " + aspect.tag());
            require(wand.capacity(wand.create(wand.rod(stack), wand.cap(stack), true, true)) == 1_499_999_850, "Sceptre overflow");
        });
        if (ModList.get().isLoaded("modular_machinery_reborn")) MachineryVerification.run(server);
        BasicCompatVerification.run(server);
        AutomationVerification.run(server);
        ResourceVerification.run(server);
        MergedCoreVerification.run(server);
        StructureVerification.run(server);
        if (ModList.get().isLoaded("allthemodium") && ModList.get().isLoaded("kubejs")) check("KubeJS final dragon recipe startup", () -> BasicCompatVerification.dragonRecipe(server));
        if (ModList.get().isLoaded("forestry") && ModList.get().isLoaded("mobees")) BeeVerification.run(server);
        if (ModList.get().isLoaded("sophisticatedbackpacks")) check("backpack verification setup", BackpackVerification::run);
        server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((ignored, error) -> server.execute(() -> {
            check("datapack reload", () -> { if (error != null) throw new AssertionError(error); });
            if (ModList.get().isLoaded("allthemodium") && ModList.get().isLoaded("kubejs")) check("KubeJS final dragon recipe reload", () -> BasicCompatVerification.dragonRecipe(server));
            finish(server);
        }));
    }
    private static void finish(MinecraftServer server) {
        check("save all dimensions", () -> server.saveEverything(true, true, true));
        try { Files.writeString(Path.of("omniverse-verification.json"), new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS)); }
        catch (Exception error) { throw new RuntimeException(error); }
        server.halt(false);
    }
}
