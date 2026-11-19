package gadomancymodern.maze;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.EldritchMazeSavedData;

/** Dimension-owned instances. No static player, server, or level references survive unload. */
public class MazeSessionsData extends SavedData {
    public EldritchMazeSavedData mazes=new EldritchMazeSavedData();
    public final Map<UUID,Session> sessions=new LinkedHashMap<>();
    public int nextCenter=29;
    public static MazeSessionsData get(ServerLevel level){if(!level.dimension().equals(MazeSessions.DIMENSION))throw new IllegalArgumentException("Not Gadomancy outer lands");return level.getDataStorage().computeIfAbsent(MazeSessionsData::load,MazeSessionsData::new,"gadomancy_labyrinth_sessions");}
    @Override public boolean isDirty(){return super.isDirty()||mazes.isDirty();}
    @Override public CompoundTag save(CompoundTag tag){tag.put("Mazes",mazes.save(new CompoundTag()));tag.putInt("NextCenter",nextCenter);var rows=new ListTag();for(var s:sessions.values())rows.add(s.save());tag.put("Sessions",rows);return tag;}
    public static MazeSessionsData load(CompoundTag tag){var data=new MazeSessionsData();data.mazes=EldritchMazeSavedData.load(tag.getCompound("Mazes"));data.nextCenter=Math.max(29,tag.getInt("NextCenter"));for(var raw:tag.getList("Sessions",Tag.TAG_COMPOUND)){var t=(CompoundTag)raw;try{var s=Session.load(t);if(data.mazes.plan(s.centerX,s.centerZ).isPresent())data.sessions.put(s.owner,s);}catch(RuntimeException e){com.mojang.logging.LogUtils.getLogger().error("Skipping malformed Gadomancy maze session",e);}}return data;}
    public static class Session {
        public UUID owner;public ResourceKey<Level> origin;public Vec3 originPos;public BlockPos sourcePortal;
        public int centerX,centerZ,buildIndex;public boolean entered,closed,bossSeen,exitOpened;
        public final Map<UUID,BlockPos> bosses=new LinkedHashMap<>();
        public CompoundTag save(){var t=new CompoundTag();t.putUUID("Owner",owner);t.putString("Origin",origin.location().toString());t.putDouble("X",originPos.x);t.putDouble("Y",originPos.y);t.putDouble("Z",originPos.z);t.putLong("Source",sourcePortal.asLong());t.putInt("CenterX",centerX);t.putInt("CenterZ",centerZ);t.putInt("BuildIndex",buildIndex);t.putBoolean("Entered",entered);t.putBoolean("Closed",closed);t.putBoolean("BossSeen",bossSeen);t.putBoolean("ExitOpened",exitOpened);var list=new ListTag();bosses.forEach((id,pos)->{var row=new CompoundTag();row.putUUID("Id",id);row.putLong("Pos",pos.asLong());list.add(row);});t.put("Bosses",list);return t;}
        public static Session load(CompoundTag t){var s=new Session();s.owner=t.getUUID("Owner");s.origin=ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,ResourceLocation.parse(t.getString("Origin")));s.originPos=new Vec3(t.getDouble("X"),t.getDouble("Y"),t.getDouble("Z"));if(!Double.isFinite(s.originPos.lengthSqr()))throw new IllegalArgumentException("Invalid return position");s.sourcePortal=BlockPos.of(t.getLong("Source"));s.centerX=t.getInt("CenterX");s.centerZ=t.getInt("CenterZ");s.buildIndex=Math.max(0,t.getInt("BuildIndex"));s.entered=t.getBoolean("Entered");s.closed=t.getBoolean("Closed");s.bossSeen=t.getBoolean("BossSeen");s.exitOpened=t.getBoolean("ExitOpened");for(var raw:t.getList("Bosses",Tag.TAG_COMPOUND)){var row=(CompoundTag)raw;s.bosses.put(row.getUUID("Id"),BlockPos.of(row.getLong("Pos")));}return s;}
    }
}
