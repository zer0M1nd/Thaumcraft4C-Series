package fmmodern;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import tc4revival.resources.*;

class OriginalResourceTest {
    @TempDir Path temporary;
    @Test void originalArchiveConvertsEveryDeclaredResourceWithoutLaunchingClient() throws Exception {
        var json=JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/forbidden_magic/thaumcraft/original_resources.json"))).getAsJsonObject();
        assertEquals(json.getAsJsonObject("mapping").get("assets/forbidden_magic/textures/entity/heretic.png"),
                json.getAsJsonObject("mapping").get("assets/forbidden_magic/textures/entity/villager/profession/heretic.png"),
                "Vanilla profession-layer renderers must resolve the same original heretic skin");
        var requirement=AddonResourceRequirements.decode("forbidden_magic",json).get(0);
        var snapshot=OriginalResources.read(Path.of("references/Forbidden Magic-1.7.10-0.575.jar"),requirement.mapping(),requirement.legacyModId(),false);
        assertEquals(requirement.sha256(),snapshot.sha256());
        var prepared=AddonResourceRequirements.prepare(temporary,new AddonResourceRequirements.Resolved(requirement,snapshot));
        var expected=new java.util.HashSet<>(requirement.mapping().keySet());expected.add("pack.mcmeta");
        requirement.mapping().forEach((target,source)->{if(target.endsWith(".png")&&snapshot.resources().containsKey(source+".mcmeta"))expected.add(target+".mcmeta");});
        assertEquals(expected,prepared.allowed());
        for(String path:prepared.allowed())assertTrue(Files.size(prepared.directory().resolve(path))>0,path);
        for(var language:requirement.languages().entrySet()){
            var translated=JsonParser.parseString(Files.readString(prepared.directory().resolve(language.getKey()))).getAsJsonObject();
            // The declaration is a union of legacy locales. Missing strings and English text in
            // zh_cn must stay absent so real translations in lower-priority packs can fall back.
            var original=language(snapshot.resources().get(requirement.mapping().get(language.getKey())));
            var english=language(snapshot.resources().get(requirement.mapping().get(language.getKey().replace("/zh_cn.json","/en_us.json"))));
            var expectedKeys=new java.util.HashSet<String>();
            language.getValue().forEach((target,source)->{
                String value=original.get(source);
                if(value!=null&&!(language.getKey().endsWith("/zh_cn.json")&&value.equals(english.get(source))
                        &&value.codePoints().noneMatch(c->Character.UnicodeScript.of(c)==Character.UnicodeScript.HAN)))expectedKeys.add(target);
            });
            assertFalse(expectedKeys.isEmpty(),language.getKey());
            assertEquals(expectedKeys,translated.keySet(),language.getKey());
        }
    }
    private static java.util.Map<String,String> language(byte[] data){
        var values=new java.util.HashMap<String,String>();
        for(String line:new String(data,java.nio.charset.StandardCharsets.UTF_8).split("\\R")){
            if(line.startsWith("#")||!line.contains("="))continue;
            int split=line.indexOf('=');values.put(line.substring(0,split),line.substring(split+1));
        }
        return values;
    }
}
