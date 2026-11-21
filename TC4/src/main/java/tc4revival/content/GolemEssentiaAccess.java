package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.api.aspects.EssentiaStorageApi;
import thaumcraft.api.aspects.IEssentiaStorage;

/** Physical workers may use a network machine directly without exporting it to a storage bus. */
public final class GolemEssentiaAccess {
    private GolemEssentiaAccess() {}
    public static IEssentiaStorage at(ServerLevel level,BlockPos pos,Direction face,boolean input) {
        if(!level.hasChunkAt(pos))return null;
        var tile=level.getBlockEntity(pos);
        var explicit=thaumcraft.api.golems.GolemEssentiaProviders.find(tile,face);
        if(explicit!=null)return explicit;
        if(tile instanceof EssentiaTransport transport && !(input?transport.canInput(face):transport.canOutput(face)))return null;
        // Network machines deliberately return an empty storage-bus adapter to prevent cycles.
        // A golem carries a finite amount; invoking their own public inventory is not re-export.
        if(tile instanceof IEssentiaStorage storage)return storage;
        return EssentiaStorageApi.supports(tile,face)?EssentiaStorageApi.at(level,pos,face):null;
    }
}
