package tc4revival.content;

import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;

/** Keep minecraft:villager identity for breeding, curing, targeting and village population. */
public  class TCVillagers {
    private static final DeferredRegister<VillagerProfession> PROFESSIONS=DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS,"thaumcraft");
    public static final RegistryObject<VillagerProfession> WIZARD_PROFESSION=PROFESSIONS.register("wizard",()->profession("wizard"));
    public static final RegistryObject<VillagerProfession> BANKER_PROFESSION=PROFESSIONS.register("banker",()->profession("banker"));
    private static final DeferredRegister<net.minecraft.world.entity.ai.village.poi.PoiType> POIS=DeferredRegister.create(ForgeRegistries.POI_TYPES,"thaumcraft");
    public static final RegistryObject<net.minecraft.world.entity.ai.village.poi.PoiType> WIZARD_POI=POIS.register("wizard",()->new net.minecraft.world.entity.ai.village.poi.PoiType(ImmutableSet.copyOf(TCArcane.BLOCK.get().getStateDefinition().getPossibleStates()),1,1));
    public static final RegistryObject<net.minecraft.world.entity.ai.village.poi.PoiType> BANKER_POI=POIS.register("banker",()->new net.minecraft.world.entity.ai.village.poi.PoiType(ImmutableSet.copyOf(TCArcane.WAND_PEDESTAL.get().getStateDefinition().getPossibleStates()),1,1));
    private static VillagerProfession profession(String name) {
        var id=net.minecraft.resources.ResourceLocation.parse("thaumcraft:"+name);
        return new VillagerProfession(name,h->h.is(id),h->h.is(id),ImmutableSet.of(),ImmutableSet.of(),net.minecraft.sounds.SoundEvents.VILLAGER_WORK_LIBRARIAN);
    }
    public static void register(IEventBus bus) { POIS.register(bus);PROFESSIONS.register(bus); }
    public static LegacyVillagerTrades.Role role(Villager villager) {
        var profession=villager.getVillagerData().getProfession();
        return WIZARD_PROFESSION.isPresent()&&profession==WIZARD_PROFESSION.get()?LegacyVillagerTrades.Role.WIZARD:
                BANKER_PROFESSION.isPresent()&&profession==BANKER_PROFESSION.get()?LegacyVillagerTrades.Role.BANKER:null;
    }
    public static Villager create(LegacyVillagerTrades.Role role,Level level) {
        var villager=EntityType.VILLAGER.create(level);setRole(villager,role);return villager;
    }
    public static void setRole(Villager villager,LegacyVillagerTrades.Role role) {
        villager.setVillagerData(villager.getVillagerData().setProfession(role==LegacyVillagerTrades.Role.WIZARD?WIZARD_PROFESSION.get():BANKER_PROFESSION.get()).setLevel(1));
    }
}
