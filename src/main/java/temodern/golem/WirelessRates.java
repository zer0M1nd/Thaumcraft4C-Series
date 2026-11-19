package temodern.golem;

/** Legacy per-network-update budgets (20 ticks), separate from physical carrying capacity. */
public record WirelessRates(int items,int fluid,int essentia) {
    public static WirelessRates of(int orderUpgrades,boolean advanced){
        int tier=Math.max(0,Math.min(2,orderUpgrades)),scale=advanced?2:1;
        return new WirelessRates(new int[]{8,24,32}[tier]*scale,new int[]{100,250,500}[tier]*scale,new int[]{4,12,16}[tier]*scale);
    }
}
