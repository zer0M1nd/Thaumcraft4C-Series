package emtmodern.item;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import thaumcraft.api.research.ResearchCategories;

/** The author's actual conditional research registrations, including their explicit dependency tests. */
@Mod.EventBusSubscriber(modid="emt",bus=Mod.EventBusSubscriber.Bus.MOD)
public class LegacyResearchConfig {
    public static final Map<String,ForgeConfigSpec.BooleanValue> DISABLED=new LinkedHashMap<>();
    public static final Map<String,String[]> REQUIREMENTS=new LinkedHashMap<>();
    public static final ForgeConfigSpec SPEC;
    static {
        var b=new ForgeConfigSpec.Builder();
        for(String id:("thaumium_drill thaumium_chainsaw thaumium_omnitool rockbreaker_drill stream_chainsaw electric_goggles nano_goggles quantum_goggles iron_omnitool diamond_omnitool explosion_focus christmas_focus energy_ball_focus shield_focus shield potentia_generator ignis_generator auram_generator arbor_generator aer_generator wand_charger tiny_uranium compressed_solar solar_goggles electric_traveller_boots nano_traveller_boots quantum_traveller_boots electric_scribing_tools ethereal_processor water_solar dark_solar order_solar fire_solar air_solar earth_solar uu_matter_infusion portable_node electric_growth_hoe charge_focus wand_charge_focus inventory_charging_ring armor_charging_ring thaumium_wings nano_wings quantum_wings infused_quantum_chestplate").split(" ")) {
            if(id.equals("thaumium_drill"))b.comment("旧版保留行为：此选项被 disable_thaumium_omnitool 覆盖。", "Legacy author and GTNH bug retained: this setting is overwritten by disable_thaumium_omnitool; see that option.");
            if(id.equals("thaumium_omnitool"))b.comment("旧版实际行为：同时控制钻头及依赖它的万能工具、碎岩钻研究。", "Legacy effective behavior: disables the drill and dependent omnitool/rockbreaker research, as the original field assignment did.");
            DISABLED.put(id,b.comment("禁用 "+id+" 研究；初始 false，可选 true/false；下级研究仍受原有依赖约束；重启世界。", "Disable "+id+" research; default false, true/false; child research still obeys legacy dependencies; restart the world.").define("disable_"+id,false));REQUIREMENTS.put(id,new String[]{id});
        }
        // Original EMTConfigHandler assigns the omn itool option to the drill field a second time.
        REQUIREMENTS.put("thaumium_drill",new String[]{"thaumium_omnitool"});
        REQUIREMENTS.put("thaumium_omnitool",new String[]{"thaumium_omnitool","thaumium_chainsaw"});
        REQUIREMENTS.put("rockbreaker_drill",new String[]{"rockbreaker_drill","thaumium_omnitool"});
        dependency("stream_chainsaw","thaumium_chainsaw");
        dependency("nano_goggles","electric_goggles");dependency("quantum_goggles","electric_goggles","nano_goggles");
        dependency("diamond_omnitool","iron_omnitool");dependency("shield","shield_focus");
        for(String id:new String[]{"ignis_generator","auram_generator","arbor_generator","aer_generator","wand_charger"})dependency(id,"potentia_generator");
        for(String id:new String[]{"solar_goggles","water_solar","dark_solar","order_solar","fire_solar","air_solar","earth_solar"})dependency(id,"compressed_solar");
        dependency("nano_traveller_boots","electric_traveller_boots");dependency("quantum_traveller_boots","electric_traveller_boots","nano_traveller_boots");
        dependency("nano_wings","thaumium_wings");dependency("quantum_wings","thaumium_wings","nano_wings");dependency("infused_quantum_chestplate","thaumium_wings","nano_wings","quantum_wings");
        SPEC=b.build();
    }
    public static void dependency(String id,String... prerequisites){var all=new String[prerequisites.length+1];all[0]=id;System.arraycopy(prerequisites,0,all,1,prerequisites.length);REQUIREMENTS.put(id,all);}
    public static boolean enabled(String id){for(String key:REQUIREMENTS.get(id))if(DISABLED.get(key).get())return false;return true;}
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event){event.enqueueWork(()->{
        REQUIREMENTS.keySet().forEach(id->ResearchCategories.registerEnablement("emt:"+id,()->enabled(id)));
        // The original energy-ball recipe uses a different (unobtainable) key; the switch gates that recipe too.
        ResearchCategories.registerEnablement("emt:legacy_wand_focus_energy_ball",()->enabled("energy_ball_focus"));
    });}
}
