package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Full physical cubes whose visible faces inset where the adjacent material does not block movement. */
public  class EldritchDecorBlock extends Block {
    public enum Kind { GLOWING, GLYPHED, CARVED }
    public static final BooleanProperty NORTH=BooleanProperty.create("north"),SOUTH=BooleanProperty.create("south"),WEST=BooleanProperty.create("west"),EAST=BooleanProperty.create("east"),UP=BooleanProperty.create("up"),DOWN=BooleanProperty.create("down");
    private final Kind kind;
    public EldritchDecorBlock(Kind kind) {
        super(Properties.of().strength(kind==Kind.CARVED ? 4 : 2,kind==Kind.CARVED ? 100 : 30).noOcclusion().sound(SoundType.STONE)
                .lightLevel(state->kind==Kind.CARVED ? 5 : 12).isValidSpawn((state,world,pos,type)->false));
        this.kind=kind; var state=stateDefinition.any(); for (Direction side:Direction.values()) state=state.setValue(property(side),false); registerDefaultState(state);
    }
    public Kind kind() { return kind; }
    public static BooleanProperty property(Direction side) { return switch(side) { case NORTH->NORTH; case SOUTH->SOUTH; case WEST->WEST; case EAST->EAST; case UP->UP; case DOWN->DOWN; }; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(NORTH,SOUTH,WEST,EAST,UP,DOWN); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return connected(defaultBlockState(),context.getLevel(),context.getClickedPos()); }
    public static BlockState connected(BlockState state,LevelReader level,BlockPos pos) {
        for (Direction side:Direction.values()) state=state.setValue(property(side),level.getBlockState(pos.relative(side)).blocksMotion()); return state;
    }
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        return state.setValue(property(side),neighbor.blocksMotion());
    }
    @Override public int getExpDrop(BlockState state,LevelReader level,RandomSource random,BlockPos pos,int fortune,int silkTouch) { return kind==Kind.GLYPHED ? 1+random.nextInt(4) : 0; }
}
