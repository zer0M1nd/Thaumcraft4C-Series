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
    private static final TagKey<Block> EARLY_PICK=BlockTags.create(ResourceLocation.parse("thaumcraft:golem_harvest_before_max_age"));
    public static final TagKey<Block> GROWTH_STAGE=BlockTags.create(ResourceLocation.parse("thaumcraft:golem_non_harvestable_growth_stages"));
    public static boolean pickedByUse(BlockState state) {
        return state.is(PICK) || state.getBlock() instanceof SweetBerryBushBlock
                || state.hasProperty(BlockStateProperties.BERRIES);
    }
    public static boolean mature(BlockState state) {
        if(state.is(GROWTH_STAGE)||state.getBlock() instanceof StemBlock||state.getBlock() instanceof AttachedStemBlock)return false;
        if(state.getBlock() instanceof SweetBerryBushBlock && state.hasProperty(SweetBerryBushBlock.AGE))
            return state.getValue(SweetBerryBushBlock.AGE)>=2;
        if(state.hasProperty(BlockStateProperties.BERRIES))return state.getValue(BlockStateProperties.BERRIES);
        if(state.getBlock() instanceof CropBlock crop)return crop.isMaxAge(state);
        if(!state.is(BlockTags.CROPS)&&!pickedByUse(state))return false;
        for(var property:state.getProperties()) {
            if(property instanceof IntegerProperty age&&age.getName().equals("age")) {
                int lastStage=java.util.Collections.max(age.getPossibleValues());
                return state.getValue(age)>=lastStage-(state.is(EARLY_PICK)?1:0);
            }
            if(state.is(PICK)&&property instanceof BooleanProperty ripe
                    && (ripe.getName().equals("mature") || ripe.getName().startsWith("has_") && ripe.getName().endsWith("berries")))
                return state.getValue(ripe);
        }
        // Some pickable plants use separate blocks for the fruiting and bare forms.
        return state.is(PICK);
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
