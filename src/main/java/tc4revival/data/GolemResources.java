package tc4revival.data;

import java.util.*;
import tc4revival.content.*;

/** Shared flattened identities for item models, languages and later recipe/scan mappings. */
public  class GolemResources implements net.minecraft.data.DataProvider {
    private final net.minecraft.data.PackOutput output;
    public GolemResources(net.minecraft.data.PackOutput output) { this.output=output; }
    public record Identity(String path,String legacy,int metadata,String texture) {}
    public static List<Identity> identities() {
        var result=new ArrayList<Identity>();
        for (var material:GolemMaterial.values()) { String path="golem_"+material.name().toLowerCase(Locale.ROOT); result.add(new Identity(path,"ItemGolemPlacer",material.ordinal(),path)); }
        for (var core:GolemCoreType.values()) { String path="golem_core_"+core.name().toLowerCase(Locale.ROOT); result.add(new Identity(path,"ItemGolemCore",core.ordinal(),path)); }
        result.add(new Identity("golem_core_blank","ItemGolemCore",100,"golem_core_blank"));
        String[] elements={"air","earth","fire","water","order","entropy"},decorations={"hat","glasses","bowtie","fez","dart","visor","armor","mace"};
        for (int i=0;i<elements.length;i++) result.add(new Identity("golem_upgrade_"+elements[i],"ItemGolemUpgrade",i,"golem_upgrade_"+elements[i]));
        for (int i=0;i<decorations.length;i++) result.add(new Identity("golem_decoration_"+decorations[i],"ItemGolemDecoration",i,"golemdeco"+(i==0?"tophat":decorations[i])));
        result.add(new Identity("golem_bell","GolemBell",0,"ironbell"));
        result.add(new Identity("traveling_trunk","TrunkSpawner",0,"blank"));
        return List.copyOf(result);
    }
    @Override public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput cache) {
        var assets=output.getOutputFolder(net.minecraft.data.PackOutput.Target.RESOURCE_PACK).resolve("thaumcraft");
        var data=output.getOutputFolder(net.minecraft.data.PackOutput.Target.DATA_PACK);
        var model=new com.google.gson.JsonObject(); model.addProperty("parent","minecraft:block/cube_all");
        var textures=new com.google.gson.JsonObject(); textures.addProperty("all","thaumcraft:block/thaumiumblock"); model.add("textures",textures);
        var tag=new com.google.gson.JsonObject(); tag.addProperty("replace",false); var values=new com.google.gson.JsonArray(); values.add("thaumcraft:thaumium_block"); tag.add("values",values);
        var fetter=UtilityDeviceResources.model("golem_stone_side","golem_stone_top","arcane_stone");
        var active=UtilityDeviceResources.model("golem_stone_side","golem_stone_top_active","arcane_stone");
        var cube=new com.google.gson.JsonArray(); cube.add(UtilityDeviceResources.cube(0,0,0,16,16,16,"side","top","bottom",-1));
        fetter.add("elements",cube); active.add("elements",cube.deepCopy());
        var hungryChest=new com.google.gson.JsonObject(); var chestTextures=new com.google.gson.JsonObject(); chestTextures.addProperty("particle","thaumcraft:blocks/woodplain"); hungryChest.add("textures",chestTextures);
        return java.util.concurrent.CompletableFuture.allOf(
                net.minecraft.data.DataProvider.saveStable(cache,hungryChest,assets.resolve("models/block/hungry_chest.json")),
                net.minecraft.data.DataProvider.saveStable(cache,UtilityDeviceResources.states(Map.of("","hungry_chest")),assets.resolve("blockstates/hungry_chest.json")),
                net.minecraft.data.DataProvider.saveStable(cache,UtilityDeviceResources.loot("hungry_chest"),data.resolve("thaumcraft/loot_tables/blocks/hungry_chest.json")),
                net.minecraft.data.DataProvider.saveStable(cache,fetter,assets.resolve("models/block/golem_fetter.json")),
                net.minecraft.data.DataProvider.saveStable(cache,active,assets.resolve("models/block/golem_fetter_active.json")),
                net.minecraft.data.DataProvider.saveStable(cache,UtilityDeviceResources.states(Map.of("powered=false","golem_fetter","powered=true","golem_fetter_active")),assets.resolve("blockstates/golem_fetter.json")),
                net.minecraft.data.DataProvider.saveStable(cache,UtilityDeviceResources.loot("golem_fetter"),data.resolve("thaumcraft/loot_tables/blocks/golem_fetter.json")),
                net.minecraft.data.DataProvider.saveStable(cache,model,assets.resolve("models/block/thaumium_block.json")),
                net.minecraft.data.DataProvider.saveStable(cache,UtilityDeviceResources.states(Map.of("","thaumium_block")),assets.resolve("blockstates/thaumium_block.json")),
                net.minecraft.data.DataProvider.saveStable(cache,UtilityDeviceResources.loot("thaumium_block"),data.resolve("thaumcraft/loot_tables/blocks/thaumium_block.json")),
                net.minecraft.data.DataProvider.saveStable(cache,tag,data.resolve("forge/tags/items/storage_blocks/thaumium.json")));
    }
    @Override public String getName() { return "TC4 golem resources"; }
}
