package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class GolemSystemServerTests {
    private static FakePlayer player(GameTestHelper test,BlockPos pos,String name) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),name));
        player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5); return player;
    }
    private static GolemEntity golem(GameTestHelper test,BlockPos pos,GolemMaterial material,GolemCoreType core) {
        var golem=TCEntities.GOLEM.get().create(test.getLevel());
        golem.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var config=new GolemConfiguration(material,true); config.installCore(core);
        golem.state().configure(config); golem.state().home(pos,Direction.UP); golem.state().owner("GolemBatch");
        golem.refreshConfiguration(); golem.setHealth(golem.getMaxHealth()); return golem;
    }
    @GameTest(template="wand_test_empty")
    public static void golemConfigurationPersistenceAndMenus(GameTestHelper test) {
        var pos=test.absolutePos(new BlockPos(3,3,3)); var player=player(test,pos,"GolemBatch");
        for (var material:GolemMaterial.values()) for (var core:GolemCoreType.values()) {
            var original=golem(test,pos,material,core);
            original.configuration().installUpgrade(2); original.refreshConfiguration(); original.configuration().toggle(7,true);
            original.carried(new ItemStack(Items.DIAMOND,2)); original.filters().sample(0,new ItemStack(Items.COAL,3));
            original.filters().color(0,11); original.setHealth(original.getMaxHealth()-1);
            var data=new CompoundTag(); original.saveWithoutId(data);
            var restored=TCEntities.GOLEM.get().create(test.getLevel()); restored.load(data);
            test.assertTrue(restored.configuration().material()==material && restored.configuration().core()==core,"Material and core survive entity NBT");
            test.assertTrue(restored.getHealth()==original.getHealth(),"Material health is not truncated by default attributes");
            test.assertTrue(restored.carried().getCount()==2 && restored.filters().sample(0).getCount()==3 && restored.filters().color(0)==11,"Cargo and ghost configuration remain separate");
            test.assertTrue(restored.configuration().toggle(7),"Filter toggles persist");
        }
        var entity=golem(test,pos,GolemMaterial.THAUMIUM,GolemCoreType.FILL);
        var menu=new GolemMenu(TCArcane.GOLEM_MENU.get(),1,player.getInventory(),entity); player.containerMenu=menu;
        menu.setCarried(new ItemStack(Items.DIAMOND,8)); menu.clicked(0,0,ClickType.PICKUP,player);
        test.assertTrue(menu.getCarried().getCount()==8 && entity.filters().sample(0).getCount()==8,"Placing a ghost sample consumes no real diamonds");
        menu.setCarried(ItemStack.EMPTY); menu.clicked(0,0,ClickType.PICKUP,player);
        test.assertTrue(menu.getCarried().isEmpty() && entity.filters().sample(0).getCount()==7,"Ghost decrement cannot extract items");
        menu.clicked(0,0,ClickType.QUICK_MOVE,player);
        test.assertTrue(entity.filters().sample(0).isEmpty() && menu.getCarried().isEmpty(),"Ghost shift-clear creates no items");
        menu.removed(player); test.assertTrue(entity.workEnabled(),"Closing configuration resumes work");
        pass(test,"configuration-persistence-ghost-menu");
    }
    @GameTest(template="wand_test_empty")
    public static void golemGatherAndDepositConservation(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3));
        level.setBlock(pos.below(),TCArcane.HUNGRY_CHEST.get().defaultBlockState(),3);
        var chest=(HungryChestBlockEntity)level.getBlockEntity(pos.below());
        var entity=golem(test,pos,GolemMaterial.WOOD,GolemCoreType.GATHER);
        var item=new ItemEntity(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,new ItemStack(Items.DIAMOND,9)); item.setNoPickUpDelay(); level.addFreshEntity(item);
        var gather=new GolemGatherGoal(entity); test.assertTrue(gather.canUse(),"Gather selects nearby real drop"); gather.start(); gather.tick(); gather.stop();
        test.assertTrue(entity.carried().getCount()==4 && item.getItem().getCount()==5,"Wood carry limit conserves remaining drops");
        var deposit=new GolemHomePlaceGoal(entity); test.assertTrue(deposit.canUse(),"Home accepts physical cargo"); deposit.start(); deposit.stop();
        test.assertTrue(entity.carried().isEmpty() && chest.getItem(0).getCount()==4 && item.getItem().getCount()==5,"Deposit conserves all nine diamonds");
        item.discard(); pass(test,"gather-deposit-conservation");
    }
    @GameTest(template="wand_test_empty")
    public static void travelingTrunkProtectionAndPortableInventory(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3)); var owner=player(test,pos,"TrunkOwner"); var stranger=player(test,pos,"TrunkStranger");
        var trunk=TCEntities.TRAVELING_TRUNK.get().create(level); trunk.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0); trunk.ownerName("TrunkOwner"); trunk.upgrade(3);
        test.assertTrue(trunk.mayOpen(owner) && !trunk.mayOpen(stranger),"Water upgrade protects owner access");
        test.assertTrue(!trunk.hurt(level.damageSources().generic(),10) && trunk.getHealth()==75,"Water trunk ignores damage");
        trunk.setHealth(70); stranger.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,2));
        trunk.mobInteract(stranger,InteractionHand.MAIN_HAND);
        test.assertTrue(trunk.getHealth()==74 && stranger.getMainHandItem().getCount()==1,"Stranger may feed before owner check");
        trunk.upgrade(4); trunk.inventory.setItem(35,new ItemStack(Items.DIAMOND,17)); trunk.inventory.setItem(0,new ItemStack(Items.COAL,6));
        var data=new CompoundTag(); trunk.saveWithoutId(data); var restored=TCEntities.TRAVELING_TRUNK.get().create(level); restored.load(data);
        test.assertTrue(restored.inventory.getItem(35).getCount()==17 && restored.rows()==3,"Hidden fourth storage row survives NBT without earth upgrade");
        level.addFreshEntity(trunk); TCItems.GOLEM_BELL.get().onLeftClickEntity(new ItemStack(TCItems.GOLEM_BELL.get()),owner,trunk);
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3));
        var portable=drops.stream().filter(item->item.getItem().is(TCItems.TRAVELING_TRUNK.get())).findFirst().orElseThrow();
        restored.loadInventory(portable.getItem().getTag().getList("inventory",10));
        test.assertTrue(trunk.isRemoved() && restored.inventory.getItem(35).getCount()==17 && drops.size()==1,"Order recovery packages all inventory without loose duplicate drops");
        portable.discard(); pass(test,"trunk-protection-portable-inventory");
    }
    @GameTest(template="wand_test_empty")
    public static void hungryChestPartialPickupAndPersistence(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3)); level.setBlock(pos,TCArcane.HUNGRY_CHEST.get().defaultBlockState(),3);
        var chest=(HungryChestBlockEntity)level.getBlockEntity(pos);
        for (int slot=0;slot<27;slot++) chest.setItem(slot,new ItemStack(Items.COBBLESTONE,64)); chest.setItem(26,new ItemStack(Items.DIAMOND,62));
        var item=new ItemEntity(level,pos.getX()+.5,pos.getY()+.95,pos.getZ()+.5,new ItemStack(Items.DIAMOND,5)); level.addFreshEntity(item);
        TCArcane.HUNGRY_CHEST.get().entityInside(chest.getBlockState(),level,pos,item);
        test.assertTrue(chest.getItem(26).getCount()==64 && item.getItem().getCount()==3,"Partial pickup neither loses nor duplicates excess");
        var restored=new HungryChestBlockEntity(pos,chest.getBlockState()); restored.load(chest.saveWithoutMetadata());
        test.assertTrue(restored.getItem(26).getCount()==64 && restored.getContainerSize()==27,"Chest inventory persists");
        test.assertTrue(TCArcane.HUNGRY_CHEST.get().getAnalogOutputSignal(chest.getBlockState(),level,pos)==15,"Full chest drives full comparator output");
        chest.triggerEvent(2,2); HungryChestBlockEntity.tick(level,pos,chest.getBlockState(),chest);
        test.assertTrue(chest.previousLid==.2f && chest.lid==.1f,"Eating event animates and then closes lid");
        item.discard(); pass(test,"hungry-chest-partial-pickup-persistence");
    }
    @GameTest(template="wand_test_empty")
    public static void advancedGolemInfusionPreservesPortableState(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3));
        for (var material:GolemMaterial.values()) {
            String path="golem_"+material.name().toLowerCase(java.util.Locale.ROOT);
            var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:"+path));
            var input=new ItemStack(item); input.setHoverName(net.minecraft.network.chat.Component.literal("Configured golem"));
            var tag=input.getOrCreateTag(); tag.putByte("core",(byte)GolemCoreType.GATHER.ordinal()); tag.putByteArray("upgrades",new byte[]{2}); tag.putString("deco","HG");
            var markers=new net.minecraft.nbt.ListTag(); markers.add(new GolemMarker(pos,level.dimension().location(),Direction.NORTH,(byte)3).save()); tag.put("markers",markers);
            var samples=new net.minecraft.nbt.ListTag(); var sample=new ItemStack(Items.COAL,7).save(new CompoundTag()); sample.putByte("Slot",(byte)0); samples.add(sample); tag.put("Inventory",samples);
            var ingredients=java.util.List.of(new ItemStack(Items.REDSTONE),new ItemStack(Items.GLOWSTONE_DUST),new ItemStack(Items.GUNPOWDER),new ItemStack(TCItems.ESSENTIA_JAR.get()),new ItemStack(TCItems.ZOMBIE_BRAIN.get()));
            var allowed=new InfusionRecipe.Input(input,ingredients,key->key.equals("ADVANCEDGOLEM"));
            var recipe=InfusionRecipe.find(level,allowed).orElseThrow();
            test.assertTrue(recipe.getId().getPath().endsWith(path+"_advanced"),"Every material selects its advanced infusion");
            test.assertTrue(!recipe.matches(new InfusionRecipe.Input(input,ingredients,key->false),level),"Advanced infusion requires research");
            var result=recipe.assemble(allowed,level.registryAccess()); var expected=tag.copy(); expected.putByte("advanced",(byte)1);
            test.assertTrue(result.is(item) && result.getTag().equals(expected) && !input.getTag().contains("advanced"),"Infusion patches a copy without losing portable state or mutating input");
            var entity=TCEntities.GOLEM.get().create(level); GolemPlacerItem.restore(entity,result,material,pos,Direction.UP,"GolemBatch");
            test.assertTrue(entity.configuration().advanced() && entity.configuration().core()==GolemCoreType.GATHER && entity.configuration().upgrades(2)==1,"Placing the infused golem restores core and upgrade");
            test.assertTrue(entity.getName().getString().equals("Configured golem") && entity.markers().save().size()==1 && entity.filters().sample(0).getCount()==7,"Name, marker and ghost sample survive infusion and placement");
        }
        pass(test,"advanced-infusion-portable-state");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Golem system test passed: {}",name); test.succeed(); }
    @GameTest(template="wand_test_empty")
    public static void golemEssentiaTransferAndPriority(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3));
        for (var jarPos:java.util.List.of(pos.below(),pos.east(),pos.east(2))) level.setBlock(jarPos,TCArcane.ESSENTIA_JAR.get().defaultBlockState(),3);
        var source=(EssentiaJarBlockEntity)level.getBlockEntity(pos.below());
        var destination=(EssentiaJarBlockEntity)level.getBlockEntity(pos.east());
        var empty=(EssentiaJarBlockEntity)level.getBlockEntity(pos.east(2));
        var air=tc4revival.aspect.TCAspects.catalog().require("aer"); source.addToContainer(air,12);
        var labeled=new ItemStack(TCItems.FILLED_ESSENTIA_JAR.get()); labeled.getOrCreateTag().putString("AspectFilter","aer"); destination.readItem(labeled); destination.addToContainer(air,60);
        var entity=golem(test,pos,GolemMaterial.WOOD,GolemCoreType.ESSENTIA);
        entity.markers().replace(java.util.List.of(new GolemMarker(pos.east(2),level.dimension().location(),Direction.UP,(byte)-1)));
        var gather=new GolemEssentiaGatherGoal(entity); test.assertTrue(gather.canUse(),"Essentia core finds its home jar"); gather.start();
        test.assertTrue(entity.essentiaAmount()==4 && source.amount()==8,"Gather respects material capacity and source conservation");
        test.assertTrue(pos.east().equals(GolemEssentiaTargets.find(entity)),"Connected labeled partial jar precedes marked empty jar");
        var deposit=new GolemEssentiaEmptyGoal(entity); test.assertTrue(deposit.canUse(),"Nearby preferred jar is reachable for deposit"); deposit.start();
        test.assertTrue(entity.essentiaAmount()==0 && entity.essentia()==null && destination.amount()==64 && empty.amount()==0,"Deposit fills preferred jar without duplicating essentia");
        pass(test,"essentia-transfer-priority");
    }
    @GameTest(template="wand_test_empty")
    public static void golemHarvestAndStalePumpSources(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3));
        level.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(),3);
        var wheat=net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE,7); level.setBlock(pos,wheat,3);
        level.setBlock(pos.above(2),net.minecraft.world.level.block.Blocks.GLOWSTONE.defaultBlockState(),3);
        test.runAfterDelay(5,()->{
        test.assertTrue(level.getRawBrightness(pos,0)>=9,"Harvest fixture has propagated crop lighting");
        var entity=golem(test,pos,GolemMaterial.WOOD,GolemCoreType.HARVEST); entity.configuration().installUpgrade(4); entity.refreshConfiguration();
        var seed=new ItemEntity(level,pos.getX()+.5,pos.getY(),pos.getZ()+.5,new ItemStack(Items.WHEAT_SEEDS)); level.addFreshEntity(seed);
        var harvest=new GolemHarvestGoal(entity); boolean selected=false;
        for (int scan=0;scan<200 && !selected;scan++) selected=harvest.canUse();
        test.assertTrue(selected,"Harvest scan locates mature wheat"); harvest.start(); for (int tick=0;tick<11;tick++) harvest.tick(); harvest.stop();
        var planted=level.getBlockState(pos);
        test.assertTrue(planted.is(net.minecraft.world.level.block.Blocks.WHEAT) && planted.getValue(net.minecraft.world.level.block.CropBlock.AGE)==0,
                "Order harvest replants wheat at age zero; actual="+planted+" soil="+level.getBlockState(pos.below())
                +" light="+level.getRawBrightness(pos,0)+" survives="+wheat.canSurvive(level,pos)+" seed="+seed.getItem());
        var origin=pos.offset(0,2,0); var far=origin.east();
        level.setBlock(origin,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2); level.setBlock(far,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
        var queue=new GolemPumpQueue(); var fluid=net.minecraft.world.level.material.Fluids.WATER;
        test.assertTrue(far.equals(queue.next(level,origin,fluid,4)),"Pump visits farther source first");
        test.assertTrue(GolemLiquidSources.takeSource(level,far,fluid) && level.getFluidState(far).isEmpty(),"Pumping removes the actual fluid source");
        test.assertTrue(!GolemLiquidSources.takeSource(level,far,fluid),"A consumed source cannot yield a second bucket");
        test.assertTrue(GolemLiquidSources.takeSource(level,origin,fluid),"Remaining source can be pumped once");
        test.assertTrue(queue.next(level,origin,fluid,4)==null,"Queued duplicate coordinates cannot pump fluid after both sources disappear");
        pass(test,"harvest-replant-stale-pump");
        });
    }
    @GameTest(template="wand_test_empty")
    public static void golemUseCoreEmptyHandAndGuardRules(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3)); var target=pos.east();
        level.setBlock(target.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        level.setBlock(target,net.minecraft.world.level.block.Blocks.LEVER.defaultBlockState().setValue(net.minecraft.world.level.block.LeverBlock.FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR),3);
        var entity=golem(test,pos,GolemMaterial.WOOD,GolemCoreType.USE);
        entity.markers().replace(java.util.List.of(new GolemMarker(target,level.dimension().location(),Direction.UP,(byte)-1)));
        var use=new GolemUseItemGoal(entity); entity.tickCount=10;
        test.assertTrue(use.canUse(),"Use core can interact empty-handed without a home inventory"); use.start(); use.tick();
        test.assertTrue(level.getBlockState(target).getValue(net.minecraft.world.level.block.LeverBlock.POWERED) && entity.carried().isEmpty(),"Empty-hand lever interaction completes without cargo dereference failure");
        var guard=golem(test,pos,GolemMaterial.WOOD,GolemCoreType.GUARD);
        var cow=net.minecraft.world.entity.EntityType.COW.create(level); cow.setPos(entity.position());
        var creeper=net.minecraft.world.entity.EntityType.CREEPER.create(level); creeper.setPos(entity.position());
        var owner=player(test,pos,"GolemBatch");
        test.assertTrue(!GolemCombatTargets.valid(guard,cow) && !GolemCombatTargets.valid(guard,creeper),"Unupgraded guard excludes animals and creepers");
        guard.configuration().installUpgrade(4);
        test.assertTrue(GolemCombatTargets.valid(guard,cow) && GolemCombatTargets.valid(guard,creeper) && !GolemCombatTargets.valid(guard,owner),"Order enables optional target classes while still protecting owner");
        guard.configuration().toggle(4,true); test.assertTrue(!GolemCombatTargets.valid(guard,creeper),"Creeper toggle suppresses target");
        var tag=new CompoundTag(); tag.putByte("GolemType",(byte)GolemMaterial.THAUMIUM.ordinal()); tag.putBoolean("advanced",true); tag.putByteArray("upgrades",new byte[]{-1,2,3});
        var gap=GolemConfiguration.load(tag);
        test.assertTrue(gap.displayedUpgrade(0)==-1 && gap.displayedUpgrade(1)==2 && gap.displayedUpgrade(2)==3 && gap.upgrades(2)==1,"Empty slots do not mask subsequent upgrades");
        pass(test,"use-core-guard-rules-upgrade-gap");
    }
}
