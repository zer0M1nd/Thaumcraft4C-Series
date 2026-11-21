package fmmodern;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

/** Separate registered harvesting tiers retain elemental durability, speed and repair material. */
public final class ToolTiers {
    public static final Tier MORPH = register("morph",4,List.of(Tiers.NETHERITE));
    public static final Tier TAINT = register("taint",5,List.of(MORPH));
    private static Tier register(String name,int level,List<Object> after){
        var elemental=tc4revival.content.ElementalToolTier.INSTANCE;
        var id=ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID,name);
        return TierSortingRegistry.registerTier(new ForgeTier(level,elemental.getUses(),elemental.getSpeed(),elemental.getAttackDamageBonus(),elemental.getEnchantmentValue(),BlockTags.create(ResourceLocation.fromNamespaceAndPath(ForbiddenMagic.ID,"needs_"+name+"_tool")),elemental::getRepairIngredient),id,after,List.of());
    }
    private ToolTiers(){}
}
