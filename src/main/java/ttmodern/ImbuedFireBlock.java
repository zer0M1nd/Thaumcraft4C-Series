package ttmodern;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class ImbuedFireBlock extends BaseFireBlock {
    public static final IntegerProperty AGE=IntegerProperty.create("age",0,15);
    private static final java.util.Map<Direction,net.minecraft.world.level.block.state.properties.BooleanProperty> FACES=java.util.Map.of(
        Direction.NORTH,FireBlock.NORTH,Direction.SOUTH,FireBlock.SOUTH,Direction.EAST,FireBlock.EAST,Direction.WEST,FireBlock.WEST,Direction.UP,FireBlock.UP);
    private final String aspect;
    public ImbuedFireBlock(String aspect) { super(Properties.of().replaceable().noCollission().instabreak().lightLevel(s->15).sound(SoundType.WOOL).noLootTable(),1);this.aspect=aspect;var state=stateDefinition.any().setValue(AGE,0);for(var face:FACES.values())state=state.setValue(face,false);registerDefaultState(state); }
    public String aspect() { return aspect; }
    // BaseFireBlock's placement chooses minecraft:fire/soul_fire; never call that factory here.
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) { return visualState(defaultBlockState(),context.getLevel(),context.getClickedPos()); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(AGE,FireBlock.NORTH,FireBlock.SOUTH,FireBlock.EAST,FireBlock.WEST,FireBlock.UP); }
    /** Old BlockFire renderer used floor flames on support and attached flames beside combustible faces. */
    public BlockState visualState(BlockState state,BlockGetter world,BlockPos pos) {
        var below=pos.below();var support=world.getBlockState(below);
        boolean floor=support.isFaceSturdy(world,below,Direction.UP)||support.isFlammable(world,below,Direction.UP);
        for(var entry:FACES.entrySet()){
            var next=pos.relative(entry.getKey());
            state=state.setValue(entry.getValue(),!floor&&world.getBlockState(next).isFlammable(world,next,entry.getKey().getOpposite()));
        }
        return state;
    }
    @Override protected boolean canBurn(BlockState state) { return !state.isAir(); }
    @Override public boolean canSurvive(BlockState state,LevelReader world,BlockPos pos) { return true; }
    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor world,BlockPos pos,BlockPos neighborPos) { return visualState(state,world,pos); }
    private int delay() { return aspect.equals("perditio")?1:200; }
    @Override public void onPlace(BlockState state,Level world,BlockPos pos,BlockState old,boolean moving) { var rendered=visualState(state,world,pos);if(rendered!=state)world.setBlock(pos,rendered,2);world.scheduleTick(pos,this,delay()+world.random.nextInt(10)); }
    private boolean fuel(ServerLevel world,BlockPos pos) { for(var direction:Direction.values()){var next=pos.relative(direction);if(world.hasChunkAt(next)&&ImbuedFireRules.transform(aspect,world,next)!=null)return true;}return false; }
    private void transformOrSet(ServerLevel world,BlockPos pos,BlockState fallback) {
        var converted=ImbuedFireRules.transform(aspect,world,pos);world.setBlock(pos,converted!=null?converted:fallback,3);
    }
    @Override public void tick(BlockState state,ServerLevel world,BlockPos pos,RandomSource random) {
        // Keep a pending update while disabled so turning fire ticking back on resumes existing sources.
        world.scheduleTick(pos,this,(TinkererConfig.FIRE.get()&&TinkererConfig.FIRE_SPREAD.get()?delay():200)+random.nextInt(3));
        if(!TinkererConfig.FIRE.get()||!TinkererConfig.FIRE_SPREAD.get()||!world.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK))return;
        if(world.isRainingAt(pos)||Direction.Plane.HORIZONTAL.stream().anyMatch(d->world.isRainingAt(pos.relative(d)))){world.removeBlock(pos,false);return;}
        int age=state.getValue(AGE);boolean fuel=fuel(world,pos);
        if(age!=0&&!fuel){world.removeBlock(pos,false);return;}
        if(fuel&&random.nextInt(20)==0)for(var direction:Direction.values()){var next=pos.relative(direction);if(!world.hasChunkAt(next))continue;var converted=ImbuedFireRules.transform(aspect,world,next);if(converted!=null)world.setBlock(next,converted,3);}
        if(age>=15)return;
        if(age>0)world.setBlock(pos,state.setValue(AGE,Math.min(15,age+random.nextInt(3)/2)),2);
        boolean humid=world.getBiome(pos).is(net.minecraft.tags.BiomeTags.INCREASED_FIRE_BURNOUT);
        for(var direction:Direction.values()) {
            var next=pos.relative(direction);if(!world.hasChunkAt(next))continue;var target=world.getBlockState(next);var conversion=ImbuedFireRules.transform(aspect,world,next);
            int flammability=conversion!=null?100:ImbuedFireRules.result(aspect,target)?0:target.getFlammability(world,next,direction.getOpposite());
            if(random.nextInt(humid?250:300)>=flammability)continue;
            boolean tnt=target.is(Blocks.TNT);
            transformOrSet(world,next,random.nextInt(age+10)<5&&!world.isRainingAt(next)?defaultBlockState().setValue(AGE,Math.max(1,Math.min(15,age+random.nextInt(5)/4))):Blocks.AIR.defaultBlockState());
            if(tnt)TntBlock.explode(world,next);
        }
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=2;y++) {
            if(x==0&&y==0&&z==0)continue;var next=pos.offset(x,y,z);if(!world.hasChunkAt(next)||!world.isEmptyBlock(next)||!fuel(world,next))continue;
            int chance=(170/(age+30))+70;if(humid)chance/=2;
            if(random.nextInt(100)>chance||world.isRainingAt(next)||Direction.Plane.HORIZONTAL.stream().anyMatch(d->world.isRainingAt(next.relative(d))))continue;
            world.setBlock(next,defaultBlockState().setValue(AGE,Math.min(age+1,15)),3);
        }
    }
}
