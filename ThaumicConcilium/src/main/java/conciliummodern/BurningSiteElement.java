package conciliummodern;

import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** The original 9-by-9 Witchery village piece, with a serializable modern jigsaw identity. */
public final class BurningSiteElement extends StructurePoolElement {
    public static final Codec<BurningSiteElement> CODEC=RecordCodecBuilder.create(instance->
            instance.group(projectionCodec()).apply(instance,BurningSiteElement::new));
    public BurningSiteElement(StructureTemplatePool.Projection projection) {super(projection);}
    @Override public Vec3i getSize(StructureTemplateManager manager,Rotation rotation) {return new Vec3i(9,6,9);}
    @Override public BoundingBox getBoundingBox(StructureTemplateManager manager,BlockPos pos,Rotation rotation) {
        BlockPos first=at(pos,rotation,0,-1,0),last=at(pos,rotation,8,4,8);
        return BoundingBox.fromCorners(first,last);
    }
    @Override public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(
            StructureTemplateManager manager,BlockPos pos,Rotation rotation,RandomSource random) {
        var tag=new CompoundTag();tag.putString("name","minecraft:building_entrance");
        tag.putString("target","minecraft:empty");tag.putString("pool","minecraft:empty");
        tag.putString("joint","aligned");tag.putString("final_state","minecraft:gravel");
        var state=Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION,FrontAndTop.NORTH_UP).rotate(rotation);
        return List.of(new StructureTemplate.StructureBlockInfo(at(pos,rotation,4,0,0),state,tag));
    }
    @Override public boolean place(StructureTemplateManager manager,WorldGenLevel level,StructureManager structures,
            ChunkGenerator generator,BlockPos origin,BlockPos pivot,Rotation rotation,BoundingBox chunkBox,
            RandomSource random,boolean keepJigsaws) {
        var optional=WitcheryBridge.host();if(optional.isEmpty())return false;
        var host=optional.get();BlockState stockade=host.stockade();var hunterType=host.witchHunter();
        if(stockade==null||hunterType==null)return false;
        // The road connector determines the flat floor height, as for modern rigid village houses.
        for(int x=0;x<=8;x++)for(int z=0;z<=8;z++) {
            for(int y=0;y<=3;y++)set(level,chunkBox,at(origin,rotation,x,y,z),Blocks.AIR.defaultBlockState());
            set(level,chunkBox,at(origin,rotation,x,-1,z),Blocks.DIRT.defaultBlockState());
            BlockState floor=Blocks.GRAVEL.defaultBlockState();
            if(x>=3&&x<=5&&z>=3&&z<=5)floor=switch(random.nextInt(3)) {
                case 0->Blocks.COAL_BLOCK.defaultBlockState();case 1->Blocks.HAY_BLOCK.defaultBlockState();default->Blocks.OAK_LOG.defaultBlockState();
            };
            set(level,chunkBox,at(origin,rotation,x,0,z),floor.rotate(rotation));
            if(x==0||x==8||z==0||z==8) {
                var wall=Blocks.COBBLESTONE_WALL.defaultBlockState()
                        .setValue(WallBlock.NORTH_WALL,z>0&&(x==0||x==8)?WallSide.LOW:WallSide.NONE)
                        .setValue(WallBlock.SOUTH_WALL,z<8&&(x==0||x==8)?WallSide.LOW:WallSide.NONE)
                        .setValue(WallBlock.WEST_WALL,x>0&&(z==0||z==8)?WallSide.LOW:WallSide.NONE)
                        .setValue(WallBlock.EAST_WALL,x<8&&(z==0||z==8)?WallSide.LOW:WallSide.NONE)
                        .setValue(WallBlock.UP,(x==0||x==8)&&(z==0||z==8));
                set(level,chunkBox,at(origin,rotation,x,1,z),wall.rotate(rotation));
            }
        }
        for(int y=1;y<=4;y++)set(level,chunkBox,at(origin,rotation,4,y,4),stockade.rotate(rotation));
        BlockPos spawn=at(origin,rotation,2,1,2);
        // A structure piece is placed once for every intersecting chunk. Only the hunter's own chunk spawns it.
        if(chunkBox.isInside(spawn)&&level.ensureCanWrite(spawn)) {
            var hunter=hunterType.create(level.getLevel());
            if(hunter!=null) {
                hunter.moveTo(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,rotation.rotate(Direction.NORTH).toYRot(),0);
                hunter.setPersistenceRequired();
                net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(hunter,level,level.getCurrentDifficultyAt(spawn),MobSpawnType.STRUCTURE,null,null);
                level.addFreshEntity(hunter);
            }
        }
        return true;
    }
    @Override public StructurePoolElementType<?> getType() {return ConciliumWorldgen.BURNING_SITE.get();}
    private static BlockPos at(BlockPos origin,Rotation rotation,int x,int y,int z) {return origin.offset(new BlockPos(x,y,z).rotate(rotation));}
    private static void set(WorldGenLevel level,BoundingBox bounds,BlockPos pos,BlockState state) {
        if(bounds.isInside(pos)&&level.ensureCanWrite(pos))level.setBlock(pos,state,2);
    }
}
