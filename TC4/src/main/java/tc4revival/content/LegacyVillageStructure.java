package tc4revival.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

/** Retain modern roads, then add bounded legacy village components at free road edges. */
public  class LegacyVillageStructure extends Structure {
    public static final Codec<LegacyVillageStructure> CODEC=JigsawStructure.CODEC.fieldOf("delegate").xmap(LegacyVillageStructure::new,s->s.delegate).codec();
    private final JigsawStructure delegate;
    public LegacyVillageStructure(JigsawStructure delegate) {
        super(new StructureSettings(delegate.biomes(),delegate.spawnOverrides(),delegate.step(),delegate.terrainAdaptation())); this.delegate=delegate;
    }
    @Override public StructureType<?> type() { return LegacyVillageStructures.TYPE.get(); }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return delegate.findGenerationPoint(context).map(stub->new GenerationStub(stub.position(),builder->{
            var existing=stub.getPiecesBuilder().build().pieces(); existing.forEach(builder::addPiece);
            thaumcraft.api.worldgen.VillageAdditions.generate(context,builder);
            var random=context.random(); boolean wizard=random.nextBoolean(),banker=random.nextBoolean();
            if (!wizard&&!banker) return;
            record Site(BlockPos entrance,Direction face) {}
            var sites=new ArrayList<Site>();
            for (var piece:existing) {
                if (!(piece instanceof PoolElementStructurePiece pool)) continue;
                var encoded=net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE,context.registryAccess()),pool.getElement()).result();
                if (encoded.isEmpty() || !encoded.get().toString().contains("/streets/")) continue;
                var bounds=piece.getBoundingBox();
                for (int x=bounds.minX()+2;x<=bounds.maxX()-2;x+=4) {
                    sites.add(new Site(new BlockPos(x,0,bounds.minZ()-1),Direction.SOUTH));
                    sites.add(new Site(new BlockPos(x,0,bounds.maxZ()+1),Direction.NORTH));
                }
                for (int z=bounds.minZ()+2;z<=bounds.maxZ()-2;z+=4) {
                    sites.add(new Site(new BlockPos(bounds.minX()-1,0,z),Direction.EAST));
                    sites.add(new Site(new BlockPos(bounds.maxX()+1,0,z),Direction.WEST));
                }
            }
            for (int i=sites.size()-1;i>0;i--) Collections.swap(sites,i,random.nextInt(i+1));
            for (var site:sites) {
                if (!wizard&&!banker) break;
                for (int attempt=0;attempt<5;attempt++) {
                    boolean tower=wizard&&(!banker||random.nextInt(40)<15);
                    var role=tower?LegacyVillagerTrades.Role.WIZARD:LegacyVillagerTrades.Role.BANKER;
                    var preview=LegacyVillagePiece.atEntrance(role,site.entrance().relative(site.face().getOpposite(),tower?1:0),site.face(),random);
                    preview.setDesert(delegate.biomes().stream().anyMatch(h->h.is(net.minecraft.world.level.biome.Biomes.DESERT)));
                    var collision=preview.collisionBounds();
                    long height=0; int count=0;
                    for (int x=collision.minX();x<=collision.maxX();x++) for(int z=collision.minZ();z<=collision.maxZ();z++) {
                        height+=Math.max(context.chunkGenerator().getSeaLevel(),context.chunkGenerator().getFirstFreeHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,context.heightAccessor(),context.randomState()));count++;
                    }
                    int floor=(int)(height/count);
                    if (floor+12>=context.heightAccessor().getMaxBuildHeight()) continue;
                    preview.move(0,floor,0);
                    if (builder.findCollisionPiece(preview.collisionBounds())!=null) continue;
                    builder.addPiece(preview);
                    if (tower) wizard=false; else banker=false;
                    break;
                }
            }
        }));
    }
}
