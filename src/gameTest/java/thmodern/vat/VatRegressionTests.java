package thmodern.vat;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public class VatRegressionTests {
    private static VatEntity vat(GameTestHelper test){
        var center=test.absolutePos(new BlockPos(3,5,3));
        for(int part=0;part<36;part++)test.getLevel().setBlock(center.offset(VatStructure.offset(part)),Horizons.VAT.get().defaultBlockState().setValue(VatBlock.PART,part),2);
        return (VatEntity)test.getLevel().getBlockEntity(center);
    }
    private static FakePlayer player(GameTestHelper test,VatEntity vat){
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"VatInputTest"));player.setPos(vat.getBlockPos().offset(2,-1,0).getCenter());return player;
    }
    @GameTest(template="empty") public static void effigyAcceptsActualMenuClickAndShiftClick(GameTestHelper test){
        var vat=vat(test);var player=player(test,vat);player.setGameMode(GameType.SURVIVAL);
        var menu=new VatMenu(1,player.getInventory(),vat.getBlockPos(),vat);
        menu.setCarried(new ItemStack(Horizons.CORPSE_EFFIGY.get()));menu.clicked(0,0,ClickType.PICKUP,player);
        test.assertTrue(vat.getItem(0).isEmpty(),"Survival preserves the legacy incarnation research gate");
        thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,"incarnationVat");menu.broadcastChanges();menu.clicked(0,0,ClickType.PICKUP,player);
        test.assertTrue(vat.getItem(0).is(Horizons.CORPSE_EFFIGY.get())&&menu.getCarried().isEmpty(),"A real slot click accepts the effigy after research");
        vat.removeItem(0,1);player.getInventory().setItem(9,new ItemStack(Horizons.CORPSE_EFFIGY.get()));menu.quickMoveStack(player,2);
        test.assertTrue(vat.getItem(0).is(Horizons.CORPSE_EFFIGY.get())&&player.getInventory().getItem(9).isEmpty(),"Shift-click takes the sample slot too");
        vat.tick();for(int i=0;i<80;i++)vat.tick();vat.accept("victus",8);vat.accept("sano",8);vat.tick();
        test.assertTrue(vat.hasEffigy()&&vat.getItem(0).isEmpty(),"GUI insertion produces the living effigy after the legacy costs");test.succeed();
    }
    @GameTest(template="empty") public static void directEffigyInsertionAndCreativeUseDoNotLoseItems(GameTestHelper test){
        var vat=vat(test);var player=player(test,vat);var held=new ItemStack(Horizons.CORPSE_EFFIGY.get());
        player.setGameMode(GameType.CREATIVE);test.assertTrue(vat.insertEffigy(player,held)&&held.getCount()==1,"Creative insertion does not require research or consume the held effigy");
        test.assertTrue(!vat.insertEffigy(player,held)&&held.getCount()==1,"An occupied slot is not overwritten");
        vat.removeItem(0,1);player.setGameMode(GameType.SURVIVAL);thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,"incarnationVat");
        test.assertTrue(vat.insertEffigy(player,held)&&held.isEmpty(),"Survival consumes exactly one effigy");test.succeed();
    }
    @GameTest(template="empty") public static void detachedSubjectsHaveStablePoseAfterCaptureAndReload(GameTestHelper test){
        var vat=vat(test);var cow=EntityType.COW.create(test.getLevel());cow.moveTo(vat.getBlockPos().above().getCenter());
        cow.setYRot(137);cow.setYHeadRot(120);cow.yBodyRot=90;cow.yBodyRotO=-70;cow.yHeadRotO=-10;cow.hurtTime=9;
        test.getLevel().addFreshEntity(cow);test.assertTrue(vat.capture(cow),"Capture succeeds");
        var restored=new VatEntity(vat.getBlockPos(),vat.getBlockState());restored.setLevel(test.getLevel());restored.load(vat.saveWithoutMetadata());restored.tick();
        var subject=restored.subject();test.assertTrue(subject!=null&&subject.yBodyRotO==subject.yBodyRot&&subject.yHeadRotO==subject.yHeadRot&&subject.yRotO==subject.getYRot(),"Old and current rotation are identical after deserialization");
        test.assertTrue(subject.xo==subject.getX()&&subject.yo==subject.getY()&&subject.zo==subject.getZ()&&subject.hurtTime==0,"No stale position or frozen hurt animation is interpolated each frame");test.succeed();
    }
    @GameTest(template="empty") public static void playerRestraintDoesNotRepeatedlyTeleportAndReleasesCleanly(GameTestHelper test){
        var vat=vat(test);var player=new CountingPlayer(test.getLevel());player.setPos(vat.getBlockPos().offset(2,-1,0).getCenter());
        test.assertTrue(vat.capture(player),"Player enters vat");int entered=player.teleports;var anchor=vat.anchor();
        test.assertTrue(VatContainment.containing(player)==vat,"Entry resolves controller: position="+player.position()+", anchor="+anchor+", holds="+vat.holds(player)+", removed="+player.isRemoved());
        for(int i=0;i<100;i++){
            player.setDeltaMovement(.2,-.0784,.2);player.travel(new Vec3(1,0,1));vat.tick();
            test.assertTrue(player.position().distanceToSqr(anchor)<1e-10&&player.getDeltaMovement().lengthSqr()==0,"Real Player.travel is restrained without gravity or input drift: tick="+i+", pos="+player.position()+", anchor="+anchor+", velocity="+player.getDeltaMovement()+", holds="+vat.holds(player)+", subject="+vat.subject());
        }
        test.assertTrue(player.teleports==entered,"The vat does not send a corrective teleport every tick");
        test.assertTrue(vat.release()&&VatContainment.containing(player)==null,"Release removes restraint immediately");
        player.setDeltaMovement(.1,0,0);player.travel(Vec3.ZERO);test.assertTrue(player.position().distanceToSqr(vat.getBlockPos().above().getCenter())>.001,"Normal movement resumes outside the vat");test.succeed();
    }
    static class CountingPlayer extends FakePlayer {
        int teleports;
        CountingPlayer(ServerLevel level){super(level,new GameProfile(UUID.randomUUID(),"VatMotionTest"));}
        // Forge's FakePlayer connection deliberately ignores teleport packets. Apply the same
        // authoritative position here while counting how often the vat requests a teleport.
        @Override public void teleportTo(ServerLevel level,double x,double y,double z,float yaw,float pitch){teleports++;moveTo(x,y,z,yaw,pitch);}
    }
}
