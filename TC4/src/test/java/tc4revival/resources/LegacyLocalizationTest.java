package tc4revival.resources;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LegacyLocalizationTest {
    @TempDir Path game;
    private Path zip(String name, Map<String,String> files) throws Exception {
        Path path=game.resolve(name);
        try(var out=new ZipOutputStream(Files.newOutputStream(path))) {
            for(var entry:files.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));out.closeEntry();
            }
        }
        return path;
    }
    @Test void onlyExplicitKeysAndNamespacesAreExposedAndUpstreamFirstFileWins() throws Exception {
        var first=zip("first.zip",Map.of("assets/legacy/lang/zh_CN.lang","\uFEFFold=中文=值\n#comment=x\nother=不要加载",
                "assets/minecraft/lang/zh_cn.lang","item.stone=污染", "assets/legacy/textures/a.png","not language"));
        var second=zip("second.zip",Map.of("assets/legacy/lang/zh_cn.lang","old=后面的版本不应覆盖\nmissing=不同版本"));
        var mapping=new LegacyLocalization.Mapping("example","assets/legacy/lang/zh_cn.lang",
                Map.of("item.example.name","old","missing","missing"),Map.of());
        var prepared=LegacyLocalization.prepare(game,List.of(first,second),List.of(mapping));
        assertEquals(1,prepared.entries());
        assertEquals(Set.of("pack.mcmeta","assets/example/lang/zh_cn.json"),prepared.allowed());
        var json=JsonParser.parseString(Files.readString(prepared.directory().resolve("assets/example/lang/zh_cn.json"))).getAsJsonObject();
        assertEquals("中文=值",json.get("item.example.name").getAsString());assertEquals(1,json.size());
        Files.writeString(prepared.directory().resolve("assets/example/lang/zh_cn.json"),"damaged");
        assertEquals(prepared,LegacyLocalization.prepare(game,List.of(first,second),List.of(mapping)));
        assertTrue(Files.readString(prepared.directory().resolve("assets/example/lang/zh_cn.json")).contains("中文=值"));
    }
    @Test void declarationsCannotSelectTexturesTraversalOrOtherLocales() {
        for(String source:List.of("assets/legacy/lang/en_US.lang","assets/../lang/zh_cn.lang","assets/legacy/textures/a.png")) {
            String text="{\"format\":1,\"minecraft_version\":\"1.7.10\",\"sources\":[{\"path\":\""+source+"\",\"keys\":{}}]}";
            assertThrows(java.io.IOException.class,()->LegacyLocalization.decode("example",JsonParser.parseString(text).getAsJsonObject()));
        }
    }
    private Pack pack(String id, String en, String zh) throws Exception {
        Path root=game.resolve(id.replace('/','_'));
        Path lang=root.resolve("assets/example/lang");Files.createDirectories(lang);
        Files.writeString(lang.resolve("en_us.json"),en);Files.writeString(lang.resolve("zh_cn.json"),zh);
        SharedConstants.tryDetectVersion();
        return Pack.create(id,Component.literal(id),true,key->new PathPackResources(key,root,false),
                new Pack.Info(Component.literal(id),15,FeatureFlagSet.of()),PackType.CLIENT_RESOURCES,Pack.Position.BOTTOM,true,PackSource.BUILT_IN);
    }
    @Test void actualLanguageLoaderUsesChineseFallbackThenAnyOtherPackIncludingVanilla() throws Exception {
        var fallback=pack(LegacyLocalization.PACK_ID,"{}","{\"missing\":\"补全\",\"official\":\"补全\",\"vanillaKey\":\"补全\",\"user\":\"补全\"}");
        var vanilla=pack("vanilla","{}","{\"vanillaKey\":\"原版\"}");
        var supplement=pack(LegacyOfficialTranslations.PACK_ID,"{}","{\"official\":\"维护版\",\"missing\":\"维护版补全\"}");
        var external=pack("thaumcraft_original_resources","{\"missing\":\"English\",\"englishOnly\":\"English fallback\"}","{\"official\":\"官方\"}");
        var mod=pack("mod_resources","{}","{}");
        var userA=pack("file/a","{}","{\"user\":\"用户甲\"}");
        var userB=pack("file/b","{}","{\"user\":\"用户乙\"}");
        for(var users:List.of(List.of(userA,userB),List.of(userB,userA))) {
            var selected=new ArrayList<>(users);selected.addAll(List.of(mod,supplement,fallback,external,vanilla));
            var ordered=ExternalResourceOrder.normalize(selected);
            assertEquals(LegacyLocalization.PACK_ID,ordered.get(0).getId());
            assertEquals(LegacyOfficialTranslations.PACK_ID,ordered.get(1).getId());
            try(var manager=new MultiPackResourceManager(PackType.CLIENT_RESOURCES,ordered.stream().map(Pack::open).toList())) {
                var language=net.minecraft.client.resources.language.ClientLanguage.loadFrom(manager,List.of("en_us","zh_cn"),false);
                assertEquals("维护版补全",language.getOrDefault("missing"));assertEquals("官方",language.getOrDefault("official"));
                assertEquals("原版",language.getOrDefault("vanillaKey"));assertEquals("English fallback",language.getOrDefault("englishOnly"));
                assertEquals(users.get(1)==userB?"用户乙":"用户甲",language.getOrDefault("user"));
            }
        }
        assertEquals(List.of(fallback,vanilla,userA),ExternalResourceOrder.normalize(List.of(vanilla,userA,fallback)));
    }
    @Test void missingOptionalModIsNotARequiredDependency() throws Exception {
        assertTrue(I18nUpdateBridge.find().isEmpty());assertTrue(I18nUpdateBridge.cached(game).isEmpty());
    }
    @Test void allInstalledProjectDeclarationsAreValidAndOnlyOptIntoOwnedOutput() throws Exception {
        int count=0;
        var roots=new ArrayList<Path>();roots.add(Path.of("src/main/resources/data"));
        try(var projects=Files.list(Path.of("../Addons"))) {
            projects.filter(Files::isDirectory).forEach(p->roots.add(p.resolve("src/main/resources/data")));
        }
        for(var root:roots) if(Files.isDirectory(root)) try(var files=Files.walk(root)) {
            for(var file:files.filter(p->p.getFileName().toString().equals("legacy_localization.json")).toList()) {
                String owner=file.getParent().getParent().getFileName().toString();
                var declaration=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                for(var entry:LegacyLocalization.decode(owner,declaration)) assertEquals(owner,entry.owner());
                for(var entry:LegacyOfficialTranslations.decode(owner,declaration)) assertEquals(owner,entry.owner());
                var originals=file.resolveSibling("original_resources.json");
                if(Files.isRegularFile(originals))assertFalse(AddonResourceRequirements.decode(owner,JsonParser.parseString(Files.readString(originals)).getAsJsonObject()).isEmpty());
                count++;
            }
        }
        assertEquals(23,count); // Includes the five new ports with original-resource declarations.
    }
}
