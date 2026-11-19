package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class Issues0924ServerTests {
    @GameTest(template="wand_test_empty")
    public static void voidShellCreativeSelectionAndBreaking(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:eldritch_void"));
        test.assertTrue(block instanceof EldritchVoidBlock,"Registered outer void shell");
        level.setBlockAndUpdate(pos,block.defaultBlockState());
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setGameMode(GameType.SURVIVAL);
        var state=level.getBlockState(pos);
        test.assertTrue(state.getShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(player)).isEmpty(),"Survival cannot target shell");
        test.assertTrue(state.getDestroySpeed(level,pos)<0,"Shell remains unbreakable in survival");
        player.setGameMode(GameType.CREATIVE);
        test.assertTrue(!state.getShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.of(player)).isEmpty(),"Creative ray can hit shell");
        test.assertTrue(player.gameMode.destroyBlock(pos)&&level.isEmptyBlock(pos),"Real creative mining removes shell");
        player.setGameMode(GameType.SURVIVAL);test.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=200)
    public static void oceanNodesUseSurfaceAndIndependentUnderwaterRolls(GameTestHelper test) {
        var level=test.getLevel();var chunk=new ChunkPos(test.absolutePos(BlockPos.ZERO));
        int x=chunk.getMinBlockX(),z=chunk.getMinBlockZ(),sea=level.getSeaLevel();
        var config=tc4revival.config.TCConfig.SERVER;int rarity=config.nodeRarity.get();boolean branch=config.branchFeatures.get();
        var ocean=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.OCEAN);
        // BiomeManager samples neighbouring quart cells at chunk edges.
        for(int cx=chunk.x-1;cx<=chunk.x+1;cx++)for(int cz=chunk.z-1;cz<=chunk.z+1;cz++)
            level.getChunk(cx,cz).fillBiomesFromNoise((qx,qy,qz,sampler)->ocean,level.getChunkSource().randomState().sampler());
        // Remove the elevated test-template floor too: it must not become ocean floor.
        for(int y=5;y<level.getMaxBuildHeight();y++)level.setBlock(new BlockPos(x,y,z),y<sea-10?Blocks.STONE.defaultBlockState():y<sea?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        test.assertTrue(level.getBiome(new BlockPos(x,sea,z)).is(net.minecraft.tags.BiomeTags.IS_OCEAN),"Fixture is ocean even after biome smoothing");
        test.assertTrue(level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,x,z)==sea-10,"Fixture ocean floor has no floating test platform");
        try {
            config.nodeRarity.set(36);config.branchFeatures.set(true);
            var surface=new Rolls(0,0);
            test.assertTrue(NaturalAuraStructures.wildNode(level,chunk,surface,false),"Surface roll succeeds");
            test.assertTrue(level.getBlockEntity(new BlockPos(x,sea,z)) instanceof AuraNodeBlockEntity,"Ocean first node is above water, despite modern seabed caves");
            test.assertTrue(surface.rolls==1,"Successful surface node skips underwater roll entirely");
            level.setBlockAndUpdate(new BlockPos(x,sea,z),Blocks.AIR.defaultBlockState());
            var underwater=new Rolls(1,0);
            test.assertTrue(NaturalAuraStructures.wildNode(level,chunk,underwater,false)&&underwater.rolls==2,"Failed surface roll gets independent same-rarity roll");
            var below=new BlockPos(x,sea-10,z);
            test.assertTrue(level.getBlockEntity(below) instanceof AuraNodeBlockEntity&&level.getBlockState(below).getValue(AuraNodeBlock.WATERLOGGED),"Extra node is really underwater and waterlogged");
            test.assertTrue(level.isEmptyBlock(new BlockPos(x,sea,z)),"Underwater branch does not also create surface node");
            level.setBlockAndUpdate(below,Blocks.WATER.defaultBlockState());
            test.assertTrue(!NaturalAuraStructures.wildNode(level,chunk,new Rolls(1,1),false),"Both failed rolls produce no node");
            config.branchFeatures.set(false);
            test.assertTrue(!NaturalAuraStructures.wildNode(level,chunk,new Rolls(1,0),false),"Disabling modern additions suppresses extra nodes");
            test.assertTrue(!NaturalAuraStructures.wildNode(level,chunk,new Rolls(0,0),true),"Existing temple aura suppresses both attempts");
        } finally {config.nodeRarity.set(rarity);config.branchFeatures.set(branch);}
        test.succeed();
    }
    private static class Rolls extends LegacyRandomSource {
        int rolls;private final int first,second;
        Rolls(int first,int second){super(942);this.first=first;this.second=second;}
        @Override public int nextInt(int bound){if(bound==36)return rolls++==0?first:second;return 0;}
    }
}
