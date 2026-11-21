package wtmodern;
import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;
public class WarpConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue EASY,PERMANENT,ENABLED,GLOBAL,REBOUND;
    public static final ForgeConfigSpec.IntValue MULTIPLIER;
    public static final Map<String,ForgeConfigSpec.BooleanValue> EVENT_ENABLED=new LinkedHashMap<>();
    public static final Map<String,ForgeConfigSpec.IntValue> SEVERITIES=new LinkedHashMap<>();
    static {
        var b=new ForgeConfigSpec.Builder();b.push("general");
        EASY=b.comment("Original author's less expensive recipes; reload datapacks after changing.").comment("原作者简化配方模式", "设置说明 / Setting: Original author's less expensive recipe mode", "初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("wussMode",false);
        PERMANENT=b.comment("允许移除永久扭曲", "设置说明 / Setting: Allow removal of permanent warp", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("allowPermWarpRemoval",true);
        MULTIPLIER=b.comment("Permanent warp removal cost. Minimum 1 prevents legacy division by zero.").comment("移除永久扭曲的费用倍数；最小 1 避免除零", "设置说明 / Setting: Permanent warp removal cost multiplier; minimum 1 avoids division by zero", "初始值 / Default: " + String.valueOf(2) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(100000), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("permWarpMult",2,1,100000);
        ENABLED=b.comment("启用本附属原有扭曲事件，独立于新增功能模式", "设置说明 / Setting: Enable this addon's original warp events independently of Revival profiles", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("addWarpEffects",true);GLOBAL=b.comment("允许影响其他玩家或世界的全局扭曲事件", "设置说明 / Setting: Allow global warp events affecting other players or the world", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("allowGlobalWarpEffects",true);REBOUND=b.comment("禁用扭曲反弹", "设置说明 / Setting: Disable warp rebound", "初始值 / Default: " + String.valueOf(false) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("disableRebound",false);b.pop();
        b.push("events");for(var e:WarpRules.EVENTS){b.push(e.id());EVENT_ENABLED.put(e.id(),b.comment("启用当前原有扭曲事件；仍受总开关及全局事件权限约束", "设置说明 / Setting: Enable this original warp event; master and global-event settings still apply", "初始值 / Default: " + String.valueOf(true) + "; 可选 / Allowed: true, false", "修改后重启世界或服务器 / Restart the world/server after editing.").define("enabled",true));SEVERITIES.put(e.id(),b.comment("触发当前事件的最低扭曲值", "设置说明 / Setting: Minimum warp required for this event", "初始值 / Default: " + String.valueOf(e.severity()) + "; 范围 / Range: " + String.valueOf(1) + ".." + String.valueOf(100000), "修改后重启世界或服务器 / Restart the world/server after editing.").defineInRange("minimumWarp",e.severity(),1,100000));b.pop();}b.pop();SPEC=b.build();
    }
    public static java.util.List<WarpRules.Event> enabled(){return WarpRules.EVENTS.stream().filter(e->EVENT_ENABLED.get(e.id()).get()&&(!e.global()||GLOBAL.get())).map(e->new WarpRules.Event(e.id(),SEVERITIES.get(e.id()).get(),e.global())).toList();}
}
