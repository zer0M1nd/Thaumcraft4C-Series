package tc4revival.content;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class ItemMirrorServerTests {
    private static ItemMirrorBlockEntity mirror(GameTestHelper test,BlockPos relative,Direction facing) {
        test.setBlock(relative.relative(facing.getOpposite()),Blocks.STONE);
        test.setBlock(relative,TCArcane.ITEM_MIRROR.get().defaultBlockState().setValue(ItemMirrorBlock.FACING,facing));
        return (ItemMirrorBlockEntity)test.getBlockEntity(relative);
    }
    private static void pair(GameTestHelper test,ItemMirrorBlockEntity a,ItemMirrorBlockEntity b) {
        a.remember(GlobalPos.of(b.getLevel().dimension(),b.getBlockPos()));
        test.assertTrue(a.restoreLink() && a.valid(false) && b.valid(false),"Both mirrors establish reciprocal coordinates");
    }
    private static int queued(ItemMirrorBlockEntity mirror) { return mirror.queuedItems().stream().mapToInt(ItemStack::getCount).sum(); }
    private static ItemEntity item(ServerLevel world,BlockPos pos,ItemStack stack) {
        var entity=new ItemEntity(world,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,stack); entity.setNoGravity(true); world.addFreshEntity(entity); return entity;
    }
    private static FakePlayer player(GameTestHelper test) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(java.util.UUID.randomUUID(),"MirrorTest"));
        var pos=test.absolutePos(new BlockPos(8,8,8)); player.setPos(pos.getX(),pos.getY(),pos.getZ()); return player;
    }
    @GameTest(template="tree_test_empty")
    public static void itemMirrorQueueCooldownAndSavedLink(GameTestHelper test) {
        var world=test.getLevel(); var a=mirror(test,new BlockPos(5,8,8),Direction.EAST); var b=mirror(test,new BlockPos(20,8,8),Direction.EAST); pair(test,a,b);
        var payload=new ItemStack(Items.DIAMOND,7); payload.setHoverName(net.minecraft.network.chat.Component.literal("mirror cargo"));
        var source=item(world,a.getBlockPos(),payload);
        a.getBlockState().entityInside(world,a.getBlockPos(),source);
        test.assertTrue(!source.isAlive() && queued(b)==7 && a.instability()==7 && b.instability()==0,"Collision transfers the complete named stack into the distant queue");
        var saved=b.saveWithoutMetadata(); var restored=new ItemMirrorBlockEntity(b.getBlockPos(),b.getBlockState()); restored.load(saved);
        test.assertTrue(queued(restored)==7 && restored.destination().equals(b.destination()) && restored.linked(),"Queue, item tags and link survive persistence");
        test.assertTrue(restored.queuedItems().get(0).getHoverName().getString().equals("mirror cargo") && !b.getUpdateTag().contains("Items"),"Network updates do not expose or duplicate the server queue");
        for (int tick=0;tick<23;tick++) b.tick();
        var emitted=world.getEntitiesOfClass(ItemEntity.class,new AABB(b.getBlockPos()).inflate(1));
        test.assertTrue(queued(b)==5 && emitted.stream().mapToInt(e->e.getItem().getCount()).sum()==2 && b.instability()==2,"Only one item is emitted per eligible tick after the twenty-tick warmup");
        for (var entity:emitted) {
            test.assertTrue(entity.getPortalCooldown()==20 && Math.abs(entity.getX()-(b.getBlockPos().getX()+.2))<1e-6 && Math.abs(entity.getDeltaMovement().x-.15)<1e-6,"Legacy ejection offset, velocity and portal cooldown are retained");
            b.getBlockState().entityInside(world,b.getBlockPos(),entity);
            test.assertTrue(entity.isAlive() && queued(a)==0,"Emitted items cannot immediately bounce back through the mirror"); entity.discard();
        }
        var drops=Block.getDrops(a.getBlockState(),world,a.getBlockPos(),a);
        test.assertTrue(ItemMirrorItem.legacyMetadata(drops.get(0))==1 && b.getBlockPos().equals(ItemMirrorItem.destination(drops.get(0)).pos()) && b.valid(false),"Drop preview keeps target and leaves the live link intact");
        var position=a.getBlockPos(); world.setBlockAndUpdate(position,Blocks.AIR.defaultBlockState());
        test.assertTrue(!b.linked(),"Removing one mirror disconnects the partner");
        world.setBlockAndUpdate(position,TCArcane.ITEM_MIRROR.get().defaultBlockState().setValue(ItemMirrorBlock.FACING,Direction.EAST));
        var replacement=(ItemMirrorBlockEntity)world.getBlockEntity(position); replacement.remember(ItemMirrorItem.destination(drops.get(0)));
        test.assertTrue(replacement.restoreLink() && b.valid(false),"A carried mirror can restore its old pair");
        pass(test,"queue-cooldown-save-drop-reconnect");
    }
    @GameTest(template="tree_test_empty")
    public static void itemMirrorAutomationInstabilityAndCancellation(GameTestHelper test) {
        var world=test.getLevel(); var a=mirror(test,new BlockPos(5,8,8),Direction.EAST); var b=mirror(test,new BlockPos(20,8,8),Direction.UP); pair(test,a,b);
        var handler=a.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new); var input=new ItemStack(Items.GOLD_INGOT,64);
        test.assertTrue(handler.insertItem(0,input,true).isEmpty() && queued(b)==0 && a.instability()==0,"Automation simulation has no side effects");
        test.assertTrue(handler.insertItem(0,input,false).isEmpty() && input.getCount()==64 && queued(b)==64 && handler.extractItem(0,64,false).isEmpty(),"Automation copies only accepted items into the private remote queue");
        a.remember(GlobalPos.of(world.dimension(),b.getBlockPos()));
        test.assertTrue(!a.linked() && handler.insertItem(0,new ItemStack(Items.IRON_INGOT,3),false).isEmpty() && queued(b)==67,"Legacy automation accepts a destination without requiring reciprocal pairing");
        var data=b.saveWithoutMetadata(); data.putInt("instability",100); b.load(data);
        java.util.function.Consumer<net.minecraftforge.event.entity.EntityJoinLevelEvent> reject=event->{
            if (event.getEntity() instanceof ItemEntity && event.getLevel()==world && event.getEntity().position().distanceToSqr(Vec3.atCenterOf(b.getBlockPos()))<4) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(reject);
        try {
            for (int tick=0;tick<25;tick++) b.tick();
            test.assertTrue(queued(b)==67 && b.instability()==98,"Rejected output keeps the entire queue; passive instability falls once each twenty ticks");
        } finally { MinecraftForge.EVENT_BUS.unregister(reject); }
        var nodePos=a.getBlockPos().east(3); world.setBlockAndUpdate(nodePos,TCArcane.ENERGIZED_NODE.get().defaultBlockState());
        var node=(EnergizedNodeBlockEntity)world.getBlockEntity(nodePos); node.configure(new tc4revival.aspect.AspectAmounts().add(tc4revival.aspect.TCAspects.catalog().require("ordo"),25),AuraNodeType.NORMAL,null,"mirror-order"); node.tick();
        int before=a.instability(),remote=b.instability(); a.tick();
        test.assertTrue(a.instability()==before-2 && b.instability()==remote-1,"Order CV drains one instability from each end, in addition to the local passive decay");
        world.setBlockAndUpdate(nodePos,Blocks.AIR.defaultBlockState());
        var oldOptional=a.getCapability(ForgeCapabilities.ITEM_HANDLER); a.invalidateCaps(); test.assertTrue(!oldOptional.isPresent(),"Unload invalidates the item capability"); a.reviveCaps();
        test.assertTrue(a.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(),"Reload exposes a fresh capability for the same queue");
        pass(test,"automation-instability-order-cancel");
    }
    @GameTest(template="tree_test_empty")
    public static void handMirrorMenuFailureAndConservation(GameTestHelper test) {
        var world=test.getLevel(); var target=mirror(test,new BlockPos(20,8,8),Direction.NORTH); var player=player(test); var hand=new ItemStack(TCItems.HAND_MIRROR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,hand);
        var context=new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(target.getBlockPos()),Direction.NORTH,target.getBlockPos(),false));
        TCItems.HAND_MIRROR.get().onItemUseFirst(hand,context);
        test.assertTrue(target.getBlockPos().equals(ItemMirrorItem.destination(hand).pos()) && TCItems.HAND_MIRROR.get().isFoil(hand),"Using a hand mirror binds its target and adds its foil state");
        var menu=new HandMirrorMenu(1,player.getInventory(),InteractionHand.MAIN_HAND); player.containerMenu=menu;
        player.getInventory().setItem(9,new ItemStack(Items.EMERALD,11)); menu.quickMoveStack(player,1);
        var delivered=world.getEntitiesOfClass(ItemEntity.class,new AABB(target.getBlockPos()).inflate(1));
        test.assertTrue(delivered.stream().mapToInt(e->e.getItem().getCount()).sum()==11 && player.getInventory().getItem(9).isEmpty() && menu.getSlot(0).getItem().isEmpty() && queued(target)==0 && target.instability()==0,"Shift transfer sends the whole stack once and bypasses instability and the queue"); delivered.forEach(ItemEntity::discard);
        player.getInventory().setItem(10,new ItemStack(Items.IRON_INGOT,6));
        java.util.function.Consumer<net.minecraftforge.event.entity.EntityJoinLevelEvent> reject=event->{ if (event.getEntity() instanceof ItemEntity && event.getEntity().position().distanceToSqr(Vec3.atCenterOf(target.getBlockPos()))<4) event.setCanceled(true); };
        MinecraftForge.EVENT_BUS.addListener(reject);
        try { menu.quickMoveStack(player,2); test.assertTrue(menu.getSlot(0).getItem().getCount()==6 && player.getInventory().getItem(10).isEmpty(),"Rejected entity spawn leaves the input available to recover"); }
        finally { MinecraftForge.EVENT_BUS.unregister(reject); }
        menu.removed(player); menu.removed(player);
        var refunds=world.getEntitiesOfClass(ItemEntity.class,player.getBoundingBox().inflate(3));
        test.assertTrue(refunds.stream().filter(e->e.getItem().is(Items.IRON_INGOT)).mapToInt(e->e.getItem().getCount()).sum()==6,"Closing refunds the failed input once"); refunds.forEach(ItemEntity::discard);
        world.setBlockAndUpdate(target.getBlockPos(),Blocks.AIR.defaultBlockState());
        var payload=new ItemStack(Items.DIAMOND,3);
        test.assertTrue(!HandMirrorItem.transport(hand,payload,player) && !hand.hasTag() && payload.getCount()==3,"Missing target breaks the binding without consuming caller inventory");
        player.containerMenu=player.inventoryMenu;
        pass(test,"hand-binding-menu-failure-conservation");
    }
    @GameTest(template="tree_test_empty",timeoutTicks=200)
    public static void itemMirrorCrossDimensionAndRecipes(GameTestHelper test) {
        var world=test.getLevel(); var other=world.getServer().getLevel(Level.NETHER); test.assertTrue(other!=null,"Nether is available");
        var local=mirror(test,new BlockPos(8,8,8),Direction.WEST);
        var pos=new BlockPos(local.getBlockPos().getX(),Math.min(other.getMaxBuildHeight()-2,other.getMinBuildHeight()+160),local.getBlockPos().getZ()); var support=pos.below();
        var old=other.getBlockState(pos); var oldSupport=other.getBlockState(support);
        var oldEntity=other.getBlockEntity(pos); var oldNbt=oldEntity==null?null:oldEntity.saveWithFullMetadata();
        var oldSupportEntity=other.getBlockEntity(support); var oldSupportNbt=oldSupportEntity==null?null:oldSupportEntity.saveWithFullMetadata();
        var chunk=new net.minecraft.world.level.ChunkPos(pos); boolean previouslyForced=other.getForcedChunks().contains(chunk.toLong());
        Runnable cleanup=()->{
            other.setBlockAndUpdate(pos,old);
            if (oldNbt!=null && other.getBlockEntity(pos)!=null) { other.getBlockEntity(pos).load(oldNbt); other.getBlockEntity(pos).setChanged(); }
            other.setBlockAndUpdate(support,oldSupport);
            if (oldSupportNbt!=null && other.getBlockEntity(support)!=null) { other.getBlockEntity(support).load(oldSupportNbt); other.getBlockEntity(support).setChanged(); }
            if (!previouslyForced) other.setChunkForced(chunk.x,chunk.z,false);
        };
        try {
            test.assertTrue(other.setBlockAndUpdate(support,Blocks.STONE.defaultBlockState()) && other.setBlockAndUpdate(pos,TCArcane.ITEM_MIRROR.get().defaultBlockState()),"The cross-dimension fixture must be placed inside the destination build height");
            var remote=(ItemMirrorBlockEntity)other.getBlockEntity(pos); pair(test,local,remote);
            var cargo=item(world,local.getBlockPos(),new ItemStack(Items.QUARTZ,4));
            test.assertTrue(local.transport(cargo) && queued(remote)==4 && remote.destination().dimension().equals(world.dimension()),"Paired transport reaches the other dimension and stores a reciprocal dimension key");
            var player=player(test); var hand=new ItemStack(TCItems.HAND_MIRROR.get()); ItemMirrorItem.writeDestination(hand.getOrCreateTag(),GlobalPos.of(other.dimension(),pos),"Nether");
            test.assertTrue(HandMirrorItem.transport(hand,new ItemStack(Items.REDSTONE,5),player),"Hand mirror accepts direct output before the remote chunk becomes entity-ticking");
            other.setChunkForced(chunk.x,chunk.z,true);
            test.startSequence().thenWaitUntil(()->test.assertTrue(other.isPositionEntityTicking(pos),"Waiting for the remote entity section to become accessible"))
                    .thenExecute(()->{
                        try {
                            var emitted=other.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4));
                            int direct=emitted.stream().filter(e->e.getItem().is(Items.REDSTONE)).mapToInt(e->e.getItem().getCount()).sum();
                            int released=emitted.stream().filter(e->e.getItem().is(Items.QUARTZ)).mapToInt(e->e.getItem().getCount()).sum();
                            // Block entities may tick while the remote entity section is becoming accessible.
                            test.assertTrue(direct==5 && released+queued(remote)==4 && remote.queuedItems().stream().allMatch(s->s.is(Items.QUARTZ))
                                    && emitted.stream().allMatch(e->e.getItem().is(Items.REDSTONE)||e.getItem().is(Items.QUARTZ)),"Activating the remote chunk conserves both direct and queued cargo: pos="+pos+", queued="+queued(remote)+", emitted="+emitted.stream().map(e->e.getItem()+" at "+e.position()).toList());
                            emitted.forEach(ItemEntity::discard); verifyRecipes(test,local);
                        } finally { cleanup.run(); }
                        pass(test,"cross-dimension-recipes-config");
                    });
        } catch (RuntimeException failure) { cleanup.run(); throw failure; }
    }
    private static void verifyRecipes(GameTestHelper test,ItemMirrorBlockEntity local) {
        var world=test.getLevel();
        var input=new InfusionRecipe.Input(new ItemStack(TCItems.material(MaterialType.MIRROR_GLASS)),java.util.List.of(new ItemStack(Items.GOLD_INGOT),new ItemStack(Items.GOLD_INGOT),new ItemStack(Items.GOLD_INGOT),new ItemStack(Items.ENDER_PEARL)),key->true);
        var recipe=InfusionRecipe.find(world,input).orElseThrow(); test.assertTrue(recipe.assemble(input,world.registryAccess()).is(TCItems.ITEM_MIRROR.get()),"Registered infusion produces an item mirror");
        var handInput=new InfusionRecipe.Input(new ItemStack(TCItems.ITEM_MIRROR.get()),java.util.List.of(new ItemStack(Items.STICK),new ItemStack(Items.COMPASS),new ItemStack(Items.MAP)),key->true);
        var handRecipe=InfusionRecipe.find(world,handInput).orElseThrow(); test.assertTrue(handRecipe.assemble(handInput,world.registryAccess()).is(TCItems.HAND_MIRROR.get()),"Registered infusion produces a hand mirror");
        var linkedInput=new InfusionRecipe.Input(ItemMirrorItem.linkedStack(GlobalPos.of(world.dimension(),local.getBlockPos()),"Overworld"),java.util.List.of(handInput.getItem(1),handInput.getItem(2),handInput.getItem(3)),key->true);
        test.assertTrue(!handRecipe.matches(linkedInput,world),"Linked metadata cannot replace the unlinked mirror in hand-mirror crafting");
        boolean allowed=tc4revival.config.TCConfig.SERVER.allowMirrors.get();
        try { tc4revival.config.TCConfig.SERVER.allowMirrors.set(false); test.assertTrue(!recipe.matches(input,world) && !handRecipe.matches(handInput,world),"Mirror config disables both acquisition recipes"); }
        finally { tc4revival.config.TCConfig.SERVER.allowMirrors.set(allowed); }
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Item mirror test passed: {}",name); test.succeed(); }
}
