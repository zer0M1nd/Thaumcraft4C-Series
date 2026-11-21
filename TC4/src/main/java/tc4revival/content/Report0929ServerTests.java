package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class Report0929ServerTests {
    private static class Roll extends LegacyRandomSource {
        final boolean hit;int rolls;
        Roll(boolean hit){super(929);this.hit=hit;}
        @Override public int nextInt(int bound){if(bound==36){rolls++;return hit?0:1;}return 0;}
    }
    @GameTest(template="wand_test_empty")
    public static void naturalAuraInjectedIntoUntaggedAndEndBiomes(GameTestHelper t) {
        var registry=t.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for(var biome:registry.holders().toList()) {
            long count=biome.value().getGenerationSettings().features().stream().flatMap(set->set.stream())
                    .filter(feature->feature.is(ResourceLocation.parse("thaumcraft:natural_aura"))).count();
            t.assertTrue(count==1,"Exactly one aura dispatcher in biome "+biome.key().location());
        }
        var custom=registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.parse("report929:untagged")));
        t.assertTrue(!custom.is(net.minecraft.tags.BiomeTags.IS_OVERWORLD)&&!custom.is(net.minecraft.tags.BiomeTags.IS_NETHER),"Test biome has no legacy injection tags");
        t.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=400)
    public static void naturalAuraCustomDimensionAndEndOuterIsland(GameTestHelper t) {
        var config=tc4revival.config.TCConfig.SERVER;int previous=config.nodeRarity.get();
        try {
            config.nodeRarity.set(36);
            var custom=t.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse("report929:sky")));
            t.assertTrue(custom!=null,"Custom test dimension loaded");
            checkSurface(t,custom,new ChunkPos(8,8));
            var end=t.getLevel().getServer().getLevel(Level.END);
            checkSurface(t,end,new ChunkPos(80,0));
            t.assertTrue(NaturalAuraStructures.allowsWildNodes(Level.END,new ChunkPos(0,0))
                    &&NaturalAuraStructures.allowsWildNodes(Level.END,new ChunkPos(64,0))
                    &&NaturalAuraStructures.allowsWildNodes(Level.END,new ChunkPos(65,0)),"Refactor requirement enables every dimension, including the central End");
            t.assertTrue(NaturalAuraStructures.allowsWildNodes(TCEldritch.OUTER_LANDS,new ChunkPos(80,0)),"Outer Lands now follow the configurable all-dimension default");
            t.succeed();
        }finally{config.nodeRarity.set(previous);}
    }
    private static void checkSurface(GameTestHelper t,ServerLevel level,ChunkPos chunk) {
        var pos=new BlockPos(chunk.getMinBlockX(),100,chunk.getMinBlockZ());
        for(int cx=chunk.x-1;cx<=chunk.x+1;cx++)for(int cz=chunk.z-1;cz<=chunk.z+1;cz++)level.getChunk(cx,cz);
        // An explicit floating island column, including air below it and no terrain above it.
        for(int y=level.getMinBuildHeight();y<level.getMaxBuildHeight();y++)level.setBlock(new BlockPos(pos.getX(),y,pos.getZ()),y==100?Blocks.END_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        var miss=new Roll(false);t.assertTrue(!NaturalAuraStructures.wildNode(level,chunk,miss,false)&&miss.rolls==1,"One normal-rarity miss, no extra attempts in "+level.dimension().location());
        var hit=new Roll(true);t.assertTrue(NaturalAuraStructures.wildNode(level,chunk,hit,false)&&hit.rolls==1,"One normal-rarity hit in "+level.dimension().location());
        t.assertTrue(level.getBlockEntity(pos.above()) instanceof AuraNodeBlockEntity,"Node is above island terrain, not in void");
        level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        t.assertTrue(!NaturalAuraStructures.wildNode(level,chunk,new Roll(true),false),"Empty void column creates no node");
    }
    @GameTest(template="wand_test_empty")
    public static void mirroredGlassLegacyArcaneRecipeAndPayment(GameTestHelper t) {
        var level=t.getLevel();var pos=t.absolutePos(new BlockPos(2,2,2));var original=level.getBlockState(pos);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MirrorGlassTest"));
        player.moveTo(pos.getCenter());player.setGameMode(GameType.SURVIVAL);
        thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,"BASICARTIFACE");
        try {
            level.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),3);var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
            table.setItem(2,new ItemStack(TCItems.material(MaterialType.QUICKSILVER),2));table.setItem(7,new ItemStack(Items.GLASS_PANE,2));
            var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);table.setItem(10,wand);
            var menu=new ArcaneWorkbenchMenu(1,player.getInventory(),table);player.containerMenu=menu;menu.refresh();
            t.assertTrue(menu.getSlot(0).getItem().is(TCItems.material(MaterialType.MIRROR_GLASS)),"Scattered inputs produce mirrored glass");
            menu.clicked(0,0,ClickType.PICKUP,player);
            t.assertTrue(menu.getCarried().is(TCItems.material(MaterialType.MIRROR_GLASS))&&menu.getCarried().getCount()==1,"One mirrored glass output");
            t.assertTrue(wand.getTag().getInt("ignis")==9000&&wand.getTag().getInt("terra")==9000,"Exactly 10 fire and 10 earth vis consumed");
            t.assertTrue(table.getItem(2).getCount()==1&&table.getItem(7).getCount()==1,"Consumes one quicksilver and one pane");
            table.setItem(7,new ItemStack(Items.RED_STAINED_GLASS_PANE));menu.refresh();t.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Colored panes rejected like legacy colorless ore tag");
            t.succeed();
        }finally{level.setBlock(pos,original,3);player.containerMenu=player.inventoryMenu;}
    }
}
