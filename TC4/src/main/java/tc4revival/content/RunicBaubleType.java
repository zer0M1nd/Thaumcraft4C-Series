package tc4revival.content;

public enum RunicBaubleType {
    LESSER_RING("runic_ring_lesser","ItemRingRunic",0,"ring",1,RunicUpgradeGear.Upgrade.NONE),
    RING("runic_ring","ItemRingRunic",1,"ring",5,RunicUpgradeGear.Upgrade.NONE),
    CHARGED_RING("runic_ring_charged","ItemRingRunic",2,"ring",4,RunicUpgradeGear.Upgrade.CHARGED),
    HEALING_RING("runic_ring_regen","ItemRingRunic",3,"ring",4,RunicUpgradeGear.Upgrade.HEALING),
    AMULET("runic_amulet","ItemAmuletRunic",0,"necklace",8,RunicUpgradeGear.Upgrade.NONE),
    EMERGENCY_AMULET("runic_amulet_emergency","ItemAmuletRunic",1,"necklace",7,RunicUpgradeGear.Upgrade.EMERGENCY),
    GIRDLE("runic_girdle","ItemGirdleRunic",0,"belt",10,RunicUpgradeGear.Upgrade.NONE),
    KINETIC_GIRDLE("runic_girdle_kinetic","ItemGirdleRunic",1,"belt",9,RunicUpgradeGear.Upgrade.KINETIC);
    private final String path,legacy,slot;
    private final int metadata,charge;
    private final RunicUpgradeGear.Upgrade upgrade;
    RunicBaubleType(String path,String legacy,int metadata,String slot,int charge,RunicUpgradeGear.Upgrade upgrade) { this.path=path; this.legacy=legacy; this.metadata=metadata; this.slot=slot; this.charge=charge; this.upgrade=upgrade; }
    public String path() { return path; }
    public String legacy() { return legacy; }
    public String slot() { return slot; }
    public int metadata() { return metadata; }
    public int charge() { return charge; }
    public RunicUpgradeGear.Upgrade upgrade() { return upgrade; }
}
