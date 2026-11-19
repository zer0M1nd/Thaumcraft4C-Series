package automagy;

import automagy.logic.EssentiaCostSetting;
import automagy.logic.NetherRuneCatalog;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.*;

/** Server settings applied when selecting a new cycle; paid in-flight costs stay saved. */
public final class NetherConfig {
    private record Settings(ForgeConfigSpec.IntValue duration,
        ForgeConfigSpec.ConfigValue<List<? extends String>> cost,
        ForgeConfigSpec.ConfigValue<List<? extends String>> bait,
        ForgeConfigSpec.IntValue uses,ForgeConfigSpec.IntValue instability) {}
    private static final Map<String,Settings> SETTINGS=new LinkedHashMap<>();
    private static final Set<String> WARNED=new HashSet<>();
    public static ForgeConfigSpec.IntValue BASIC_LAVA_CHANCE;
    public static ForgeConfigSpec.BooleanValue IGNORE_CUSTOM;
    private NetherConfig() {}
    public static void define(ForgeConfigSpec.Builder builder){
        builder.push("nethermind");
        IGNORE_CUSTOM=builder.comment("忽略下方下界思维自定义参数，使用当前默认值；新世界初始 true，已有现代配置迁移为 false；可选 true/false，重启世界。",
                "Ignore the custom Nethermind settings below and use current defaults; new worlds default true, existing port configs migrate to false. true/false; restart the world.").define("ignoreSection",true);
        BASIC_LAVA_CHANCE=builder.comment("基础符文产出岩浆的概率（百分比）；初始 15，范围 0..100；仅 ignoreSection=false 时生效；重启世界。", "Basic rune lava chance in percent; default 15, range 0..100; requires ignoreSection=false; restart the world.").defineInRange("basicLavaPercent",15,0,100);
        var definitions=new ArrayList<>(NetherRuneCatalog.BUILTINS.values());definitions.add(NetherRuneCatalog.basic(false));
        for(var rune:definitions){if(rune.speed())continue;builder.push(rune.key());
            var duration=builder.comment("本符文周期 tick 数；初始 "+rune.duration()+"，范围 1..1000000；仅 ignoreSection=false 时生效；重启世界。", "Cycle ticks for this rune; default "+rune.duration()+", range 1..1000000; requires ignoreSection=false; restart the world.").defineInRange("durationTicks",rune.duration(),1,1000000);
            var cost=cost(builder,"cost",rune.cost());
            var bait=cost(builder,"baitCost",rune.baitCost());
            var uses=builder.comment("每份诱饵的使用次数；初始 "+Math.max(1,rune.baitUses())+"，范围 1..65535；仅 ignoreSection=false 时生效；重启世界。", "Uses per bait payment; default "+Math.max(1,rune.baitUses())+", range 1..65535; requires ignoreSection=false; restart the world.").defineInRange("baitUses",Math.max(1,rune.baitUses()),1,65535);
            var instability=builder.comment("本符文额外不稳定度；初始 "+rune.instability()+"，范围 0..1000；仅 ignoreSection=false 时生效；重启世界。", "Extra instability for this rune; default "+rune.instability()+", range 0..1000; requires ignoreSection=false; restart the world.").defineInRange("extraInstability",rune.instability(),0,1000);
            SETTINGS.put(rune.key(),new Settings(duration,cost,bait,uses,instability));builder.pop();
        }builder.pop();
    }
    private static ForgeConfigSpec.ConfigValue<List<? extends String>> cost(ForgeConfigSpec.Builder b,String name,Map<String,Integer> defaults){
        return b.comment("要素标识=数量；初始 "+defaults+"。重复项取最后一个，0 删除费用，空列表免费；未知要素回退默认值。仅 ignoreSection=false 时生效；重启世界。",
                "Aspect tag=amount; default "+defaults+". Last duplicate wins; zero removes a cost; an empty list is free. Unknown aspects fall back to defaults. Requires ignoreSection=false; restart the world.")
            .defineListAllowEmpty(name,defaults.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getKey()+"="+e.getValue()).toList(),o->o instanceof String s&&EssentiaCostSetting.entry(s).isPresent());
    }
    private static Map<String,Integer> resolve(List<? extends String> values,Map<String,Integer> fallback){
        var result=EssentiaCostSetting.parse(values);
        for(String key:result.keySet())if(tc4revival.aspect.TCAspects.catalog().find(key).isEmpty()){
            if(WARNED.add(key))com.mojang.logging.LogUtils.getLogger().warn("Automagy: unknown essentia aspect {}; using default recipe costs",key);
            return fallback;
        }return result;
    }
    public static NetherRuneCatalog.Definition apply(NetherRuneCatalog.Definition rune){
        if(rune==null)return null;if(IGNORE_CUSTOM.get())return rune;var s=SETTINGS.get(rune.key());if(s==null)return rune;
        return new NetherRuneCatalog.Definition(rune.key(),rune.creature(),rune.speed(),rune.output(),s.duration.get(),resolve(s.cost.get(),rune.cost()),resolve(s.bait.get(),rune.baitCost()),s.uses.get(),s.instability.get(),rune.lureDistance());
    }
    public static int basicLavaChance(){return IGNORE_CUSTOM.get()?15:BASIC_LAVA_CHANCE.get();}
}
