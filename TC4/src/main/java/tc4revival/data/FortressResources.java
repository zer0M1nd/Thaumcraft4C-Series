package tc4revival.data;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.*;

public  class FortressResources implements DataProvider {
    public static final String[][] IDENTITIES={{"fortress_helmet","ItemHelmetFortress","thaumiumfortresshelm"},
            {"fortress_chestplate","ItemChestplateFortress","thaumiumfortresschest"},{"fortress_leggings","ItemLeggingsFortress","thaumiumfortresslegs"}};
    public static final String[][] VOID_IDENTITIES={{"void_robe_helmet","ItemHelmetVoidFortress","voidrobehelm","blank","ItemHelmetVoidRobe"},
            {"void_robe_chestplate","ItemChestplateVoidFortress","voidrobechestover","voidrobechest","ItemChestplateVoidRobe"},
            {"void_robe_leggings","ItemLeggingsVoidFortress","voidrobelegsover","voidrobelegs","ItemLeggingsVoidRobe"}};
    public static final String[][] MOBILITY_IDENTITIES={{"hover_harness","HoverHarness","hoverharness"},
            {"hover_girdle","ItemGirdleHover","hovergirdle"},{"traveller_boots","BootsTraveller","bootstraveler"}};
    public static java.util.List<String[]> all() { return java.util.stream.Stream.of(IDENTITIES,VOID_IDENTITIES,MOBILITY_IDENTITIES).flatMap(java.util.Arrays::stream).toList(); }
    private final PackOutput output;
    public FortressResources(PackOutput output) { this.output=output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var writes=new ArrayList<CompletableFuture<?>>();
        var magic=new JsonObject(); var values=new com.google.gson.JsonArray();
        for (String name:new String[]{"minecraft:magic","minecraft:indirect_magic","minecraft:thorns","thaumcraft:taint","thaumcraft:eldritch_orb"}) values.add(name);
        magic.add("values",values);
        writes.add(DataProvider.saveStable(cache,magic,output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("forge/tags/damage_type/is_magic.json")));
        for (var item:all()) {
            var model=new JsonObject(); model.addProperty("parent","minecraft:item/generated");
            var textures=new JsonObject(); textures.addProperty("layer0","thaumcraft:item/"+item[2]); model.add("textures",textures);
            if (item.length>3) textures.addProperty("layer1","thaumcraft:item/"+item[3]);
            writes.add(DataProvider.saveStable(cache,model,output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("thaumcraft/models/item/"+item[0]+".json")));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    @Override public String getName() { return "TC4 fortress armor models"; }
}
