package wgmodern;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import static wgmodern.WGData.*;

public class WallMirrorData {
    public static void add(WGData d,CachedOutput c,List<CompletableFuture<?>> jobs){
        jobs.add(d.save(c,"assets/witchinggadgets/blockstates/wall_mirror.json",map("variants",map("",map("model","witchinggadgets:block/wall_mirror")))));
        jobs.add(d.save(c,"assets/witchinggadgets/models/block/wall_mirror.json",map("textures",map("particle","witchinggadgets:blocks/white"))));
        jobs.add(d.save(c,"assets/witchinggadgets/models/item/wall_mirror.json",map("parent","builtin/entity","textures",map("particle","witchinggadgets:blocks/white"))));
        jobs.add(d.save(c,"data/witchinggadgets/loot_tables/blocks/wall_mirror.json",map("type","minecraft:block","pools",List.of(map("rolls",1,"entries",List.of(map("type","minecraft:item","name","witchinggadgets:wall_mirror")),"conditions",List.of(map("condition","minecraft:block_state_property","block","witchinggadgets:wall_mirror","properties",map("half","lower"))))))));
        // The author's first argument is the research key; the second is merely a recipe-list suffix.
        var aspects=Map.of("vacuos",20,"iter",20,"alienis",20,"vitreus",20);var parts=List.of("thaumcraft:focus_portable_hole","thaumcraft:entropy_shard","minecraft:ender_pearl","minecraft:gold_ingot","minecraft:gold_ingot","minecraft:chiseled_quartz_block");
        jobs.add(d.save(c,"data/witchinggadgets/recipes/wall_mirror.json",map("type","thaumcraft:infusion","research","witchinggadgets:WALLMIRROR","instability",8,"aspects",aspects,"center",map("ingredient",item("thaumcraft:item_mirror")),"components",parts.stream().map(p->map("ingredient",item(p))).toList(),"result",item("witchinggadgets:wall_mirror"))));
        d.display.add(map("key","witchinggadgets:wall_mirror","class","modern","data",map("kind","infusion","research","witchinggadgets:WALLMIRROR","input",stack("thaumcraft:item_mirror"),"output",stack("witchinggadgets:wall_mirror"),"ingredients",sequence(parts.stream().map(WGData::stack).toList()),"aspects",aspects,"instability",8)));
        // No research-catalog entry: the author left WALLMIRROR research and teleportation commented out.
    }
}
