package technomancy;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.*;
import net.minecraftforge.event.level.BlockEvent;

/** Machine world edits honor loaded chunks, the world border and Forge protection events. */
public final class WorldActions {
    public static FakePlayer actor(MachineEntity m){
        return actor(m.server(),m.getBlockPos(),m.owner);
    }
    public static FakePlayer actor(net.minecraft.server.level.ServerLevel level,BlockPos pos,UUID owner){
        var player=FakePlayerFactory.get(level,new GameProfile(owner==null?UUID.nameUUIDFromBytes("technomancy".getBytes(java.nio.charset.StandardCharsets.UTF_8)):owner,"[Technomancy]"));
        player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);return player;
    }
    public static boolean allowed(MachineEntity m,BlockPos p){return m.server().hasChunkAt(p)&&m.server().getWorldBorder().isWithinBounds(p)&&!m.server().isOutsideBuildHeight(p);}
    public static boolean mayBreak(MachineEntity m,BlockPos p){
        if(!allowed(m,p))return false;var state=m.server().getBlockState(p);
        return state.getDestroySpeed(m.server(),p)>=0&&!MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(m.server(),p,state,actor(m)));
    }
    public static boolean place(MachineEntity m,BlockPos p,BlockState state){
        if(!allowed(m,p)||!m.server().getBlockState(p).canBeReplaced())return false;
        var snapshot=BlockSnapshot.create(m.server().dimension(),m.server(),p);
        if(MinecraftForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(snapshot,m.server().getBlockState(p.below()),actor(m))))return false;
        return m.server().setBlock(p,state,3);
    }
    public static boolean replace(MachineEntity m,BlockPos p,BlockState state){
        if(!mayBreak(m,p))return false;
        var snapshot=BlockSnapshot.create(m.server().dimension(),m.server(),p);
        if(MinecraftForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(snapshot,m.server().getBlockState(p.below()),actor(m))))return false;
        return m.server().setBlock(p,state,3);
    }
    private WorldActions(){}
}
