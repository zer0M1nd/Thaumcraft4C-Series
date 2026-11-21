package heartwork.content;

import heartwork.Heartwork;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import thaumcraft.api.IScribeTools;

public class SwissResearcherItem extends SwissItem implements IScribeTools {
    public SwissResearcherItem(int durability) { super(durability, "researcher"); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return Heartwork.item("thaumcraft:scribing_tools").onItemUseFirst(stack, context);
    }
}
