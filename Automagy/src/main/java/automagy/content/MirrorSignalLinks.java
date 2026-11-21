package automagy.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Loaded crystals form graph edges, never persisted power sources. */
public final class MirrorSignalLinks {
    private static final Map<ServerLevel,Set<BlockPos>> LOADED=new WeakHashMap<>();
    public static void loaded(RedcrystalBlockEntity tile){if(tile.kind().equals("merc")&&tile.getLevel() instanceof ServerLevel level)LOADED.computeIfAbsent(level,k->new HashSet<>()).add(tile.getBlockPos());}
    public static void removed(RedcrystalBlockEntity tile){if(tile.getLevel() instanceof ServerLevel level){var set=LOADED.get(level);if(set!=null)set.remove(tile.getBlockPos());}}
    public static boolean mirror(BlockEntity tile){return tile instanceof tc4revival.content.ItemMirrorBlockEntity||tile instanceof tc4revival.content.EssentiaMirrorBlockEntity;}
    private static GlobalPos destination(BlockEntity tile){
        if(tile instanceof tc4revival.content.ItemMirrorBlockEntity mirror)return mirror.linked()?mirror.destination():null;
        if(tile instanceof tc4revival.content.EssentiaMirrorBlockEntity mirror)return mirror.linked()?mirror.mirrorDestination():null;
        return null;
    }
    public static List<GlobalPos> contacts(ServerLevel level,RedcrystalBlockEntity tile){
        var local=tile.mirror;boolean valid=local!=null&&local.dimension().equals(level.dimension())&&local.pos().distSqr(tile.getBlockPos())<=64&&level.hasChunkAt(local.pos())&&mirror(level.getBlockEntity(local.pos()));
        if(tile.mirrorValid!=valid){tile.mirrorValid=valid;tile.changed();}
        if(!valid)return List.of();
        var destination=destination(level.getBlockEntity(local.pos()));if(destination==null)return List.of();
        var remote=level.getServer().getLevel(destination.dimension());if(remote==null||!remote.hasChunkAt(destination.pos()))return List.of();
        var far=remote.getBlockEntity(destination.pos());if(!local.equals(destination(far)))return List.of();
        var result=new ArrayList<GlobalPos>();
        for(var position:LOADED.getOrDefault(remote,Set.of()))if(remote.hasChunkAt(position)&&position.distSqr(destination.pos())<=64&&remote.getBlockEntity(position) instanceof RedcrystalBlockEntity other&&destination.equals(other.mirror))
            result.add(GlobalPos.of(remote.dimension(),position));
        result.sort(Comparator.comparingLong(p->p.pos().asLong()));return result;
    }
    public static CompoundTag write(GlobalPos target){var tag=new CompoundTag();tag.putString("Dimension",target.dimension().location().toString());tag.putLong("Position",target.pos().asLong());return tag;}
    public static GlobalPos read(CompoundTag tag){var id=ResourceLocation.tryParse(tag.getString("Dimension"));return id==null||!tag.contains("Position",Tag.TAG_LONG)?null:GlobalPos.of(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id),BlockPos.of(tag.getLong("Position")));}
    private MirrorSignalLinks(){}
}
