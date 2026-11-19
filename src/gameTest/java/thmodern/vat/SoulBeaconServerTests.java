package thmodern.vat;
import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import thmodern.soul.*;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class SoulBeaconServerTests {
    private static VatEntity body(GameTestHelper test,BlockPos center){
        var level=test.getLevel();for(int part=0;part<36;part++)level.setBlock(center.offset(VatStructure.offset(part)),Horizons.VAT.get().defaultBlockState().setValue(VatBlock.PART,part),2);
        level.setBlock(center.above(),Horizons.SOUL_BEACON.get().defaultBlockState(),2);var vat=(VatEntity)level.getBlockEntity(center);
        vat.setItem(0,new ItemStack(Horizons.CORPSE_EFFIGY.get()));vat.tick();for(int i=0;i<80;i++)vat.tick();vat.accept("victus",8);vat.accept("sano",8);vat.tick();return vat;
    }
    @GameTest(template="empty")
    public static void deathTransfersOnceKeepsExperienceAndReplacesBodyAbilities(GameTestHelper test){
        var level=test.getLevel();var center=test.absolutePos(new BlockPos(4,5,4));var vat=body(test,center);
        var saved=vat.saveWithoutMetadata();saved.put("SelfInfusions",SelfInfusions.write(Set.of("diamond_skin")));vat.load(saved);
        var player=new thmodern.testing.NetworkTestPlayer(level,new GameProfile(UUID.randomUUID(),"SoulReturnTest"));player.setPos(center.above(2).getCenter());
        test.assertTrue(((SoulBeaconEntity)level.getBlockEntity(center.above())).bind(player),"Nearby player binds a beacon");
        SelfInfusions.replace(player,Set.of("quicksilver_limbs"));player.getInventory().setItem(0,new ItemStack(Items.DIAMOND,3));player.experienceLevel=12;player.totalExperience=100;player.experienceProgress=.25f;
        var origin=center.offset(5,0,0).getCenter();
        // The former body lies outside the empty template; clear only this fixture's drops.
        var dropBounds=new net.minecraft.world.phys.AABB(origin,origin).inflate(2);
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,dropBounds,e->e.getItem().is(Items.DIAMOND)).forEach(net.minecraft.world.entity.Entity::discard);
        player.setPos(origin);player.setHealth(0);var event=new LivingDeathEvent(player,player.damageSources().generic());SoulReturn.death(event);
        test.assertTrue(event.isCanceled()&&player.getHealth()==player.getMaxHealth(),"A real death event is cancelled only after successful body transfer");
        test.assertTrue(vat.subject()==player&&!vat.hasEffigy()&&vat.mode()==VatEntity.IDLE,"Exactly one body is consumed and the player occupies the vat");
        test.assertTrue(SelfInfusions.has(player,"diamond_skin")&&!SelfInfusions.has(player,"quicksilver_limbs"),"New body replaces rather than accumulates prior upgrades");
        test.assertTrue(player.experienceLevel==12&&player.totalExperience==100&&player.experienceProgress==.25f,"Experience is retained");
        test.assertTrue(player.getInventory().getItem(0).isEmpty(),"Equipment is left at the former body");
        int dropped=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin,origin).inflate(2)).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();test.assertTrue(dropped==3,"Inventory has exactly one old-location drop; actual="+dropped);
        test.assertTrue(!SoulReturn.restore(player),"Consumed body cannot resurrect twice");
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,dropBounds,e->e.getItem().is(Items.DIAMOND));
        test.assertTrue(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==3,"Second restore cannot duplicate inventory drops");
        drops.forEach(net.minecraft.world.entity.Entity::discard);
        test.assertTrue(vat.release()&&player.getY()==center.getY()+2,"Exit stands above the beacon rather than inside it");test.succeed();
    }
    @GameTest(template="empty")
    public static void missingBodyBrokenStructureAndInvalidDimensionCannotConsumeInventory(GameTestHelper test){
        var level=test.getLevel();var center=test.absolutePos(new BlockPos(4,5,4));var vat=body(test,center);
        var player=new thmodern.testing.NetworkTestPlayer(level,new GameProfile(UUID.randomUUID(),"SoulReturnRefuse"));player.setPos(center.above(2).getCenter());var beacon=(SoulBeaconEntity)level.getBlockEntity(center.above());beacon.bind(player);player.getInventory().setItem(0,new ItemStack(Items.EMERALD));
        var binding=player.getPersistentData().getCompound(SoulReturn.BINDING).copy();player.getPersistentData().getCompound(SoulReturn.BINDING).putString("Dimension","test:missing");
        test.assertTrue(!SoulReturn.restore(player)&&vat.hasEffigy(),"An absent dimension safely refuses return");player.getPersistentData().put(SoulReturn.BINDING,binding);
        level.setBlock(center.offset(VatStructure.offset(0)),Blocks.AIR.defaultBlockState(),2);
        test.assertTrue(!SoulReturn.restore(player)&&player.getInventory().getItem(0).is(Items.EMERALD),"A damaged vat cannot consume the player's inventory");
        player.setPos(center.offset(20,0,0).getCenter());test.assertTrue(!beacon.bind(player),"Remote callers cannot bind an out-of-range beacon");test.succeed();
    }
}
