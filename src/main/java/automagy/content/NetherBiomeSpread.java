package automagy.content;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.Level;
/** Modern biome columns are 4x4 quart cells; update loaded chunks and explicitly notify tracking clients. */
final class NetherBiomeSpread {
 static void apply(ServerLevel level,BlockPos origin,boolean creature){if(level.dimension().equals(Level.NETHER)||level.random.nextInt(100)>=25)return;
  if(creature){var p=origin.offset(level.random.nextInt(8)-level.random.nextInt(8),0,level.random.nextInt(8)-level.random.nextInt(8));if(level.hasChunkAt(p)&&!level.getBiome(p).is(Biomes.NETHER_WASTES))column(level,p,Biomes.NETHER_WASTES);}
  var p=origin.offset(level.random.nextInt(12)-level.random.nextInt(12),0,level.random.nextInt(12)-level.random.nextInt(12));if(!level.hasChunkAt(p))return;var current=level.getBiome(p);if(current.is(tc4revival.content.TCBiomes.EERIE)||creature&&current.is(Biomes.NETHER_WASTES))return;column(level,p,tc4revival.content.TCBiomes.EERIE);
 }
 private static void column(ServerLevel level,BlockPos pos,ResourceKey<Biome> key){var holder=level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(key);if(holder.isEmpty())return;var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);if(chunk==null)return;int qx=QuartPos.fromBlock(pos.getX()),qz=QuartPos.fromBlock(pos.getZ());chunk.fillBiomesFromNoise((x,y,z,sampler)->x==qx&&z==qz?holder.get():chunk.getNoiseBiome(x,y,z),level.getChunkSource().randomState().sampler());chunk.setUnsaved(true);var packet=net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket.forChunks(java.util.List.of(chunk));for(var player:level.getChunkSource().chunkMap.getPlayers(chunk.getPos(),false))player.connection.send(packet);}
 private NetherBiomeSpread(){}
}
