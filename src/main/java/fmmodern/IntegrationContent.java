package fmmodern;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraftforge.fml.ModList;

/** Only this bridge references optional integrations. Items keep stable IDs even without their dependency. */
public final class IntegrationContent {
    public static boolean botania(){return ModList.get().isLoaded("botania");}
    public static boolean bloodMagic(){return ModList.get().isLoaded("bloodmagic");}
    public static boolean botanyEnabled(){return botania()&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.CROSS_MOD)&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.BOTANIA);}
    public static boolean bloodEnabled(){return bloodMagic()&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.CROSS_MOD)&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.BLOOD_MAGIC);}
    public static boolean creativeVisible(String name){
        if(fmmodern.ForbiddenConfig.value(ForbiddenConfig.NO_LUST)&&java.util.Set.of("lust_shard","pain_collar","riding_crop").contains(name))return false;
        if(!fmmodern.ForbiddenConfig.value(ForbiddenConfig.WRATH_CAGE)&&java.util.Set.of("wrath_cage","diabolist_fork","mob_crystal").contains(name))return false;
        if(fmmodern.ForbiddenConfig.value(ForbiddenConfig.GLUTTONY)==1&&java.util.Set.of("ring_nutrition","arcane_cake").contains(name))return false;
        boolean conversion=name.startsWith("wand_rod_")||name.startsWith("wand_cap_");
        if(conversion&&!java.util.Set.of("wand_rod_tainted","wand_rod_infernal","wand_rod_profane").contains(name)&&!fmmodern.ForbiddenConfig.value(ForbiddenConfig.CROSS_WAND))return false;
        if(java.util.Set.of("bloodwell","boundwell","blood_rapier","eldritch_orb","tainted_blood_shard","wand_cap_alchemical").contains(name)||name.startsWith("wand_rod_blood"))return bloodEnabled();
        if(name.equals("bloodthorn"))return bloodEnabled()&&botanyEnabled();
        if(java.util.Set.of("euclidaisy","whisperweed","tainthistle").contains(name)||name.startsWith("wand_rod_livingwood")||name.startsWith("wand_rod_dreamwood")||name.startsWith("wand_cap_manasteel")||name.startsWith("wand_cap_elementium")||name.startsWith("wand_cap_terrasteel"))return botanyEnabled();
        return true;
    }
    public static void register(){
        for(String name:new String[]{"blood","blood_inert","blood_staff"})Content.item("wand_rod_"+name,()->new Item(new Item.Properties()));
        Content.item("wand_cap_alchemical",()->new Item(new Item.Properties()));Content.item("tainted_blood_shard",()->new Item(new Item.Properties()));
        if(bloodMagic())fmmodern.compat.BloodIntegration.register();
        else for(String name:new String[]{"bloodwell","boundwell","blood_rapier","eldritch_orb"})Content.item(name,()->new Item(new Item.Properties().stacksTo(1)));
        for(String name:new String[]{"livingwood","livingwood_inert","dreamwood","dreamwood_inert","dreamwood_staff"})Content.item("wand_rod_"+name,()->new Item(new Item.Properties()));
        for(String name:new String[]{"manasteel","manasteel_inert","elementium","elementium_inert","terrasteel"})Content.item("wand_cap_"+name,()->new Item(new Item.Properties()));
        if(botania())fmmodern.compat.Botany.register();
        else for(String name:new String[]{"euclidaisy","whisperweed","tainthistle","bloodthorn"})Content.block(name,()->new FlowerBlock(net.minecraft.world.effect.MobEffects.BLINDNESS,8,net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(Blocks.DANDELION)));
    }
    public static void setup(){
        for(String key:new String[]{"BLOODMAGIC","BLOODWELL","BLOODRAPIER","ELDRITCHORB"})thaumcraft.api.research.ResearchCategories.registerEnablement("forbidden_magic:"+key,IntegrationContent::bloodEnabled);
        for(String key:new String[]{"ROD_blood","ROD_blood_staff","CAP_alchemical"})thaumcraft.api.research.ResearchCategories.registerEnablement("forbidden_magic:"+key,()->bloodEnabled()&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.CROSS_WAND));
        if(bloodMagic())fmmodern.compat.BloodIntegration.setup();
        for(String research:new String[]{"BOTANY","EUCLIDAISY","WHISPERWEED","TAINTHISTLE"})thaumcraft.api.research.ResearchCategories.registerEnablement("forbidden_magic:"+research,IntegrationContent::botanyEnabled);
        for(String research:new String[]{"ROD_livingwood","ROD_dreamwood","ROD_dreamwood_staff","CAP_manasteel","CAP_elementium","CAP_terrasteel"})thaumcraft.api.research.ResearchCategories.registerEnablement("forbidden_magic:"+research,()->IntegrationContent.botanyEnabled()&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.CROSS_WAND));
        thaumcraft.api.research.ResearchCategories.registerEnablement("forbidden_magic:BLOODTHORN",()->IntegrationContent.botanyEnabled()&&IntegrationContent.bloodMagic()&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.BLOOD_MAGIC));
        if(botania())fmmodern.compat.Botany.setup();
    }
    private IntegrationContent(){}
}
