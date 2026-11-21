package emtmodern.energy;
import net.minecraftforge.common.ForgeConfigSpec;
public class ProcessorConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue SPEED,BONUS;
    static{var builder=new ForgeConfigSpec.Builder();SPEED=builder.comment("Original processing duration in ticks.").comment("虚无处理器完成一批处理所需的 tick 数；数值越大越慢", "设置说明 / Setting: Ethereal Processor processing duration in ticks; larger values are slower", "初始值 / Default: " + String.valueOf(400) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(1000000), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("ethereal_processor_speed",400,1,1000000);BONUS=builder.comment("Original one-in-N thaumium bonus chance; last input does not roll.").comment("虚无处理器额外产出的概率分母 N，概率为 1/N；最后一个输入不触发", "设置说明 / Setting: Ethereal Processor bonus chance denominator N (1/N); the last input does not roll", "初始值 / Default: " + String.valueOf(10) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(Integer.MAX_VALUE), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("ethereal_processor_bonus",10,1,Integer.MAX_VALUE);SPEC=builder.build();}
}
