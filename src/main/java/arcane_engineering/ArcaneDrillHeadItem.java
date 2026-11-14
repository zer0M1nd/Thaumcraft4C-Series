package arcane_engineering;

import blusunrize.immersiveengineering.common.items.DrillheadItem;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public class ArcaneDrillHeadItem extends DrillheadItem {
    private final boolean voidMetal;
    public ArcaneDrillHeadItem(boolean voidMetal) {
        super(new DrillHeadPerm(voidMetal ? "void" : "thaumium", TagKey.create(Registries.ITEM,ResourceLocation.parse("forge:ingots/"+(voidMetal?"void":"thaumium"))),
            3,1,Tiers.DIAMOND,voidMetal?12:10,voidMetal?7:6,voidMetal?4000:3000,ArcaneEngineering.id("item/drill_"+(voidMetal?"void":"thaumium"))));
        this.voidMetal=voidMetal;
    }
    @Override public void damageHead(ItemStack head,int amount) { if(!voidMetal) setHeadDamage(head,Math.max(0,Math.min(getMaximumHeadDamage(head),getHeadDamage(head)+amount))); }
    @Override public int getHeadDamage(ItemStack head) { return voidMetal?0:super.getHeadDamage(head); }
}
