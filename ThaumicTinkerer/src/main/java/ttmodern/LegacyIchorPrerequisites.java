package ttmodern;

import java.util.List;

/** Frozen TC4 4.2.3.5 and original Thaumic Tinkerer 2.5-164 research, before Ichor.
 * Optional/removed entries are skipped only when absent or disabled; later addons cannot extend this list. */
final class LegacyIchorPrerequisites {
    static final List<String> KEYS = List.of(
        // BASICS
        "ASPECTS", "PECH", "NODES", "WARP", "RESEARCH",
        "KNOWFRAG", "THAUMONOMICON", "ORE", "PLANTS", "ENCHANT",
        "NODETAPPER1", "NODEPRESERVE", "NODEJAR", "NODETAPPER2", "RESEARCHER1",
        "DECONSTRUCTOR", "RESEARCHER2", "RESEARCHDUPE", "CRIMSON",
        // THAUMATURGY
        "BASICTHAUMATURGY", "FOCUSFIRE", "FOCUSFROST", "FOCUSHELLBAT", "FOCUSEXCAVATION",
        "FOCUSWARDING", "FOCUSSHOCK", "FOCUSTRADE", "FOCUSPORTABLEHOLE", "FOCUSPOUCH",
        "CAP_iron", "CAP_gold", "CAP_thaumium", "CAP_copper", "CAP_silver",
        "ROD_wood", "ROD_greatwood", "ROD_reed", "ROD_blaze", "ROD_obsidian",
        "ROD_ice", "ROD_quartz", "ROD_bone", "ROD_silverwood", "SCEPTRE",
        "ROD_greatwood_staff", "ROD_reed_staff", "ROD_blaze_staff", "ROD_obsidian_staff", "ROD_ice_staff",
        "ROD_quartz_staff", "ROD_bone_staff", "ROD_silverwood_staff", "WANDPED", "VISAMULET",
        "WANDPEDFOC", "NODESTABILIZER", "NODESTABILIZERADV", "VISPOWER", "VISCHARGERELAY",
        "FOCALMANIPULATION", "VAMPBAT",
        // ALCHEMY
        "PHIAL", "CRUCIBLE", "NITOR", "ALUMENTUM", "ALCHEMICALDUPLICATION",
        "ALCHEMICALMANUFACTURE", "ENTROPICPROCESSING", "LIQUIDDEATH", "BOTTLETAINT", "THAUMIUM",
        "PUREIRON", "PUREGOLD", "PURECOPPER", "PURETIN", "PURESILVER",
        "PURELEAD", "TRANSIRON", "TRANSGOLD", "TRANSCOPPER", "TRANSTIN",
        "TRANSSILVER", "TRANSLEAD", "TALLOW", "ETHEREALBLOOM", "BATHSALTS",
        "SANESOAP", "ARCANESPA", "DISTILESSENTIA", "JARLABEL", "JARVOID",
        "TUBES", "TUBEFILTER", "ESSENTIACRYSTAL", "CENTRIFUGE", "THAUMATORIUM",
        // ARTIFICE
        "BASICARTIFACE", "ARCANESTONE", "GRATE", "TABLE", "ARCTABLE",
        "RESTABLE", "THAUMOMETER", "PAVETRAVEL", "PAVEWARD", "GOGGLES",
        "ARCANEEAR", "SINSTONE", "LEVITATOR", "INFERNALFURNACE", "BELLOWS",
        "ENCHFABRIC", "INFUSION", "FLUXSCRUB", "RUNICARMOR", "RUNICCHARGED",
        "RUNICHEALING", "RUNICKINETIC", "RUNICEMERGENCY", "BANNERS", "RUNICAUGMENTATION",
        "BOOTSTRAVELLER", "HOVERHARNESS", "HOVERGIRDLE", "MIRROR", "MIRRORHAND",
        "MIRRORESSENTIA", "ARCANEBORE", "ARCANELAMP", "LAMPGROWTH", "LAMPFERTILITY",
        "BONEBOW", "PRIMALARROW", "ELEMENTALAXE", "ELEMENTALPICK", "ELEMENTALSWORD",
        "ELEMENTALSHOVEL", "ELEMENTALHOE", "WARDEDARCANA", "JARBRAIN", "INFUSIONENCHANTMENT",
        "ARMORFORTRESS", "HELMGOGGLES", "MASKGRINNINGDEVIL", "MASKANGRYGHOST", "MASKSIPPINGFIEND",
        // GOLEMANCY
        "HUNGRYCHEST", "GOLEMFETTER", "TRAVELTRUNK", "GOLEMSTRAW", "GOLEMWOOD",
        "GOLEMTALLOW", "GOLEMCLAY", "GOLEMFLESH", "GOLEMSTONE", "GOLEMIRON",
        "GOLEMTHAUMIUM", "GOLEMBELL", "COREGATHER", "COREFILL", "COREEMPTY",
        "CORESORTING", "COREUSE", "COREHARVEST", "COREFISHING", "CORELUMBER",
        "COREGUARD", "COREBUTCHER", "CORELIQUID", "COREALCHEMY", "UPGRADEAIR",
        "UPGRADEEARTH", "UPGRADEFIRE", "UPGRADEWATER", "UPGRADEORDER", "UPGRADEENTROPY",
        "ADVANCEDGOLEM", "TINYHAT", "TINYGLASSES", "TINYBOWTIE", "TINYFEZ",
        "TINYDART", "TINYVISOR", "TINYARMOR", "TINYHAMMER",
        // THAUMIC_TINKERER_164
        "thaumic_tinkerer:ANIMATION_TABLET", "thaumic_tinkerer:ASPECT_ANALYZER", "thaumic_tinkerer:BLOOD_SWORD", "thaumic_tinkerer:BRIGHT_NITOR", "thaumic_tinkerer:CLEANSING_TALISMAN",
        "thaumic_tinkerer:DARK_QUARTZ", "thaumic_tinkerer:DIMENSION_SHARDS", "thaumic_tinkerer:DISLOCATOR", "thaumic_tinkerer:ENCHANTER", "thaumic_tinkerer:FIRE_AER",
        "thaumic_tinkerer:FIRE_AQUA", "thaumic_tinkerer:FIRE_IGNIS", "thaumic_tinkerer:FIRE_ORDO", "thaumic_tinkerer:FIRE_PERDITIO", "thaumic_tinkerer:FIRE_TERRA",
        "thaumic_tinkerer:FOCUS_DEFLECT", "thaumic_tinkerer:FOCUS_DISLOCATION", "thaumic_tinkerer:FOCUS_ENDER_CHEST", "thaumic_tinkerer:FOCUS_FLIGHT", "thaumic_tinkerer:FOCUS_HEAL",
        "thaumic_tinkerer:FOCUS_SMELT", "thaumic_tinkerer:FOCUS_TELEKINESIS", "thaumic_tinkerer:FUNNEL", "thaumic_tinkerer:GASEOUS_LIGHT", "thaumic_tinkerer:GASEOUS_SHADOW",
        "thaumic_tinkerer:GAS_REMOVER", "thaumic_tinkerer:GOLEM_CONNECTOR", "thaumic_tinkerer:INFUSED_POTIONS", "thaumic_tinkerer:INTERFACE", "thaumic_tinkerer:LEVITATOR_LOCOMOTIVE",
        "thaumic_tinkerer:MAGNETS", "thaumic_tinkerer:PERIPHERALS", "thaumic_tinkerer:PLATFORM", "thaumic_tinkerer:REMOTE_PLACER", "thaumic_tinkerer:REPAIRER",
        "thaumic_tinkerer:REVEALING_HELM", "thaumic_tinkerer:SHARE_TOME", "thaumic_tinkerer:SPELL_CLOTH", "thaumic_tinkerer:SUMMON", "thaumic_tinkerer:TTENCH_ASCENT_BOOST",
        "thaumic_tinkerer:TTENCH_AUTO_SMELT", "thaumic_tinkerer:TTENCH_DESINTEGRATE", "thaumic_tinkerer:TTENCH_DISPERSED", "thaumic_tinkerer:TTENCH_FINAL", "thaumic_tinkerer:TTENCH_FOCUSED",
        "thaumic_tinkerer:TTENCH_POUNCE", "thaumic_tinkerer:TTENCH_QUICK_DRAW", "thaumic_tinkerer:TTENCH_SHATTER", "thaumic_tinkerer:TTENCH_SHOCKWAVE", "thaumic_tinkerer:TTENCH_SLOW_FALL",
        "thaumic_tinkerer:TTENCH_TUNNEL", "thaumic_tinkerer:TTENCH_VALIANCE", "thaumic_tinkerer:TTENCH_VAMPIRISM", "thaumic_tinkerer:XP_TALISMAN"
    );
    private LegacyIchorPrerequisites() {}
}
