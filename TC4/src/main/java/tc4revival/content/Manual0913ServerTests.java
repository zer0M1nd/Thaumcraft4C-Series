package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Regression code only; execution requires the user's later test authorization. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class Manual0913ServerTests {
    @GameTest(template="wand_test_empty")
    public static void creativeBellSelectionSurvivesRealPlayerInteraction(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"CreativeBell"));
        player.getAbilities().instabuild=true;player.moveTo(pos.getX(),pos.getY(),pos.getZ());
        var golem=TCEntities.GOLEM.get().create(level);golem.moveTo(pos.getCenter());
        test.assertTrue(level.addFreshEntity(golem),"Golem joins the world");
        try {
            for (var hand:InteractionHand.values()) {
                var bell=new ItemStack(TCItems.GOLEM_BELL.get());player.setItemInHand(hand,bell);
                test.assertTrue(player.interactOn(golem,hand).consumesAction(),"Real player interaction handles bell");
                var held=player.getItemInHand(hand);
                test.assertTrue(GolemBellItem.selected(held,level)==golem,"Creative binding persists on held stack, not a discarded copy");
                GolemBellItem.changeMarker(held,player,pos,net.minecraft.core.Direction.UP);
                test.assertTrue(held.hasTag() && !held.getTag().getList("markers",10).isEmpty(),"Held bell retains a marker");
                test.assertTrue(!golem.markers().save().isEmpty(),"Selected golem receives the marker");
                GolemBellItem.resetMarkers(held,player);
            }
        } finally {golem.discard();}
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void naturalSpawnCandidatesSurvivePoolRequery(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        var original=level.getBiome(pos).unwrapKey().orElseThrow();
        int checked=0;
        try {
            for(var biome:java.util.List.of(TCBiomes.MAGICAL_FOREST,TCBiomes.TAINTED_LAND,TCBiomes.EERIE,Biomes.PLAINS,Biomes.NETHER_WASTES)) {
                WorldBiomeChanges.changeColumn(level,pos,biome);
                var base=level.getBiome(pos).value().getMobSettings().getMobs(MobCategory.MONSTER);
                var first=ForgeEventFactory.getPotentialSpawns(level,MobCategory.MONSTER,pos,base);
                var second=ForgeEventFactory.getPotentialSpawns(level,MobCategory.MONSTER,pos,base);
                for(var entry:first.unwrap()) if(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entry.type).getNamespace().equals("thaumcraft")) {
                    test.assertTrue(second.unwrap().contains(entry),"NaturalSpawner identity membership survives re-query for "+entry.type+" in "+biome.location());
                    checked++;
                }
            }
            test.assertTrue(checked>=8,"All configured natural spawn families must be represented");
            var normal=NaturalSpawnEntries.get(TCEntities.FIRE_BAT.get(),10,1,2);
            var seasonal=NaturalSpawnEntries.get(TCEntities.FIRE_BAT.get(),5,1,2);
            test.assertTrue(normal!=seasonal && seasonal==NaturalSpawnEntries.get(TCEntities.FIRE_BAT.get(),5,1,2),"Seasonal weights remain separate stable candidates");
        } finally {WorldBiomeChanges.changeColumn(level,pos,original);}
        test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void strongerNodeActuallyTakesVisFromNeighbour(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),3);
        level.setBlock(pos.east(),TCArcane.AURA_NODE.get().defaultBlockState(),3);
        var receiver=(AuraNodeBlockEntity)level.getBlockEntity(pos);
        var donor=(AuraNodeBlockEntity)level.getBlockEntity(pos.east());
        var air=tc4revival.aspect.TCAspects.catalog().require("aer");
        var large=new tc4revival.aspect.AspectAmounts();large.add(air,1000);
        var small=new tc4revival.aspect.AspectAmounts();small.add(air,100);
        receiver.configure(large,AuraNodeType.NORMAL,AuraNodeModifier.BRIGHT,"receiver",false);
        receiver.take(air,300);
        donor.configure(small,AuraNodeType.NORMAL,null,"donor",false);
        boolean transferred=false;
        for(int attempt=0;attempt<20000;attempt++) {
            int before=receiver.aspects().amount(air);receiver.tick();
            if(donor.aspects().amount(air)<100) {
                test.assertTrue(donor.aspects().amount(air)==99,"Discharge takes one point from donor");
                test.assertTrue(receiver.aspects().amount(air)>before,"Receiver gains the stolen point");
                transferred=true;break;
            }
        }
        test.assertTrue(transferred,"Legacy random neighbouring-node attempts eventually transfer vis");
        test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void undergroundLandCannotPaintAnOceanColumn(GameTestHelper test) {
        var registry=test.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var ocean=registry.getHolderOrThrow(Biomes.OCEAN);var plains=registry.getHolderOrThrow(Biomes.PLAINS);
        var delegate=new net.minecraft.world.level.biome.BiomeSource() {
            @Override protected com.mojang.serialization.Codec<? extends net.minecraft.world.level.biome.BiomeSource> codec() { return CODEC; }
            @Override protected java.util.stream.Stream<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> collectPossibleBiomes() {
                return java.util.stream.Stream.of(ocean,plains);
            }
            @Override public net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> getNoiseBiome(int x,int y,int z,net.minecraft.world.level.biome.Climate.Sampler sampler) {
                return y==16?ocean:plains;
            }
        };
        var source=new LegacyNaturalBiomeSource(delegate,registry.getHolderOrThrow(TCBiomes.MAGICAL_FOREST),registry.getHolderOrThrow(TCBiomes.TAINTED_LAND),815491,0,Integer.MAX_VALUE);
        var sampler=test.getLevel().getChunkSource().randomState().sampler();
        for(int x=-128;x<=128;x+=8) {
            test.assertTrue(source.getNoiseBiome(x,16,0,sampler).is(Biomes.OCEAN),"Ocean surface stays ocean");
            test.assertTrue(source.getNoiseBiome(x,0,0,sampler).is(Biomes.PLAINS),"Eligible underground climate does not introduce taint under ocean");
        }
        test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void boulderLobesRespectDecorationBoundary(GameTestHelper test) {
        var world=test.getLevel();var origin=test.absolutePos(new BlockPos(4,4,4));
        int count=0;
        for(int seed=0;seed<24;seed++) {
            for(var p:BlockPos.betweenClosed(origin.offset(-4,-3,-4),origin.offset(2,2,2)))
                world.setBlock(p,(p.getY()<origin.getY()?net.minecraft.world.level.block.Blocks.DIRT:net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(),2);
            count+=ForestMossFeature.boulder(world,origin,net.minecraft.util.RandomSource.create(seed),TCArcane.TAINT_CRUST.get().defaultBlockState(),p->p.getX()>=origin.getX())?1:0;
            for(var p:BlockPos.betweenClosed(origin.offset(-4,-3,-4),origin.offset(-1,2,2)))
                test.assertTrue(!world.getBlockState(p).is(TCArcane.TAINT_CRUST.get()),"No lobe may seed taint beyond the allowed biome boundary");
        }
        test.assertTrue(count>0,"Clipping still permits boulders inside their biome");
        test.succeed();
    }
}
