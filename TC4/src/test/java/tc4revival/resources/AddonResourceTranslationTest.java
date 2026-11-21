package tc4revival.resources;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AddonResourceTranslationTest {
    @TempDir Path game;

    @Test void migratedAddonCanRetainItsLegacyAssetsOnlyWhenDeclared() throws Exception {
        var json=JsonParser.parseString("{\"name\":\"Legacy\",\"legacy_modid\":\"old\",\"sha256\":\""+"a".repeat(64)+"\",\"download\":\"https://example.org/source\",\"mapping\":{\"assets/legacy/textures/item/brass.png\":\"assets/old/brass.png\"}}").getAsJsonObject();
        assertThrows(java.io.IOException.class,()->AddonResourceRequirements.decode("future",json));
        var aliases=new JsonArray();aliases.add("legacy");json.add("resource_namespace_aliases",aliases);
        var requirement=AddonResourceRequirements.decode("future",json).get(0);
        var source=new OriginalResources.Snapshot(game.resolve("old.jar"),"sample",Map.of("assets/old/brass.png",new byte[]{1,2,3}));
        var prepared=AddonResourceRequirements.prepare(game,new AddonResourceRequirements.Resolved(requirement,source));
        assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(prepared.directory().resolve("assets/legacy/textures/item/brass.png")));
        assertThrows(java.io.IOException.class,()->AddonResourceRequirements.prepare(game,new AddonResourceRequirements.Resolved(requirement,source),out->out.put("assets/another/item.png",new byte[]{1})));
        aliases.add("thaumcraft");
        assertThrows(java.io.IOException.class,()->AddonResourceRequirements.decode("future",json));
    }

    @Test void multipleOriginalArchivesCanSupplyDisjointLanguageKeysButNotConflictingKeysOrImages() throws Exception {
        String source="{\"name\":\"Example\",\"legacy_modid\":\"example\",\"sha256\":\"%s\",\"download\":\"https://example.org/source\",\"mapping\":{\"assets/example/lang/zh_cn.json\":\"assets/old/lang/zh_CN.lang\"},\"languages\":{\"assets/example/lang/zh_cn.json\":{\"%s\":\"old\"}}}";
        var root=JsonParser.parseString(source.formatted("a".repeat(64),"first")).getAsJsonObject();
        var additional=new JsonArray();additional.add(JsonParser.parseString(source.formatted("b".repeat(64),"second")));root.add("additional_sources",additional);
        assertEquals(2,AddonResourceRequirements.decode("example",root).size());
        additional.set(0,JsonParser.parseString(source.formatted("b".repeat(64),"first")));
        assertThrows(java.io.IOException.class,()->AddonResourceRequirements.decode("example",root));
        root.remove("languages");additional.get(0).getAsJsonObject().remove("languages");
        assertThrows(java.io.IOException.class,()->AddonResourceRequirements.decode("example",root));
    }

    @Test void legacyAnimationAndBareSoundNamesRemainUsableInModernLoader() throws Exception {
        String image="assets/example/textures/items/animated.png", sounds="assets/example/sounds.json";
        var requirement=new AddonResourceRequirements.Requirement("example","Example","Example","","",
                Map.of(image,"old.png",sounds,"old-sounds.json"),Map.of());
        String animation="{\"animation\":{\"frames\":[{index:0,time:120},1,2]}}";
        String audio="{\"event\":{\"sounds\":[\"tone\",{\"name\":\"chime\",\"volume\":0.5},\"minecraft:click\"]}}";
        var source=new OriginalResources.Snapshot(game.resolve("old.jar"),"sample",Map.of(
                "old.png",new byte[]{1},"old.png.mcmeta",animation.getBytes(StandardCharsets.UTF_8),
                "old-sounds.json",audio.getBytes(StandardCharsets.UTF_8)));
        var prepared=AddonResourceRequirements.prepare(game,new AddonResourceRequirements.Resolved(requirement,source));
        var strict=net.minecraft.util.GsonHelper.parse(Files.readString(prepared.directory().resolve(image+".mcmeta")));
        var frames=strict.getAsJsonObject("animation").getAsJsonArray("frames");
        assertEquals(120,frames.get(0).getAsJsonObject().get("time").getAsInt());
        assertEquals(2,frames.get(2).getAsInt());
        var list=net.minecraft.util.GsonHelper.parse(Files.readString(prepared.directory().resolve(sounds)))
                .getAsJsonObject("event").getAsJsonArray("sounds");
        assertEquals("example:tone",list.get(0).getAsString());
        assertEquals("example:chime",list.get(1).getAsJsonObject().get("name").getAsString());
        assertEquals(0.5,list.get(1).getAsJsonObject().get("volume").getAsDouble());
        assertEquals("minecraft:click",list.get(2).getAsString());
        assertArrayEquals(new byte[]{1},Files.readAllBytes(prepared.directory().resolve(image)));
    }

    @Test void crashAffectedModelsParseWithMinecraftModelLoader() throws Exception {
        Path addons=Path.of("../Addons");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(addons));
        int count=0;
        for(String project:List.of("Automagy","ThaumicHorizons")) {
            Path root=addons.resolve(project+"/src/main/resources/assets");
            try(var files=Files.walk(root)) {
                for(var file:files.filter(p->p.toString().endsWith(".json")&&p.toString().contains("models")).toList()) {
                    String name=file.getFileName().toString();
                    if(name.startsWith("tally_")||name.startsWith("thaumostatic_")||List.of("blood_phial.json","injection_phial.json","suicide_pill.json").contains(name)) {
                        assertDoesNotThrow(()->net.minecraft.client.renderer.block.model.BlockModel.fromString(Files.readString(file)),file.toString());
                        count++;
                    }
                }
            }
        }
        assertTrue(count>=42,"All crash log model variants covered");
    }

    @Test void automagyItemAspectDataUsesTheRealBaseDecoder() throws Exception {
        Path file=Path.of("../Addons/Automagy/src/main/resources/data/automagy/item_aspects/explicit.json");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isRegularFile(file));
        assertFalse(tc4revival.aspect.item.ItemAspectJson.decode(JsonParser.parseString(Files.readString(file))).isEmpty());
    }

    @Test void missingTranslationsFallbackWithoutHidingAuthoredTranslations() throws Exception {
        String en="assets/example/lang/en_us.json", zh="assets/example/lang/zh_cn.json";
        var keys=Map.of("modern.present","old.present","modern.fallback","old.fallback","modern.authored","old.absent","modern.placeholder","old.placeholder");
        var requirement=new AddonResourceRequirements.Requirement("example","Example","Example","","",
                Map.of(en,"en.lang",zh,"zh.lang"),Map.of(en,keys,zh,keys));
        var source=new OriginalResources.Snapshot(game.resolve("old.jar"),"sample",Map.of(
                "en.lang","\uFEFFold.present=English\nold.fallback=Fallback=value\nold.placeholder=Not translated".getBytes(StandardCharsets.UTF_8),
                "zh.lang","old.present=中文\nold.placeholder=Not translated".getBytes(StandardCharsets.UTF_8)));
        var resolved=new AddonResourceRequirements.Resolved(requirement,source);
        var prepared=AddonResourceRequirements.prepare(game,resolved);
        var translated=JsonParser.parseString(Files.readString(prepared.directory().resolve(zh))).getAsJsonObject();
        assertEquals("中文",translated.get("modern.present").getAsString());
        assertFalse(translated.has("modern.fallback"), "Do not mask lower-priority Chinese translations with English");
        assertFalse(translated.has("modern.authored"));
        assertFalse(translated.has("modern.placeholder"),"An English placeholder must not hide a real Chinese fallback");
        var english=JsonParser.parseString(Files.readString(prepared.directory().resolve(en))).getAsJsonObject();
        assertEquals("English",english.get("modern.present").getAsString());
        assertEquals("Fallback=value",english.get("modern.fallback").getAsString());
        assertEquals("Not translated",english.get("modern.placeholder").getAsString());
        assertFalse(english.has("modern.authored"));
        assertEquals(prepared,AddonResourceRequirements.prepare(game,resolved));
    }

    @Test void installedAddonReferenceLanguagesCanAllBePrepared() throws Exception {
        var roots=Map.of("ThaumicTinkerer","thaumic_tinkerer","ForbiddenMagic","forbidden_magic",
                "ThaumicEnergistics","thaumicenergistics","ThaumicHorizons","thaumichorizons","Automagy","automagy");
        for(var entry:roots.entrySet()) {
            Path root=Path.of("../Addons",entry.getKey());
            org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(root));
            var json=JsonParser.parseString(Files.readString(root.resolve("src/main/resources/data/"+entry.getValue()+"/thaumcraft/original_resources.json"))).getAsJsonObject();
            var mapping=new TreeMap<String,String>();
            json.getAsJsonObject("mapping").entrySet().forEach(e->mapping.put(e.getKey(),e.getValue().getAsString()));
            var languages=new TreeMap<String,Map<String,String>>();
            if(json.has("languages"))json.getAsJsonObject("languages").entrySet().forEach(e->{
                var keys=new TreeMap<String,String>();e.getValue().getAsJsonObject().entrySet().forEach(k->keys.put(k.getKey(),k.getValue().getAsString()));languages.put(e.getKey(),keys);
            });
            var requirement=new AddonResourceRequirements.Requirement(entry.getValue(),entry.getKey(),json.get("legacy_modid").getAsString(),json.get("sha256").getAsString(),json.get("download").getAsString(),mapping,languages);
            Path workspace=Path.of("../..");
            Path archives=workspace.resolve("References/Projects").resolve(entry.getKey());
            if (!Files.isDirectory(archives)) archives=workspace.resolve("References/Addons");
            Path reference;
            try(var files=Files.list(archives)) {
                reference=files.filter(p->p.toString().endsWith(".jar")).filter(p->{try{return OriginalResources.sha256(Files.readAllBytes(p)).equals(requirement.sha256());}catch(Exception ex){throw new RuntimeException(ex);}}).findFirst().orElseThrow();
            }
            var snapshot=OriginalResources.read(reference,mapping,requirement.legacyModId(),false);
            var prepared=AddonResourceRequirements.prepare(game,new AddonResourceRequirements.Resolved(requirement,snapshot));
            try(var original=new java.util.zip.ZipFile(reference.toFile());
                var pack=new ExternalResourcePack("animation-test",prepared.directory(),prepared.allowed())) {
                for(var resource:mapping.entrySet()) if(resource.getKey().endsWith(".png") && original.getEntry(resource.getValue()+".mcmeta")!=null) {
                    String target=resource.getKey()+".mcmeta";
                    assertTrue(prepared.allowed().contains(target),target);
                    String[] parts=target.split("/",3);
                    var stream=pack.getResource(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(parts[1],parts[2]));
                    assertNotNull(stream,target);
                    try(var input=stream.get(); var expected=original.getInputStream(original.getEntry(resource.getValue()+".mcmeta"))) {
                        assertArrayEquals(expected.readAllBytes(),input.readAllBytes(),target);
                    }
                }
            }
            for(String language:languages.keySet())assertTrue(JsonParser.parseString(Files.readString(prepared.directory().resolve(language))).isJsonObject());
            var expected=new HashSet<net.minecraft.resources.ResourceLocation>();
            for(String path:prepared.allowed())if(path.startsWith("assets/")) {
                var parts=path.split("/",3);
                var id=net.minecraft.resources.ResourceLocation.tryParse(parts[1]+":"+parts[2]);
                assertNotNull(id,"Invalid modern resource path: "+path);
                expected.add(id);
            }
            try(var pack=new ExternalResourcePack(entry.getValue(),prepared.directory(),prepared.allowed())) {
                var seen=new HashSet<net.minecraft.resources.ResourceLocation>();
                pack.listResources(net.minecraft.server.packs.PackType.CLIENT_RESOURCES,entry.getValue(),"",(id,stream)->{
                    seen.add(id);
                    assertDoesNotThrow(()->{try(var input=stream.get()){assertNotNull(input);}});
                });
                assertEquals(expected,seen,"Every converted resource must be enumerated and readable: "+entry.getKey());
            }
        }
    }
}
