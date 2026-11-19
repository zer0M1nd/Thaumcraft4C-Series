package thaumcraft.api;

public class ItemRunic extends net.minecraft.world.item.Item implements IRunicArmor {
    private final int charge;
    public ItemRunic(Properties properties,int charge) { super(properties);if(charge<0)throw new IllegalArgumentException("Negative shield");this.charge=charge; }
    @Override public int getRunicCharge(net.minecraft.world.item.ItemStack stack) { return charge; }
}
