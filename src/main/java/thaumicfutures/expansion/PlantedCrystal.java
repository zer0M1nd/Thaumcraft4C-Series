package thaumicfutures.expansion;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** TC6's four sizes and limited generations, fed by TC4's ordinary regenerating aura nodes. */
@Mod.EventBusSubscriber(modid="thaumicfutures")
public class PlantedCrystal extends BaseEntityBlock {
    public static final IntegerProperty SIZE=IntegerProperty.create("size",0,3),GENERATION=IntegerProperty.create("generation",1,4);
    public static final int VIS_PER_GROWTH=10,RANGE=16;
    public final ShardType shard;
    public PlantedCrystal(ShardType shard){
        super(Properties.of().strength(.25f).noOcclusion().randomTicks().sound(SoundType.AMETHYST).lightLevel(state->5));
        this.shard=shard;registerDefaultState(stateDefinition.any().setValue(SIZE,0).setValue(GENERATION,1));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(SIZE,GENERATION);}
    public Aspect aspect(){return TCAspects.catalog().require(new String[]{"aer","ignis","aqua","terra","ordo","perditio"}[shard.metadata()]);}
    public boolean supports(BlockState state){return state.getBlock() instanceof LegacyOreBlock ore&&ore.metadata()==shard.metadata()+1;}
    public boolean supportedAt(LevelReader level,BlockPos pos,Direction side){return supports(level.getBlockState(pos.relative(side)));}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        for(var side:Direction.values())if(supportedAt(level,pos,side))return true;return false;
    }
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos adjacent){
        if(!canSurvive(state,level,pos)){
            if(!level.isClientSide())Block.dropResources(state,level,pos,level.getBlockEntity(pos));
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state,side,neighbor,level,pos,adjacent);
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Tile(pos,state);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        VoxelShape shape=Shapes.empty();
        for(var side:Direction.values())if(supports(level.getBlockState(pos.relative(side))))shape=Shapes.or(shape,switch(side){
            case DOWN->box(0,0,0,16,8,16);case UP->box(0,8,0,16,16,16);
            case NORTH->box(0,0,0,16,16,8);case SOUTH->box(0,0,8,16,16,16);
            case WEST->box(0,0,0,8,16,16);case EAST->box(8,0,0,16,16,16);
        });
        return shape.isEmpty()?box(4,0,4,12,8,12):shape;
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        return List.of(new ItemStack(TCItems.shard(shard),state.getValue(SIZE)+1));
    }
    @Override public ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state){return new ItemStack(TCItems.shard(shard));}
    private AuraNodeBlockEntity nearestNode(ServerLevel level,BlockPos pos){
        AuraNodeBlockEntity best=null;double distance=RANGE*RANGE+1;
        for(int x=(pos.getX()-RANGE)>>4;x<=(pos.getX()+RANGE)>>4;x++)
            for(int z=(pos.getZ()-RANGE)>>4;z<=(pos.getZ()+RANGE)>>4;z++){
                var chunk=level.getChunkSource().getChunkNow(x,z);if(chunk==null)continue;
                for(var entity:chunk.getBlockEntities().values())if(entity instanceof AuraNodeBlockEntity node&&node.base().amount(aspect())>0){
                    double d=pos.distSqr(node.getBlockPos());
                    if(d<distance){distance=d;best=node;}
                }
            }
        return best;
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled()||random.nextInt(3+state.getValue(GENERATION))!=0)return;
        int size=state.getValue(SIZE),generation=state.getValue(GENERATION);
        var node=nearestNode(level,pos);
        int charge=node==null?0:node.aspects().amount(aspect());
        if(charge<=VIS_PER_GROWTH){
            // The final isolated seed survives poor aura, as in TC6.
            boolean touching=false;
            for(var side:Direction.values())if(level.getBlockState(pos.relative(side)).is(this)){touching=true;break;}
            if(size>0||touching){
                level.setBlockAndUpdate(pos,size>0?state.setValue(SIZE,size-1):Blocks.AIR.defaultBlockState());
                if(node!=null)node.addCharge(aspect(),Math.min(VIS_PER_GROWTH,Math.max(0,node.base().amount(aspect())-charge)));
            }
            return;
        }
        // An undisturbed node at >= 75% of its base is the TC4 equivalent of surplus ambient aura.
        if(charge<=Math.max(VIS_PER_GROWTH,(node.base().amount(aspect())*3)/4))return;
        if(size<3&&size<5-generation+pos.asLong()%3L){
            if(node.take(aspect(),VIS_PER_GROWTH)){node.sync();level.setBlockAndUpdate(pos,state.setValue(SIZE,size+1));}
        }else if(generation<4){
            var target=pos.offset(random.nextInt(3)-1,random.nextInt(3)-1,random.nextInt(3)-1);
            if(!target.equals(pos)&&random.nextInt(16)==0&&level.hasChunkAt(target)&&level.isEmptyBlock(target)
                    &&canSurvive(defaultBlockState(),level,target)&&node.take(aspect(),VIS_PER_GROWTH)){
                int next=Math.min(4,generation+(random.nextInt(6)==0?0:1));
                node.sync();level.setBlockAndUpdate(target,defaultBlockState().setValue(GENERATION,next));
            }
        }
    }
    @SubscribeEvent public static void plant(PlayerInteractEvent.RightClickBlock event){
        if(!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled()||event.getFace()==null)return;
        var stack=event.getItemStack();PlantedCrystal crystal=null;
        for(var type:RestoredContent.PLANTED_CRYSTALS.keySet())if(stack.is(TCItems.shard(type))){crystal=RestoredContent.PLANTED_CRYSTALS.get(type).get();break;}
        if(crystal==null||!crystal.supports(event.getLevel().getBlockState(event.getPos())))return;
        var level=event.getLevel();var player=event.getEntity();var target=event.getPos().relative(event.getFace());
        var context=new BlockPlaceContext(player,event.getHand(),stack,event.getHitVec());
        if(!level.getBlockState(target).canBeReplaced(context)||!level.getFluidState(target).isEmpty()
                ||!level.mayInteract(player,target)||!player.mayUseItemAt(target,event.getFace(),stack))return;
        if(!level.isClientSide){
            var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,target);
            if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,event.getFace()))return;
            if(level.setBlockAndUpdate(target,crystal.defaultBlockState())){
                if(!player.getAbilities().instabuild)stack.shrink(1);
                level.playSound(null,target,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            }
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));event.setCanceled(true);
    }
    public static class Tile extends BlockEntity {
        public Tile(BlockPos pos,BlockState state){super(RestoredContent.CRYSTAL_ENTITY.get(),pos,state);}
    }
}
