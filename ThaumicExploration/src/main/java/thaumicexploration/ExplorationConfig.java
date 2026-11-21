package thaumicexploration;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

/** Recipe switches load before datapacks; the core synchronizes authoritative research availability. */
public final class ExplorationConfig {
    public static final ForgeConfigSpec SPEC;
    public static final Map<String,ForgeConfigSpec.BooleanValue> FEATURES=new LinkedHashMap<>();
    public static final ForgeConfigSpec.BooleanValue BREAD,MAGIC_WOOD,MOD_WOOD,MOD_STONE;
    public static final Map<String,String> RESEARCH=Map.ofEntries(
        Map.entry("THINKTANK","thinkTank"),Map.entry("URN","everfullUrn"),Map.entry("BURN","everburnUrn"),
        Map.entry("REPLICATOR","replicator"),Map.entry("CRUCSOULS","crucibleSouls"),Map.entry("TALISMANFOOD","nourishment"),
        Map.entry("METEORBOOTS","boots"),Map.entry("COMETBOOTS","boots"),Map.entry("CAP_SOJOURNER","sojourner"),Map.entry("CAP_MECHANIST","mechanist"),
        Map.entry("ENCHBINDING","enchantments"),Map.entry("ENCHDISARM","enchantments"),Map.entry("ENCHNIGHTVISION","enchantments"),
        Map.entry("DREAMCATCHER","dreamcatcher"),Map.entry("JARSEAL","boundInventories"),Map.entry("CHESTSEAL","boundInventories"),Map.entry("ROD_BREAD","breadWand"));
    static {
        var b=new ForgeConfigSpec.Builder();
        for(String key:new LinkedHashSet<>(RESEARCH.values()))FEATURES.put(key,b.comment("Original feature switch; disables its recipes and research. / 原版功能开关：控制配方与研究。 Restart world / 重启世界生效。").define(key,!key.equals("breadWand")));
        BREAD=FEATURES.get("breadWand");
        MAGIC_WOOD=b.define("allowMagicPlankReplication",true);MOD_WOOD=b.define("allowModWoodReplication",true);MOD_STONE=b.define("allowModStoneReplication",true);SPEC=b.build();
    }
    public static void register(){RESEARCH.forEach((key,option)->thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicexploration:"+key,()->enabled(key,option)));}
    private static boolean enabled(String key,String option){
        // Recipe previews also ask the extension API directly. On a remote client the
        // synchronized research snapshot must win over its unrelated COMMON file.
        if(net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient()){
            var snapshot=tc4revival.research.ResearchDefinitions.display();
            if(snapshot!=null&&snapshot.json().has("disabledResearch")){
                for(var disabled:snapshot.json().getAsJsonArray("disabledResearch"))if(disabled.getAsString().equals("thaumicexploration:"+key))return false;
                return true;
            }
        }
        return FEATURES.get(option).get();
    }
    public static boolean replicable(ItemStack stack){
        var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        boolean core=id.getNamespace().equals("thaumcraft"),vanilla=id.getNamespace().equals("minecraft");
        if(stack.is(ItemTags.LOGS)||stack.is(ItemTags.LEAVES))return !core;
        if(stack.is(ItemTags.PLANKS)||stack.is(ItemTags.WOODEN_SLABS)||stack.is(ItemTags.WOODEN_STAIRS))return (vanilla||MOD_WOOD.get())&&(!core||MAGIC_WOOD.get());
        if(!vanilla&&!MOD_STONE.get())return false;
        return stack.is(ItemTags.create(ExplorationMod.id("replicable")));
    }
    private ExplorationConfig(){}
}
