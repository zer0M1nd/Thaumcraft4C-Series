package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import thaumcraft.api.golems.GolemCropRegistry;

/** Native crop ages and item placement preserve mod-specific soils, seeds and loot. */
public class ModernCropHarvest {
    public static final TagKey<Block> PICK=BlockTags.create(ResourceLocation.parse("thaumcraft:golem_harvest_by_use"));
    public static final TagKey<Block> GROWTH_STAGE=BlockTags.create(ResourceLocation.parse("thaumcraft:golem_non_harvestable_growth_stages"));
    public static boolean mature(BlockState state) {
        if(state.is(GROWTH_STAGE)||state.getBlock() instanceof StemBlock||state.getBlock() instanceof AttachedStemBlock)return false;
        if(state.getBlock() instanceof CropBlock crop)return crop.isMaxAge(state);
        if(!state.is(BlockTags.CROPS)&&!state.is(PICK))return false;
        if(state.hasProperty(BlockStateProperties.BERRIES))return state.getValue(BlockStateProperties.BERRIES);
        for(var property:state.getProperties())if(property instanceof IntegerProperty age&&age.getName().equals("age"))
            return state.getValue(age).equals(java.util.Collections.max(age.getPossibleValues()));
        return false;
    }
    public static boolean plantable(ItemStack stack) {
        if(stack.is(net.minecraftforge.common.Tags.Items.SEEDS))return true;
        if(stack.getItem() instanceof BlockItem item) {
            var block=item.getBlock();var state=block.defaultBlockState();
            return block instanceof CropBlock||state.is(BlockTags.CROPS)||state.is(GROWTH_STAGE)
                    ||block instanceof SugarCaneBlock||block instanceof CactusBlock||block instanceof NetherWartBlock;
        }
        return stack.getItem() instanceof net.minecraftforge.common.IPlantable;
    }
    public static boolean pick(GolemCropRegistry.Context context) {
        if(!context.allowed(context.pos()))return false;
        var player=context.player();var saved=player.getMainHandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        try {
            return context.original().use(context.level(),player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(context.pos()),Direction.UP,context.pos(),false)).consumesAction();
        } finally {player.setItemInHand(InteractionHand.MAIN_HAND,saved);}
    }
}
