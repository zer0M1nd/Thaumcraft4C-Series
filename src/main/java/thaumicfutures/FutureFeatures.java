package thaumicfutures;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import thaumcraft.api.config.*;
import thaumcraft.api.research.*;
public final class FutureFeatures {
    public enum Group {
        EX_DEORUM("ex_deorum", "星辉生万物的神秘桶、人偶、碎片、筛分、热源与空岛配方。", "Ex Astris thaumium barrels, dolls, shards, sieving, heat and skyblock recipes for Ex Deorum."),
        RESTORATION("thaumcraft6", "神秘时代六代设备与材料复刻。", "Devices and materials adapted from Thaumcraft 6."),
        RAILCRAFT("railcraft", "铁路神秘撬棍、源质矿车与装卸设备。", "Railcraft thaumic crowbars, essentia carts and loading equipment."),
        BACKPACKS("backpacks", "精妙背包的神秘升级、源质、法杖充能与法杖核心管理；关闭后保留库存。", "Sophisticated Backpack arcane upgrades, essentia, wand charging and focus management; inventories are retained."),
        BOTANIA("botania", "植物魔法揭示头盔、魔力墨水与护甲联动。", "Botania revealing helmets, mana ink and armor integration."),
        EVILCRAFT("evilcraft", "邪恶工艺血液杖端与血脉书写工具。", "EvilCraft blood wand cap and veined scribing tools."),
        TINKERS("tinkers", "匠魂神秘材料、特性和护甲效果。", "Tinkers' thaumic materials, traits and armor effects."),
        AVARITIA("avaritia", "无尽贪婪的元始珍珠、阿卡西记录、无限法杖与催化剂联动。", "Avaritia pearls, Akashic record, infinity wand and catalyst integration."),
        AVARITIA_CATALYST("avaritia_catalyst", "无尽催化剂增加极致元始珍珠门槛；依赖无尽贪婪联动。", "Add an extremely primordial pearl requirement to infinity catalysts; requires Avaritia integration.", RevivalConfig.Defaults.CHALLENGE),
        FORESTRY_BEES("forestry_bees", "林业魔法蜜蜂、蜂巢、突变、产物、效果与蜂箱设备。", "Forestry magic bees, hives, mutations, products, effects and apiary equipment."),
        INDUSTRIAL("industrial_foregoing", "工业先锋与神秘作物、源质、培育及生物处理联动。", "Industrial Foregoing thaumic crops, essentia, growing and mob processing."),
        EXTERNAL_HEAT("external_heat", "使用沉浸工程等外部热源加热神秘炼金设备。", "External heat, including Immersive Engineering, for thaumic alchemy equipment."),
        CHEMTHROWER("chemthrower", "沉浸工程化学喷射器使用神秘液体的效果。", "Immersive Engineering chemthrower effects for thaumic fluids."),
        MACHINERY("machinery_ports", "模块化机械的灵气与源质接口和配方要求；关闭后相关配方不能免费运行。", "Modular Machinery vis/essentia ports and requirements; disabled requirements never become free recipes."),
        FOOD("food_integrations", "潘马斯等食物系统与神秘食物的玩法联动；要素补充独立保留。", "Gameplay integration with food systems such as Pam's; aspect supplementation remains independent.");

        final String key, chinese, english;
        final RevivalConfig.Defaults defaults;
        private RevivalConfig.Option option;
        Group(String key, String chinese, String english) { this(key,chinese,english,RevivalConfig.Defaults.BENEFIT); }
        Group(String key, String chinese, String english, RevivalConfig.Defaults defaults) {
            this.key=key;this.chinese=chinese;this.english=english;this.defaults=defaults;
        }
        public boolean enabled() {
            // Force initialization before integrations (e.g. Forestry plugins) ask for defaults.
            if (!CONFIG.option(key).get()) return false;
            return this != AVARITIA_CATALYST || AVARITIA.enabled();
        }
        public RevivalConfig.Option option() { return CONFIG.option(key); }
    }

    public static final RevivalConfig CONFIG;
    public static final ForgeConfigSpec SPEC;
    static {var b=new ForgeConfigSpec.Builder();CONFIG=new RevivalConfig(b,true,()->tc4revival.config.TCConfig.SERVER.revival.profile());
        for(var group:Group.values())group.option=CONFIG.feature(group.key,group.chinese,group.english,group.defaults);
        SPEC=b.build();CONFIG.bind(SPEC);}
    public static void register(){
        FeatureRecipeCondition.register();
        // Omniverse's additional meat combinations use the restoration nuggets owned here.
        // Registering the rule here also keeps Omniverse independently loadable without Futures.
        FeatureRecipes.register(ResourceLocation.parse("thaumicfutures:omniverse_meat_treat"),
                id->id.getNamespace().equals("thaumicomniverse") && id.getPath().startsWith("meat_treat/"),Group.RESTORATION::enabled);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
            new MigratingConfigSpec(SPEC).fromSibling("thaumicomniverse-revival-server.toml"),"thaumicfutures-revival-server.toml");
        for(var group:Group.values())FeatureRecipes.register(ResourceLocation.parse("thaumicfutures:"+group.key.replace('.','/')),id->group==recipeGroup(id),group::enabled);
        ResearchCategories.registerEnablementFilter(ResourceLocation.parse("thaumicfutures:features"),FutureFeatures::researchEnabled);
        ResearchPrerequisites.register(ResourceLocation.parse("thaumicfutures:features"),(entry,catalog,parents)->new ResearchPrerequisites.Parents(
            parents.visible().stream().filter(FutureFeatures::researchEnabled).toList(),parents.hidden().stream().filter(FutureFeatures::researchEnabled).toList()));
    }
    private static Group group(String kind,String key){var name=Ownership.group(kind,key);return name==null?null:Group.valueOf(name);}
    public static Group itemGroup(String name){return name.equals("quartz_sliver") ? Group.RESTORATION : group("items",name);}
    public static Group recipeGroup(ResourceLocation id){
        if (id.getNamespace().equals("thaumicfutures") && id.getPath().equals("restored/quartz_slivers")) return Group.RESTORATION;
        return group("recipes",id.toString());
    }
    public static boolean ownsItem(String name){return itemGroup(name)!=null;}
    public static boolean itemEnabled(String name){var group=itemGroup(name);return group==null||group.enabled();}
    public static boolean researchEnabled(String name){var group=group("research",name);return group==null||group.enabled();}
}
