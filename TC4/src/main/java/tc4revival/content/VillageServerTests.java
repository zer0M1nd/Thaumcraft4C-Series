package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class VillageServerTests {
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void villageBuildingsPersistenceAndGeneration(GameTestHelper test) {
        var level=test.getLevel();var random=RandomSource.create(190191);var origin=test.absolutePos(new BlockPos(10,3,10));
        var serialization=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
        var clip=new net.minecraft.world.level.levelgen.structure.BoundingBox(origin.getX()-40,level.getMinBuildHeight(),origin.getZ()-40,origin.getX()+80,level.getMaxBuildHeight()-1,origin.getZ()+80);
        var built=new ArrayList<LegacyVillagePiece>();
        for(var role:LegacyVillagerTrades.Role.values())for(int r=0;r<4;r++) {
            var entrance=origin.offset(r*18,0,role==LegacyVillagerTrades.Role.BANKER?30:0);
            var piece=LegacyVillagePiece.atEntrance(role,entrance,Direction.from2DDataValue(r),random);
            piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),random,clip,new net.minecraft.world.level.ChunkPos(entrance),entrance);
            built.add(piece);
            test.assertTrue(level.getBlockState(piece.workstation()).is(role==LegacyVillagerTrades.Role.WIZARD?TCArcane.BLOCK.get():TCArcane.WAND_PEDESTAL.get()),"Building places matching workplace in every rotation");
            var copy=new LegacyVillagePiece(serialization,piece.createTag(serialization));
            test.assertTrue(copy.role()==role&&copy.position(2,6,2).equals(piece.position(2,6,2))&&copy.getBoundingBox().equals(piece.getBoundingBox()),"Piece persistence for each role/rotation");
            if(role==LegacyVillagerTrades.Role.WIZARD) {
                test.assertTrue(level.getBlockState(piece.position(3,5,3)).is(net.minecraft.world.level.block.Blocks.GLOWSTONE),"Tower light");
                var chest=(net.minecraft.world.Container)level.getBlockEntity(piece.position(2,6,2));
                test.assertTrue(chest!=null&&!chest.isEmpty(),"Actual old weighted tower chest generated");
                test.assertTrue(level.getBlockState(piece.position(4,10,3)).getValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN)==(r%2==0),"Reused ladder metadata controls hatch state by rotation");
            }else test.assertTrue(level.getBlockState(piece.position(1,0,2)).is(net.minecraft.world.level.block.Blocks.DIRT),"Banker house dirt floor");
        }
        var registry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for(var biome:List.of("plains","desert","savanna"))test.assertTrue(registry.get(ResourceLocation.parse("minecraft:village_"+biome)) instanceof LegacyVillageStructure,"Natural village registration: "+biome);
        test.assertTrue(!(registry.get(ResourceLocation.parse("minecraft:village_taiga")) instanceof LegacyVillageStructure),"New village biome excluded");
        test.runAfterDelay(80,()->{
            var entities=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new net.minecraft.world.phys.AABB(clip.minX(),clip.minY(),clip.minZ(),clip.maxX()+1,clip.maxY()+1,clip.maxZ()+1));
            test.assertTrue(entities.size()==8&&entities.stream().filter(e->TCVillagers.role(e)==LegacyVillagerTrades.Role.WIZARD).count()==4,"One resident per house and rotation");
            for(var piece:built)test.assertTrue(entities.stream().anyMatch(v->v.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE).map(job->job.pos().equals(piece.workstation())).orElse(false)),"Every generated resident claims its own workplace");
            entities.forEach(net.minecraft.world.entity.Entity::discard);
            pass(test,"eight-buildings-loot-persistence-registration");
        });
    }
    @GameTest(template="wand_test_empty",timeoutTicks=200)
    public static void villageNaturalPiecePlanning(GameTestHelper test) {
        var level=test.getLevel();var generator=level.getChunkSource().getGenerator();
        var structure=(LegacyVillageStructure)level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).get(ResourceLocation.parse("minecraft:village_plains"));
        int towers=0,banks=0;
        for(int sample=0;sample<24;sample++) {
            var context=new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(level.registryAccess(),generator,generator.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),815491+sample,new net.minecraft.world.level.ChunkPos(256+sample*32,256),level,b->true);
            var stub=structure.findGenerationPoint(context).orElseThrow();var pieces=stub.getPiecesBuilder().build().pieces();
            var custom=pieces.stream().filter(p->p instanceof LegacyVillagePiece).map(p->(LegacyVillagePiece)p).toList();
            long wizard=custom.stream().filter(p->p.role()==LegacyVillagerTrades.Role.WIZARD).count();
            long banker=custom.stream().filter(p->p.role()==LegacyVillagerTrades.Role.BANKER).count();
            test.assertTrue(wizard<=1&&banker<=1,"Default-size per-village piece limits");
            towers+=wizard;banks+=banker;
            for(var piece:custom)for(var other:pieces)if(piece!=other)test.assertTrue(!piece.collisionBounds().intersects(other instanceof LegacyVillagePiece legacy?legacy.collisionBounds():other.getBoundingBox()),"Selected component does not intersect existing building bounds");
        }
        test.assertTrue(towers>0&&banks>0,"Production jigsaw wrapper chooses both building types: "+towers+"/"+banks);
        pass(test,"natural-piece-planning-"+towers+"-"+banks);
    }
    @GameTest(template="tree_test_empty",timeoutTicks=100)
    public static void villageDesertMaterialRules(GameTestHelper test) {
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(8,3,8));var random=RandomSource.create(78);
        var tower=LegacyVillagePiece.atEntrance(LegacyVillagerTrades.Role.WIZARD,origin,Direction.NORTH,random);tower.setDesert(true);
        var bank=LegacyVillagePiece.atEntrance(LegacyVillagerTrades.Role.BANKER,origin.east(16),Direction.NORTH,random);bank.setDesert(true);
        var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
        var copy=new LegacyVillagePiece(context,tower.createTag(context));
        for(var piece:List.of(copy,bank))piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),random,piece.getBoundingBox(),new net.minecraft.world.level.ChunkPos(origin),origin);
        test.assertTrue(level.getBlockState(copy.position(3,0,3)).is(net.minecraft.world.level.block.Blocks.CUT_SANDSTONE)&&level.getBlockState(copy.position(1,1,3)).is(net.minecraft.world.level.block.Blocks.SANDSTONE),"Persisted desert tower substitutes planks and cobblestone");
        test.assertTrue(level.getBlockState(bank.position(0,2,0)).is(net.minecraft.world.level.block.Blocks.SANDSTONE)&&level.getBlockState(bank.position(1,0,2)).is(net.minecraft.world.level.block.Blocks.DIRT),"Desert logs become sandstone while dirt remains");
        pass(test,"desert-palette-persistence");
    }
    @GameTest(template="wand_test_empty",timeoutTicks=600)
    public static void villageNaturalWorld(GameTestHelper test) {
        var level=test.getLevel();test.assertTrue(level.getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator,"Requires isolated normal terrain world");
        var registry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var holder=registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,ResourceLocation.parse("minecraft:village_plains")));
        var placement=(net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement)level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder).get(0);
        var chosen=new EnumMap<LegacyVillagerTrades.Role,LegacyVillagePiece>(LegacyVillagerTrades.Role.class);
        for(int region=0;region<128&&chosen.size()<2;region++) {
            var candidate=placement.getPotentialStructureChunk(level.getSeed(),(32+region%16)*placement.spacing(),(32+region/16)*placement.spacing());
            var starts=level.getChunk(candidate.x,candidate.z,net.minecraft.world.level.chunk.ChunkStatus.STRUCTURE_STARTS);
            for(var start:starts.getAllStarts().values())if(start.isValid()&&start.getStructure() instanceof LegacyVillageStructure)
                for(var piece:start.getPieces())if(piece instanceof LegacyVillagePiece legacy)chosen.putIfAbsent(legacy.role(),legacy);
        }
        test.assertTrue(chosen.size()==2,"Both TC building types occur in actual terrain structure starts");
        for(var piece:chosen.values()) {
            var bounds=piece.getBoundingBox();
            for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++){level.getChunk(x,z);level.setChunkForced(x,z,true);}
            tc4revival.TC4Revival.LOGGER.info("Natural village building: role={}, origin={}, bounds={}",piece.role(),piece.position(0,0,0),bounds);
            if(piece.role()==LegacyVillagerTrades.Role.WIZARD)test.assertTrue(level.getBlockEntity(piece.position(2,6,2)) instanceof net.minecraft.world.Container chest&&!chest.isEmpty(),"Natural tower chest filled through world generation");
            else test.assertTrue(level.getBlockState(piece.position(1,0,2)).is(net.minecraft.world.level.block.Blocks.DIRT),"Natural banker house floor");
        }
        test.startSequence().thenWaitUntil(()->{
            for(var piece:chosen.values()) {
                var p=piece.workstation();
                test.assertTrue(level.isPositionEntityTicking(p),"Waiting for natural resident chunk activation");
                var villagers=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new net.minecraft.world.phys.AABB(p).inflate(24));
                test.assertTrue(villagers.stream().anyMatch(v->TCVillagers.role(v)==piece.role()),"Natural resident profession "+piece.role());
            }
        }).thenExecute(()->pass(test,"actual-world-buildings-and-residents"));
    }
    private static void pass(GameTestHelper test,String name){tc4revival.TC4Revival.LOGGER.info("Village system test passed: {}",name);test.succeed();}
}
