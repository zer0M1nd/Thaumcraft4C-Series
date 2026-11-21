package automagy;
import net.minecraftforge.common.ForgeConfigSpec;
/** Per-world server options; defaults preserve Automagy's legacy behavior. */
public final class AutomagyConfig {
 public static final ForgeConfigSpec SPEC;
 public static final ForgeConfigSpec.ConfigValue<String> NETHER_ASPECT;
 public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> AMNESIAC_ITEMS;
 public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> INVENTORY_BLACKLIST,EXTRA_SHELVES;
 public static final ForgeConfigSpec.BooleanValue LOONIUM_BLACKLIST;
 public static final ForgeConfigSpec.BooleanValue ADVANCED_JARRING,REDCRYSTAL_LIGHT,DIMENSION_LURE,TANK_RAIN,TANK_PRESERVE_WATER,HEART_DROPS,HEART_NETHER_ONLY,THINKING_CAP_RECIPE;
 public static final ForgeConfigSpec.IntValue REFINING_SLIVERS,WATER_BOTTLE,COW_MILKING_COOLDOWN,PHIAL_XP,NETHER_SPIRE_CHANCE;
 static {var b=new ForgeConfigSpec.Builder();
  ADVANCED_JARRING=b.comment("高级节点装罐；初始 true，可选 true/false；需要高级装罐研究和已认主结构，保留节点亮度；重启世界。", "Advanced node jarring; default true, true/false; requires its research and owned warded structure, preserves node brightness; restart the world.").define("advancedNodeJarring",true);
  REFINING_SLIVERS=b.comment("熵解提炼每块蕴魔石的晶屑数；初始 11，范围 0..64；0 禁用研究和配方；重启世界。", "Slivers from each infused stone by Entropic Refining; default 11, range 0..64; 0 disables research and recipes; restart the world.").defineInRange("sliversFromInfusedStone",11,0,64);
  REDCRYSTAL_LIGHT=b.comment("红石水晶有信号时发光；初始 true，可选 true/false；重启世界。", "Redcrystals emit light while powered; default true, true/false; restart the world.").define("redcrystalEmitsLight",true);
  DIMENSION_LURE=b.comment("维度诱饵研究与合成开关；初始 true，可选 true/false；关闭后旧设备停止运行，存量保留；重启世界。", "Dimensional Lure research and crafting; default true, true/false. Disabled existing machines pause and retain contents; restart the world.").define("allowDimensionLure",true);
  b.push("thirstyTank");
  WATER_BOTTLE=b.comment("水瓶容量 mB；初始 0（仅在罐内已有水时换瓶，液量不变），-1 禁止，-2 读取水瓶的 Forge 流体容量（无注册时为 0）；正值自定义，范围 -2..1000000；重启世界。", "Water bottle amount in mB; default 0 (exchange only when the tank already contains water, without changing its amount), -1 disables, -2 reads the bottle's Forge fluid capacity (0 if unregistered). Positive values customize it; range -2..1000000; restart the world.").defineInRange("waterBottleAmount",0,-2,1000000);
  TANK_RAIN=b.comment("暴露于天空的渴水之罐收集雨水；初始 true，可选 true/false；重启世界。", "Collect rainwater when exposed to the sky; default true, true/false; restart the world.").define("drinkRain",true);
  TANK_PRESERVE_WATER=b.comment("保留紧邻渴水之罐的水源；初始 true，可选 true/false；重启世界。", "Preserve water sources directly adjacent to the tank; default true, true/false; restart the world.").define("preserveAdjacentWaterSource",true);
  COW_MILKING_COOLDOWN=b.comment("同一头牛两次自动挤奶的间隔 tick，所有罐共用；初始 2000，范围 0..2147483647；重启世界。", "Ticks between automatic milkings of one cow, shared by all tanks; default 2000, range 0..2147483647; restart the world.").defineInRange("cowMilkingCooldown",2000,0,Integer.MAX_VALUE);
  b.pop();b.push("experience");
  PHIAL_XP=b.comment("每瓶浓缩经验的经验点数，也用于遗忘石；初始 95，范围 1..10545；重启世界。", "XP points in a phial, also used by the amnesiac stone; default 95, range 1..10545; restart the world.").defineInRange("phialXP",95,1,10545);
  AMNESIAC_ITEMS=b.comment("遗忘石转化列表：物品ID=经验[,返还物品ID]，或物品ID=phial,*；phial 使用上方数值，* 返回容器。初始为经验瓶 8 点和浓缩经验瓶，重复项取最后一个，未知可选物品忽略，空列表禁用转化；重启世界。", "Amnesiac conversions: item_id=XP[,remainder_id] or item_id=phial,*; phial uses the setting above, * returns the container. Defaults: XP bottle=8 and the XP phial; last duplicate wins, unknown optional items are ignored, empty disables conversions; restart the world.")
      .defineListAllowEmpty("amnesiacDrainItems",java.util.List.of("minecraft:experience_bottle=8,minecraft:glass_bottle","automagy:experience_phial=phial,*"),o->o instanceof String line&&automagy.logic.ExperienceItemRule.parse(line,95).isPresent());
  b.pop();b.push("netherHeart");
  HEART_DROPS=b.comment("烈焰人刷怪笼掉落余烬之心；初始 true，可选 true/false；重启世界。", "Blaze spawners drop ember hearts; default true, true/false; restart the world.").define("spawnerDrop",true);
  HEART_NETHER_ONLY=b.comment("余烬之心仅在下界掉落；依赖 spawnerDrop；初始 true，可选 true/false；重启世界。", "Ember hearts drop only in the Nether; requires spawnerDrop; default true, true/false; restart the world.").define("netherOnly",true);
  b.pop();NetherConfig.define(b);b.push("worldgen");
  NETHER_SPIRE_CHANCE=b.comment("每 N 个下界区块尝试生成尖塔；初始 140，范围 0..1000000，0 禁用。需大型露天岩浆湖，仅影响新生成区块；重启世界。", "One Nether Spire attempt per N Nether chunks; default 140, range 0..1000000, 0 disables. Requires a large exposed lava lake; new chunks only; restart the world.").defineInRange("netherSpireChance",140,0,1000000);
  b.pop();b.push("creativeTools");
  THINKING_CAP_RECIPE=b.comment("允许用思考之帽制作高级傀儡；初始 true，可选 true/false；重启世界。", "Allow the Thinking Cap in advanced golem crafting; default true, true/false; restart the world.").define("thinkingCapCrafting",true);
  b.pop();b.push("inventarium");
  INVENTORY_BLACKLIST=b.comment("物品目录和隐形抄写员忽略的容器方块 ID 或 #方块标签；初始为三个计量器及兼容标签；空列表不额外排除，数据包黑名单仍生效；重启世界。", "Container block IDs or #block tags ignored by the Inventarium and Unseen Scribes; defaults: three tally blocks and the compatibility tag. Empty adds no exclusions; datapack blacklist still applies; restart the world.")
      .defineListAllowEmpty("containerBlacklist",java.util.List.of("automagy:tally_inventory","automagy:tally_world","automagy:tally_drops","#automagy:inventarium_container_blacklist"),AutomagyConfig::blockRule);
  EXTRA_SHELVES=b.comment("额外书架方块 ID 或 #方块标签，每个增加 81 槽；初始空列表，数据包书架标签和专用书架 API 始终可用；重启世界。", "Additional bookshelf block IDs or #block tags, 81 slots each; default empty. Datapack shelf tags and the custom shelf API remain available; restart the world.")
      .defineListAllowEmpty("otherBookshelves",java.util.List.of(),AutomagyConfig::blockRule);
  b.pop();b.push("compatibility");
  NETHER_ASPECT=b.comment("可选下界要素标识；初始 infernus，未知标识忽略，空字符串禁用；填写禁忌魔法提供的标识；重启世界。", "Optional Nether aspect identifier; default infernus, unknown identifiers ignored, empty disables; use the identifier supplied by Forbidden Magic; restart the world.").define("netherAspect","infernus");
  LOONIUM_BLACKLIST=b.comment("禁止咒波之花产出浓缩经验瓶；新世界初始 false，已有现代配置迁移为 true；可选 true/false。数据包显式黑名单不受此开关撤销；重启世界。", "Prevent Loonium from producing XP phials; new worlds default false, existing port configs migrate to true; true/false. Explicit datapack blacklists are never removed by this option; restart the world.").define("botaniaBlacklistPhialXP",false);
  b.pop();SPEC=b.build();
 }
 private static boolean blockRule(Object value){return value instanceof String text&&net.minecraft.resources.ResourceLocation.tryParse(text.startsWith("#")?text.substring(1):text)!=null;}
 public static boolean matches(net.minecraft.world.level.block.state.BlockState state,ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> rules){
  for(String text:rules.get()){
   var id=net.minecraft.resources.ResourceLocation.parse(text.startsWith("#")?text.substring(1):text);
   if(text.startsWith("#")?state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,id)):id.equals(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock())))return true;
  }return false;
 }
 private AutomagyConfig(){}
}
