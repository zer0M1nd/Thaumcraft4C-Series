package tc4revival.content;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraftforge.registries.*;
import tc4revival.config.TCConfig;

/** Registered starts make new-world obelisks visible to vanilla /locate structure. */
public  class EldritchRingStructure extends Structure {
    public static final Codec<EldritchRingStructure> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.create(instance->instance.group(
            settingsCodec(instance),Codec.intRange(-1,3).optionalFieldOf("boss",-1).forGetter(value->value.boss)).apply(instance,EldritchRingStructure::new));
    private final int boss;
    private static final DeferredRegister<StructureType<?>> TYPES=DeferredRegister.create(Registries.STRUCTURE_TYPE,"thaumcraft");
    private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,"thaumcraft");
    public static final RegistryObject<StructureType<EldritchRingStructure>> TYPE=TYPES.register("eldritch_obelisk",()->()->CODEC);
    public static final RegistryObject<StructurePieceType> PIECE=PIECES.register("eldritch_obelisk",()->Piece::new);
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus){TYPES.register(bus);PIECES.register(bus);}
    public EldritchRingStructure(StructureSettings settings){this(settings,-1);}
    public EldritchRingStructure(StructureSettings settings,int boss){super(settings);this.boss=boss;}
    @Override public StructureType<?> type(){return TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        if(context.chunkGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource || !TCConfig.SERVER.generates(TCConfig.WorldFeature.STRUCTURES))return Optional.empty();
        int x=context.chunkPos().getMinBlockX()+8,z=context.chunkPos().getMinBlockZ()+8;
        int y=context.chunkGenerator().getFirstFreeHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,context.heightAccessor(),context.randomState())-1;
        if(y<context.chunkGenerator().getSeaLevel() || y+8>=context.heightAccessor().getMaxBuildHeight())return Optional.empty();
        // Keep the entire ring in one chunk so each piece writes only its own decoration region.
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
            int h=context.chunkGenerator().getFirstFreeHeight(x+dx,z+dz,Heightmap.Types.WORLD_SURFACE_WG,context.heightAccessor(),context.randomState())-1;
            if(Math.abs(h-y)>1)return Optional.empty();
            var state=context.chunkGenerator().getBaseColumn(x+dx,z+dz,context.heightAccessor(),context.randomState()).getBlock(h);
            if(!state.getFluidState().isEmpty() || !EldritchRing.soil(state.getBlock()))return Optional.empty();
        }
        var ground=new BlockPos(x,y,z);var random=context.random();
        return Optional.of(new GenerationStub(ground,builder->builder.addPiece(new Piece(ground,11+random.nextInt(6)*2,11+random.nextInt(6)*2,random.nextLong(),boss<0?random.nextInt(4):boss))));
    }
    public static  class Piece extends StructurePiece {
        private final BlockPos ground;
        private final int width,height,boss;
        private final long seed;
        private boolean placed;
        Piece(BlockPos ground,int width,int height,long seed,int boss){
            super(PIECE.get(),0,new BoundingBox(ground.getX()-3,ground.getY()-4,ground.getZ()-3,ground.getX()+3,ground.getY()+7,ground.getZ()+3));
            this.ground=ground;this.width=width;this.height=height;this.seed=seed;this.boss=boss;
        }
        Piece(StructurePieceSerializationContext context,CompoundTag tag){
            super(PIECE.get(),tag);ground=BlockPos.of(tag.getLong("Ground"));width=tag.getInt("Width");height=tag.getInt("Height");seed=tag.getLong("Seed");boss=tag.contains("Boss")?Math.floorMod(tag.getInt("Boss"),4):Math.floorMod((int)seed,4);placed=tag.getBoolean("Placed");
        }
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag){
            tag.putInt("Boss",boss);tag.putLong("Ground",ground.asLong());tag.putInt("Width",width);tag.putInt("Height",height);tag.putLong("Seed",seed);tag.putBoolean("Placed",placed);
        }
        @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
            if(!placed && clip.isInside(ground))placed=EldritchRing.generateStructure(level,ground,width,height,RandomSource.create(seed),boss);
        }
    }
}
