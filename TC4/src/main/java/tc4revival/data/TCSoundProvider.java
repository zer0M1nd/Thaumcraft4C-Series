package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public  class TCSoundProvider implements DataProvider {
    private final PackOutput output;
    public TCSoundProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var root = new JsonObject();
        var dart=new JsonObject(); var dartFiles=new JsonArray(); dartFiles.add("thaumcraft:golemironshoot"); dart.add("sounds",dartFiles); root.add("golemironshoot",dart);
        var upgrade=new JsonObject(); var upgradeFiles=new JsonArray(); upgradeFiles.add("thaumcraft:upgrade"); upgrade.add("sounds",upgradeFiles); root.add("upgrade",upgrade);
        var coins=new JsonObject(); var coinFiles=new JsonArray(); coinFiles.add("thaumcraft:coins"); coins.add("sounds",coinFiles); root.add("coins",coins);
        var jarSounds=new JsonArray();
        for (int index=1;index<=4;index++) jarSounds.add("thaumcraft:jar"+index);
        var jar=new JsonObject(); jar.add("sounds",jarSounds); root.add("jar",jar);
        for (String name : java.util.List.of("runic_shield_effect", "runic_shield_charge", "creak", "squeek", "tool", "alembicknock", "infuserstart", "infuser", "cameraticks", "learn", "write", "erase", "whispers", "cameraclack", "hhon", "hhoff", "pech_idle", "pech_trade", "pech_dice", "pech_hit", "pech_death", "pech_charge", "doorfail", "key", "urnbreak", "egidle", "egdeath", "egattack", "egscreech", "crabclaw", "crabtalk", "crabdeath", "chant", "monolith", "tentacle", "heartbeat", "page", "wand", "shock", "zap", "jacobs", "craftstart", "craftfail", "crystal", "ice", "fireloop", "rumble", "wandfail", "gore", "bubble", "spill", "swarmattack", "swarm", "fly", "roots", "wisplive", "wispdead", "evilportal", "pump", "brain")) {
            var files = new JsonArray();
            var variants = switch (name) {
                case "tentacle"->java.util.List.of("tentacle1","tentacle2","tentacle3");
                case "chant"->java.util.List.of("chant1","chant2","chant3");
                case "egidle"->java.util.List.of("egidle1","egidle2");
                case "crabtalk"->java.util.List.of("crabtalk1","crabtalk2","crabtalk3");
                case "pech_idle"->java.util.List.of("pech_idle1","pech_idle2","pech_idle3");
                case "pech_hit","pech_charge"->java.util.List.of(name+"1",name+"2");
                case "creak", "squeek", "tool" -> java.util.List.of(name+"1",name+"2");
                case "wisplive" -> java.util.List.of("wisplive1","wisplive2","wisplive3");
                case "brain" -> java.util.List.of("brain1","brain2","brain3","brain4");
                case "roots" -> java.util.List.of("roots1","roots2","roots3");
                case "fly" -> java.util.List.of("fly1","fly2");
                case "swarm" -> java.util.List.of("swarm1","swarm2","swarm3");
                case "gore" -> java.util.List.of("gore1","gore2");
                case "bubble" -> java.util.List.of("bubble1","bubble2","bubble3","bubble4");
                case "ice" -> java.util.List.of("ice1", "ice2", "ice3");
                case "shock" -> java.util.List.of("shock1", "shock2");
                case "zap" -> java.util.List.of("zap1", "zap2");
                case "page" -> java.util.List.of("page1", "page2");
                case "pump" -> java.util.List.of("pump1", "pump2", "pump3");
                case "wand" -> java.util.List.of("wand1", "wand2", "wand3");
                case "write" -> java.util.List.of("write1", "write2");
                case "cameraclack" -> java.util.List.of("cameraclack1", "cameraclack2", "cameraclack3");
                default -> java.util.List.of(name);
            };
            for (String file : variants) {
                var sound = new JsonObject();
                sound.addProperty("name", "thaumcraft:" + file);
                sound.addProperty("stream", false);
                files.add(sound);
            }
            var definition = new JsonObject(); definition.add("sounds", files);
            root.add(name, definition);
        }
        for (String name:java.util.List.of("wind","swing")) {
            var files=new JsonArray();
            for (int index=1;index<=2;index++) { var sound=new JsonObject(); sound.addProperty("name","thaumcraft:"+name+index); sound.addProperty("stream",false); files.add(sound); }
            var definition=new JsonObject(); definition.add("sounds",files); root.add(name,definition);
        }
        return DataProvider.saveStable(cache, root, output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("thaumcraft/sounds.json"));
    }
    @Override public String getName() { return "TC4 sound definitions"; }
}


