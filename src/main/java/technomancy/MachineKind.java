package technomancy;

/** Physical capacities and identities from the author's 0.12.5 release. */
public enum MachineKind {
    QUANTUM_JAR(0,640), CREATIVE_JAR(0,640), ESSENTIA_RESERVOIR(0,256),
    ESSENTIA_DYNAMO(40000,64), NODE_DYNAMO(40000,0),
    ENERGY_CONDENSER(5000000,64), ECOLOGIC_TRANSMUTER(800000,0),
    NODE_FABRICATOR(50000000,256), ORDERLY_LAMP(0,32),
    ESSENTIA_COIL(0,0), ITEM_COIL(0,0), GALVANIZED_BELLOWS(20000,0),
    IGNIS_INCINERATOR(0,64), ELDRITCH_CONSUMER(1000000,512),
    ADVANCED_DECONSTRUCTION_TABLE(0,0), ESSENTIA_FUSOR(10000,192),
    BLOOD_DYNAMO(40000,0,"bloodmagic"), BLOOD_FABRICATOR(50000000,0,"bloodmagic"),
    SANGUINE_INFUSER(0,0,"bloodmagic"), HIPPIE_DYNAMO(40000,0,"botania"),
    MANA_FABRICATOR(2000000,0,"botania"), MANA_EXCHANGER(10000,0,"botania"),
    BOTANICAL_PURIFIER(0,0,"botania"),
    EARTH_CATALYST(0,0), FIRE_CATALYST(0,0), WATER_CATALYST(0,0), LIGHT_CATALYST(0,0), DARK_CATALYST(0,0),
    FIRE_CRYSTAL(0,0), WATER_CRYSTAL(0,0), EARTH_CRYSTAL(0,0), LIGHT_CRYSTAL(0,0),
    DARK_CRYSTAL(0,0), EXISTENCE_FOUNTAIN(0,0), EXISTENCE_BURNER(0,0),
    DYNAMIC_EXISTENCE_BURNER(100000,0), EXISTENCE_PYLON_1(0,0), EXISTENCE_PYLON_2(0,0),
    EXISTENCE_PYLON_3(0,0), EXISTENCE_CROP_FERTILISER(0,0),
    EXISTENCE_CROP_HARVESTER(0,0), EXISTENCE_SEALING_DEVICE(0,0);

    public final int energyCapacity, essentiaCapacity;
    public final String dependency;
    MachineKind(int energy, int essentia) { this(energy,essentia,""); }
    MachineKind(int energy, int essentia, String dependency) {
        this.energyCapacity=energy; this.essentiaCapacity=essentia; this.dependency=dependency;
    }
    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
    public boolean enabled() { return dependency.isEmpty() || net.minecraftforge.fml.ModList.get().isLoaded(dependency); }
    public boolean dynamo() { return this==ESSENTIA_DYNAMO||this==NODE_DYNAMO||this==BLOOD_DYNAMO||this==HIPPIE_DYNAMO; }
    public boolean processor() { return this==IGNIS_INCINERATOR||this==SANGUINE_INFUSER||this==BOTANICAL_PURIFIER; }
    public boolean jar() { return this==QUANTUM_JAR||this==CREATIVE_JAR||this==ESSENTIA_RESERVOIR; }
    public boolean crystal() { return name().endsWith("_CRYSTAL"); }
    public boolean catalyst() { return name().endsWith("_CATALYST"); }
}
