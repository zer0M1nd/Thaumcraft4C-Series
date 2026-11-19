package thmodern.plane;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.registries.ForgeRegistries;
/** Five-by-five ring, with its keystone slot at the top midpoint. */
public final class GatewayLayout {
 public record Cell(int across,int down,String material){public BlockPos at(BlockPos slot,boolean xAligned){return slot.offset(xAligned?across:0,-down,xAligned?0:across);}}
 private GatewayLayout(){}
 public static List<Cell> cells(){var result=new ArrayList<Cell>();for(int across=-2;across<=2;across++)for(int down=0;down<=4;down++){
  if(across==0&&down==0)continue;boolean border=Math.abs(across)==2||down==0||down==4;boolean corner=Math.abs(across)==2&&(down==0||down==4);
  result.add(new Cell(across,down,!border?"minecraft:air":corner?"thaumcraft:advanced_alchemical_construct":"thaumcraft:eldritch_stone"));
 }return List.copyOf(result);}
 public static boolean matches(ServerLevel level,BlockPos slot,boolean xAligned){for(var cell:cells()){var at=cell.at(slot,xAligned);if(!level.hasChunkAt(at)||level.isOutsideBuildHeight(at))return false;var expected=ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(cell.material));if(expected==null||!level.getBlockState(at).is(expected))return false;}return true;}
 /** Slot order retains legacy A south, B north, C east, D west. */
 public static Direction outward(int side){return switch(side){case 0->Direction.SOUTH;case 1->Direction.NORTH;case 2->Direction.EAST;case 3->Direction.WEST;default->throw new IllegalArgumentException("Invalid gateway side");};}
 public static List<BlockPos> mouth(PocketPlanes.Plane plane,int side){var direction=outward(side);var center=plane.center().relative(direction,plane.parameters().radius());var result=new ArrayList<BlockPos>();for(int across=-1;across<=1;across++)for(int y=-2;y<=0;y++)result.add(center.offset(direction.getAxis()==Direction.Axis.Z?across:0,y,direction.getAxis()==Direction.Axis.X?across:0));return List.copyOf(result);}
}
