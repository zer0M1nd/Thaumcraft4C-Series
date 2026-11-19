package ttmodern;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.*;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class UtilityServerTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test,String name) {
        return new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),name));
    }
    @GameTest(template="wand_test_empty") public static void tinkererShareBookOfflineOwnerAndIdempotency(GameTestHelper test) {
        var owner=player(test,"TTOwner");var reader=player(test,"TTReader");var data=KnowledgeSavedData.get(owner.server);
        var aspect=tc4revival.aspect.TCAspects.catalog().require("lucrum");
        data.update(owner.getUUID(),owner.getRandom()::nextInt,k->{k.discover(aspect);k.setPool(aspect,(short)120);k.rememberResearch("thaumic_tinkerer:BLOOD_SWORD");k.rememberResearch("TT_TEST_CLUE");return null;});
        var book=new ItemStack(Tinkerer.SHARE_BOOK.get());owner.setItemInHand(InteractionHand.MAIN_HAND,book);book.getItem().use(owner.level(),owner,InteractionHand.MAIN_HAND);
        test.assertTrue(book.getTag().getUUID("Owner").equals(owner.getUUID()),"Book binds a UUID on server");
        data.update(owner.getUUID(),owner.getRandom()::nextInt,k->{k.rememberResearch("thaumic_tinkerer:SUMMON");return null;});
        reader.setItemInHand(InteractionHand.MAIN_HAND,book);book.getItem().use(reader.level(),reader,InteractionHand.MAIN_HAND);
        var first=data.snapshot(reader.getUUID()).orElseThrow();test.assertTrue(first.knows(aspect)&&first.pool(aspect)==0,"Shared discovery does not copy research points");
        test.assertTrue(first.researchMarkers().containsAll(java.util.Set.of("TT_TEST_CLUE","thaumic_tinkerer:SUMMON","thaumic_tinkerer:BLOOD_SWORD")),"Offline owner's latest state and hidden clues are shared");
        book.getItem().use(reader.level(),reader,InteractionHand.MAIN_HAND);var next=data.snapshot(reader.getUUID()).orElseThrow();test.assertTrue(next.totalWarp()==first.totalWarp(),"Repeated use never adds duplicate research warp");
        var foreign=book.copy();foreign.getOrCreateTag().putUUID("Owner",UUID.randomUUID());var visitor=player(test,"TTVisitor");visitor.setItemInHand(InteractionHand.MAIN_HAND,foreign);foreign.getItem().use(visitor.level(),visitor,InteractionHand.MAIN_HAND);
        test.assertTrue(data.snapshot(visitor.getUUID()).orElseThrow().researchMarkers().contains("thaumic_tinkerer:SUMMON"),"Book snapshot works when owner is absent from this world");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererPlatformDirectionAndCamouflagePersistence(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));level.setBlock(pos,Tinkerer.PLATFORM.get().defaultBlockState(),3);var state=level.getBlockState(pos);var player=player(test,"TTPlatform");
        player.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);test.assertTrue(!state.getCollisionShape(level,pos,CollisionContext.of(player)).isEmpty(),"Platform catches descending player");
        player.setShiftKeyDown(true);test.assertTrue(state.getCollisionShape(level,pos,CollisionContext.of(player)).isEmpty(),"Sneaking passes downward");player.setShiftKeyDown(false);player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);test.assertTrue(state.getCollisionShape(level,pos,CollisionContext.of(player)).isEmpty(),"Player passes upward from below");
        var entity=(PlatformEntity)level.getBlockEntity(pos);entity.cover(Blocks.OAK_LOG.defaultBlockState());var restored=new PlatformEntity(pos,entity.getBlockState());restored.load(entity.saveWithoutMetadata());test.assertTrue(restored.cover().is(Blocks.OAK_LOG),"Camouflage survives serialization");level.removeBlock(pos,false);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererWardSlabMobBarrierAndRedstone(GameTestHelper test) {
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));world.setBlock(pos,Tinkerer.WARD_SLAB.get().defaultBlockState(),3);Tinkerer.WARD_SLAB.get().tick(world.getBlockState(pos),world,pos,world.random);
        var above=pos.above();var barrier=world.getBlockState(above);test.assertTrue(barrier.is(Tinkerer.WARD_BARRIER.get()),"Ward slab creates barrier");var cow=EntityType.COW.create(world);var player=player(test,"TTWard");
        test.assertTrue(!barrier.getCollisionShape(world,above,CollisionContext.of(cow)).isEmpty(),"Mobs cannot cross enabled barrier");test.assertTrue(barrier.getCollisionShape(world,above,CollisionContext.of(player)).isEmpty(),"Players can cross barrier");
        world.setBlock(pos.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);test.assertTrue(barrier.getCollisionShape(world,above,CollisionContext.of(cow)).isEmpty(),"Redstone disables collision immediately");world.removeBlock(pos.west(),false);world.removeBlock(pos,false);Tinkerer.WARD_BARRIER.get().tick(barrier,world,above,world.random);test.assertTrue(world.isEmptyBlock(above),"Orphan barrier is removed");world.removeBlock(pos.above(2),false);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererInkwellUsesBaseInkContract(GameTestHelper test) {
        var ink=new ItemStack(Tinkerer.INFUSED_INKWELL.get());var tool=(thaumcraft.api.IScribeTools)ink.getItem();test.assertTrue(tool.getInk(ink)==800&&tc4revival.content.ScribingToolsItem.accepts(ink),"Research table accepts all 800 units through API");tool.setInk(ink,23);test.assertTrue(ink.getDamageValue()==777&&tc4revival.content.ScribingToolsItem.ink(ink)==23,"Ink consumption persists in item damage");
        var helm=new ItemStack(Tinkerer.REVEALING_HELM.get());test.assertTrue(helm.getMaxDamage()==500&&((thaumcraft.api.IVisDiscountGear)helm.getItem()).getVisDiscount(helm,null,null)==5,"Helmet durability and discount match original");test.succeed();
    }
}
