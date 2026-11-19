package tc4revival.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/** Dynamic damage registries are supplied by data packs, rather than the static Forge registry bus. */
public  class FocusDamageProvider implements DataProvider {
    private final PackOutput output;
    public FocusDamageProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var definition = new JsonObject(); definition.addProperty("message_id", "fireball");
        definition.addProperty("scaling", "never"); definition.addProperty("exhaustion", .1);
        definition.addProperty("effects", "burning");
        var tag = new JsonObject(); tag.addProperty("replace", false);
        var values = new JsonArray(); values.add("thaumcraft:ember"); tag.add("values", values);
        var root = output.getOutputFolder(PackOutput.Target.DATA_PACK);
        var taint = new JsonObject(); taint.addProperty("message_id","taint"); taint.addProperty("scaling","never"); taint.addProperty("exhaustion",.3);
        var tentacle=new JsonObject(); tentacle.addProperty("message_id","tentacle"); tentacle.addProperty("scaling","when_caused_by_living_non_player"); tentacle.addProperty("exhaustion",.1);
        var bypass = new JsonObject(); bypass.addProperty("replace",false); var bypassValues = new JsonArray(); bypassValues.add("thaumcraft:taint"); bypass.add("values",bypassValues);
        var arrows=new java.util.ArrayList<CompletableFuture<?>>();
        var orb=new JsonObject();orb.addProperty("message_id","indirectMagic");orb.addProperty("scaling","never");orb.addProperty("exhaustion",0);
        arrows.add(DataProvider.saveStable(cache,orb,root.resolve("thaumcraft/damage_type/eldritch_orb.json")));
        var dissolve=new JsonObject(); dissolve.addProperty("message_id","dissolve"); dissolve.addProperty("scaling","never"); dissolve.addProperty("exhaustion",.3);
        bypassValues.add("thaumcraft:dissolve"); arrows.add(DataProvider.saveStable(cache,dissolve,root.resolve("thaumcraft/damage_type/dissolve.json")));
        for (String arrow:java.util.List.of("air","fire","order")) {
            var damage=new JsonObject(); damage.addProperty("message_id",arrow+"arrow"); damage.addProperty("scaling","never"); damage.addProperty("exhaustion",.1);
            arrows.add(DataProvider.saveStable(cache,damage,root.resolve("thaumcraft/damage_type/"+arrow+"_arrow.json")));
        }
        values.add("thaumcraft:fire_arrow"); bypassValues.add("thaumcraft:air_arrow"); bypassValues.add("thaumcraft:order_arrow");
        var projectile=new JsonObject(); projectile.addProperty("replace",false); var projectiles=new JsonArray();
        projectiles.add("thaumcraft:eldritch_orb");
        for (String arrow:java.util.List.of("air","fire","order")) projectiles.add("thaumcraft:"+arrow+"_arrow"); projectile.add("values",projectiles);
        arrows.add(DataProvider.saveStable(cache,projectile,root.resolve("minecraft/tags/damage_type/is_projectile.json")));
        var magic=new JsonObject(); magic.addProperty("replace",false); var magicValues=new JsonArray(); magicValues.add("thaumcraft:air_arrow"); magicValues.add("thaumcraft:order_arrow"); magic.add("values",magicValues);
        magicValues.add("thaumcraft:eldritch_orb");
        arrows.add(DataProvider.saveStable(cache,magic,root.resolve("minecraft/tags/damage_type/witch_resistant_to.json")));
        return CompletableFuture.allOf(DataProvider.saveStable(cache, definition, root.resolve("thaumcraft/damage_type/ember.json")),
                CompletableFuture.allOf(arrows.toArray(CompletableFuture[]::new)),
                DataProvider.saveStable(cache,tentacle,root.resolve("thaumcraft/damage_type/tentacle.json")),
                DataProvider.saveStable(cache, taint, root.resolve("thaumcraft/damage_type/taint.json")),
                DataProvider.saveStable(cache, bypass, root.resolve("minecraft/tags/damage_type/bypasses_armor.json")),
                DataProvider.saveStable(cache, tag, root.resolve("minecraft/tags/damage_type/is_fire.json")));
    }
    @Override public String getName() { return "TC4 focus damage types"; }
}
