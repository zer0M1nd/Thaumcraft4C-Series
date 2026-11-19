package thaumicomniverse.compat.create;

import com.simibubi.create.content.kinetics.base.*;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import tc4revival.content.VisNetwork;
import tc4revival.aspect.TCAspects;

public class ArcaneMotor extends DirectionalKineticBlock implements IBE<ArcaneMotor.Tile> {
    public ArcaneMotor(){super(Properties.copy(Blocks.IRON_BLOCK).strength(3).noOcclusion());}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return com.simibubi.create.AllShapes.MOTOR_BLOCK.get(state.getValue(FACING));}
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction face){return face==state.getValue(FACING);}
    @Override public Direction.Axis getRotationAxis(BlockState state){return state.getValue(FACING).getAxis();}
    @Override public Class<Tile> getBlockEntityClass(){return Tile.class;}
    @Override public BlockEntityType<? extends Tile> getBlockEntityType(){return ArcaneCreateContent.MOTOR_TILE.get();}
    public static double stress(int fire,int order){return fire<=0?256:Math.min(8,fire)*1024d*(1+Math.max(0,Math.min(8,order))*.25);}
    public static int rpm(int air){return 16<<Math.max(0,Math.min(8,air))/2;}
    public static class Tile extends GeneratingKineticBlockEntity {
        private int fire,order,air;
        public Tile(BlockPos pos,BlockState state){super(ArcaneCreateContent.MOTOR_TILE.get(),pos,state);}
        @Override public void initialize(){super.initialize();if(!level.isClientSide)updateGeneratedRotation();}
        @Override public float getGeneratedSpeed(){return ArcaneContraptions.enabled()?convertToDirection(rpm(air),getBlockState().getValue(FACING)):0;}
        @Override public float calculateAddedStressCapacity(){return lastCapacityProvided=(float)(stress(fire,order)/rpm(air));}
        @Override public float calculateStressApplied(){return 0;}
        @Override public void tick(){
            super.tick();if(level==null||level.isClientSide)return;
            int f=0,o=0,a=0;
            if(ArcaneContraptions.enabled()){
                f=VisNetwork.drain(level,worldPosition,TCAspects.catalog().require("ignis"),8);
                if(f>0)o=VisNetwork.drain(level,worldPosition,TCAspects.catalog().require("ordo"),8);
                a=VisNetwork.drain(level,worldPosition,TCAspects.catalog().require("aer"),8);
            }
            if(f!=fire||o!=order||a!=air||getGeneratedSpeed()!=getSpeed()){fire=f;order=o;air=a;updateGeneratedRotation();setChanged();}
        }
        @Override public void write(CompoundTag tag,boolean client){super.write(tag,client);tag.putInt("FireCV",fire);tag.putInt("OrderCV",order);tag.putInt("AirCV",air);}
        @Override protected void read(CompoundTag tag,boolean client){super.read(tag,client);fire=Math.max(0,Math.min(8,tag.getInt("FireCV")));order=Math.max(0,Math.min(8,tag.getInt("OrderCV")));air=Math.max(0,Math.min(8,tag.getInt("AirCV")));}
    }
}
