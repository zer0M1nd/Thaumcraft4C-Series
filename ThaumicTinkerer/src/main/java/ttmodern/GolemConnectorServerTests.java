package ttmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class GolemConnectorServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererGolemBindingAndAtomicMarkers(GameTestHelper test){
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));world.setBlockAndUpdate(pos,Tinkerer.GOLEM_CONNECTOR.get().defaultBlockState());var connector=(GolemConnectorEntity)world.getBlockEntity(pos);
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ConnectorOwner"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var golem=TCEntities.GOLEM.get().create(world);golem.moveTo(pos.getX()+2,pos.getY(),pos.getZ());golem.state().owner("AnotherOwner");world.addFreshEntity(golem);
        test.assertTrue(!connector.bind(player,golem),"Other owners cannot bind the golem");golem.state().owner("ConnectorOwner");test.assertTrue(connector.bind(player,golem)&&connector.golem()==golem,"Owner can bind live nearby golem");
        var home=pos.offset(1,0,1);connector.home(home,Direction.EAST);test.assertTrue(golem.home().equals(home)&&golem.homeFacing()==Direction.EAST,"Home and facing change through authoritative state");
        var marker=new GolemMarker(home,world.dimension().location(),Direction.UP,(byte)2);connector.markers(List.of(marker));test.assertTrue(GolemConnectorEntity.marker(GolemConnectorEntity.markerMap(marker)).equals(marker),"Modern dimension IDs and marker fields round trip");
        boolean rejected=false;try{connector.markers(List.of(marker,new GolemMarker(new BlockPos(home.getX(),world.getMaxBuildHeight(),home.getZ()),world.dimension().location(),Direction.DOWN,(byte)-1)));}catch(IllegalArgumentException expected){rejected=true;}test.assertTrue(rejected&&connector.markers().equals(List.of(marker)),"One invalid entry leaves the whole old list unchanged");
        var saved=connector.saveWithoutMetadata();connector.load(saved);test.assertTrue(connector.golem()==golem,"UUID and authorized owner survive load");
        golem.setPos(pos.getX()+31,pos.getY(),pos.getZ());test.assertTrue(connector.golem()==null,"Moving outside the cube invalidates access immediately");golem.setPos(pos.getX()+2,pos.getY(),pos.getZ());test.assertTrue(connector.golem()==golem,"Returning restores access");
        golem.state().owner("NewOwner");test.assertTrue(connector.golem()==null,"Ownership change revokes existing bindings");golem.state().owner("ConnectorOwner");golem.discard();test.assertTrue(connector.golem()==null,"Removed golem is never returned from a cached reference");world.removeBlock(pos,false);test.succeed();
    }
}
