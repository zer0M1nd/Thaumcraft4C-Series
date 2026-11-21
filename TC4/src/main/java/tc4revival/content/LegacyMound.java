package tc4revival.content;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
public  class LegacyMound {
    public record Write(BlockPos offset,BlockState state,int flags) {}
    private static final List<Write> PLAN=load();
    private LegacyMound() {}
    public static List<Write> plan() { return PLAN; }
    private static List<Write> load() {
        try(var input=LegacyMound.class.getResourceAsStream("/data/thaumcraft/legacy_structures/mound.csv")) {
            var result=new ArrayList<Write>();
            for(String line:new String(java.util.Objects.requireNonNull(input).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                if(line.isBlank())continue;var p=line.strip().split(",");int metadata=Integer.parseInt(p[4]);
                Block block=switch(p[3]) {case "air"->Blocks.AIR;case "cobblestone"->Blocks.COBBLESTONE;case "mossy_cobblestone"->Blocks.MOSSY_COBBLESTONE;case "dirt"->Blocks.DIRT;case "grass"->Blocks.GRASS_BLOCK;case "tallgrass"->Blocks.GRASS;case "stone_stairs"->Blocks.COBBLESTONE_STAIRS;case "ladder"->Blocks.LADDER;case "iron_bars"->Blocks.IRON_BARS;case "stonebrick"->metadata==3?Blocks.CHISELED_STONE_BRICKS:Blocks.STONE_BRICKS;default->throw new IllegalArgumentException(p[3]);};
                var state=block.defaultBlockState();
                if(block instanceof StairBlock)state=state.setValue(StairBlock.FACING,switch(metadata&3){case 0->Direction.EAST;case 1->Direction.WEST;case 2->Direction.SOUTH;default->Direction.NORTH;}).setValue(StairBlock.HALF,(metadata&4)!=0?net.minecraft.world.level.block.state.properties.Half.TOP:net.minecraft.world.level.block.state.properties.Half.BOTTOM);
                if(block==Blocks.LADDER)state=state.setValue(LadderBlock.FACING,Direction.from3DDataValue(metadata));
                result.add(new Write(new BlockPos(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2])),state,Integer.parseInt(p[5])));
            }
            return List.copyOf(result);
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
    public static boolean generate(ServerLevel level,BlockPos origin,RandomSource random) {
        if(origin.getY()-1<level.getMinBuildHeight()||origin.getY()+18>=level.getMaxBuildHeight())return false;
        for(int[] p:new int[][]{{9,9},{0,0},{18,0},{18,18},{0,18}})if(!LegacySurfaceRuins.valid(level,origin.offset(p[0],9,p[1])))return false;
        generateStructure(level,origin,random,new net.minecraft.world.level.levelgen.structure.BoundingBox(origin.getX(),level.getMinBuildHeight(),origin.getZ(),origin.getX()+18,level.getMaxBuildHeight()-1,origin.getZ()+18));return true;
    }
    public static void generateStructure(net.minecraft.world.level.WorldGenLevel level,BlockPos origin,RandomSource random,net.minecraft.world.level.levelgen.structure.BoundingBox clip) {
        for(var write:PLAN) { var target=origin.offset(write.offset());if(clip.isInside(target))level.setBlock(target,write.state(),write.flags()); }
        // Legacy metadata did not store pane connections. Resolve them after all
        // room walls exist, and let worldgen finish edges spanning chunk clips.
        for(var write:PLAN) {
            var target=origin.offset(write.offset());
            if(!clip.isInside(target) || !write.state().is(Blocks.IRON_BARS))continue;
            var state=level.getBlockState(target);
            if(!state.is(Blocks.IRON_BARS))continue;
            level.setBlock(target,Block.updateFromNeighbourShapes(state,level,target),2);
            level.getChunk(target).markPosForPostprocessing(target);
        }
        for(int z:new int[]{7,11}) {
            float roll=random.nextFloat();int rarity=roll<.1f?2:roll<.33f?1:0;boolean crate=random.nextFloat()<.3f;var target=origin.offset(9,1,z);
            if(clip.isInside(target))level.setBlock(target,TCEldritch.loot(crate,rarity).defaultBlockState(),3);
        }
        var chest=origin.offset(10,1,9);boolean trapped=random.nextInt(3)==0;long chestSeed=random.nextLong();boolean extra=random.nextInt(5)==0;
        if(clip.isInside(chest)) {
            level.setBlock(chest,(trapped?Blocks.TRAPPED_CHEST:Blocks.CHEST).defaultBlockState(),3);
            if(level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity inventory)
                inventory.setLootTable(net.minecraft.resources.ResourceLocation.parse("thaumcraft:chests/"+(extra?"dungeon_double":"dungeon")),chestSeed);
        }
        var trap=origin.offset(10,-1,9);if(trapped&&clip.isInside(trap))level.setBlock(trap,Blocks.TNT.defaultBlockState(),3);
        for(int z:new int[]{4,14}) { var pos=origin.offset(4,5,z);if(clip.isInside(pos))LegacySurfaceRuins.spawner(level,pos,z==4?net.minecraft.world.entity.EntityType.SKELETON:net.minecraft.world.entity.EntityType.ZOMBIE,random); }
    }
}
