package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class ModernTagsServerTests {
    private static Block block(String id){return ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(id));}
    private static void checkMod(GameTestHelper t,String id){t.assertTrue(net.minecraftforge.fml.ModList.get().isLoaded(id),"Required real compatibility mod loaded: "+id);}
    @GameTest(template="wand_test_empty")
    public static void modernCropMaturityAndRealHarvest(GameTestHelper t){
        checkMod(t,"farmersdelight");checkMod(t,"mysticalagriculture");
        var level=t.getLevel();var pos=t.absolutePos(new BlockPos(2,2,2));
        var golem=TCEntities.GOLEM.get().create(level);golem.setPos(Vec3.atCenterOf(pos));
        golem.configuration().installCore(GolemCoreType.HARVEST);golem.configuration().installUpgrade(4);
        String[] crops={"minecraft:beetroots","farmersdelight:cabbages","farmersdelight:onions","mysticalagriculture:inferium_crop"};
        for(String id:crops){
            var crop=(CropBlock)block(id);level.setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState());
            level.setBlockAndUpdate(pos,crop.defaultBlockState());
            t.assertTrue(!GolemCropHarvest.mature(level,pos),"Do not harvest young "+id);
            level.setBlockAndUpdate(pos,crop.getStateForAge(crop.getMaxAge()));
            t.assertTrue(GolemCropHarvest.mature(level,pos),"Native max age for "+id);
            var seed=new ItemStack(crop.asItem());
            t.assertTrue(!seed.isEmpty(),"Actual crop seed exists "+id);
            level.addFreshEntity(new ItemEntity(level,pos.getX()+.5,pos.getY()+.2,pos.getZ()+.5,seed));
            GolemCropHarvest.harvest(golem,pos,level.getBlockState(pos));
            t.assertTrue(level.getBlockState(pos).is(crop)&&!crop.isMaxAge(level.getBlockState(pos)),"Replanted with native seed placement "+id);
            level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(6)).forEach(ItemEntity::discard);
        }
        var tomatoes=(CropBlock)block("farmersdelight:tomatoes");level.setBlockAndUpdate(pos,tomatoes.getStateForAge(tomatoes.getMaxAge()));
        GolemCropHarvest.harvest(golem,pos,level.getBlockState(pos));
        t.assertTrue(level.getBlockState(pos).is(tomatoes)&&!tomatoes.isMaxAge(level.getBlockState(pos)),"Tomato native use preserves vine");
        var rice=block("farmersdelight:rice");var panicles=(CropBlock)block("farmersdelight:rice_panicles");
        level.setBlockAndUpdate(pos.below(2),Blocks.DIRT.defaultBlockState());level.setBlock(pos.below(),rice.defaultBlockState(),2);level.setBlock(pos,panicles.getStateForAge(panicles.getMaxAge()),2);
        t.assertTrue(level.getBlockState(pos.below()).canSurvive(level,pos.below()),"Rice planted over valid submerged soil");
        t.assertTrue(!GolemCropHarvest.mature(level,pos.below()),"Rice base not harvested");
        GolemCropHarvest.harvest(golem,pos,level.getBlockState(pos));
        t.assertTrue(level.getBlockState(pos.below()).is(rice),"Rice base survives harvest");
        var budding=block("farmersdelight:budding_tomatoes").defaultBlockState();
        for(var p:budding.getProperties())if(p instanceof IntegerProperty age&&age.getName().equals("age"))budding=budding.setValue(age,Collections.max(age.getPossibleValues()));
        t.assertTrue(!ModernCropHarvest.mature(budding),"Budding tomato must grow into vine before harvesting");
        golem.discard();t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void nativeClustersUseActualFortuneDropsAndRecipes(GameTestHelper t){
        var level=t.getLevel();var pos=t.absolutePos(new BlockPos(2,2,2));
        var worker=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ClusterTest"));
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.enchant(Enchantments.BLOCK_FORTUNE,3);
        for(String metal:List.of("iron","gold","copper")){
            var cluster=TCItems.cluster(ClusterType.valueOf(metal.toUpperCase(Locale.ROOT)));
            for(String prefix:List.of("","deepslate_")){
                var ore=block("minecraft:"+prefix+metal+"_ore");
                for(int attempt=0;attempt<12;attempt++)for(var drop:Block.getDrops(ore.defaultBlockState(),level,pos,null,worker,tool)){
                    var result=SpecialMining.apply(drop,20,net.minecraft.util.RandomSource.create(7));
                    t.assertTrue(result.is(cluster)&&result.getCount()==drop.getCount(),"Cluster retains exact vanilla fortune count "+metal);
                }
            }
            for(boolean raw:List.of(false,true)){
                var recipe=(CrucibleRecipe)level.getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:crucible/pure_"+metal+(raw?"_from_raw":""))).orElseThrow();
                var input=new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("minecraft:"+(raw?"raw_"+metal:"deepslate_"+metal+"_ore"))));
                t.assertTrue(recipe.catalystMatches(input),"Ore/raw tag input "+metal);
                int count=raw?1:2;
                t.assertTrue(recipe.getResultItem(level.registryAccess()).getCount()==count,"Cluster output multiplier");
                t.assertTrue(recipe.costs().amount(tc4revival.aspect.TCAspects.catalog().require("metallum"))==count&&recipe.costs().amount(tc4revival.aspect.TCAspects.catalog().require("ordo"))==count,"Matching essentia fee");
            }
        }
        var pick=new ItemStack(TCItems.ELEMENTAL_PICKAXE.get());pick.enchant(Enchantments.SILK_TOUCH,1);
        for(int attempt=0;attempt<15;attempt++){
            var drops=Block.getDrops(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),level,pos,null,worker,pick);
            t.assertTrue(drops.size()==1&&drops.get(0).is(Items.DEEPSLATE_IRON_ORE),"Silk Touch never converts intact ore");
        }
        t.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=300)
    public static void golemRoutesThroughIronDoor(GameTestHelper t){
        var level=t.getLevel();var start=t.absolutePos(new BlockPos(2,2,2));
        for(int z=-1;z<=8;z++)for(int x=-1;x<=1;x++)for(int y=-1;y<=2;y++)
            level.setBlockAndUpdate(start.offset(x,y,z),y==-1||y==2||x!=0?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        var door=start.offset(0,0,3);var state=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.SOUTH);
        level.setBlock(door,state,2);level.setBlock(door.above(),state.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);
        var golem=TCEntities.GOLEM.get().create(level);golem.setPos(Vec3.atBottomCenterOf(start));level.addFreshEntity(golem);
        var goal=new GolemOpenDoorGoal(golem);
        golem.setOnGround(true);
        var path=golem.getNavigation().createPath(start.offset(0,0,6),0);
        t.assertTrue(path!=null&&path.canReach(),"Native hand-openable door accepted by route planner");
        golem.getNavigation().moveTo(path,.6);
        boolean[] running={false},opened={false};
        t.onEachTick(()->{
            if(!running[0]&&goal.canUse()){goal.start();running[0]=true;opened[0]=true;}
            if(running[0]){goal.tick();if(!goal.canContinueToUse()){goal.stop();running[0]=false;}}
        });
        t.succeedWhen(()->{
            t.assertTrue(opened[0]&&golem.getZ()>door.getZ()+1.5,"Golem opens and crosses actual iron doorway");
            if(running[0])goal.stop();golem.discard();
        });
    }
    @GameTest(template="wand_test_empty")
    public static void modernLogsForLumberBeansAndChainsaw(GameTestHelper t)throws Exception{
        var level=t.getLevel();var pos=t.absolutePos(new BlockPos(2,2,2));
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"fillbiome "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" "+pos.getX()+" "+pos.getY()+" "+pos.getZ()+" thaumcraft:magical_forest");
        t.assertTrue(LegacyBiomeProfiles.get(level.getBiome(pos)).magical(),"Magical-biome bean fixture");
        var method=Class.forName("emtmodern.item.StreamChainsaw").getMethod("furthest",net.minecraft.world.level.Level.class,BlockPos.class);
        int checked=0;
        for(var b:ForgeRegistries.BLOCKS){
            if(!b.defaultBlockState().is(BlockTags.LOGS))continue;
            level.setBlock(pos.above(),b.defaultBlockState(),2);
            t.assertTrue(TCArcane.MANA_POD.get().defaultBlockState().canSurvive(level,pos),"Mana bean support "+ForgeRegistries.BLOCKS.getKey(b));
            level.setBlock(pos.above(3),b.defaultBlockState(),2);
            t.assertTrue(method.invoke(null,level,pos.above()).equals(pos.above(3)),"Chainsaw accepts tagged log "+ForgeRegistries.BLOCKS.getKey(b));
            level.removeBlock(pos.above(),false);level.removeBlock(pos.above(3),false);checked++;
        }
        t.assertTrue(checked>=20,"All loaded logs exercised");
        var golem=TCEntities.GOLEM.get().create(level);golem.setPos(Vec3.atCenterOf(pos));golem.state().home(pos,Direction.DOWN);golem.configuration().installCore(GolemCoreType.LUMBER);
        level.setBlock(pos,Blocks.CHERRY_LOG.defaultBlockState(),2);level.setBlock(pos.above(2),Blocks.CHERRY_LOG.defaultBlockState(),2);
        var goal=new GolemLumberGoal(golem);boolean found=false;for(int n=0;n<512&&!found;n++)found=goal.canUse();
        t.assertTrue(found,"Lumber goal selects cherry logs");goal.start();for(int n=0;n<120&&!level.isEmptyBlock(pos.above(2));n++)goal.tick();
        t.assertTrue(level.isEmptyBlock(pos.above(2)),"Lumber goal actually chops the furthest cherry log");goal.stop();golem.discard();
        t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void addonClusterRecipesAndModernInfusionTags(GameTestHelper t)throws Exception{
        var level=t.getLevel();
        for(boolean raw:List.of(false,true)){
            var r=(CrucibleRecipe)level.getRecipeManager().byKey(ResourceLocation.parse("witchinggadgets:cluster_nickel"+(raw?"_from_raw":""))).orElseThrow();
            var stack=new ItemStack(raw?Items.FLINT:Items.DIAMOND_ORE);
            int n=raw?1:2;
            t.assertTrue(r.matches(new CrucibleRecipe.Input(stack,r.costs(),k->true),level),"WG ore/raw matching");
            t.assertTrue(r.getResultItem(level.registryAccess()).getCount()==n&&r.costs().amount(tc4revival.aspect.TCAspects.catalog().require("ordo"))==n,"WG output and fees");
        }
        var converted=SpecialMining.apply(new ItemStack(Items.FLINT,9),20,net.minecraft.util.RandomSource.create(2));
        t.assertTrue(converted.is(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("witchinggadgets:nickel_cluster")))&&converted.getCount()==9,"Third-party raw metal tag retains count");
        for(String path:List.of("self_photosynthetic_skin","self_amphibious","seawolf")){
            var resource=level.getServer().getResourceManager().getResource(ResourceLocation.parse("thaumichorizons:vat_infusions/"+path+".json")).orElseThrow();
            try(var reader=resource.openAsReader()){
                var data=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                String tag=path.equals("self_photosynthetic_skin")?"minecraft:small_flowers":"forge:raw_fishes";
                boolean found=false;for(var ingredient:data.getAsJsonArray("ingredients")){
                    var obj=ingredient.getAsJsonObject();if(!obj.has("tag")||!obj.get("tag").getAsString().equals(tag))continue;
                    var accepted=net.minecraft.world.item.crafting.Ingredient.fromJson(obj);
                    t.assertTrue(accepted.test(new ItemStack(path.equals("self_photosynthetic_skin")?Items.DANDELION:Items.TROPICAL_FISH)),"Modern infusion tag accepts expanded material");found=true;
                }
                t.assertTrue(found,"Modern tag present "+path);
            }
        }
        t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void largeOakReplacesModernLeaves(GameTestHelper t){
        var level=t.getLevel();var pos=t.absolutePos(new BlockPos(2,2,2));
        level.setBlockAndUpdate(pos.below(),Blocks.DIRT.defaultBlockState());
        // A crown of cherry leaves would block the old four-species whitelist.
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=0;y<24;y++)
            level.setBlock(pos.offset(x,y,z),Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),2);
        t.assertTrue(new ForestOakGrowth().grow(level,pos,net.minecraft.util.RandomSource.create(3)),"Large oak grows through tagged modern leaves");
        t.assertTrue(level.getBlockState(pos).is(Blocks.OAK_LOG),"Actual trunk replaces cherry canopy");
        t.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void modernBonemealAndOpenableTags(GameTestHelper t){
        var level=t.getLevel();var pos=t.absolutePos(new BlockPos(2,2,2));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"HoeTest"));
        var hoe=new ItemStack(TCItems.ELEMENTAL_HOE.get());player.setItemInHand(InteractionHand.MAIN_HAND,hoe);
        level.setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState());level.setBlockAndUpdate(pos,Blocks.BEETROOTS.defaultBlockState());
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        for(int n=0;n<16&&!((CropBlock)Blocks.BEETROOTS).isMaxAge(level.getBlockState(pos));n++)hoe.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        t.assertTrue(((CropBlock)Blocks.BEETROOTS).isMaxAge(level.getBlockState(pos)),"Hoe applies bonemeal beyond legacy crop whitelist");
        int doors=0;for(var b:ForgeRegistries.BLOCKS){var s=b.defaultBlockState();if(s.is(BlockTags.DOORS)&&b instanceof DoorBlock){t.assertTrue(GolemDoorNavigation.usable(s)==DoorBlock.isWoodenDoor(s),"Native hand-opening rule "+ForgeRegistries.BLOCKS.getKey(b));doors++;}}
        t.assertTrue(doors>=10,"Modern doors enumerated");
        var magic=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,ResourceLocation.parse("forge:is_magic"));
        t.assertTrue(level.damageSources().magic().is(magic),"Public magic damage tag includes vanilla magic");
        t.succeed();
    }
}
