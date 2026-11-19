package conciliummodern;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumicconcilium") @PrefixGameTestTemplate(false)
public final class GolemBehaviourServerTests {
    @GameTest(template="empty",timeoutTicks=80)
    public static void extensionSaveAndIndependentCommands(GameTestHelper test) {
        var player=player(test,"ConciliumOwner");
        var first=golem(test,ConciliumGolems.VALET,4,2,4,player.getGameProfile().getName());
        var second=golem(test,ConciliumGolems.VALET,8,2,4,player.getGameProfile().getName());
        first.configuration().toggle(2,true);first.carried(new ItemStack(Items.BUCKET,2));
        var paper=new ItemStack(Items.PAPER);paper.setHoverName(Component.literal("Open"));first.filters().sample(0,paper);first.filters().color(0,6);
        var data=new CompoundTag();data.putString("Command","Open");data.putBoolean("Pending",true);ConciliumGolems.data(first,data);
        data.putString("Command","Changed outside");
        test.assertTrue(ConciliumGolems.data(first).getString("Command").equals("Open")&&ConciliumGolems.data(second).isEmpty(),"Command storage copies inputs and is per golem");
        var saved=new CompoundTag();first.saveWithoutId(saved);var restored=TCEntities.GOLEM.get().create(test.getLevel());restored.load(saved);
        test.assertTrue(ConciliumGolems.VALET.equals(restored.configuration().customTask())&&restored.configuration().toggle(2),"Actual entity NBT preserves task and switches");
        test.assertTrue(ConciliumGolems.data(restored).getBoolean("Pending")&&restored.filters().color(0)==6&&restored.filters().sample(0).getHoverName().getString().equals("Open"),"Pending command and colored ghost paper survive reload");
        test.assertTrue(restored.carried().is(Items.BUCKET)&&restored.carried().getCount()==2&&restored.ownerName().equals(player.getGameProfile().getName()),"Physical cargo and owner survive reload separately");
        test.assertTrue(ValetCommandGoal.actor(first)!=ValetCommandGoal.actor(second),"Independent valet FakePlayers cannot share temporary inventories");
        first.discard();second.discard();restored.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void menusRejectForeignOwnersAndGhostExtraction(GameTestHelper test) {
        var owner=player(test,"ConciliumMenu");var foreign=player(test,"OtherMenu");
        for(var task:List.of(ConciliumGolems.ASSISTANT,ConciliumGolems.VALET)) {
            var golem=golem(test,task,5,2,5,owner.getGameProfile().getName());
            owner.moveTo(golem.position());foreign.moveTo(golem.position());
            var menu=new ConciliumGolemMenu(1,owner.getInventory(),golem);owner.containerMenu=menu;
            var original=new ItemStack(Items.PAPER,19);original.setHoverName(Component.literal("Lever"));menu.setCarried(original);
            menu.clicked(0,0,ClickType.PICKUP,owner);
            test.assertTrue(menu.getCarried().getCount()==19&&golem.filters().sample(0).getCount()==1,"Ghost insertion does not consume cursor or duplicate a real stack");
            menu.setCarried(ItemStack.EMPTY);menu.clicked(0,0,ClickType.PICKUP,owner);
            test.assertTrue(menu.getCarried().isEmpty(),"Ghost sample cannot be extracted onto cursor");
            test.assertTrue(!menu.stillValid(foreign)&&!menu.clickMenuButton(foreign,50),"Foreign owner cannot operate task menu");
            for(int button:new int[]{-1,53,57,68,999})test.assertTrue(!menu.clickMenuButton(owner,button),"Non-task button rejected: "+button);
            if(task.equals(ConciliumGolems.VALET))for(int button=50;button<=52;button++)test.assertTrue(menu.clickMenuButton(owner,button)&&golem.configuration().toggle(button-50),"Valet switch accepted: "+button);
            else for(int button=50;button<=52;button++)test.assertTrue(!menu.clickMenuButton(owner,button),"Assistant has no valet switches");
            menu.removed(owner);owner.containerMenu=owner.inventoryMenu;test.assertTrue(golem.workEnabled(),"Closing actual menu resumes golem work");golem.discard();
        }
        test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void assistantSupplyPreservesFullJarsAndCarryLimit(GameTestHelper test) {
        var golem=golem(test,ConciliumGolems.ASSISTANT,6,2,6,"");
        BlockPos chestPos=test.absolutePos(new BlockPos(6,2,5));test.getLevel().setBlock(chestPos,Blocks.CHEST.defaultBlockState(),3);
        golem.state().home(chestPos.south(),Direction.SOUTH);
        var chest=(ChestBlockEntity)test.getLevel().getBlockEntity(chestPos);
        var full=new ItemStack(TCItems.ESSENTIA_JAR.get());
        ((StoredAspectItem)full.getItem()).setStoredAspects(full,new AspectAmounts().add(TCAspects.catalog().require("aer"),32));
        chest.setItem(0,full.copy());chest.setItem(1,new ItemStack(TCItems.ESSENTIA_JAR.get(),64));
        var supply=new AssistantJarSupplyGoal(golem);supply.start();
        int transferred=Math.min(64,golem.configuration().carryLimit());
        test.assertTrue(AssistantJarSupplyGoal.emptyJar(golem.carried())&&golem.carried().getCount()==transferred,"Supply transfers only empty jars up to real carry capacity");
        test.assertTrue(ItemStack.matches(chest.getItem(0),full)&&chest.getItem(1).getCount()==64-transferred,"Full jar and untransferred stock remain in source inventory");
        for(int i=0;i<5;i++)supply.tick();test.assertTrue(!supply.canContinueToUse(),"Chest interaction ends after original five ticks");supply.stop();
        test.assertTrue(!AssistantJarSupplyGoal.emptyJar(new ItemStack(Items.GLASS_BOTTLE))&&!AssistantJarSupplyGoal.emptyJar(full),"No vanilla bottle or filled jar is accepted as capture supply");
        golem.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void assistantTargetProtectionAndUpgradeRules(GameTestHelper test) {
        var golem=golem(test,ConciliumGolems.ASSISTANT,5,2,5,"");var goal=new AssistantCaptureGoal(golem);
        var cow=EntityType.COW.create(test.getLevel());cow.moveTo(golem.getX()+1,golem.getY(),golem.getZ(),0,0);
        test.assertTrue(goal.valid(cow),"Unfiltered adult animal is eligible");cow.setAge(-24000);test.assertTrue(!goal.valid(cow),"Baby cannot be captured");cow.setAge(0);
        var wolf=EntityType.WOLF.create(test.getLevel());wolf.moveTo(cow.position());wolf.setTame(true);test.assertTrue(!goal.valid(wolf),"Tamed pet cannot be captured");
        var zombie=EntityType.ZOMBIE.create(test.getLevel());zombie.moveTo(cow.position());test.assertTrue(!goal.valid(zombie),"Enemy needs Entropy upgrade");
        test.assertTrue(golem.configuration().installUpgrade(5),"Entropy fixture installed");test.assertTrue(goal.valid(zombie),"Entropy permits eligible enemies");
        test.assertTrue(golem.configuration().installUpgrade(4),"Order fixture installed");cow.setHealth(cow.getMaxHealth());test.assertTrue(!goal.valid(cow),"Order skips targets that need weakening");cow.setHealth(1);test.assertTrue(goal.valid(cow),"Order accepts weak target");
        var passenger=EntityType.PIG.create(test.getLevel());passenger.startRiding(cow,true);test.assertTrue(!goal.valid(cow)&&!goal.valid(passenger),"Both ends of a riding relation are protected");passenger.stopRiding();
        var iron=EntityType.IRON_GOLEM.create(test.getLevel());iron.moveTo(cow.position());iron.setHealth(1);test.assertTrue(!goal.valid(iron),"Other golems remain protected even when weak");
        var wither=EntityType.WITHER.create(test.getLevel());wither.moveTo(cow.position());wither.setHealth(1);test.assertTrue(!goal.valid(wither),"Bosses cannot be captured");
        golem.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void bellRecoveryAndDismantleKeepCorrectCore(GameTestHelper test) {
        var owner=player(test,"ConciliumBell");owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.GOLEM_BELL.get()));
        for(int mode=0;mode<2;mode++)for(var task:List.of(ConciliumGolems.ASSISTANT,ConciliumGolems.VALET)) {
            int x=mode==0?5:18,z=task.equals(ConciliumGolems.ASSISTANT)?5:18;
            var golem=golem(test,task,x,2,z,owner.getGameProfile().getName());owner.moveTo(golem.position());owner.setShiftKeyDown(mode==1);
            golem.carried(new ItemStack(Items.APPLE,3));var data=new CompoundTag();data.putString("Command","Never reuse");ConciliumGolems.data(golem,data);
            owner.attack(golem);test.assertTrue(!golem.isAlive(),"Actual bell attack recovers golem");
            var drops=test.getLevel().getEntitiesOfClass(ItemEntity.class,golem.getBoundingBox().inflate(2));
            var portable=drops.stream().map(ItemEntity::getItem).filter(stack->stack.is(TCItems.golem(GolemMaterial.THAUMIUM))).findFirst().orElse(ItemStack.EMPTY);
            test.assertTrue(!portable.isEmpty(),"Bell drops material golem item");
            test.assertTrue(drops.stream().map(ItemEntity::getItem).filter(s->s.is(Items.APPLE)).mapToInt(ItemStack::getCount).sum()==3,"Cargo is returned exactly once");
            if(mode==0)test.assertTrue(portable.getTag().getString("AddonTask").equals(task.toString()),"Normal recovery preserves addon task");
            else {
                test.assertTrue(!portable.getOrCreateTag().contains("AddonTask")&&!portable.getOrCreateTag().getCompound("AddonData").contains(ConciliumGolems.DATA.toString()),"Dismantle clears task and pending command");
                Item expected=task.equals(ConciliumGolems.ASSISTANT)?ConciliumGolems.ASSISTANCE_CORE.get():ConciliumGolems.VALET_CORE.get();
                test.assertTrue(drops.stream().map(ItemEntity::getItem).filter(s->s.is(expected)).mapToInt(ItemStack::getCount).sum()==1,"Dismantle returns the real addon core exactly once");
            }
            drops.forEach(Entity::discard);
        }
        test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void ownerChatCommandsOneMarkedLever(GameTestHelper test) {
        var owner=player(test,"ConciliumChat");var golem=golem(test,ConciliumGolems.VALET,6,2,7,owner.getGameProfile().getName());
        var other=golem(test,ConciliumGolems.VALET,9,2,7,"OtherOwner");owner.moveTo(golem.position());
        var lever=test.absolutePos(new BlockPos(6,2,6));var second=lever.east(2);
        for(var pos:List.of(lever,second)){test.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);test.getLevel().setBlock(pos,Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE,AttachFace.FLOOR),3);}
        var paper=new ItemStack(Items.PAPER);paper.setHoverName(Component.literal("Open"));golem.filters().sample(0,paper);
        golem.markers().replace(List.of(new GolemMarker(lever,test.getLevel().dimension().location(),Direction.UP,(byte)-1),new GolemMarker(second,test.getLevel().dimension().location(),Direction.UP,(byte)-1)));
        ConciliumGolems.chat(new net.minecraftforge.event.ServerChatEvent(owner,"Open",Component.literal("Open")));
        test.assertTrue(ConciliumGolems.data(golem).getBoolean("Pending")&&!ConciliumGolems.data(other).getBoolean("Pending"),"Actual chat event queues only owner's nearby valet");
        var goal=new ValetCommandGoal(golem);
        if(ConciliumGolems.available(ConciliumGolems.VALET)) {
            test.assertTrue(goal.canUse(),"Actual command matcher selects marked lever");goal.start();goal.tick();goal.stop();
            test.assertTrue(test.getLevel().getBlockState(lever).getValue(LeverBlock.POWERED)&&!test.getLevel().getBlockState(second).getValue(LeverBlock.POWERED),"Empty-hand native right click operates the first marked lever only");
            test.assertTrue(!ConciliumGolems.data(golem).getBoolean("Pending")&&ValetCommandGoal.actor(golem).getMainHandItem().isEmpty()&&!ValetCommandGoal.actor(golem).isShiftKeyDown(),"Request is consumed once and temporary actor state cleared");
            golem.tickCount+=10;test.assertTrue(!goal.canUse(),"No repeated activation without another command");
        } else {
            test.assertTrue(!goal.canUse(),"Missing Gadomancy prevents actual valet execution");
            com.mojang.logging.LogUtils.getLogger().info("CONCILIUM_BRANCH_UNAVAILABLE: valet native lever execution needs Gadomancy; chat ownership asserted.");
        }
        golem.discard();other.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void colossusMountDamageAndPersistentHome(GameTestHelper test) {
        var giant=ConciliumGolems.BYDLO.get().create(test.getLevel());var pos=test.absolutePos(new BlockPos(10,2,10));giant.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);giant.setNoAi(true);test.getLevel().addFreshEntity(giant);
        var rider=player(test,"ConciliumRider");var second=player(test,"OtherRider");rider.moveTo(giant.position());second.moveTo(giant.position());
        test.assertTrue(giant.getMaxHealth()==90&&!giant.removeWhenFarAway(10000),"Colossus retains original health and persistence");
        test.assertTrue(rider.startRiding(giant)&&!second.startRiding(giant)&&giant.getControllingPassenger()==rider,"Only one player controls the colossus");
        var target=EntityType.IRON_GOLEM.create(test.getLevel());target.moveTo(giant.position());float before=target.getHealth();test.assertTrue(giant.doHurtTarget(target),"Colossus hits actual living target");
        float damage=before-target.getHealth();test.assertTrue(damage>=10&&damage<=24&&target.getDeltaMovement().y>=.4,"Original random melee damage and upward knockback apply");
        giant.aiStep();var saved=new CompoundTag();giant.saveWithoutId(saved);test.assertTrue(saved.contains("ConciliumHome"),"Home anchor is saved after first AI step");
        var restored=ConciliumGolems.BYDLO.get().create(test.getLevel());restored.load(saved);var again=new CompoundTag();restored.saveWithoutId(again);test.assertTrue(again.getLong("ConciliumHome")==saved.getLong("ConciliumHome"),"Home survives actual entity NBT reload");
        rider.stopRiding();giant.discard();restored.discard();test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void vengefulControlDoesNotReleaseOtherCaster(GameTestHelper test) {
        var player=player(test,"ConciliumControlled");var first=ConciliumGolems.VENGEFUL.get().create(test.getLevel());var second=ConciliumGolems.VENGEFUL.get().create(test.getLevel());
        var pos=test.absolutePos(new BlockPos(5,2,5));player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);first.moveTo(player.position());second.moveTo(player.position());
        test.assertTrue(first.getMainHandItem().is(TCItems.GOLEM_BELL.get())&&first.getMaxHealth()==20&&first.getAttributeValue(Attributes.FOLLOW_RANGE)==32,"Vengeful golem has original combat equipment and attributes");
        CrimsonControl.enslave(player,first,60);CrimsonControl.enslave(player,second,60);test.assertTrue(player.hasEffect(CrimsonControl.ENSLAVED.get()),"Two real caster identities apply control");
        CrimsonControl.release(player,first);test.assertTrue(player.hasEffect(CrimsonControl.ENSLAVED.get()),"One caster release cannot remove another caster's control");CrimsonControl.release(player,second);test.assertTrue(!player.hasEffect(CrimsonControl.ENSLAVED.get()),"Last caster releases control");
        first.getRandom().setSeed(19);boolean controlled=false;for(int i=0;i<20;i++){first.performRangedAttack(player,1);controlled|=player.hasEffect(CrimsonControl.ENSLAVED.get());}
        test.assertTrue(controlled,"Real ranged attack reaches the control implementation");CrimsonControl.release(player,first);test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void assistantCaptureCancellationAndRealJarRelease(GameTestHelper test) {
        ConciliumTestWorld.ready(test,java.util.List.of(new BlockPos(5,2,5),new BlockPos(12,2,5)),()->assistantCaptureFixture(test));
    }
    private static void assistantCaptureFixture(GameTestHelper test) {
        var golem=golem(test,ConciliumGolems.ASSISTANT,5,2,5,"");golem.carried(new ItemStack(TCItems.ESSENTIA_JAR.get(),2));
        if(!ConciliumGolems.available(ConciliumGolems.ASSISTANT)) {
            test.assertTrue(!new AssistantCaptureGoal(golem).canUse(),"Missing optional hosts prevent assistant capture");golem.discard();
            com.mojang.logging.LogUtils.getLogger().info("CONCILIUM_BRANCH_UNAVAILABLE: assistant capture, cancellation and real jar release need Gadomancy + Thaumic Horizons.");test.succeed();return;
        }
        var cow=EntityType.COW.create(test.getLevel());cow.moveTo(golem.getX()+1,golem.getY(),golem.getZ(),0,0);cow.setNoAi(true);cow.setHealth(1);cow.setCustomName(Component.literal("Saved specimen"));cow.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.DIAMOND_HELMET));test.getLevel().addFreshEntity(cow);var identity=cow.getUUID();
        // Mob's NoAI also disables its effective server movement. Let the actual
        // golem land, withholding jars so its installed goal cannot capture early.
        golem.carried(ItemStack.EMPTY);golem.setNoAi(false);
        whenGrounded(test,golem,cow,0,()->{
        golem.setNoAi(true);golem.carried(new ItemStack(TCItems.ESSENTIA_JAR.get(),2));
        test.assertTrue(golem.tickCount>0&&cow.tickCount>0&&golem.onGround(),"Capture fixture has ticked and landed before navigation: golem="+golem.tickCount+", cow="+cow.tickCount+", onGround="+golem.onGround());
        var sampleItem=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumichorizons","blood_sample"));var sample=new ItemStack(sampleItem);sample.getOrCreateTag().getCompound("critter");var sampleTag=new CompoundTag();sampleTag.putString("id","minecraft:pig");sample.getOrCreateTag().put("critter",sampleTag);golem.filters().sample(0,sample);
        test.assertTrue(!new AssistantCaptureGoal(golem).valid(cow),"Actual Horizons blood sample filters target species");sampleTag.putString("id","minecraft:cow");sample.getOrCreateTag().put("critter",sampleTag);golem.filters().sample(0,sample);test.assertTrue(new AssistantCaptureGoal(golem).valid(cow),"Matching blood sample accepts eligible species");golem.filters().sample(0,ItemStack.EMPTY);
        java.util.function.Consumer<net.minecraftforge.event.entity.player.AttackEntityEvent> protectedTarget=event->{if(event.getTarget()==cow)event.setCanceled(true);};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST,false,net.minecraftforge.event.entity.player.AttackEntityEvent.class,protectedTarget);
        try {captureAttempt(test,golem);test.assertTrue(cow.isAlive()&&golem.carried().getCount()==2,"Denied interaction preserves target and both empty jars");}
        finally {net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protectedTarget);}
        var jarItem=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumichorizons","creature_jar"));
        java.util.function.Consumer<net.minecraftforge.event.entity.EntityJoinLevelEvent> reject=event->{if(event.getLevel()==test.getLevel()&&event.getEntity() instanceof ItemEntity drop&&drop.getItem().is(jarItem)&&drop.distanceToSqr(golem)<16)event.setCanceled(true);};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST,false,net.minecraftforge.event.entity.EntityJoinLevelEvent.class,reject);
        try {captureAttempt(test,golem);test.assertTrue(cow.isAlive()&&golem.carried().getCount()==2,"Rejected jar entity spawn cannot consume living target or supply");}
        finally {net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(reject);}
        captureAttempt(test,golem);test.assertTrue(!cow.isAlive()&&golem.carried().getCount()==1,"Successful capture commits exactly one target and one empty jar");
        var jars=test.getLevel().getEntitiesOfClass(ItemEntity.class,golem.getBoundingBox().inflate(3),drop->drop.getItem().is(jarItem));test.assertTrue(jars.size()==1,"Successful capture creates one real Horizons creature jar");
        var captured=jars.get(0).getItem().copy();jars.get(0).discard();test.assertTrue(captured.getTag().getCompound("Captured").getUUID("UUID").equals(identity),"Creature jar carries original entity UUID");
        var player=player(test,"ConciliumRelease");var floor=test.absolutePos(new BlockPos(12,1,5));test.getLevel().setBlock(floor,Blocks.STONE.defaultBlockState(),3);player.moveTo(floor.getX()+2.5,floor.getY()+1,floor.getZ()+.5,0,0);player.setItemInHand(InteractionHand.MAIN_HAND,captured);
        var placement=new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor),Direction.UP,floor,false));
        test.assertTrue(captured.useOn(placement).consumesAction(),"Captured product is accepted by real Horizons jar placement");
        var blockEntity=test.getLevel().getBlockEntity(floor.above());test.assertTrue(blockEntity instanceof WandInteractable,"Placed jar exposes actual wand release callback");
        var release=new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor.above()),Direction.UP,floor.above(),false));
        test.assertTrue(((WandInteractable)blockEntity).onWandUseFirst(ItemStack.EMPTY,release).consumesAction(),"Real jar releases captured creature once");
        var restored=test.getLevel().getEntity(identity);test.assertTrue(restored instanceof net.minecraft.world.entity.animal.Cow&&restored.getName().getString().equals("Saved specimen")&&((LivingEntity)restored).getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET),"Release preserves original type, UUID, name and equipment");
        test.assertTrue(test.getLevel().getBlockState(floor.above()).isAir(),"Consumed jar cannot release a second creature");restored.discard();golem.discard();test.succeed();
        });
    }
    private static void captureAttempt(GameTestHelper test,GolemEntity golem) {
        var goal=new AssistantCaptureGoal(golem);test.assertTrue(goal.canUse(),"Real capture AI finds reachable candidate");goal.start();goal.tick();goal.stop();
    }
    private static void whenGrounded(GameTestHelper test,GolemEntity golem,LivingEntity cow,int waited,Runnable action) {
        test.assertTrue(!golem.isRemoved()&&!cow.isRemoved(),"Capture actors remain present while physics settles");
        if(golem.tickCount>0&&cow.tickCount>0&&golem.onGround()){action.run();return;}
        if(waited>=40){test.fail("Capture actors never landed: golem="+golem.tickCount+", cow="+cow.tickCount+", onGround="+golem.onGround()+", y="+golem.getY());return;}
        test.runAfterDelay(1,()->whenGrounded(test,golem,cow,waited+1,action));
    }
    private static FakePlayer player(GameTestHelper test,String name) {var p=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),name));p.setGameMode(GameType.SURVIVAL);return p;}
    private static GolemEntity golem(GameTestHelper test,ResourceLocation task,int x,int y,int z,String owner) {
        for(int xx=x-2;xx<=x+2;xx++)for(int zz=z-2;zz<=z+2;zz++)test.setBlock(new BlockPos(xx,y-1,zz),Blocks.STONE);
        var golem=TCEntities.GOLEM.get().create(test.getLevel());var config=new GolemConfiguration(GolemMaterial.THAUMIUM,true);config.installCore(task.equals(ConciliumGolems.ASSISTANT)?GolemCoreType.EMPTY:GolemCoreType.USE);config.extension(null,task,null);
        golem.state().configure(config);golem.state().owner(owner);var pos=test.absolutePos(new BlockPos(x,y,z));golem.state().home(pos,Direction.SOUTH);golem.restrictTo(pos,32);golem.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);golem.refreshConfiguration();golem.setNoAi(true);test.getLevel().addFreshEntity(golem);return golem;
    }
    private GolemBehaviourServerTests() {}
}
