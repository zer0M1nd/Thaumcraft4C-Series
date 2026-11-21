package temodern.resources;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Validates the connection between supplied artwork and Minecraft's resource loader. */
class ModernResourcesTest {
    @Test void assemblerRimPreservesInteriorAndUsesVisibleThaumiumMidtones() throws Exception {
        var original=json(ART.resolve("runtime/assets/thaumicenergistics/models/block/arcane_assembler.json"));
        var actual=json(RES.resolve("assets/thaumicenergistics/models/block/arcane_assembler.json"));
        var before=original.getAsJsonArray("elements");var after=actual.getAsJsonArray("elements");
        assertEquals(before.size()+1,after.size());
        for(int i=0;i<before.size();i++)assertEquals(before.get(i),after.get(i),"Original internal geometry and UVs");
        var rim=ImageIO.read(RES.resolve("assets/thaumicenergistics/textures/modern/block/arcane_outer_rim.png").toFile());
        var shades=new HashSet<Integer>();
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            int pixel=rim.getRGB(x,y);boolean edge=x==0||x==15||y==0||y==15;
            assertEquals(edge?255:0,pixel>>>24,"Exactly one pixel on each outer edge");
            if(edge){shades.add(pixel);assertTrue((pixel&255)>=121&&((pixel>>8)&255)>=65,"Rim uses thaumium midtones, not near-black multiplied shadows");}
        }
        assertTrue(shades.size()>4,"AE2 border relief is retained");
    }
    static final Path RES = Path.of("src/main/resources");
    static final Path ART_ROOT = Path.of("../../../Docs/Projects/ThaumicEnergistics/art");
    static final Path ART = ART_ROOT.resolve("0920_02-handoff");
    static JsonObject json(Path file) throws Exception {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    @Test void deliveredArtworkIsUnchangedAndModelsOnlyGainParticleMaterials() throws Exception {
        var corrections=json(ART_ROOT.resolve("resource-review-0.2.6/manifest.json")).getAsJsonObject("files");
        json(ART_ROOT.resolve("report0921_1/manifest.json")).getAsJsonObject("files").entrySet().forEach(e->corrections.add(e.getKey(),e.getValue()));
        corrections.remove("assets/ae2/screens/terminals/thaumicenergistics_arcane.json");
        try (var files = Files.walk(ART.resolve("runtime/assets"))) {
            for (var source : files.filter(Files::isRegularFile).toList()) {
                var relative=ART.resolve("runtime").relativize(source);
                var target = RES.resolve(relative);
                if(relative.toString().replace('\\','/').equals("assets/ae2/screens/terminals/thaumicenergistics_arcane.json"))continue; // Slot/button layout checked separately below.
                if(corrections.has(relative.toString().replace('\\','/')))continue;
                if (source.toString().endsWith(".json") && source.toString().contains("models")) {
                    var expected = json(source);
                    boolean drive = expected.has("parent") && expected.get("parent").getAsString().equals("ae2:block/drive/drive_cell");
                    if ((expected.has("elements") || drive) && expected.has("textures") && !expected.getAsJsonObject("textures").has("particle")) {
                        var textures = expected.getAsJsonObject("textures");
                        textures.addProperty("particle", "#" + textures.keySet().iterator().next());
                    }
                    assertEquals(expected, json(target), target.toString());
                } else assertArrayEquals(Files.readAllBytes(source), Files.readAllBytes(target), target.toString());
            }
        }
        for(var correction:corrections.entrySet())assertEquals(correction.getValue().getAsString(),HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(RES.resolve(correction.getKey())))),correction.getKey());
        var approved = json(RES.resolve("META-INF/thaumicenergistics-modern-assets.json")).getAsJsonObject("textures");
        assertEquals(58, approved.size());
        for (var entry : approved.entrySet()) {
            assertEquals(entry.getValue().getAsString(), HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(RES.resolve(entry.getKey())))));
        }
    }

    @Test void everyModernModelTextureIsStitchedAndEveryCustomParentExists() throws Exception {
        var stitched = new HashSet<String>();
        for (var source : json(RES.resolve("assets/minecraft/atlases/blocks.json")).getAsJsonArray("sources")) {
            var entry = source.getAsJsonObject();
            if (entry.has("resource")) stitched.add(entry.get("resource").getAsString());
        }
        try (var files = Files.walk(RES.resolve("assets/thaumicenergistics/models"))) {
            for (var source : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                var model = json(source);
                if (model.has("parent")) {
                    String parent = model.get("parent").getAsString();
                    if (parent.startsWith("thaumicenergistics:"))
                        assertTrue(Files.exists(RES.resolve("assets/thaumicenergistics/models/" + parent.split(":")[1] + ".json")), parent);
                }
                if (!model.has("textures")) continue;
                for (var entry : model.getAsJsonObject("textures").entrySet()) {
                    String texture = entry.getValue().getAsString();
                    if (!texture.startsWith("thaumicenergistics:modern/")) continue;
                    assertTrue(Files.isRegularFile(RES.resolve("assets/thaumicenergistics/textures/" + texture.split(":")[1] + ".png")), texture);
                    assertTrue(stitched.contains(texture), "Missing atlas source: " + texture);
                }
            }
        }
        var legacy = json(RES.resolve("data/thaumicenergistics/thaumcraft/original_resources.json")).getAsJsonObject("mapping");
        assertFalse(legacy.has("assets/thaumicenergistics/textures/items/essentia.cell.1k.png"));
        assertTrue(legacy.has("assets/thaumicenergistics/textures/items/foci.aewrench.png"));
    }

    @Test void allCellTiersShareOnlyTheIndicatorTintAndPurpleDriveShell() throws Exception {
        var map = json(ART.resolve("integration/storage_cells.json"));
        assertEquals(11, map.getAsJsonArray("cells").size());
        byte[] firstDrive = null;
        for (var value : map.getAsJsonArray("cells")) {
            var cell = value.getAsJsonObject();
            var item = cell.get("item").getAsString().split(":")[1];
            var model = json(RES.resolve("assets/thaumicenergistics/models/item/" + item + ".json"));
            assertEquals("minecraft:item/generated", model.get("parent").getAsString());
            assertEquals("thaumicenergistics:modern/item/essentia_cell_led", model.getAsJsonObject("textures").get("layer1").getAsString());
            var drive = json(RES.resolve("assets/thaumicenergistics/models/" + cell.get("drive_model").getAsString().split(":")[1] + ".json"));
            assertEquals("ae2:block/drive/drive_cell", drive.get("parent").getAsString());
            var png = RES.resolve("assets/thaumicenergistics/textures/" + drive.getAsJsonObject("textures").get("cell").getAsString().split(":")[1] + ".png");
            var shell=ImageIO.read(png.toFile());
            assertEquals(0xff282331,shell.getRGB(1,3),"North face lamp uses mirrored U at texture x=1");
            assertEquals(shell.getRGB(3,3),shell.getRGB(4,3),"No second lamp hole on the right");
            if (firstDrive == null) firstDrive = Files.readAllBytes(png);
            else assertArrayEquals(firstDrive, Files.readAllBytes(png), "Drive shell must not vary by capacity");
        }
        var mask = ImageIO.read(RES.resolve("assets/thaumicenergistics/textures/modern/item/essentia_cell_led.png").toFile());
        assertEquals(16, mask.getWidth()); assertEquals(16, mask.getHeight());
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
            assertEquals(x == 9 && y == 12 ? 0xffffffff : 0, mask.getRGB(x,y), "LED mask at " + x + "," + y);
    }

    @Test void guiSlotFramesMatchTheDeliveredLayoutAtEverySlot() throws Exception {
        var panels = json(ART.resolve("integration/gui_layouts.json")).getAsJsonObject("panels");
        for (var entry : panels.entrySet()) {
            var panel = entry.getValue().getAsJsonObject();
            var image = ImageIO.read(RES.resolve("assets/thaumicenergistics/textures/modern/gui/" + entry.getKey() + ".png").toFile());
            assertEquals(256, image.getWidth()); assertEquals(256, image.getHeight());
            int width = panel.getAsJsonArray("panel").get(0).getAsInt(), height = panel.getAsJsonArray("panel").get(1).getAsInt();
            for (var value : panel.getAsJsonObject("slots").entrySet()) {
                var slot = value.getValue().getAsJsonObject();
                for (int row = 0; row < slot.get("rows").getAsInt(); row++) for (int col = 0; col < slot.get("columns").getAsInt(); col++) {
                    int x = slot.getAsJsonArray("item_origin").get(0).getAsInt() + col * 18;
                    int y = slot.getAsJsonArray("item_origin").get(1).getAsInt() + row * 18;
                    assertTrue(x > 0 && y > 0 && x + 17 < width && y + 17 < height, entry.getKey() + " " + value.getKey());
                    assertNotEquals(image.getRGB(x-1,y-1), image.getRGB(x+8,y+8), "Missing slot frame at " + entry.getKey() + " " + x + "," + y);
                    assertEquals(255, image.getRGB(x+8,y+8) >>> 24);
                }
            }
        }
        var style = json(RES.resolve("assets/ae2/screens/terminals/thaumicenergistics_arcane.json"));
        var slots=style.getAsJsonObject("slots");
        // Coordinates verified against the shipped 1.1.3.0 JAR, whose base height is 243 (not GTNH's 244).
        var expected=Map.of("CRAFTING_GRID",new int[]{44,90},"CRAFTING_RESULT",new int[]{116,126},
                "thaumicenergistics_wand",new int[]{116,90},"thaumicenergistics_armor",new int[]{8,81},
                "PLAYER_INVENTORY",new int[]{8,162},"PLAYER_HOTBAR",new int[]{8,220});
        for(var entry:expected.entrySet()){
            var actual=slots.getAsJsonObject(entry.getKey());
            assertEquals(entry.getValue()[0],actual.get("left").getAsInt());
            assertEquals(entry.getValue()[1],243-actual.get("bottom").getAsInt());
        }
        var bottom=style.getAsJsonObject("terminalStyle").getAsJsonObject("bottom");
        assertEquals("thaumicenergistics:textures/gui/arcane.crafting.png",bottom.get("texture").getAsString());
        assertEquals(JsonParser.parseString("[0,71,195,172]"),bottom.getAsJsonArray("srcRect"));
        assertEquals(Set.of("clearCraftingGrid","clearToPlayerInv","swapArmor"),style.getAsJsonObject("widgets").keySet());
    }

    @Test void arcaneDisplayRetainsNativeGrayFrameAndDoesNotTintIt() throws Exception {
        var base=json(RES.resolve("assets/thaumicenergistics/models/part/arcane_terminal_base.json"));
        assertEquals("ae2:part/display_base",base.get("parent").getAsString());
        var overlay=ImageIO.read(RES.resolve("assets/thaumicenergistics/textures/modern/part/terminal_green.png").toFile());
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)if(x<=2||x>=13||y<=2||y>=13)
            assertEquals(0,overlay.getRGB(x,y)>>>24,"Green overlay must leave AE2's outer frame untouched");
        var item=json(RES.resolve("assets/thaumicenergistics/models/item/arcane_terminal.json"));
        assertTrue(item.getAsJsonObject("textures").asMap().values().stream().anyMatch(v->v.getAsString().equals("ae2:part/monitor_front")));
        for(var element:item.getAsJsonArray("elements"))for(var face:element.getAsJsonObject().getAsJsonObject("faces").asMap().values())
            assertFalse(face.getAsJsonObject().has("tintindex"),"AE cable dye must not recolor the fixed green display");
    }
}
