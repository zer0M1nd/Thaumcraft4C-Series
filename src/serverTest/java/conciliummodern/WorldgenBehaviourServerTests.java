package conciliummodern;

import java.lang.reflect.Proxy;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class WorldgenBehaviourServerTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void towerPlacesOriginalLandmarksAndPersistentArchers(GameTestHelper test) {
        BlockPos origin=platform(test);
        test.assertTrue(CrimsonTowerFeature.generate(test.getLevel(),origin,RandomSource.create(143)),"Actual tower helper accepts flat source terrain");
        var level=test.getLevel();
        test.assertTrue(level.getBlockState(origin).is(TCArcane.GREATWOOD_LOG.get())&&level.getBlockState(origin.above(3)).is(TCArcane.GREATWOOD_LOG.get()),"Four-block lower pillar retained");
        test.assertTrue(level.getBlockState(origin.offset(2,3,1)).getValue(RotatedPillarBlock.AXIS)==Direction.Axis.X&&level.getBlockState(origin.offset(0,3,-3)).getValue(RotatedPillarBlock.AXIS)==Direction.Axis.Z,"Both horizontal log axes and negative-Z row retained");
        test.assertTrue(level.getBlockState(origin.offset(2,0,1)).is(TCEldritch.ROCK.get()),"Legacy cosmetic metadata 12 maps to eldritch rock");
        var bottom=level.getBlockState(origin.offset(3,6,1));var top=level.getBlockState(origin.offset(3,7,1));
        test.assertTrue(bottom.getBlock() instanceof StairBlock&&bottom.getValue(StairBlock.FACING)==Direction.NORTH&&bottom.getValue(StairBlock.HALF)==Half.BOTTOM&&top.getValue(StairBlock.HALF)==Half.TOP,"Opposed window stairs retain original metadata direction and half");
        test.assertTrue(level.getBlockState(origin.offset(3,0,-2)).is(TCArcane.LEVITATOR.get())&&level.getBlockState(origin.offset(3,9,-2)).isAir()&&level.getBlockState(origin.offset(2,9,-2)).is(Blocks.RED_TERRACOTTA),"Working levitator, open shaft and red platform retained");
        for(var pos:List.of(origin.offset(3,10,3),origin.offset(3,10,-7),origin.offset(-2,10,-2),origin.offset(8,10,-2))) {
            var state=level.getBlockState(pos);test.assertTrue(state.is(Blocks.RED_WALL_BANNER)&&state.canSurvive(level,pos),"Four modern wall banners have actual support");
            test.assertTrue(level.getBlockEntity(pos) instanceof BannerBlockEntity banner&&ItemStack.isSameItemSameTags(banner.getItem(),CrimsonBanner.item()),"Every banner uses real crimson pattern NBT");
        }
        var chest=level.getBlockEntity(origin.offset(5,10,-2));test.assertTrue(chest instanceof ChestBlockEntity&&chest.saveWithFullMetadata().getString("LootTable").equals(CrimsonTowerFeature.LOOT.toString()),"Chest uses real delayed original loot table");
        var archers=level.getEntitiesOfClass(CrimsonBrigade.class,new AABB(origin.offset(-9,-2,-9),origin.offset(10,14,9)),CrimsonBrigade::archer);
        test.assertTrue(archers.size()>=3&&archers.size()<=7,"Three fixed plus zero-to-four extra archers");
        for(var archer:archers)test.assertTrue(archer.isPersistenceRequired()&&archer.getMainHandItem().is(Items.BOW),"Archer spawn initialized real equipment and persistence");
        archers.forEach(Entity::discard);test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void towerRejectsBadGroundAndExistingContainersAtomically(GameTestHelper test) {
        var origin=platform(test);var invalid=origin.offset(-2,-1,-4);test.getLevel().setBlock(invalid,Blocks.WATER.defaultBlockState(),3);
        test.assertTrue(!CrimsonTowerFeature.generate(test.getLevel(),origin,RandomSource.create(42))&&test.getLevel().getBlockState(origin).isAir(),"Bad platform rejects before any tower block is placed");
        test.getLevel().setBlock(invalid,Blocks.GRASS_BLOCK.defaultBlockState(),3);var protectedPos=origin.offset(3,8,0);
        test.getLevel().setBlock(protectedPos,Blocks.CHEST.defaultBlockState(),3);var chest=(ChestBlockEntity)test.getLevel().getBlockEntity(protectedPos);chest.setItem(0,new ItemStack(Items.DIAMOND,7));
        test.assertTrue(!CrimsonTowerFeature.generate(test.getLevel(),origin,RandomSource.create(42)),"Container overlap rejects the whole tower");
        test.assertTrue(test.getLevel().getBlockState(origin).isAir()&&chest.getItem(0).is(Items.DIAMOND)&&chest.getItem(0).getCount()==7,"Rejected generation leaves both space and foreign inventory untouched");test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void towerLootUsesSixRealEntriesAndLegacyRollRange(GameTestHelper test) {
        var table=test.getLevel().getServer().getLootData().getLootTable(CrimsonTowerFeature.LOOT);
        Set<String> allowed=Set.of("thaumcraft:void_nugget","thaumcraft:crimson_rites","minecraft:bow","minecraft:arrow","thaumcraft:triple_meat_treat","thaumcraft:knowledge_fragment");var seen=new HashSet<String>();
        var params=new LootParams.Builder(test.getLevel()).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(test.absolutePos(new BlockPos(3,2,3)))).create(LootContextParamSets.CHEST);
        boolean minimum=false,maximum=false;
        for(int seed=1;seed<=128;seed++) {
            var loot=table.getRandomItems(params,seed);test.assertTrue(loot.size()>=2&&loot.size()<=7,"Legacy setMin(2), setMax(8) yields two-to-seven actual rolls");minimum|=loot.size()==2;maximum|=loot.size()==7;
            for(var stack:loot) {
                String id=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();test.assertTrue(allowed.contains(id),"Only source loot entries are present: "+id);seen.add(id);
                int max=id.equals("minecraft:arrow")?16:id.equals("minecraft:bow")||id.equals("thaumcraft:crimson_rites")?1:3;
                test.assertTrue(stack.getCount()>=1&&stack.getCount()<=max,"Source per-entry quantity bounds retained");
            }
        }
        test.assertTrue(seen.equals(allowed)&&minimum&&maximum,"Seeded real loot execution exercises all six identities and both roll endpoints");test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void villageCandidatesKeepVanillaWeightsAndLimitPerStructure(GameTestHelper test) {
        boolean enabled=ConciliumWorldgen.GENERATE_BURNING_SITES.get();int weight=ConciliumWorldgen.BURNING_WEIGHT.get();
        try {
            ConciliumWorldgen.GENERATE_BURNING_SITES.set(true);ConciliumWorldgen.BURNING_WEIGHT.set(10);
            var registry=test.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);var manager=test.getLevel().getStructureManager();
            withHost(true,true,()->{
                for(String biome:List.of("plains","desert","savanna","snowy","taiga")) {
                    var pool=registry.get(new ResourceLocation("minecraft","village/"+biome+"/houses"));test.assertTrue(pool!=null,"Vanilla village pool exists: "+biome);
                    var pieces=new ArrayList<PoolElementStructurePiece>();var result=ConciliumWorldgen.villageCandidates(registry,pieces,pool,RandomSource.create(9));
                    var additions=result.stream().filter(e->e instanceof BurningSiteElement).toList();test.assertTrue(additions.size()==10&&result.size()==pool.size()+10,"Original weight added to "+biome);
                    var vanilla=new IdentityHashMap<StructurePoolElement,Integer>();for(var element:pool.getShuffledTemplates(RandomSource.create(10)))vanilla.merge(element,1,Integer::sum);
                    var preserved=new IdentityHashMap<StructurePoolElement,Integer>();for(var element:result)if(!(element instanceof BurningSiteElement))preserved.merge(element,1,Integer::sum);
                    test.assertTrue(vanilla.equals(preserved),"Every original weighted candidate including empty entries remains");
                    var addition=additions.get(0);pieces.add(new PoolElementStructurePiece(manager,addition,BlockPos.ZERO,1,Rotation.NONE,addition.getBoundingBox(manager,BlockPos.ZERO,Rotation.NONE)));
                    test.assertTrue(ConciliumWorldgen.villageCandidates(registry,pieces,pool,RandomSource.create(9)).stream().noneMatch(e->e instanceof BurningSiteElement),"Accepted piece prevents a second site in same structure");
                    test.assertTrue(ConciliumWorldgen.villageCandidates(registry,List.of(),pool,RandomSource.create(9)).size()==pool.size()+10,"Independent village starts with independent capacity");
                }
            });
            for(boolean stockade:new boolean[]{false,true})withHost(stockade,false,()->test.assertTrue(!ConciliumWorldgen.burningSiteAvailable(),"Incomplete real host capabilities never enable a counterfeit site"));
            withHost(true,true,()->{ConciliumWorldgen.BURNING_WEIGHT.set(0);test.assertTrue(!ConciliumWorldgen.burningSiteAvailable(),"Zero weight disables site");});
        } finally {ConciliumWorldgen.GENERATE_BURNING_SITES.set(enabled);ConciliumWorldgen.BURNING_WEIGHT.set(weight);}
        test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void burningSiteCodecRotationsAndClippedPlacement(GameTestHelper test) {
        var manager=test.getLevel().getStructureManager();var element=new BurningSiteElement(StructureTemplatePool.Projection.RIGID);
        var encoded=StructurePoolElement.CODEC.encodeStart(NbtOps.INSTANCE,element).result().orElseThrow();var decoded=StructurePoolElement.CODEC.parse(NbtOps.INSTANCE,encoded).result().orElseThrow();
        test.assertTrue(decoded instanceof BurningSiteElement&&decoded.getProjection()==StructureTemplatePool.Projection.RIGID,"Real dispatch codec preserves saved site identity and projection");
        for(var rotation:Rotation.values()) {
            var box=decoded.getBoundingBox(manager,BlockPos.ZERO,rotation);test.assertTrue(box.getXSpan()==9&&box.getYSpan()==6&&box.getZSpan()==9,"Rotated collision box includes foundation and stockade top");
            var joint=decoded.getShuffledJigsawBlocks(manager,BlockPos.ZERO,rotation,RandomSource.create(2)).get(0);
            test.assertTrue(box.isInside(joint.pos())&&JigsawBlock.getFrontFacing(joint.state())==rotation.rotate(Direction.NORTH)&&joint.nbt().getString("name").equals("minecraft:building_entrance"),"All rotations retain a compatible road connector");
        }
        withHost(true,true,()->{
            var origin=test.absolutePos(new BlockPos(10,2,10));var level=test.getLevel();var generator=level.getChunkSource().getGenerator();
            var west=new BoundingBox(origin.getX(),origin.getY()-1,origin.getZ(),origin.getX()+3,origin.getY()+4,origin.getZ()+8);
            var east=new BoundingBox(origin.getX()+4,origin.getY()-1,origin.getZ(),origin.getX()+8,origin.getY()+4,origin.getZ()+8);
            test.assertTrue(element.place(manager,level,level.structureManager(),generator,origin,BlockPos.ZERO,Rotation.NONE,west,RandomSource.create(6),false),"First clipped part places");
            test.assertTrue(element.place(manager,level,level.structureManager(),generator,origin,BlockPos.ZERO,Rotation.NONE,east,RandomSource.create(7),false),"Second clipped part places");
            for(int x=0;x<=8;x++)for(int z=0;z<=8;z++) {
                test.assertTrue(level.getBlockState(origin.offset(x,-1,z)).is(Blocks.DIRT),"Whole 9x9 foundation survives clipped placement");
                var floor=level.getBlockState(origin.offset(x,0,z));
                test.assertTrue(x>=3&&x<=5&&z>=3&&z<=5?floor.is(Blocks.COAL_BLOCK)||floor.is(Blocks.HAY_BLOCK)||floor.is(Blocks.OAK_LOG):floor.is(Blocks.GRAVEL),"Original gravel and random central fuel palette retained");
                if(x==0||x==8||z==0||z==8)test.assertTrue(level.getBlockState(origin.offset(x,1,z)).is(Blocks.COBBLESTONE_WALL),"Boundary wall complete");
            }
            for(int y=1;y<=4;y++)test.assertTrue(level.getBlockState(origin.offset(4,y,4)).is(Blocks.OAK_FENCE),"Host-provided stockade used for every layer");
            var hunters=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new AABB(origin,origin.offset(9,5,9)));
            test.assertTrue(hunters.size()==1&&hunters.get(0).isPersistenceRequired(),"Host-provided hunter spawns once across clipped regions and persists");hunters.forEach(Entity::discard);
        });test.succeed();
    }
    private static BlockPos platform(GameTestHelper test) {
        for(int x=1;x<=30;x++)for(int z=1;z<=30;z++)test.setBlock(new BlockPos(x,1,z),Blocks.GRASS_BLOCK);
        return test.absolutePos(new BlockPos(12,2,12));
    }
    /** Synchronous, restored fixture for our host contract; never claims that a real Witchery implementation was tested. */
    private static void withHost(boolean stockade,boolean hunter,Runnable body) {
        try {
            var field=WitcheryBridge.class.getDeclaredField("installed");field.setAccessible(true);Object previous=field.get(null);
            var fixture=Proxy.newProxyInstance(WitcheryBridge.class.getClassLoader(),new Class<?>[]{WitcheryBridge.Host.class},(proxy,method,args)->switch(method.getName()) {
                case "stockade"->stockade?Blocks.OAK_FENCE.defaultBlockState():null;
                case "witchHunter"->hunter?EntityType.VILLAGER:null;
                case "modId"->ThaumicConcilium.ID;
                default->throw new UnsupportedOperationException("Unused test host call: "+method.getName());
            });
            try {field.set(null,fixture);body.run();}finally {field.set(null,previous);}
        } catch(ReflectiveOperationException exception) {throw new IllegalStateException(exception);}
    }
    private WorldgenBehaviourServerTests() {}
}
