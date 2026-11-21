package fmmodern;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;
import thaumcraft.api.wands.WandAttributes;

/** Server-authoritative attributes are synchronized by TC4; no separate client overrides. */
public final class WandConfiguration {
    public static final Map<String,ForgeConfigSpec.IntValue> CAPACITY=new LinkedHashMap<>();
    public static final Map<String,ForgeConfigSpec.DoubleValue> COST=new LinkedHashMap<>();
    static void define(ForgeConfigSpec.Builder b){
        b.comment("法杖属性；重启后生效并由本体同步。", "Wand attributes. Restart after editing; values are synchronized by Thaumcraft.").push("wands");
        String[] rods={"infernal","tainted","profane","livingwood","dreamwood","dreamwood_staff","blood","blood_staff"};
        int[] defaults={150,150,50,100,100,250,100,50};
        for(int i=0;i<rods.length;i++)CAPACITY.put(rods[i],b.comment("杖柄 "+rods[i]+" 每种原始要素容量；初始 "+defaults[i]+"，范围 1..1000000；重启世界。", "Each primal vis capacity for rod "+rods[i]+"; default "+defaults[i]+", range 1..1000000; restart the world.").defineInRange(rods[i]+"Capacity",defaults[i],1,1000000));
        String[] caps={"manasteel","elementium","terrasteel","alchemical"};double[] costs={.9,.8,1.8,.9};
        for(int i=0;i<caps.length;i++)COST.put(caps[i],b.comment("杖端 "+caps[i]+" 消耗倍率；初始 "+costs[i]+"，范围 0..100；炼金杖端的水消耗另减 0.1 且不低于 0；重启世界。", "Cost multiplier for cap "+caps[i]+"; default "+costs[i]+", range 0..100; alchemical water multiplier is 0.1 lower, minimum 0; restart the world.").defineInRange(caps[i]+"Cost",costs[i],0,100));
        b.pop();
    }
    static void setup(){
        CAPACITY.forEach((id,value)->WandAttributes.registerCapacity(ForbiddenMagic.ID,ForbiddenMagic.ID+":"+id,()->ForbiddenConfig.value(value)));
        WandAttributes.registerCapacity(ForbiddenMagic.ID,"forbidden_magic:profaned",()->ForbiddenConfig.value(CAPACITY.get("profane")));
        COST.forEach((id,value)->WandAttributes.registerCosts(ForbiddenMagic.ID,ForbiddenMagic.ID+":"+id,()->{
            float cost=ForbiddenConfig.value(value).floatValue();
            return id.equals("alchemical")?Map.of("*",cost,"aqua",Math.max(0,cost-.1f)):Map.of("*",cost);
        }));
    }
    private WandConfiguration(){}
}
