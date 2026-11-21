package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class MagnetServerTests {
    @GameTest(template="wand_test_empty")
    public static void tinkererMagnetPullPushPowerAndHalfBlockBoundary(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));level.setBlock(pos,Tinkerer.ITEM_MAGNET.get().defaultBlockState(),3);level.setBlock(pos.north(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        var magnet=(MagnetEntity)level.getBlockEntity(pos);var cargo=new ItemEntity(level,pos.getX()+7.8,pos.getY()+.6,pos.getZ()+.5,new ItemStack(Items.APPLE));level.addFreshEntity(cargo);
        try{
            test.assertTrue(magnet.signal()==15,"Strong redstone input detected");magnet.tick();test.assertTrue(cargo.getDeltaMovement().x<0,"Documented 7.5 range includes center distance 7.3");
            magnet.pulling(false);magnet.tick();test.assertTrue(cargo.getDeltaMovement().x>0,"Red mode pushes away");
            level.removeBlock(pos.north(),false);cargo.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);magnet.tick();test.assertTrue(cargo.getDeltaMovement().lengthSqr()==0,"No power has no effect");test.succeed();
        }finally{cargo.discard();level.removeBlock(pos,false);level.removeBlock(pos.north(),false);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererMobFilterAgePatternAndPersistence(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));level.setBlock(pos,Tinkerer.MOB_MAGNET.get().defaultBlockState(),3);var magnet=(MagnetEntity)level.getBlockEntity(pos);
        var cow=EntityType.COW.create(level);var calf=EntityType.COW.create(level);calf.setAge(-24000);var pig=EntityType.PIG.create(level);var zombie=EntityType.ZOMBIE.create(level);zombie.setBaby(true);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MagnetPattern"));
        var mould=new ItemStack(Tinkerer.SOUL_MOULD.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,mould);
        try{
            test.assertTrue(magnet.accepts(cow)&&!magnet.accepts(calf)&&magnet.accepts(zombie)&&!magnet.accepts(player),"Only breedable age is filtered; players excluded");
            magnet.adult(false);test.assertTrue(!magnet.accepts(cow)&&magnet.accepts(calf)&&magnet.accepts(zombie),"Child mode preserves non-ageable selection");
            magnet.filter.setStackInSlot(0,mould);test.assertTrue(!magnet.accepts(calf),"Blank mould matches no entity");
            Tinkerer.SOUL_MOULD.get().interactLivingEntity(mould,player,cow,net.minecraft.world.InteractionHand.MAIN_HAND);
            test.assertTrue(SoulMouldItem.pattern(mould).equals("minecraft:cow")&&magnet.accepts(calf)&&!magnet.accepts(pig)&&!magnet.accepts(zombie),"Entity type pattern combines with age");
            var saved=magnet.saveWithoutMetadata();var restored=new MagnetEntity(pos,magnet.getBlockState());restored.load(saved);test.assertTrue(!restored.adult()&&restored.accepts(calf),"Filter and mode survive save/load");
            player.setShiftKeyDown(true);Tinkerer.SOUL_MOULD.get().use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);test.assertTrue(SoulMouldItem.pattern(mould).isEmpty(),"Sneaking clears pattern");
            test.assertTrue(!magnet.filter.insertItem(0,new ItemStack(Items.DIRT),false).isEmpty(),"Automation rejects invalid filter item");test.succeed();
        }finally{magnet.filter.setStackInSlot(0,ItemStack.EMPTY);level.removeBlock(pos,false);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererMagnetMenuChecksDistanceAndConservesMould(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));level.setBlock(pos,Tinkerer.MOB_MAGNET.get().defaultBlockState(),3);var magnet=(MagnetEntity)level.getBlockEntity(pos);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MagnetMenu"));player.moveTo(pos.getCenter());
        var menu=new MagnetMenu(1,player.getInventory(),magnet);player.getInventory().setItem(9,new ItemStack(Tinkerer.SOUL_MOULD.get()));
        try{
            test.assertTrue(!menu.quickMoveStack(player,1).isEmpty()&&player.getInventory().getItem(9).isEmpty()&&magnet.filter.getStackInSlot(0).getCount()==1,"Shift transfer moves the single mould");
            test.assertTrue(menu.clickMenuButton(player,1)&&!magnet.adult(),"Nearby container button switches child mode");
            test.assertTrue(!menu.clickMenuButton(player,99),"Unknown button rejected");
            player.moveTo(pos.getCenter().add(20,0,0));test.assertTrue(!menu.clickMenuButton(player,0)&&!magnet.adult()&&menu.quickMoveStack(player,0).isEmpty(),"Distant actor cannot mutate menu");
            player.moveTo(pos.getCenter());test.assertTrue(!menu.quickMoveStack(player,0).isEmpty()&&magnet.filter.getStackInSlot(0).isEmpty(),"Mould returns to player without duplication");
            test.assertTrue(tc4revival.research.ResearchDefinitions.server().pages().require(Tinkerer.ID+":MAGNETS").size()==5,"Complete five-page research loaded");test.succeed();
        }finally{magnet.filter.setStackInSlot(0,ItemStack.EMPTY);level.removeBlock(pos,false);}
    }
}
