package tc4revival.content;

import net.minecraft.world.item.*;

/** TC4 material damage and rarity with native sword tools, sweeping and wear. */
public class ThaumiumSwordItem extends SwordItem implements RepairableItem {
    public ThaumiumSwordItem() { this(ThaumiumToolTier.INSTANCE); }
    protected ThaumiumSwordItem(Tier tier) { super(tier,4,-2.4f,new Item.Properties().rarity(Rarity.UNCOMMON)); }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.UNCOMMON; }
}
