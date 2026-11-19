package tc4revival.content;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.flag.FeatureFlagSet;

public class DeepslateOreItem extends BlockItem {
    public DeepslateOreItem(DeepslateOreBlock block){super(block,new Properties());}
    @Override public boolean isEnabled(FeatureFlagSet flags){return DeepslateOreBlock.enabled() && super.isEnabled(flags);}
    @Override public InteractionResult place(BlockPlaceContext context){return DeepslateOreBlock.enabled()?super.place(context):InteractionResult.FAIL;}
}
