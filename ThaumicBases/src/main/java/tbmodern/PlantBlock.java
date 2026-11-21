package tbmodern;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Legacy crop timing, including its extra fully-ripe state, without mutable harvest flags. */
public final class PlantBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 8);
    public record Definition(String id, int stages, int delay, String soil, String seed, String produce) {}
    private final Definition definition;
    public PlantBlock(Definition definition) {
        super(Properties.of().noCollission().instabreak().sound(SoundType.CROP).randomTicks());
        this.definition = definition;
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }
    public Definition definition() { return definition; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(2, 0, 2, 14, Math.min(14, 3 + state.getValue(AGE) * 2), 14);
    }
    @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return switch(definition.soil()) {
            case "crystal" -> Content.CRYSTALS.stream().anyMatch(id -> state.is(Content.block(id)));
            case "grass" -> state.is(BlockTags.DIRT);
            case "solid" -> state.isSolidRender(level, pos);
            default -> state.is(Blocks.FARMLAND);
        };
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below())
            && (!definition.id().equals("lucrite") || level.getBlockState(pos.below(2)).is(Content.block("salis_mundus_block")));
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) { level.destroyBlock(pos, true); return; }
        int age = state.getValue(AGE);
        if (level.getRawBrightness(pos.above(), 0) >= 9 && age < definition.stages()
            && random.nextInt((int)(definition.delay() / growthSpeed(level, pos)) + 1) == 0)
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        if (definition.id().equals("sweed") && age == definition.stages() - 1) {
            BlockPos column = pos.relative(Direction.from2DDataValue(random.nextInt(4)));
            for (int offset = 2; offset >= -1; offset--) {
                BlockPos target = column.offset(0, offset, 0);
                if (level.hasChunkAt(target) && level.isEmptyBlock(target) && canSurvive(defaultBlockState(), level, target)) {
                    level.setBlock(target, defaultBlockState(), 3); break;
                }
            }
        }
    }
    private float growthSpeed(LevelReader level, BlockPos pos) {
        float speed = 1;
        for(int x = -1; x <= 1; x++) for(int z = -1; z <= 1; z++) {
            BlockPos soilPos = pos.offset(x, -1, z);
            BlockState soil = level.getBlockState(soilPos);
            if (mayPlaceOn(soil, level, soilPos)) {
                float weight = soil.is(Blocks.FARMLAND) && soil.getValue(FarmBlock.MOISTURE) > 0 ? 3 : 1;
                speed += x == 0 && z == 0 ? weight : weight / 4;
            }
        }
        boolean row = level.getBlockState(pos.east()).is(this) || level.getBlockState(pos.west()).is(this);
        boolean column = level.getBlockState(pos.north()).is(this) || level.getBlockState(pos.south()).is(this);
        boolean diagonal = false;
        for(int x : new int[]{-1,1}) for(int z : new int[]{-1,1}) diagonal |= level.getBlockState(pos.offset(x,0,z)).is(this);
        return diagonal || row && column ? speed / 2 : speed;
    }
    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) {
        return state.getValue(AGE) < definition.stages() - 1;
    }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return level.getRawBrightness(pos.above(), 0) >= 9;
    }
    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        boolean slow = definition.id().equals("lucrite") || definition.id().equals("ashroom");
        level.setBlock(pos, state.setValue(AGE, Math.min(slow ? definition.stages() : definition.stages() - 1,
            state.getValue(AGE) + (slow ? 1 : 1 + random.nextInt(3)))), 3);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (definition.id().equals("ashroom") || state.getValue(AGE) < definition.stages() - 1) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            int fortune = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, player.getItemInHand(hand));
            harvest(server, pos, state, fortune, true).forEach(stack -> popResource(level, pos, stack));
            level.setBlock(pos, state.setValue(AGE, 0), 3);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var origin = builder.getParameter(LootContextParams.ORIGIN);
        var tool = builder.getOptionalParameter(LootContextParams.TOOL);
        int fortune = tool == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
        return harvest(builder.getLevel(), BlockPos.containing(origin), state, fortune, false);
    }
    private List<ItemStack> harvest(ServerLevel level, BlockPos pos, BlockState state, int fortune, boolean replant) {
        var drops = new ArrayList<ItemStack>();
        int age = state.getValue(AGE);
        String id = definition.id();
        if (!replant || id.equals("knose") || id.equals("sweed")) drops.add(new ItemStack(Content.item(definition.seed())));
        if (age < definition.stages() - 1) return drops;
        if (id.equals("knose")) {
            for (String crystal : Content.CRYSTALS) if (level.getBlockState(pos.below()).is(Content.block(crystal)))
                drops.add(new ItemStack(Content.item(crystal.replace("crystal_", "knose_fragment_"))));
        } else if (id.equals("sweed")) {
            int sugar = level.random.nextInt(4 + Math.min(1024, Math.max(0, fortune)));
            if (sugar > 0) drops.add(new ItemStack(Items.SUGAR, sugar));
            if (level.random.nextBoolean()) drops.add(new ItemStack(Items.SUGAR_CANE));
        } else if (!definition.produce().isEmpty()) {
            int count = 1 + (fortune <= 0 ? 0 : (int)Math.min(3, Math.round(level.random.nextDouble() * (1 + .8 * (fortune - 1)))));
            drops.add(new ItemStack(Content.item(definition.produce()), count));
        }
        if (id.equals("lucrite") && level.random.nextInt(16) == 0) drops.add(new ItemStack(Items.GOLDEN_CARROT));
        return drops;
    }
    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity entity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, entity, tool);
        if (!level.isClientSide && definition.id().equals("ashroom") && state.getValue(AGE) >= definition.stages() - 1) {
            var aspects = thaumcraft.api.aspects.Aspect.getPrimalAspects();
            int count = 8 + (int)Math.round(level.random.nextDouble() * 11.76742);
            for(int i = 0; i < count; i++) level.addFreshEntity(new tc4revival.content.AspectOrbEntity(level,
                pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, aspects.get(level.random.nextInt(aspects.size())).unwrap(), 1));
        }
    }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(Content.item(definition.seed()));
    }
}
