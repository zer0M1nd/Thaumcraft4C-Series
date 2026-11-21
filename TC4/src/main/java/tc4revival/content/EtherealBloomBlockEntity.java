package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public  class EtherealBloomBlockEntity extends BlockEntity {
    public int counter,growthCounter;
    public EtherealBloomBlockEntity(BlockPos pos,BlockState state){super(TCArcane.ETHEREAL_BLOOM_ENTITY.get(),pos,state);}
    public void tick(){
        if(level==null)return;if(counter==0)counter=level.random.nextInt(100);counter++;
        if(level instanceof ServerLevel server && counter%20==0){int x=level.random.nextInt(8)-level.random.nextInt(8),z=level.random.nextInt(8)-level.random.nextInt(8);cleanse(server,worldPosition,worldPosition.offset(x,0,z));}
        if(level.isClientSide&&growthCounter==0)level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,TCSounds.ROOTS.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,.6f,false);
        growthCounter++;
    }
    public static boolean affected(net.minecraft.core.Holder<Biome> biome){
        return biome.is(TCBiomes.TAINTED_LAND)||biome.is(TCBiomes.MAGICAL_FOREST)||biome.unwrapKey().map(key->key.location().toString().equals("thaumcraft:eerie")).orElse(false);
    }
    public static net.minecraft.resources.ResourceKey<Biome> restoration(net.minecraft.core.Holder<Biome> original){return original.is(TCBiomes.TAINTED_LAND)?Biomes.PLAINS:original.unwrapKey().orElse(Biomes.PLAINS);}
    public static net.minecraft.core.Holder<Biome> generatedBiome(ServerLevel level,BlockPos pos){
        return level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(pos.getX()),QuartPos.fromBlock(pos.getY()),QuartPos.fromBlock(pos.getZ()),level.getChunkSource().randomState().sampler());
    }
    public static net.minecraft.core.Holder<Biome> storedBiome(ServerLevel level,BlockPos pos){return level.getBiomeManager().getNoiseBiomeAtPosition(pos);}
    public static boolean cleanse(ServerLevel level,BlockPos origin,BlockPos target){
        long x=(long)target.getX()-origin.getX(),z=(long)target.getZ()-origin.getZ();
        if(target.getY()!=origin.getY()||x*x+z*z+.25>81||!level.hasChunkAt(target)||!affected(storedBiome(level,target)))return false;
        return WorldBiomeChanges.changeColumn(level,target,restoration(generatedBiome(level,target)));
    }
}
