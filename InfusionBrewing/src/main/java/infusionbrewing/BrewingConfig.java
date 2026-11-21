package infusionbrewing;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import thaumcraft.api.config.RevivalConfig;

public final class BrewingConfig {
    public static final ForgeConfigSpec SPEC;
    public static final RevivalConfig REVIVAL;
    public static final ForgeConfigSpec.BooleanValue EXPLOSIONS, SPAWNS, INFINITE, MAGIC_BUCKET, DETAILS;
    public static final ForgeConfigSpec.IntValue POWER, STACK_SIZE;
    static {
        var b=new ForgeConfigSpec.Builder();
        REVIVAL=new RevivalConfig(b,true,()->tc4revival.config.TCConfig.SERVER.revival.profile());
        b.push("original");
        EXPLOSIONS=b.comment("原作药水反应爆炸 / Original potion reaction explosions").define("enableReactExplosion",true);
        POWER=b.comment("爆炸强度 / Explosion strength").defineInRange("reactExplosionPower",3,0,32);
        SPAWNS=b.comment("流体反应生成生物 / Reaction creature spawning").define("enableReactSpawn",true);
        INFINITE=b.comment("两源相邻生成无限源 / Two adjacent sources regenerate").define("enableInfinitySource",true);
        MAGIC_BUCKET=b.comment("发射器只使用魔法桶采集药水 / Only magic buckets collect potion in dispensers").define("dispenseMagicBucket",false);
        STACK_SIZE=b.comment("普通与喷溅药水及魔法瓶堆叠上限 / Potion and magic bottle stack limit").defineInRange("potionStackSize",8,1,64);
        DETAILS=b.comment("潜行持杖查看刷怪笼详情 / Sneak-wand detailed spawner inspection").define("showSpawnerDetails",false);
        b.pop();SPEC=b.build();REVIVAL.bind(SPEC);
    }
    public static int stackSize(){return SPEC.isLoaded()?STACK_SIZE.get():8;}
    public static void register(){ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,SPEC,"infusionbrewing-server.toml");}
}
