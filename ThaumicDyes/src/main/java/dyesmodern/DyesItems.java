package dyesmodern;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.*;

public final class DyesItems {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ThaumicDyes.ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ThaumicDyes.ID);
    public static final Map<String,RegistryObject<Item>> ALL=new LinkedHashMap<>();
    static {
        ALL.put("itemCultistRobeHelmetDyed",ITEMS.register("cultist_robe_helm",()->new DyedArmorItem("CultistRobesDyed","itemCultistRobeHelmetDyed",DyesMaterial.RobesArmor,ArmorItem.Type.HELMET)));
        ALL.put("itemCultistRobeChestDyed",ITEMS.register("cultist_robe_chest",()->new DyedArmorItem("CultistRobesDyed","itemCultistRobeChestDyed",DyesMaterial.RobesArmor,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemCultistRobeLegsDyed",ITEMS.register("cultist_robe_legs",()->new DyedArmorItem("CultistRobesDyed","itemCultistRobeLegsDyed",DyesMaterial.RobesArmor,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemFortressHelmetDyed",ITEMS.register("fortress_helm",()->new DyedArmorItem("ThaumiumFortressDyed","itemFortressHelmetDyed",DyesMaterial.FortressArmor,ArmorItem.Type.HELMET)));
        ALL.put("itemFortressChestDyed",ITEMS.register("fortress_chest",()->new DyedArmorItem("ThaumiumFortressDyed","itemFortressChestDyed",DyesMaterial.FortressArmor,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemFortressLegsDyed",ITEMS.register("fortress_legs",()->new DyedArmorItem("ThaumiumFortressDyed","itemFortressLegsDyed",DyesMaterial.FortressArmor,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemCultistLeaderHelmetDyed",ITEMS.register("cultist_leader_helm",()->new DyedArmorItem("CultistPraetorDyed","itemCultistLeaderHelmetDyed",DyesMaterial.FortressArmor,ArmorItem.Type.HELMET)));
        ALL.put("itemCultistLeaderChestDyed",ITEMS.register("cultist_leader_chest",()->new DyedArmorItem("CultistPraetorDyed","itemCultistLeaderChestDyed",DyesMaterial.FortressArmor,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemCultistLeaderLegsDyed",ITEMS.register("cultist_leader_legs",()->new DyedArmorItem("CultistPraetorDyed","itemCultistLeaderLegsDyed",DyesMaterial.FortressArmor,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemCultistKnightHelmetDyed",ITEMS.register("cultist_plate_helm",()->new DyedArmorItem("CultistKnightDyed","itemCultistKnightHelmetDyed",DyesMaterial.KnightArmor,ArmorItem.Type.HELMET)));
        ALL.put("itemCultistKnightChestDyed",ITEMS.register("cultist_plate_chest",()->new DyedArmorItem("CultistKnightDyed","itemCultistKnightChestDyed",DyesMaterial.KnightArmor,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemCultistKnightLegsDyed",ITEMS.register("cultist_plate_legs",()->new DyedArmorItem("CultistKnightDyed","itemCultistKnightLegsDyed",DyesMaterial.KnightArmor,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemInhabitedPlateHelmet",ITEMS.register("inhabited_plate_helm",()->new RepairableDyesArmorItem("InhabitedKnightArmor","itemInhabitedPlateHelmet",DyesMaterial.IRON,ArmorItem.Type.HELMET)));
        ALL.put("itemInhabitedPlateChest",ITEMS.register("inhabited_plate_chest",()->new RepairableDyesArmorItem("InhabitedKnightArmor","itemInhabitedPlateChest",DyesMaterial.IRON,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemInhabitedPlateLegs",ITEMS.register("inhabited_plate_legs",()->new RepairableDyesArmorItem("InhabitedKnightArmor","itemInhabitedPlateLegs",DyesMaterial.IRON,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemIronFortressHelmet",ITEMS.register("iron_fortress_helm",()->new RepairableDyesArmorItem("IronFortressArmor","itemIronFortressHelmet",DyesMaterial.IronFortressArmor,ArmorItem.Type.HELMET)));
        ALL.put("itemIronFortressChest",ITEMS.register("iron_fortress_chest",()->new RepairableDyesArmorItem("IronFortressArmor","itemIronFortressChest",DyesMaterial.IronFortressArmor,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemIronFortressLegs",ITEMS.register("iron_fortress_legs",()->new RepairableDyesArmorItem("IronFortressArmor","itemIronFortressLegs",DyesMaterial.IronFortressArmor,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemCultistRangerHelmet",ITEMS.register("cultist_ranger_helm",()->new RepairableDyesArmorItem("CultistRanger","itemCultistRangerHelmet",DyesMaterial.IRON,ArmorItem.Type.HELMET)));
        ALL.put("itemCultistRangerChest",ITEMS.register("cultist_ranger_chest",()->new RepairableDyesArmorItem("CultistRanger","itemCultistRangerChest",DyesMaterial.IRON,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemCultistRangerLegs",ITEMS.register("cultist_ranger_legs",()->new RepairableDyesArmorItem("CultistRanger","itemCultistRangerLegs",DyesMaterial.IRON,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemCultistRangerHelmetDyed",ITEMS.register("cultist_ranger_helm_dyed",()->new DyedArmorItem("CultistRangerDyed","itemCultistRangerHelmetDyed",DyesMaterial.RobesArmor,ArmorItem.Type.HELMET)));
        ALL.put("itemCultistRangerChestDyed",ITEMS.register("cultist_ranger_chest_dyed",()->new DyedArmorItem("CultistRangerDyed","itemCultistRangerChestDyed",DyesMaterial.RobesArmor,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemCultistRangerLegsDyed",ITEMS.register("cultist_ranger_legs_dyed",()->new DyedArmorItem("CultistRangerDyed","itemCultistRangerLegsDyed",DyesMaterial.RobesArmor,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemVoidRobeHelmetDyed",ITEMS.register("void_robe_helm",()->new DyedArmorItem("VoidRobeDyed","itemVoidRobeHelmetDyed",DyesMaterial.armorMatVoid,ArmorItem.Type.HELMET)));
        ALL.put("itemVoidRobeChestDyed",ITEMS.register("void_robe_chest",()->new DyedArmorItem("VoidRobeDyed","itemVoidRobeChestDyed",DyesMaterial.armorMatVoid,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemVoidRobeLegsDyed",ITEMS.register("void_robe_legs",()->new DyedArmorItem("VoidRobeDyed","itemVoidRobeLegsDyed",DyesMaterial.armorMatVoid,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemThaumiumRobeHelmetDyed",ITEMS.register("thaumium_robe_helm",()->new DyedArmorItem("ThaumiumRobeDyed","itemThaumiumRobeHelmetDyed",DyesMaterial.armorMatThaumium,ArmorItem.Type.HELMET)));
        ALL.put("itemThaumiumRobeChestDyed",ITEMS.register("thaumium_robe_chest",()->new DyedArmorItem("ThaumiumRobeDyed","itemThaumiumRobeChestDyed",DyesMaterial.armorMatThaumium,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemThaumiumRobeLegsDyed",ITEMS.register("thaumium_robe_legs",()->new DyedArmorItem("ThaumiumRobeDyed","itemThaumiumRobeLegsDyed",DyesMaterial.armorMatThaumium,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemThaumiumBootsTraveller",ITEMS.register("boots_thaumium_traveller",()->new RepairableDyesArmorItem("ThaumiumBoots","itemThaumiumBootsTraveller",DyesMaterial.armorMatThaumium,ArmorItem.Type.BOOTS)));
        ALL.put("itemVoidBootsTraveller",ITEMS.register("boots_void_traveller",()->new RepairableDyesArmorItem("VoidBoots","itemVoidBootsTraveller",DyesMaterial.armorMatVoid,ArmorItem.Type.BOOTS)));
        ALL.put("itemCultistArcherHelmet",ITEMS.register("cultist_archer_helm",()->new RepairableDyesArmorItem("CultistArcher","itemCultistArcherHelmet",DyesMaterial.IRON,ArmorItem.Type.HELMET)));
        ALL.put("itemCultistArcherChest",ITEMS.register("cultist_archer_chest",()->new RepairableDyesArmorItem("CultistArcher","itemCultistArcherChest",DyesMaterial.IRON,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemCultistArcherLegs",ITEMS.register("cultist_archer_legs",()->new RepairableDyesArmorItem("CultistArcher","itemCultistArcherLegs",DyesMaterial.IRON,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemCultistPaladinHelmet",ITEMS.register("cultist_paladin_helm",()->new RepairableDyesArmorItem("CultistPaladin","itemCultistPaladinHelmet",DyesMaterial.FortressArmor,ArmorItem.Type.HELMET)));
        ALL.put("itemCultistArcherHelmetDyed",ITEMS.register("cultist_archer_helm_dyed",()->new DyedArmorItem("CultistArcherDyed","itemCultistArcherHelmetDyed",DyesMaterial.KnightArmor,ArmorItem.Type.HELMET)));
        ALL.put("itemCultistArcherChestDyed",ITEMS.register("cultist_archer_chest_dyed",()->new DyedArmorItem("CultistArcherDyed","itemCultistArcherChestDyed",DyesMaterial.KnightArmor,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemCultistArcherLegsDyed",ITEMS.register("cultist_archer_legs_dyed",()->new DyedArmorItem("CultistArcherDyed","itemCultistArcherLegsDyed",DyesMaterial.KnightArmor,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemResourceFabric",ITEMS.register("item_resource_fabric",()->new Item(new Item.Properties())));
        ALL.put("itemSunKnightHelmet",ITEMS.register("sun_knight_helm",()->new RepairableDyesArmorItem("SunKnight","itemSunKnightHelmet",DyesMaterial.IRON,ArmorItem.Type.HELMET)));
        ALL.put("itemSunKnightChest",ITEMS.register("sun_knight_chest",()->new RepairableDyesArmorItem("SunKnight","itemSunKnightChest",DyesMaterial.IRON,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemRunicHelmet",ITEMS.register("item_runic_helmet",()->new DyesArmorItem("ItemRunicArmor","itemRunicHelmet",DyesMaterial.RunicArmor,ArmorItem.Type.HELMET)));
        ALL.put("itemRunicChest",ITEMS.register("item_runic_chest",()->new DyesArmorItem("ItemRunicArmor","itemRunicChest",DyesMaterial.RunicArmor,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemRunicLegs",ITEMS.register("item_runic_legs",()->new DyesArmorItem("ItemRunicArmor","itemRunicLegs",DyesMaterial.RunicArmor,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemRunicBoots",ITEMS.register("item_runic_boots",()->new DyesArmorItem("ItemRunicArmor","itemRunicBoots",DyesMaterial.RunicArmor,ArmorItem.Type.BOOTS)));
        ALL.put("itemRunicGoggles",ITEMS.register("item_runic_goggles",()->new DyesArmorItem("ItemRunicGoggles","itemRunicGoggles",DyesMaterial.RunicArmor,ArmorItem.Type.HELMET)));
        ALL.put("itemRunicBootsTraveller",ITEMS.register("item_runic_boots_traveller",()->new DyesArmorItem("ItemRunicBootsTraveller","itemRunicBootsTraveller",DyesMaterial.RunicArmor,ArmorItem.Type.BOOTS)));
        ALL.put("itemHelmetMaskTX",ITEMS.register("item_mask_evil",()->new RepairableDyesArmorItem("ItemTXAncientMask","itemHelmetMaskTX",DyesMaterial.armorMatSpecial,ArmorItem.Type.HELMET)));
        ALL.put("itemRunicHelmetEnhanced",ITEMS.register("item_runic_helmet_enhanced",()->new DyesArmorItem("ItemRunicArmorEnhanced","itemRunicHelmetEnhanced",DyesMaterial.RunicII,ArmorItem.Type.HELMET)));
        ALL.put("itemRunicChestEnhanced",ITEMS.register("item_runic_chest_enhanced",()->new DyesArmorItem("ItemRunicArmorEnhanced","itemRunicChestEnhanced",DyesMaterial.RunicII,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemRunicLegsEnhanced",ITEMS.register("item_runic_legs_enhanced",()->new DyesArmorItem("ItemRunicArmorEnhanced","itemRunicLegsEnhanced",DyesMaterial.RunicII,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemRunicBootsEnhanced",ITEMS.register("item_runic_boots_enhanced",()->new DyesArmorItem("ItemRunicArmorEnhanced","itemRunicBootsEnhanced",DyesMaterial.RunicII,ArmorItem.Type.BOOTS)));
        ALL.put("itemRunicHelmetPrimal",ITEMS.register("item_runic_helmet_primal",()->new DyesArmorItem("ItemRunicArmorPrimal","itemRunicHelmetPrimal",DyesMaterial.RunicIII,ArmorItem.Type.HELMET)));
        ALL.put("itemRunicChestPrimal",ITEMS.register("item_runic_chest_primal",()->new DyesArmorItem("ItemRunicArmorPrimal","itemRunicChestPrimal",DyesMaterial.RunicIII,ArmorItem.Type.CHESTPLATE)));
        ALL.put("itemRunicLegsPrimal",ITEMS.register("item_runic_legs_primal",()->new DyesArmorItem("ItemRunicArmorPrimal","itemRunicLegsPrimal",DyesMaterial.RunicIII,ArmorItem.Type.LEGGINGS)));
        ALL.put("itemRunicBootsPrimal",ITEMS.register("item_runic_boots_primal",()->new DyesArmorItem("ItemRunicArmorPrimalBoots","itemRunicBootsPrimal",DyesMaterial.RunicIII,ArmorItem.Type.BOOTS)));
        TABS.register("dyes",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.thaumicdyes")).icon(()->new ItemStack(get("itemRunicChestPrimal"))).displayItems((parameters,output)->ALL.values().forEach(item->output.accept(item.get()))).build());
    }
    public static Item get(String legacyKey) { var item=ALL.get(legacyKey); if(item==null)throw new IllegalArgumentException("Unknown Thaumic Dyes item: "+legacyKey);return item.get(); }
    private DyesItems() {}
}
