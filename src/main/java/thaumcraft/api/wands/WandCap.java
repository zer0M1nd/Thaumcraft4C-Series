package thaumcraft.api.wands;

public  class WandCap {
    private WandCap() {}
    public static tc4revival.content.WandParts.Cap get(String id) { return tc4revival.content.WandParts.Cap.require(id); }
}
