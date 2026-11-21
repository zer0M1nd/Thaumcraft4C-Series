package conciliummodern;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Registration of the original Tainted Magic encounter branch. */
public final class CrimsonContent {
    public static final RegistryObject<CrimsonAnnales> ANNALES=ConciliumItems.ITEMS.register("crimson_annales",CrimsonAnnales::new);
    public static final RegistryObject<PontifexHammer> HAMMER=ConciliumItems.ITEMS.register("pontifex_hammer",PontifexHammer::new);
    public static final RegistryObject<PontifexRobe> HELMET=robe("helmet",ArmorItem.Type.HELMET),CHEST=robe("chestplate",ArmorItem.Type.CHESTPLATE),LEGS=robe("leggings",ArmorItem.Type.LEGGINGS),BOOTS=robe("boots",ArmorItem.Type.BOOTS);
    private static RegistryObject<PontifexRobe> robe(String id,ArmorItem.Type type){return ConciliumItems.ITEMS.register("pontifex_"+id,()->new PontifexRobe(type));}
    public static final RegistryObject<EntityType<CrimsonBrigade>> PALADIN=brigade("crimson_paladin"),ARCHER=brigade("crimson_archer"),RANGER=brigade("crimson_ranger");
    private static RegistryObject<EntityType<CrimsonBrigade>> brigade(String id){return ConciliumEntities.TYPES.register(id,()->EntityType.Builder.of(CrimsonBrigade::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":"+id));}
    public static final RegistryObject<EntityType<CrimsonPontifex>> PONTIFEX=ConciliumEntities.TYPES.register("crimson_pontifex",()->EntityType.Builder.of(CrimsonPontifex::new,MobCategory.MONSTER).sized(.75f,2.25f).clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":crimson_pontifex"));
    public static final RegistryObject<EntityType<LesserPortal>> PORTAL=ConciliumEntities.TYPES.register("lesser_portal",()->EntityType.Builder.of(LesserPortal::new,MobCategory.MONSTER).sized(1.5f,3).fireImmune().clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":lesser_portal"));
    public static final RegistryObject<EntityType<EtherealShackles>> SHACKLES=ConciliumEntities.TYPES.register("ethereal_shackles",()->EntityType.Builder.<EtherealShackles>of(EtherealShackles::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":ethereal_shackles"));
    public static final RegistryObject<EntityType<CrimsonOrb>> ORB=ConciliumEntities.TYPES.register("crimson_orb",()->EntityType.Builder.<CrimsonOrb>of(CrimsonOrb::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":crimson_orb"));
    public static boolean available(){return net.minecraftforge.fml.ModList.get().isLoaded("taintedmagic");}
    public static boolean dyes(){return ConciliumDyesIntegration.available();}
    public static ItemStack item(String id){var key=ResourceLocation.tryParse(id);return key!=null&&ForgeRegistries.ITEMS.containsKey(key)?new ItemStack(ForgeRegistries.ITEMS.getValue(key)):ItemStack.EMPTY;}
    public static void register(IEventBus bus){
        CrimsonControl.EFFECTS.register(bus);PontifexRobe.init();CrimsonDrainNetwork.init();
        for(var type:java.util.List.of(PALADIN,ARCHER,RANGER,PONTIFEX,PORTAL))ConciliumItems.ITEMS.register(type.getId().getPath()+"_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(type,0x00ffff,type==PONTIFEX||type==PORTAL?0x111111:0x00008b,new Item.Properties()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->{
            event.put(PALADIN.get(),CrimsonBrigade.attributes(60).build());event.put(ARCHER.get(),CrimsonBrigade.attributes(25).build());event.put(RANGER.get(),CrimsonBrigade.attributes(30).build());
            event.put(PONTIFEX.get(),CrimsonPontifex.attributes().build());event.put(PORTAL.get(),LesserPortal.attributes().build());
        });
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            for(String name:java.util.List.of("TCTAINTEDMAGIC","CRIMSONANNALES","CRIMSONPONTIFEX","PONTIFEXHAMMER","PONTIFEXROBE","CRIMSONINITIATION"))
                thaumcraft.api.research.ResearchCategories.registerEnablement(ThaumicConcilium.ID+":"+name,CrimsonContent::available);
        }));
    }
    private CrimsonContent(){}
}
