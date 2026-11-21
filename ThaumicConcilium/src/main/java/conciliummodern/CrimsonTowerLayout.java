package conciliummodern;

import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;

/** Ordered placement calls translated from CultistTowerWorldGen; regenerate with concilium_tower_layout.py. */
final class CrimsonTowerLayout {
    static void place(WorldGenLevel world,int x,int y,int z) {
        setColumn(x,y,z,4,log(0),world);
        setColumn(x + 1,y,z + 1,4,log(0),world);
        setColumn(x + 3,y,z + 2,4,log(0),world);
        setColumn(x + 5,y,z + 1,4,log(0),world);
        setColumn(x + 6,y,z,4,log(0),world);
        setColumn(x + 7,y,z - 2,4,log(0),world);
        setColumn(x + 6,y,z - 4,4,log(0),world);
        setColumn(x + 5,y,z - 5,4,log(0),world);
        setColumn(x + 3,y,z - 6,4,log(0),world);
        setColumn(x + 1,y,z - 5,4,log(0),world);
        setColumn(x,y,z - 4,4,log(0),world);
        setColumn(x - 1,y,z - 2,4,log(0),world);
        setColumn(x + 2,y,z + 1,3,TCEldritch.ROCK.get().defaultBlockState(),world);
        setColumn(x + 4,y,z + 1,3,TCEldritch.ROCK.get().defaultBlockState(),world);
        setColumn(x + 6,y,z - 1,3,TCEldritch.ROCK.get().defaultBlockState(),world);
        setColumn(x + 6,y,z - 3,3,TCEldritch.ROCK.get().defaultBlockState(),world);
        setColumn(x + 4,y,z - 5,3,TCEldritch.ROCK.get().defaultBlockState(),world);
        setColumn(x + 2,y,z - 5,3,TCEldritch.ROCK.get().defaultBlockState(),world);
        setColumn(x,y,z - 1,3,TCEldritch.ROCK.get().defaultBlockState(),world);
        setColumn(x,y,z - 3,3,TCEldritch.ROCK.get().defaultBlockState(),world);
        setColumn(x + 1,y + 4,z,6,log(0),world);
        setColumn(x + 5,y + 4,z,6,log(0),world);
        setColumn(x + 1,y + 4,z - 4,6,log(0),world);
        setColumn(x + 5,y + 4,z - 4,6,log(0),world);
        setColumn(x + 3,y + 4,z + 1,6,log(0),world);
        setColumn(x + 6,y + 4,z - 2,6,log(0),world);
        setColumn(x,y + 4,z - 2,6,log(0),world);
        setColumn(x + 3,y + 4,z - 5,6,log(0),world);
        setColumn(x + 2,y + 5,z,5,Blocks.RED_TERRACOTTA.defaultBlockState(),world);
        setColumn(x + 4,y + 5,z,5,Blocks.RED_TERRACOTTA.defaultBlockState(),world);
        setColumn(x + 5,y + 5,z - 1,5,Blocks.RED_TERRACOTTA.defaultBlockState(),world);
        setColumn(x + 5,y + 5,z - 3,5,Blocks.RED_TERRACOTTA.defaultBlockState(),world);
        setColumn(x + 2,y + 5,z - 4,5,Blocks.RED_TERRACOTTA.defaultBlockState(),world);
        setColumn(x + 4,y + 5,z - 4,5,Blocks.RED_TERRACOTTA.defaultBlockState(),world);
        setColumn(x + 1,y + 5,z - 1,5,Blocks.RED_TERRACOTTA.defaultBlockState(),world);
        setColumn(x + 1,y + 5,z - 3,5,Blocks.RED_TERRACOTTA.defaultBlockState(),world);
        setRowX(x + 2,y + 3,z + 1,3,log(4),world);
        setRowX(x + 2,y + 3,z - 5,3,log(4),world);
        setRowZ(x,y + 3,z,3,log(8),world);
        setRowZ(x + 6,y + 3,z,3,log(8),world);
        setRowX(x + 2,y + 4,z,3,log(4),world);
        setRowX(x + 2,y + 4,z - 4,3,log(4),world);
        setRowZ(x + 1,y + 4,z,3,log(8),world);
        setRowZ(x + 5,y + 4,z,3,log(8),world);
        set(world,x + 3,y + 6,z + 1,stairs(3));
        set(world,x + 3,y + 7,z + 1,stairs(7));
        set(world,x,y + 6,z - 2,stairs(0));
        set(world,x,y + 7,z - 2,stairs(4));
        set(world,x + 3,y + 6,z - 5,stairs(2));
        set(world,x + 3,y + 7,z - 5,stairs(6));
        set(world,x + 6,y + 6,z - 2,stairs(1));
        set(world,x + 6,y + 7,z - 2,stairs(5));
        set(world,x + 1,y + 4,z + 1,stairs(3));
        set(world,x + 3,y + 4,z + 2,stairs(3));
        set(world,x + 5,y + 4,z + 1,stairs(3));
        set(world,x + 1,y + 4,z - 5,stairs(2));
        set(world,x + 3,y + 4,z - 6,stairs(2));
        set(world,x + 5,y + 4,z - 5,stairs(2));
        set(world,x,y + 4,z,stairs(0));
        set(world,x - 1,y + 4,z - 2,stairs(0));
        set(world,x,y + 4,z - 4,stairs(0));
        set(world,x + 6,y + 4,z,stairs(1));
        set(world,x + 7,y + 4,z - 2,stairs(1));
        set(world,x + 6,y + 4,z - 4,stairs(1));
        setColumn(x + 1,y + 10,z + 1,2,log(0),world);
        setColumn(x,y + 10,z,2,log(0),world);
        setColumn(x + 3,y + 10,z + 2,2,log(0),world);
        setColumn(x + 5,y + 10,z + 1,2,log(0),world);
        setColumn(x + 6,y + 10,z,2,log(0),world);
        setColumn(x + 7,y + 10,z - 2,2,log(0),world);
        setColumn(x + 6,y + 10,z - 4,2,log(0),world);
        setColumn(x + 5,y + 10,z - 5,2,log(0),world);
        setColumn(x + 3,y + 10,z - 6,2,log(0),world);
        setColumn(x + 1,y + 10,z - 5,2,log(0),world);
        setColumn(x,y + 10,z - 4,2,log(0),world);
        setColumn(x - 1,y + 10,z - 2,2,log(0),world);
        set(world,x + 1,y + 9,z + 1,stairs(7));
        set(world,x + 3,y + 9,z + 2,stairs(7));
        set(world,x + 5,y + 9,z + 1,stairs(7));
        set(world,x + 1,y + 9,z - 5,stairs(6));
        set(world,x + 3,y + 9,z - 6,stairs(6));
        set(world,x + 5,y + 9,z - 5,stairs(6));
        set(world,x,y + 9,z,stairs(4));
        set(world,x - 1,y + 9,z - 2,stairs(4));
        set(world,x,y + 9,z - 4,stairs(4));
        set(world,x + 6,y + 9,z,stairs(5));
        set(world,x + 7,y + 9,z - 2,stairs(5));
        set(world,x + 6,y + 9,z - 4,stairs(5));
        setRowZ(x,y + 10,z,3,log(8),world);
        setRowZ(x + 6,y + 10,z,3,log(8),world);
        setRowX(x + 2,y + 10,z + 1,3,log(4),world);
        setRowX(x + 2,y + 10,z - 5,3,log(4),world);
        // Resolve modern stair joins only after every source placement has been applied.
        for(int xx=x-2;xx<=x+8;xx++)for(int yy=y;yy<=y+11;yy++)for(int zz=z-7;zz<=z+3;zz++) {
            var pos=new BlockPos(xx,yy,zz);var block=world.getBlockState(pos);
            if(block.getBlock() instanceof StairBlock)world.setBlock(pos,Block.updateFromNeighbourShapes(block,world,pos),2);
        }
    }
    private static BlockState log(int metadata) {
        return TCArcane.GREATWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                metadata==4?Direction.Axis.X:metadata==8?Direction.Axis.Z:Direction.Axis.Y);
    }
    private static BlockState stairs(int metadata) {
        var direction=switch(metadata&3) {case 0->Direction.EAST;case 1->Direction.WEST;case 2->Direction.SOUTH;default->Direction.NORTH;};
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("thaumcraft","greatwood_stairs")).defaultBlockState()
                .setValue(StairBlock.FACING,direction).setValue(StairBlock.HALF,(metadata&4)==0?Half.BOTTOM:Half.TOP);
    }
    private static void set(WorldGenLevel world,int x,int y,int z,BlockState state) {world.setBlock(new BlockPos(x,y,z),state,2);}
    private static void setColumn(int x,int y,int z,int length,BlockState state,WorldGenLevel world) {
        for(int i=y;i<y+length;i++)set(world,x,i,z,state);
    }
    private static void setRowX(int x,int y,int z,int length,BlockState state,WorldGenLevel world) {
        for(int i=x;i<x+length;i++)set(world,i,y,z,state);
    }
    private static void setRowZ(int x,int y,int z,int length,BlockState state,WorldGenLevel world) {
        for(int i=z-length;i<z;i++)set(world,x,y,i,state);
    }
    private CrimsonTowerLayout() {}
}
