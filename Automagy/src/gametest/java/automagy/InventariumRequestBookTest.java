package automagy;

import automagy.content.*;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.*;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import static automagy.gametest.Checks.*;

@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class InventariumRequestBookTest {
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void identityDoesNotChangeWhenOriginalOrReturnedTagIsModified(net.minecraft.gametest.framework.GameTestHelper h) {
        var stack=new ItemStack(Items.DIAMOND);stack.getOrCreateTag().putInt("variant",1);var key=new RequestedItem(stack);int hash=key.hashCode();
        stack.getOrCreateTag().putInt("variant",2);key.stack().getOrCreateTag().putInt("variant",3);
        assertEquals(hash,key.hashCode());assertEquals(1,key.stack().getTag().getInt("variant"));assertFalse(key.matches(stack));
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void persistenceKeepsClaimsAndExactItemDestinationIdentity(net.minecraft.gametest.framework.GameTestHelper h) {
        var book=new InventariumRequestBook(()->{});var stack=new ItemStack(Items.DIAMOND);stack.getOrCreateTag().putString("marker","first");
        var destination=GlobalPos.of(Level.NETHER,new BlockPos(-20,65,30));var key=new RequestedItem(stack);
        book.request(stack,destination,100);var claim=book.claim(key,destination,64).orElseThrow();
        var restored=new InventariumRequestBook(()->{});restored.load(book.save());
        assertEquals(book.requests(),restored.requests());assertEquals(book.claims(),restored.claims());
        assertEquals(36,restored.claim(key,destination,64).orElseThrow().amount());
        assertEquals(12,restored.finish(claim.id(),12));assertEquals(88,restored.requests().get(0).amount());
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void cancellationSurvivesReloadAndOrphansAreReleasedOnlyOnExplicitReconciliation(net.minecraft.gametest.framework.GameTestHelper h) {
        var book=new InventariumRequestBook(()->{});var stack=new ItemStack(Items.IRON_INGOT);var key=new RequestedItem(stack);var destination=GlobalPos.of(Level.OVERWORLD,BlockPos.ZERO);
        book.request(stack,destination,50);var claim=book.claim(key,destination,32).orElseThrow();book.cancel(key,destination,50);
        var restored=new InventariumRequestBook(()->{});restored.load(book.save());
        assertEquals(1,restored.claims().size());assertEquals(0,restored.deliveryLimit(claim.id()));
        restored.reconcile(Set.of());assertTrue(restored.claims().isEmpty());
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void carrierOwnershipSurvivesReloadAndCannotBeStolen(net.minecraft.gametest.framework.GameTestHelper h) {
        var book=new InventariumRequestBook(()->{});var stack=new ItemStack(Items.IRON_INGOT);
        var destination=GlobalPos.of(Level.OVERWORLD,BlockPos.ZERO);book.request(stack,destination,10);
        var claim=book.claim(new RequestedItem(stack),destination,10).orElseThrow();UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        assertTrue(book.bind(claim.id(),first));assertFalse(book.bind(claim.id(),second));
        var restored=new InventariumRequestBook(()->{});restored.load(book.save());
        assertEquals(book.identity(),restored.identity());assertTrue(restored.owned(claim.id(),first));assertFalse(restored.owned(claim.id(),second));
        restored.fail(claim.id());assertFalse(restored.owned(claim.id(),first));assertEquals(10,restored.claim(new RequestedItem(stack),destination,10).orElseThrow().amount());
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void deferredDeathNotificationsPersistAndDeduplicate(net.minecraft.gametest.framework.GameTestHelper h) {
        var cleanup=new CourierCleanupData();UUID book=UUID.randomUUID(),carrier=UUID.randomUUID();
        cleanup.add(new BlockPos(20,64,-30),book,7,carrier);cleanup.add(new BlockPos(20,64,-30),book,7,carrier);
        var saved=cleanup.save(new net.minecraft.nbt.CompoundTag());assertEquals(1,saved.getList("DeadClaims",10).size());
        assertEquals(saved,CourierCleanupData.load(saved).save(new net.minecraft.nbt.CompoundTag()));
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void removingOneTerminalCancelsOnlyItsDemandAndPreservesInFlightOwnership(net.minecraft.gametest.framework.GameTestHelper h) {
        var book=new InventariumRequestBook(()->{});var stack=new ItemStack(Items.IRON_INGOT);var item=new RequestedItem(stack);
        var first=GlobalPos.of(Level.OVERWORLD,BlockPos.ZERO);var second=GlobalPos.of(Level.OVERWORLD,new BlockPos(1,0,0));
        book.request(stack,first,10);book.request(stack,second,20);var claim=book.claim(item,first,8).orElseThrow();UUID carrier=UUID.randomUUID();book.bind(claim.id(),carrier);
        book.cancelDestination(first);
        assertEquals(1,book.requests().size());assertEquals(second,book.requests().get(0).destination());assertEquals(20,book.requests().get(0).amount());
        assertEquals(0,book.deliveryLimit(claim.id()));assertTrue(book.owned(claim.id(),carrier));
    h.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template="empty",templateNamespace="automagy") public static void deferredTerminalRemovalPersistsAndKeepsCoreIncarnationsSeparate(net.minecraft.gametest.framework.GameTestHelper h) {
        var cleanup=new CourierCleanupData();var core=new BlockPos(20,64,-30);var terminal=new BlockPos(40,64,-30);var first=UUID.randomUUID();
        cleanup.removeTerminal(core,first,terminal);cleanup.removeTerminal(core,first,terminal);cleanup.removeTerminal(core,UUID.randomUUID(),terminal);
        var saved=cleanup.save(new net.minecraft.nbt.CompoundTag());assertEquals(2,saved.getList("RemovedTerminals",10).size());
        assertEquals(saved,CourierCleanupData.load(saved).save(new net.minecraft.nbt.CompoundTag()));
    h.succeed();
    }
}
