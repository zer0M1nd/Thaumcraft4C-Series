package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class LateFocusServerTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test,BlockPos pos){var p=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"LateFocus"));p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);return p;}
    private static ItemStack wand(WandFocusItem focus){var w=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(w,new ItemStack(focus));return w;}
    private static BlockHitResult hit(BlockPos pos){return new BlockHitResult(pos.getCenter(),Direction.UP,pos,false);}
    @GameTest(template="wand_test_empty") public static void tinkererEnderFocusResetPreservesUpgradeAndDoesNotCharge(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var p=player(test,pos.north());var focus=Tinkerer.ENDER_CHEST_FOCUS.get();var wand=wand(focus);var attached=WandFocusItem.attached(wand);focus.applyUpgrade(attached,FocusUpgrade.FRUGAL,1);attached.getOrCreateTag().putBoolean("EnderStorage",true);attached.getOrCreateTag().put("Frequency",new net.minecraft.nbt.CompoundTag());WandFocusItem.attach(wand,attached);p.setItemInHand(InteractionHand.MAIN_HAND,wand);
        if(!EnderStorageBridge.available()){focus.cast(wand,level,p,InteractionHand.MAIN_HAND,null);test.assertTrue(p.containerMenu==p.inventoryMenu&&wand.getTag().getInt("ordo")==10000,"Missing optional mod does not open an unrelated inventory or charge vis");}
        level.setBlockAndUpdate(pos,Blocks.OBSIDIAN.defaultBlockState());p.setShiftKeyDown(true);focus.cast(wand,level,p,InteractionHand.MAIN_HAND,hit(pos));var reset=WandFocusItem.attached(wand);test.assertTrue(!EnderChestFocus.bound(reset)&&!reset.getTag().contains("Frequency"),"Sneak-use obsidian clears optional storage binding");test.assertTrue(focus.upgradeLevel(reset,FocusUpgrade.FRUGAL.id)==1&&wand.getTag().getInt("ordo")==10000,"Reset keeps upgrades and does not spend vis");level.removeBlock(pos,false);test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererDeflectionPaymentOwnersAndExclusions(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var p=player(test,pos);var f=Tinkerer.DEFLECTION_FOCUS.get();var w=wand(f);p.setItemInHand(InteractionHand.MAIN_HAND,w);
        var arrow=new Arrow(level,p.getX(),p.getY()+1,p.getZ()+2);arrow.setDeltaMovement(0,0,-1);level.addFreshEntity(arrow);
        var own=new Arrow(level,p.getX()+1,p.getY()+1,p.getZ()+2);own.setOwner(p);own.setDeltaMovement(0,0,-1);level.addFreshEntity(own);
        var xp=new ThrownExperienceBottle(level,p);xp.setPos(p.getX()-1,p.getY()+1,p.getZ()+2);xp.setOwner(null);xp.setDeltaMovement(0,0,-1);level.addFreshEntity(xp);
        var stationary=new Arrow(level,p.getX()+2,p.getY()+1,p.getZ());stationary.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(stationary);
        var fireball=new SmallFireball(level,p.getX(),p.getY()+1,p.getZ()+2,0,0,-1);fireball.setDeltaMovement(0,0,-1);level.addFreshEntity(fireball);var firePos=fireball.position();
        try{
            double z=arrow.getZ(),distance=arrow.distanceTo(p),ownZ=own.getZ(),xpZ=xp.getZ();var still=stationary.position();f.usingTick(w,p,100);
            test.assertTrue(Math.abs(arrow.getZ()-(z-distance*2))<.0001&&arrow.getDeltaMovement().z==-1,"Projectile advances along original direction without changing velocity");
            test.assertTrue(own.getZ()==ownZ&&xp.getZ()==xpZ&&stationary.position().equals(still),"Own projectiles, XP bottles and zero velocity excluded");
            test.assertTrue(fireball.position().equals(firePos),"Old non-IProjectile fireballs remain unaffected by modern Projectile superclass");
            test.assertTrue(w.getTag().getInt("ordo")==9992&&w.getTag().getInt("aer")==9996,"Shield tick always pays legacy centivis");
            arrow.setPos(p.getX(),p.getY()+1,p.getZ()+2);w.getTag().putInt("aer",0);z=arrow.getZ();f.usingTick(w,p,99);
            test.assertTrue(arrow.getZ()==z&&w.getTag().getInt("ordo")==9992,"Incomplete payment cannot deflect or debit other primal");
            test.assertTrue(f.upgradesForRank(new ItemStack(f),2).contains(FocusUpgrade.ENLARGE)&&!f.upgradesForRank(new ItemStack(f),1).contains(FocusUpgrade.ENLARGE),"Legacy enlarge ranks 2 and 4");test.succeed();
        }finally{arrow.discard();own.discard();xp.discard();stationary.discard();fireball.discard();}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererEnderFocusInventoryAndPayment(GameTestHelper test){
        var p=player(test,test.absolutePos(new BlockPos(2,2,2)));var other=player(test,p.blockPosition());var f=Tinkerer.ENDER_CHEST_FOCUS.get();var w=wand(f);p.setItemInHand(InteractionHand.MAIN_HAND,w);
        p.getEnderChestInventory().setItem(0,new ItemStack(Items.DIAMOND,7));
        try{
            f.cast(w,test.getLevel(),p,InteractionHand.MAIN_HAND,null);
            test.assertTrue(p.containerMenu instanceof net.minecraft.world.inventory.ChestMenu&&p.containerMenu.getSlot(0).getItem().getCount()==7,"Focus opens player's own stored ender inventory");
            test.assertTrue(other.getEnderChestInventory().isEmpty()&&w.getTag().getInt("ordo")==9900&&w.getTag().getInt("perditio")==9900,"Player inventories isolated and one vis per primal paid");
            p.closeContainer();w.getTag().putInt("perditio",0);f.cast(w,test.getLevel(),p,InteractionHand.MAIN_HAND,null);
            test.assertTrue(p.containerMenu==p.inventoryMenu&&w.getTag().getInt("ordo")==9900,"Empty wand neither opens nor partly charges");test.succeed();
        }finally{p.closeContainer();p.getEnderChestInventory().clearContent();}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererHealingProgressReleaseAndPotency(GameTestHelper test){
        var p=player(test,test.absolutePos(new BlockPos(2,2,2)));var f=Tinkerer.HEALING_FOCUS.get();var w=wand(f);p.setItemInHand(InteractionHand.MAIN_HAND,w);p.setHealth(10);
        for(int i=0;i<29;i++)f.usingTick(w,p,100-i);test.assertTrue(p.getHealth()==10&&w.getTag().getInt("aqua")==10000,"No heal or payment before 30 ticks");f.usingTick(w,p,70);
        test.assertTrue(p.getHealth()==11&&w.getTag().getInt("aqua")==9955&&w.getTag().getInt("terra")==9955,"One health costs .45 water and earth");
        for(int i=0;i<20;i++)f.usingTick(w,p,50-i);f.release(w,test.getLevel(),p,0);for(int i=0;i<10;i++)f.usingTick(w,p,30-i);test.assertTrue(p.getHealth()==11,"Release resets unfinished healing");f.release(w,test.getLevel(),p,0);
        var core=WandFocusItem.attached(w);f.applyUpgrade(core,FocusUpgrade.POTENCY,1);WandFocusItem.attach(w,core);for(int i=0;i<27;i++)f.usingTick(w,p,30-i);test.assertTrue(p.getHealth()==13,"Potency heals two health after 27 ticks");
        p.setHealth(p.getMaxHealth());int before=w.getTag().getInt("aqua");for(int i=0;i<40;i++)f.usingTick(w,p,100-i);test.assertTrue(w.getTag().getInt("aqua")==before,"Full health costs nothing");f.release(w,test.getLevel(),p,0);test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererDislocationContainerRollbackAndSelection(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var destination=pos.east(3);var p=player(test,pos.north(2));var f=Tinkerer.DISLOCATION_FOCUS.get();var w=wand(f);p.setItemInHand(InteractionHand.MAIN_HAND,w);
        level.setBlock(pos,Blocks.CHEST.defaultBlockState(),3);var chest=(ChestBlockEntity)level.getBlockEntity(pos);chest.setCustomName(net.minecraft.network.chat.Component.literal("Moved chest"));chest.setItem(0,new ItemStack(Items.DIAMOND,13));
        var guard=new PlaceGuard(destination);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(guard);
        try{
            f.cast(w,level,p,InteractionHand.MAIN_HAND,hit(pos));var core=WandFocusItem.attached(w);
            test.assertTrue(level.isEmptyBlock(pos)&&DislocationFocus.carrying(core),"Container captured in core");
            test.assertTrue(w.getTag().getInt("ordo")==7500&&w.getTag().getInt("perditio")==7500&&w.getTag().getInt("terra")==9500,"Container costs 25/25/5 vis");
            test.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1)).isEmpty(),"Capturing container spills no inventory");
            var saved=ItemStack.of(core.save(new net.minecraft.nbt.CompoundTag()));test.assertTrue(f.sortingKey(saved).equals(f.sortingKey(core))&&!f.sortingKey(core).equals(f.sortingKey(new ItemStack(f)))&&f.sortingKey(core).length()<=128,"Carried focus has stable bounded distinct selection key");
            level.setBlock(destination,Blocks.STONE.defaultBlockState(),3);f.cast(w,level,p,InteractionHand.MAIN_HAND,hit(destination.below()));test.assertTrue(level.getBlockState(destination).is(Blocks.STONE)&&DislocationFocus.carrying(WandFocusItem.attached(w)),"Occupied destination preserves cargo");
            level.removeBlock(destination,false);f.cast(w,level,p,InteractionHand.MAIN_HAND,hit(destination.below()));test.assertTrue(level.isEmptyBlock(destination)&&DislocationFocus.carrying(WandFocusItem.attached(w)),"Canceled placement rolls back and retains cargo");
            guard.deny=false;f.cast(w,level,p,InteractionHand.MAIN_HAND,hit(destination.below()));var moved=(ChestBlockEntity)level.getBlockEntity(destination);
            test.assertTrue(moved!=null&&moved.getItem(0).getCount()==13&&moved.getDisplayName().getString().equals("Moved chest")&&!DislocationFocus.carrying(WandFocusItem.attached(w)),"Successful placement restores NBT exactly once");
            test.assertTrue(w.getTag().getInt("ordo")==7500,"Placement is free");test.succeed();
        }finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(guard);level.removeBlockEntity(pos);level.removeBlockEntity(destination);level.removeBlock(pos,false);level.removeBlock(destination,false);}
    }
    public static final class PlaceGuard {
        private final BlockPos target;boolean deny=true;PlaceGuard(BlockPos pos){target=pos;}
        @net.minecraftforge.eventbus.api.SubscribeEvent public void LateFocusServerTests_placing_1(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event){if(deny&&event.getPos().equals(target))event.setCanceled(true);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererDislocationCostsAndBlacklist(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var p=player(test,pos.north(2));var f=Tinkerer.DISLOCATION_FOCUS.get();var w=wand(f);p.setItemInHand(InteractionHand.MAIN_HAND,w);
        try{
            level.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),3);f.cast(w,level,p,InteractionHand.MAIN_HAND,hit(pos));test.assertTrue(level.getBlockState(pos).is(Blocks.BEDROCK)&&w.getTag().getInt("ordo")==10000,"Unbreakable capture rejected without payment");
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);w.getTag().putInt("terra",0);f.cast(w,level,p,InteractionHand.MAIN_HAND,hit(pos));test.assertTrue(level.getBlockState(pos).is(Blocks.STONE)&&w.getTag().getInt("ordo")==10000,"Insufficient one primal leaves block and other vis intact");
            w.getTag().putInt("terra",10000);f.cast(w,level,p,InteractionHand.MAIN_HAND,hit(pos));test.assertTrue(level.isEmptyBlock(pos)&&w.getTag().getInt("ordo")==9500&&w.getTag().getInt("terra")==9900,"Plain block costs 5/5/1 vis");
            w=wand(f);p.setItemInHand(InteractionHand.MAIN_HAND,w);level.setBlock(pos,Blocks.SPAWNER.defaultBlockState(),3);f.cast(w,level,p,InteractionHand.MAIN_HAND,hit(pos));
            test.assertTrue(level.isEmptyBlock(pos)&&DislocationFocus.carrying(WandFocusItem.attached(w))&&w.getTag().getInt("ordo")==0&&w.getTag().getInt("terra")==5000,"Spawner costs 100/100/50 vis and keeps block entity");test.succeed();
        }finally{level.removeBlockEntity(pos);level.removeBlock(pos,false);}
    }
}
