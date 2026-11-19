package tc4revival.data;

import java.util.ArrayList;
import java.util.List;

/** Shared old item metadata for scanning, recipes and resource generation. */
public  class EldritchIdentities {
    public record Entry(String name,String legacy,int metadata,boolean block) {}
    private EldritchIdentities() {}
    public static List<Entry> all() {
        var entries=new ArrayList<Entry>();
        String[] devices={"eldritch_altar","eldritch_obelisk","eldritch_obelisk_segment","eldritch_cap","eldritch_glowing_stone",
                "eldritch_glyphed_stone","eldritch_carved_stone","eldritch_door_frame","eldritch_lock","eldritch_crab_spawner","eldritch_rune_trap"};
        for (int i=0;i<devices.length;i++) entries.add(new Entry(devices[i],"blockEldritch",i,true));
        String[] stones={"eldritch_stone","eldritch_rock","eldritch_stone_nonspawning","eldritch_crust","eldritch_pedestal_stone"};
        for (int i=0;i<stones.length;i++) entries.add(new Entry(stones[i],"blockCosmeticSolid",11+i,true));
        entries.add(new Entry("obsidian_tile","blockCosmeticSolid",1,true));
        entries.add(new Entry("eldritch_crystal","blockCrystal",7,true));
        entries.add(new Entry("eldritch_stairs","blockStairsEldritch",0,true));
        entries.add(new Entry("eldritch_slab","blockCosmeticSlabStone",1,true));
        for (boolean crate:new boolean[]{false,true}) for (int rarity=0;rarity<3;rarity++)
            entries.add(new Entry("loot_"+(crate ? "crate_" : "urn_")+rarity,crate ? "blockLootCrate" : "blockLootUrn",rarity,true));
        entries.add(new Entry("eldritch_eye","ItemEldritchObject",0,false)); entries.add(new Entry("runed_tablet","ItemEldritchObject",2,false));
        entries.add(new Entry("obelisk_placer","ItemEldritchObject",4,false));
        return List.copyOf(entries);
    }
}
