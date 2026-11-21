package tc4revival.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraftforge.registries.*;
import tc4revival.config.TCConfig;

/** Real starts and bounded pieces give vanilla locate reliable, persistent destinations. */
public  class SurfaceRuinStructure extends Structure {
    public enum Kind { DUNGEON, HILLTOP, TOTEM }
    public static final Codec<SurfaceRuinStructure> CODEC=RecordCodecBuilder.create(instance->instance.group(
            settingsCodec(instance),Codec.STRING.fieldOf("kind").forGetter(value->value.kind.name().toLowerCase(java.util.Locale.ROOT)))
            .apply(instance,(settings,kind)->new SurfaceRuinStructure(settings,Kind.valueOf(kind.toUpperCase(java.util.Locale.ROOT)))));
    private static final DeferredRegister<StructureType<?>> TYPES=DeferredRegister.create(Registries.STRUCTURE_TYPE,"thaumcraft");
    private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,"thaumcraft");
    public static final RegistryObject<StructureType<SurfaceRuinStructure>> TYPE=TYPES.register("surface_ruin",()->()->CODEC);
    public static final RegistryObject<StructurePieceType> PIECE=PIECES.register("surface_ruin",()->Piece::new);
    private final Kind kind;
    public SurfaceRuinStructure(StructureSettings settings,Kind kind) { super(settings);this.kind=kind; }
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) { TYPES.register(bus);PIECES.register(bus); }
    public StructureType<?> type() { return TYPE.get(); }
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if(context.chunkGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource||!TCConfig.SERVER.generates(TCConfig.WorldFeature.STRUCTURES))return Optional.empty();
        if(kind==Kind.TOTEM && context.random().nextDouble()>=Math.min(1,9d/(TCConfig.SERVER.nodeRarity.get()*10d)))return Optional.empty();
        int x=context.chunkPos().getMinBlockX()+8,z=context.chunkPos().getMinBlockZ()+8;
        int groundY=height(context,x,z);
        if(groundY<context.chunkGenerator().getSeaLevel()||groundY+10>=context.heightAccessor().getMaxBuildHeight()||groundY-10<context.heightAccessor().getMinBuildHeight())return Optional.empty();
        if(kind==Kind.HILLTOP&&groundY<85)return Optional.empty();
        int radius=kind==Kind.DUNGEON?9:kind==Kind.HILLTOP?3:0;
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
            int h=height(context,x+dx,z+dz);if(Math.abs(h-groundY)>1)return Optional.empty();
            var state=context.chunkGenerator().getBaseColumn(x+dx,z+dz,context.heightAccessor(),context.randomState()).getBlock(h);
            if(!state.getFluidState().isEmpty()||!soil(state,kind))return Optional.empty();
        }
        var ground=new BlockPos(x,groundY,z);long seed=context.random().nextLong();
        return Optional.of(new GenerationStub(ground,builder->builder.addPiece(new Piece(kind,ground,seed))));
    }
    private static int height(GenerationContext context,int x,int z) { return context.chunkGenerator().getFirstFreeHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,context.heightAccessor(),context.randomState())-1; }
    private static boolean soil(net.minecraft.world.level.block.state.BlockState state,Kind kind) {
        return state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)||state.is(net.minecraft.world.level.block.Blocks.DIRT)||state.is(net.minecraft.world.level.block.Blocks.STONE)||kind==Kind.TOTEM&&state.is(net.minecraft.world.level.block.Blocks.SAND);
    }
    public static  class Piece extends StructurePiece {
        private final Kind kind;private final BlockPos ground;private final long seed;
        Piece(Kind kind,BlockPos ground,long seed) {
            super(PIECE.get(),0,new BoundingBox(ground.getX()-(kind==Kind.DUNGEON?9:3),ground.getY()-10,ground.getZ()-(kind==Kind.DUNGEON?9:3),ground.getX()+(kind==Kind.DUNGEON?9:3),ground.getY()+9,ground.getZ()+(kind==Kind.DUNGEON?9:3)));
            this.kind=kind;this.ground=ground;this.seed=seed;
        }
        Piece(StructurePieceSerializationContext context,CompoundTag tag) { super(PIECE.get(),tag);kind=Kind.valueOf(tag.getString("Kind"));ground=BlockPos.of(tag.getLong("Ground"));seed=tag.getLong("Seed"); }
        protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) { tag.putString("Kind",kind.name());tag.putLong("Ground",ground.asLong());tag.putLong("Seed",seed); }
        public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource ignored,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
            var random=RandomSource.create(seed);
            switch(kind) {
                case DUNGEON -> {
                    var origin=ground.offset(-9,-9,-9);LegacyMound.generateStructure(level,origin,random,clip);
                    var node=ground.below();if(clip.isInside(node))AuraNodeGeneration.createRandom(level,node,RandomSource.create(seed^0x517cc1b7L),false,true,false);
                }
                case HILLTOP -> { if(clip.isInside(ground)&&HilltopStones.generate(level,ground,random))AuraNodeGeneration.createRandom(level,ground.above(5),random,false,true,false); }
                case TOTEM -> { if(clip.isInside(ground))ObsidianTotemFeature.generate(level,ground,random); }
            }
        }
    }
}
