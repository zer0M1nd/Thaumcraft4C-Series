package tc4revival.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import tc4revival.aspect.entity.EntityAspectRule;

/** Source data for DataGen, transcribed as data from ConfigAspects's vanilla entity registrations. */
public  class LegacyEntityDefinitions {
    private LegacyEntityDefinitions() {}

    public static List<EntityAspectRule> rules() {
        var rules = new ArrayList<EntityAspectRule>();
        row(rules, "zombie,zombie_villager", "Zombie", "exanimis=2 humanus=1 terra=1");
        row(rules, "giant", "Giant", "exanimis=4 humanus=3 terra=3");
        row(rules, "skeleton", "Skeleton", "exanimis=3 humanus=1 terra=1");
        row(rules, "wither_skeleton", "Skeleton", EntityAspectRule.Variant.ANY, "SkeletonType1", "exanimis=4 humanus=1 ignis=2");
        row(rules, "creeper", "Creeper", "herba=2 ignis=2");
        row(rules, "creeper", "Creeper", EntityAspectRule.Variant.POWERED, "powered1", "herba=3 ignis=3 potentia=3");
        row(rules, "horse,donkey,mule,zombie_horse,skeleton_horse", "EntityHorse", "bestia=4 terra=1 aer=1");
        row(rules, "pig", "Pig", "bestia=2 terra=2");
        row(rules, "experience_orb", "XPOrb", "cognitio=5");
        row(rules, "sheep", "Sheep", "bestia=2 terra=2");
        row(rules, "cow", "Cow", "bestia=3 terra=3");
        row(rules, "mooshroom", "MushroomCow", "bestia=3 herba=1 terra=2");
        row(rules, "snow_golem", "SnowMan", "gelum=3 aqua=1");
        row(rules, "ocelot,cat", "Ozelot", "bestia=3 perditio=3");
        row(rules, "chicken", "Chicken", "bestia=2 volatus=2 aer=1");
        row(rules, "squid", "Squid", "bestia=2 aqua=2");
        row(rules, "wolf", "Wolf", "bestia=3 terra=3");
        row(rules, "bat", "Bat", "bestia=1 volatus=1 aer=1");
        row(rules, "boat", "Boat", EntityAspectRule.Variant.OAK_BOAT, "", "machina=2 aqua=2");
        row(rules, "spider", "Spider", "bestia=3 perditio=2");
        row(rules, "slime", "Slime", "limus=2 aqua=2");
        row(rules, "ghast", "Ghast", "exanimis=3 ignis=2");
        row(rules, "zombified_piglin", "PigZombie", "exanimis=4 ignis=2");
        row(rules, "enderman", "Enderman", "alienis=4 iter=2 aer=2");
        row(rules, "cave_spider", "CaveSpider", "bestia=2 venenum=2 terra=1");
        row(rules, "silverfish", "Silverfish", "bestia=1 terra=1");
        row(rules, "blaze", "Blaze", "alienis=4 ignis=1");
        row(rules, "magma_cube", "LavaSlime", "limus=3 ignis=2");
        row(rules, "ender_dragon", "EnderDragon", "alienis=20 bestia=20 perditio=20");
        row(rules, "wither", "WitherBoss", "exanimis=20 perditio=20 ignis=15");
        row(rules, "witch", "Witch", "humanus=3 praecantatio=2 ignis=1");
        row(rules, "villager", "Villager", "humanus=3 aer=2");
        row(rules, "iron_golem", "VillagerGolem", "metallum=4 terra=3");
        row(rules, "minecart", "MinecartRideable", "machina=3 aer=2");
        row(rules, "chest_minecart", "MinecartChest", "machina=3 aer=1 vacuos=1");
        row(rules, "furnace_minecart", "MinecartFurnace", "machina=3 aer=1 ignis=1");
        row(rules, "tnt_minecart", "MinecartTNT", "machina=3 aer=1 ignis=1");
        row(rules, "hopper_minecart", "MinecartHopper", "machina=3 aer=1 permutatio=1");
        row(rules, "spawner_minecart", "MinecartSpawner", "machina=3 aer=1 praecantatio=1");
        row(rules, "end_crystal", "EnderCrystal", "alienis=3 praecantatio=3 sano=3");
        row(rules, "item_frame", "ItemFrame", "sensus=3 pannus=1");
        row(rules, "painting", "Painting", "sensus=5 pannus=3");
        return List.copyOf(rules);
    }

    private static void row(List<EntityAspectRule> output, String ids, String legacy, String quantities) {
        row(output, ids, legacy, EntityAspectRule.Variant.ANY, "", quantities);
    }

    private static void row(List<EntityAspectRule> output, String ids, String legacy, EntityAspectRule.Variant variant,
                            String qualifier, String quantities) {
        var values = Arrays.stream(quantities.split(" ")).map(pair -> {
            String[] parts = pair.split("=");
            return new EntityAspectRule.Quantity(parts[0], Integer.parseInt(parts[1]));
        }).toList();
        for (String id : ids.split(",")) output.add(new EntityAspectRule("minecraft:" + id, legacy, variant, qualifier, values));
    }
}
