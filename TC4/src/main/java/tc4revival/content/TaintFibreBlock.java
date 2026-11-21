package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import tc4revival.config.TCConfig;

public  class TaintFibreBlock extends Block {
    public static final IntegerProperty VARIANT=TaintSporeEntity.FIBRE_VARIANT;
    public TaintFibreBlock() {
        super(Properties.of().strength(1,5).noCollission().noOcclusion().randomTicks().noLootTable()
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                .sound(new net.minecraftforge.common.util.ForgeSoundType(.5f,.8f,TCSounds.GORE,TCSounds.GORE,TCSounds.GORE,TCSounds.GORE,TCSounds.GORE))
                .lightLevel(state->state.getValue(VARIANT)==2 ? 8 : state.getValue(VARIANT)==4 ? 10 : 0));
        var state=stateDefinition.any().setValue(VARIANT,0);
        for (Direction side:Direction.values()) state=state.setValue(MultifaceBlock.getFaceProperty(side),false);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        builder.add(VARIANT); for (Direction side:Direction.values()) builder.add(MultifaceBlock.getFaceProperty(side));
    }
    public static BlockState faces(BlockState state,BlockGetter level,BlockPos pos) {
        for (Direction side:Direction.values()) {
            var at=pos.relative(side);
            state=state.setValue(MultifaceBlock.getFaceProperty(side),!(level.getBlockState(at).getBlock() instanceof TaintSolidBlock) && level.getBlockState(at).isFaceSturdy(level,at,side.getOpposite()));
        }
        return state;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return TCBiomes.isTainted(context.getLevel(),context.getClickedPos()) ? faces(defaultBlockState(),context.getLevel(),context.getClickedPos()) : null;
    }
    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState adjacent,LevelAccessor level,BlockPos pos,BlockPos neighbor) {
        return faces(state,level,pos);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos from,boolean moving) {
        if (!level.isClientSide && onlyTaintNeighbors(level,pos)) level.removeBlock(pos,false);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        if (state.getValue(VARIANT)!=0) return box(3.2,0,3.2,12.8,12.8,12.8);
        for (Direction side:Direction.values()) if (state.getValue(MultifaceBlock.getFaceProperty(side))) return switch(side) {
            case DOWN->box(0,0,0,16,1,16); case UP->box(0,15,0,16,16,16);
            case NORTH->box(0,0,0,16,16,1); case SOUTH->box(0,0,15,16,16,16);
            case WEST->box(0,0,0,1,16,16); case EAST->box(15,0,0,16,16,16);
        };
        return box(0,0,0,16,1,16);
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) { poison(level,entity,1000,500); }
    static void poison(Level level,Entity entity,int playerChance,int otherChance) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || living.isInvertedHealAndHarm()) return;
        boolean player=entity instanceof Player;
        if (level.random.nextInt(player ? playerChance : otherChance)==0)
            living.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),player ? 80 : 160,0,false,true));
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        spreadBiome(level,pos,random,this);
        int kind=state.getValue(VARIANT);
        if ((kind==0 && onlyTaintNeighbors(level,pos)) || !TCBiomes.isTainted(level,pos)) { level.removeBlock(pos,false); return; }
        var target=pos.offset(random.nextInt(3)-1,random.nextInt(5)-3,random.nextInt(3)-1);
        if (level.isOutsideBuildHeight(target) || !level.hasChunkAt(target) || !TCBiomes.isTainted(level,target)) return;
        if (spread(level,target)) return;
        TaintSolidBlock.corrupt(level,target);
        if (kind==3 && TCConfig.SERVER.spawns(TCConfig.SpawnKind.TAINT_SPORES) && random.nextInt(10)==0 && level.isEmptyBlock(pos.above())) {
            level.setBlock(pos,state.setValue(VARIANT,4),3);
            var spore=new TaintSporeEntity(TCEntities.TAINT_SPORE.get(),level);
            spore.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0); level.addFreshEntity(spore);
        } else if (kind==4 && level.getEntitiesOfClass(TaintSporeEntity.class,new AABB(pos.above())).isEmpty())
            level.setBlock(pos,state.setValue(VARIANT,3),3);
    }
    public static boolean spread(ServerLevel level,BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
        var old=level.getBlockState(pos);
        if (!old.getFluidState().isEmpty() || onlyTaintNeighbors(level,pos)) return false;
        if (!(old.isAir() || old.canBeReplaced() || old.getBlock() instanceof BushBlock || old.is(BlockTags.LEAVES))) return false;
        boolean support=false;
        for (Direction side:Direction.values()) {
            var at=pos.relative(side); if (level.hasChunkAt(at) && level.getBlockState(at).isFaceSturdy(level,at,side.getOpposite())) { support=true; break; }
        }
        if (!support) return false;
        int kind=0;
        if (level.random.nextInt(10)==0 && level.isEmptyBlock(pos.above()) && level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))
            kind=level.random.nextInt(10)<9 ? 1 : level.random.nextInt(12)<10 ? 2 : 3;
        var block=TCArcane.TAINT_FIBRES.get();
        level.setBlock(pos,faces(block.defaultBlockState().setValue(VARIANT,kind),level,pos),3);
        level.blockEvent(pos,block,1,0); return true;
    }
    static boolean onlyTaintNeighbors(Level level,BlockPos pos) {
        for (Direction side:Direction.values()) {
            var at=pos.relative(side); if (!level.hasChunkAt(at)) continue;
            var state=level.getBlockState(at);
            if (!state.isAir() && !taintMaterial(state)) return false;
        }
        return true;
    }
    static boolean taintMaterial(BlockState state) {
        return state.getBlock() instanceof TaintFibreBlock || state.getBlock() instanceof TaintSolidBlock;
    }
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state) {
        return new net.minecraft.world.item.ItemStack(switch(state.getValue(VARIANT)) {
            case 1->TCItems.TAINT_GRASS.get(); case 2->TCItems.TAINT_PLANT.get();
            case 3->TCItems.TAINT_SPORE_STALK.get(); case 4->TCItems.TAINT_SPORE_STALK_MATURE.get(); default->TCItems.TAINT_FIBRES.get();
        });
    }
    static int adjacentTaint(Level level,BlockPos pos) {
        int count=0;
        for (Direction side:Direction.values()) {
            var at=pos.relative(side); if (!level.hasChunkAt(at)) continue;
            var block=level.getBlockState(at).getBlock(); if (block instanceof TaintFibreBlock || block instanceof TaintSolidBlock) count++;
        }
        return count;
    }
    static void spreadBiome(ServerLevel level,BlockPos pos,RandomSource random,Block source) {
        int rate=TCConfig.SERVER.taintSpreadRate.get(); if (rate<=0) return;
        var target=pos.offset(random.nextInt(3)-1,0,random.nextInt(3)-1);
        if (!level.hasChunkAt(target) || TCBiomes.isTainted(level,target)) return;
        long bound=(long)rate*5;
        boolean roll=bound<=Integer.MAX_VALUE ? random.nextInt((int)bound)==0 : Math.floorMod(random.nextLong(),bound)==0;
        if (roll && adjacentTaint(level,pos)>=2 && WorldBiomeChanges.changeNodeBiome(level,pos,target,TCBiomes.TAINTED_LAND)) level.blockEvent(pos,source,1,0);
    }
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int parameter) {
        if (id!=1) return super.triggerEvent(state,level,pos,id,parameter);
        if (level.isClientSide) level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),TCSounds.ROOTS.get(),net.minecraft.sounds.SoundSource.BLOCKS,.1f,.9f+level.random.nextFloat()*.2f,false);
        return true;
    }
}
