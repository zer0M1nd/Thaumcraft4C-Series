package advancedthaumaturgy;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import thaumcraft.api.config.RevivalConfig;

public class ATConfig {
    public static final ForgeConfigSpec SPEC;
    public static final RevivalConfig REVIVAL;
    public static final Map<String, ForgeConfigSpec.BooleanValue> FEATURES = new LinkedHashMap<>();
    public static final ForgeConfigSpec.BooleanValue ENGINE_TUBES;
    public static final ForgeConfigSpec.IntValue ENGINE_STORAGE, ENGINE_OUTPUT, ENERGY_PER_VALUE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FUEL_VALUES;
    private static List<? extends String> cachedFuelList;
    private static Map<String,Integer> cachedFuels;
    static {
        var b = new ForgeConfigSpec.Builder();
        REVIVAL = new RevivalConfig(b, true, () -> tc4revival.config.TCConfig.SERVER.revival.profile());
        b.push("original_features");
        for (String key : List.of("mercurial_core", "mercurial_wand", "wand_upgrades", "wandbench", "node_modifier", "fertilizer", "ethereal_jar", "microlith", "engine", "focus_void_cage", "creative_node", "altar_deployer", "exchange_in_eggs", "undead_in_bones"))
            FEATURES.put(key, b.comment("Original AT2-30r feature / 原版功能：" + key).define(key, true));
        b.pop().push("essentia_engine");
        ENGINE_TUBES = b.comment("Pull from pipes; false uses nearby jars / 管道输入；关闭后从附近罐子吸取").define("use_tubes", true);
        ENGINE_STORAGE = b.comment("Stored FE / 缓存能量").defineInRange("energy_storage", 32000, 1, 100000000);
        ENGINE_OUTPUT = b.comment("Maximum output per tick / 每 tick 输出上限").defineInRange("energy_output", 80, 1, 1000000);
        ENERGY_PER_VALUE = b.comment("FE per aspect value / 每点要素价值提供的 FE").defineInRange("energy_generation", 1000, 1, 1000000);
        FUEL_VALUES=b.comment("Fuel values as aspect=value. Zero removes a fuel; unknown aspects are ignored. / 燃料价值，要素=数值；0 禁用。").defineListAllowEmpty("fuel_values",List.of("ignis=4","terra=2","aer=2","aqua=2","ordo=4","perditio=2","arbor=1","herba=1","metallum=1","potentia=8"),o->o instanceof String s&&s.matches("[a-z0-9_:.-]+=[0-9]{1,4}"));
        b.pop(); SPEC = b.build(); REVIVAL.bind(SPEC);
    }
    public static boolean enabled(String feature) { var v = FEATURES.get(feature); return v == null || !SPEC.isLoaded() || v.get(); }
    public static boolean showItem(String id){
        if(id.equals("microlith_multiblock")||id.equals("microlith_model_multiblock"))return false;
        return enabled(featureFor(id));
    }
    public static String featureFor(String id){
        if(id.startsWith("crystal_")&&!id.equals("crystal_holder")||id.startsWith("apply_")||id.equals("infused_endstone_chunk")||id.equals("poisonous_potato"))return "wand_upgrades";
        for(var kind:MachineBlock.Kind.values())if(kind.id.equals(id))return kind.feature;
        return switch(id){case "mercurial_rod"->"mercurial_core";case "crystal_holder"->"microlith";default->id;};
    }
    public static int storage() { return SPEC.isLoaded() ? ENGINE_STORAGE.get() : 32000; }
    public static int output() { return SPEC.isLoaded() ? ENGINE_OUTPUT.get() : 80; }
    public static int energyPerValue() { return SPEC.isLoaded() ? ENERGY_PER_VALUE.get() : 1000; }
    public static boolean tubes() { return !SPEC.isLoaded() || ENGINE_TUBES.get(); }
    public static Map<String,Integer> fuels(){
        if(!SPEC.isLoaded())return MachineEntity.FUELS;
        var list=FUEL_VALUES.get();if(!list.equals(cachedFuelList)){Map<String,Integer> values=new LinkedHashMap<>();for(var entry:list){var parts=entry.split("=");int value=Integer.parseInt(parts[1]);if(value>0&&tc4revival.aspect.TCAspects.catalog().find(parts[0]).isPresent())values.put(parts[0],value);}cachedFuels=Map.copyOf(values);cachedFuelList=List.copyOf(list);}return cachedFuels;
    }
    public static void register() { ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC, "advancedthaumaturgy-server.toml"); }
}
