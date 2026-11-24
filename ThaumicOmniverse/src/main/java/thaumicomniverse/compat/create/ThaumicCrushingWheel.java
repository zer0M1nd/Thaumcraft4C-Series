package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.kinetics.crusher.*;
import com.simibubi.create.content.processing.recipe.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.content.TCItems;

/** Native wheel transport and damage, with an isolated pair and aspect processing. */
public class ThaumicCrushingWheel extends CrushingWheelBlock {
    public ThaumicCrushingWheel(){super(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2).requiresCorrectToolForDrops().noOcclusion());}
    @Override public BlockEntityType<? extends CrushingWheelBlockEntity> getBlockEntityType(){return ArcaneCreateContent.CRUSHING_WHEEL_TILE.get();}
    @Override public void updateControllers(BlockState state,Level level,BlockPos pos,Direction side){
        if(level==null||level.isClientSide||side.getAxis()==state.getValue(AXIS))return;
        var at=pos.relative(side);var other=level.getBlockState(pos.relative(side,2));
        var previous=level.getBlockState(at);boolean exists=previous.is(ArcaneCreateContent.CRUSHING_CONTROLLER.get());
        if(!other.is(this)||other.getValue(AXIS)!=state.getValue(AXIS)){
            if(exists)level.removeBlock(at,false);return;
        }
        if(!exists&&!previous.canBeReplaced())return;
        var first=getBlockEntity(level,pos);var second=getBlockEntity(level,pos.relative(side,2));
        boolean valid=ArcaneContraptions.enabled()&&first!=null&&second!=null&&first.getSpeed()!=0&&second.getSpeed()!=0
            &&(first.getSpeed()>0)!=(second.getSpeed()>0);
        var facing=Direction.DOWN;
        if(valid){
            var axis=Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(state.getValue(AXIS),Direction.AxisDirection.POSITIVE).getNormal());
            var normal=axis.cross(Vec3.atLowerCornerOf(side.getNormal())).scale(Math.signum(first.getSpeed()));
            facing=Direction.getNearest(normal.x,normal.y,normal.z);
        }
        var next=ArcaneCreateContent.CRUSHING_CONTROLLER.get().defaultBlockState()
            .setValue(CrushingWheelControllerBlock.VALID,valid).setValue(CrushingWheelControllerBlock.FACING,facing);
        if(previous!=next)level.setBlockAndUpdate(at,next);
        ArcaneCreateContent.CRUSHING_CONTROLLER.get().updateSpeed(next,level,at);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()))for(var side:Direction.values())
            if(level.getBlockState(pos.relative(side)).is(ArcaneCreateContent.CRUSHING_CONTROLLER.get()))level.removeBlock(pos.relative(side),moving);
        super.onRemove(state,level,pos,next,moving);
    }
    public static class WheelTile extends CrushingWheelBlockEntity {
        public WheelTile(BlockPos pos,BlockState state){super(ArcaneCreateContent.CRUSHING_WHEEL_TILE.get(),pos,state);}
        @Override public float calculateStressApplied(){return lastStressApplied=8;}
    }
    public static class Controller extends CrushingWheelControllerBlock {
        public Controller(){super(BlockBehaviour.Properties.of().noLootTable().noOcclusion().strength(-1,3600000));}
        @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,
                net.minecraft.world.phys.shapes.CollisionContext context){return net.minecraft.world.phys.shapes.Shapes.empty();}
        @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
        @Override public BlockEntityType<? extends CrushingWheelControllerBlockEntity> getBlockEntityType(){return ArcaneCreateContent.CRUSHING_CONTROLLER_TILE.get();}
        @Override public void updateSpeed(BlockState state,LevelAccessor level,BlockPos pos){
            if(!(level.getBlockEntity(pos) instanceof ControllerTile tile))return;
            float speed=0;
            if(state.getValue(VALID)&&ArcaneContraptions.enabled())for(var side:Direction.values()){
                var neighbour=level.getBlockState(pos.relative(side));
                if(neighbour.is(ArcaneCreateContent.CRUSHING_WHEEL.get())&&neighbour.getValue(AXIS)!=side.getAxis()
                    &&level.getBlockEntity(pos.relative(side)) instanceof WheelTile wheel){speed=Math.abs(wheel.getSpeed()/50f);break;}
            }
            if(tile.crushingspeed!=speed){tile.crushingspeed=speed;tile.sendData();}
        }
    }
    public static class ControllerTile extends CrushingWheelControllerBlockEntity {
        private boolean simulating;
        private static long bonus(long amount, net.minecraft.util.RandomSource random) {
            int remainder = (int) (amount % 20);
            return amount / 20 + (remainder > 0 && random.nextInt(20) < remainder ? 1 : 0);
        }
        public void releaseEssentia(tc4revival.aspect.AspectAmounts aspects){
            if(level==null||level.isClientSide)return;
            var direction=Vec3.atLowerCornerOf(getBlockState().getValue(CrushingWheelControllerBlock.FACING).getNormal());
            var position=Vec3.atCenterOf(worldPosition).add(direction.scale(1.05));
            var motion=direction.scale(.1);
            for(var entry:aspects.view().entrySet())for(long left=entry.getValue()+bonus(entry.getValue(),level.random);left>0;){
                int amount=(int)Math.min(left,64);left-=amount;
                var drop=new net.minecraft.world.entity.item.ItemEntity(level,position.x,position.y,position.z,
                    TCItems.ESSENCE_CRYSTAL.get().of(entry.getKey(),amount));
                drop.setDeltaMovement(motion);drop.setDefaultPickUpDelay();
                drop.getPersistentData().put("BypassCrushingWheel",net.minecraft.nbt.NbtUtils.writeBlockPos(worldPosition));
                level.addFreshEntity(drop);
            }
        }
        public ControllerTile(BlockPos pos,BlockState state){
            super(ArcaneCreateContent.CRUSHING_CONTROLLER_TILE.get(),pos,state);
            // Create's ProcessingInventory invokes its callback during simulated insertion.
            // Keep those probes from resetting a running batch or reserving output slots.
            inventory=new ProcessingInventory(stack->{if(!simulating){inventory.remainingTime=100;inventory.appliedRecipe=false;setChanged();sendData();}}){
                @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){
                    simulating=simulate;try{return super.insertItem(slot,stack,simulate);}finally{simulating=false;}
                }
                @Override public boolean isItemValid(int slot,ItemStack stack){return processingEntity==null&&super.isItemValid(slot,stack);}
            };
        }
        @Override public void tick(){
            if(!ArcaneContraptions.enabled())return;
            if(!hasEntity()&&!inventory.isEmpty()&&crushingspeed!=0){
                // Halve AFTER the native clamp, including single items and maximum RPM.
                // The native tick subtracts the full amount immediately afterwards.
                float divisor=inventory.appliedRecipe?1:(float)(Math.log(inventory.getStackInSlot(0).getCount())/Math.log(2));
                inventory.remainingTime+=Mth.clamp(crushingspeed*4/divisor,.25f,20f)*.5f;
            }
            super.tick();
        }
        @Override public Optional<ProcessingRecipe<RecipeWrapper>> findRecipe(){
            var input=inventory.getStackInSlot(0);if(input.isEmpty())return Optional.empty();
            int batchSize=input.getCount();
            var aspects=ItemStackAspects.resolve(input).orElseGet(tc4revival.aspect.AspectAmounts::new);
            var builder=new ProcessingRecipeBuilder<CrushingRecipe>(params->new CrushingRecipe(params){
                private int rolls;
                @Override protected int getMaxOutputCount(){return Integer.MAX_VALUE;}
                @Override public List<ItemStack> rollResults(){
                    var result=new ArrayList<>(super.rollResults());
                    // Create rolls once per input. Round the bonus only once for the whole batch.
                    if (++rolls==batchSize && level!=null && !level.isClientSide) {
                        for (var entry:aspects.view().entrySet()) {
                            long extra=bonus((long)entry.getValue()*batchSize,level.random);
                            while (extra>0) {
                                int count=(int)Math.min(extra,64);
                                result.add(TCItems.ESSENCE_CRYSTAL.get().of(entry.getKey(),count)); extra-=count;
                            }
                        }
                    }
                    return result;
                }
            },ResourceLocation.parse("thaumicomniverse:runtime_essentia_crushing")).duration(100);
            if(level!=null&&!level.isClientSide){
                int needed=1;
                for(var entry:aspects.view().entrySet()){
                    int amount=entry.getValue();if(amount<=0)continue;
                    long base=(long)amount*batchSize;
                    needed=Math.addExact(needed,Math.toIntExact((base+(base+19)/20+63)/64));
                    for(int left=amount;left>0;left-=Math.min(left,64))builder.output(TCItems.ESSENCE_CRYSTAL.get().of(entry.getKey(),Math.min(left,64)));
                }
                // Native crushing has a fixed output inventory and otherwise truncates
                // large batches. Reserve every result before it consumes the input.
                if(needed>inventory.getSlots()){
                    var saved=inventory.serializeNBT();saved.putInt("Size",needed);inventory.deserializeNBT(saved);
                }
            }
            // Ephemeral only: never put these generated conversions into JEI or RecipeManager.
            return Optional.of(builder.build());
        }
    }
}
