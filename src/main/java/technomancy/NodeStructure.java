package technomancy;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent;

/** The eight reserved cells are owned by a specific base and never overwrite another block. */
public final class NodeStructure {
    public static List<BlockPos> cells(BlockPos base,Direction facing){
        var result=new ArrayList<BlockPos>();var across=facing.getClockWise();
        for(int y=0;y<3;y++)for(int x=-1;x<=1;x++)if(x!=0||y!=0)result.add(base.relative(across,x).above(y));
        return result;
    }
    public static boolean room(Level level,BlockPos base,Direction facing){
        if(facing.getAxis()==Direction.Axis.Y)return false;
        for(var p:cells(base,facing))if(!level.hasChunkAt(p)||level.isOutsideBuildHeight(p)||!level.getWorldBorder().isWithinBounds(p)||!level.isEmptyBlock(p))return false;
        return true;
    }
    public static boolean belongs(BlockState state,BlockPos pos,BlockPos base){
        return state.is(TechRegistry.NODE_PART.get())&&NodePartBlock.base(pos,state).equals(base);
    }
    public static boolean intact(MachineEntity m){
        for(var p:cells(m.getBlockPos(),m.facing()))if(!m.getLevel().hasChunkAt(p)||!belongs(m.getLevel().getBlockState(p),p,m.getBlockPos()))return false;
        return true;
    }
    public static boolean create(MachineEntity m){
        if(!room(m.getLevel(),m.getBlockPos(),m.facing()))return false;
        for(var p:cells(m.getBlockPos(),m.facing()))if(!mayPlace(m,p))return false;
        var across=m.facing().getClockWise();
        for(int y=0;y<3;y++)for(int x=-1;x<=1;x++)if(x!=0||y!=0){
            var p=m.getBlockPos().relative(across,x).above(y);
            var state=TechRegistry.NODE_PART.get().defaultBlockState().setValue(NodePartBlock.FACING,m.facing()).setValue(NodePartBlock.COLUMN,x+1).setValue(NodePartBlock.ROW,y);
            if(!m.getLevel().setBlock(p,state,3)){clear(m.getLevel(),m.getBlockPos(),m.facing());return false;}
        }return true;
    }
    private static boolean mayPlace(MachineEntity m,BlockPos p){
        var snapshot=BlockSnapshot.create(m.server().dimension(),m.server(),p);
        return !MinecraftForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(snapshot,m.server().getBlockState(p.below()),WorldActions.actor(m)));
    }
    public static void clear(Level level,BlockPos base,Direction facing){
        for(var p:cells(base,facing))if(level.hasChunkAt(p)&&belongs(level.getBlockState(p),p,base))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
    }
    public static boolean rotate(MachineEntity m,net.minecraft.world.entity.player.Player player){
        var next=m.facing().getClockWise();var old=m.facing();
        if(!intact(m))return false;
        var before=new LinkedHashMap<BlockPos,BlockState>();
        before.put(m.getBlockPos(),m.getBlockState());
        for(var p:cells(m.getBlockPos(),old))before.put(p,m.server().getBlockState(p));
        // The reserved cells are unbreakable by hand, but moving them must still honor protection.
        for(var entry:before.entrySet())if(!WorldActions.allowed(m,entry.getKey())||
            MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(m.server(),entry.getKey(),entry.getValue(),player)))return false;
        for(var p:cells(m.getBlockPos(),next)){
            if(!WorldActions.allowed(m,p))return false;
            if(!m.server().isEmptyBlock(p)&&!belongs(m.server().getBlockState(p),p,m.getBlockPos()))return false;
            var snapshot=BlockSnapshot.create(m.server().dimension(),m.server(),p);
            if(MinecraftForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(snapshot,m.server().getBlockState(p.below()),player)))return false;
            before.putIfAbsent(p,m.server().getBlockState(p));
        }
        clear(m.server(),m.getBlockPos(),old);
        boolean success=m.server().setBlock(m.getBlockPos(),m.getBlockState().setValue(MachineBlock.FACING,next),3);
        var across=next.getClockWise();
        for(int y=0;y<3&&success;y++)for(int x=-1;x<=1&&success;x++)if(x!=0||y!=0){
            var p=m.getBlockPos().relative(across,x).above(y);
            var state=TechRegistry.NODE_PART.get().defaultBlockState().setValue(NodePartBlock.FACING,next).setValue(NodePartBlock.COLUMN,x+1).setValue(NodePartBlock.ROW,y);
            success=m.server().setBlock(p,state,3);
        }
        if(success){m.changed();return true;}
        before.forEach((p,state)->m.server().setBlock(p,state,3));m.changed();return false;
    }
    private NodeStructure(){}
}
