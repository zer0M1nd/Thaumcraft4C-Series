package thmodern.creature;
import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class PlanarSlimeServerTests {
 @GameTest(template="empty")
 public static void deathSplitsOnlyIntoMatchingSubtype(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
  for(var type:PlanarSlimes.TYPES.values()){
   var parent=type.get().create(level);parent.setSize(4,true);parent.setPos(pos.getCenter());parent.setNoAi(true);level.addFreshEntity(parent);parent.setHealth(0);parent.remove(Entity.RemovalReason.KILLED);
   var children=level.getEntitiesOfClass(PlanarSlime.class,parent.getBoundingBox().inflate(3),e->e.getType()==type.get()&&!e.isRemoved());
   test.assertTrue(children.size()>=2&&children.size()<=4,"Exactly one split produces 2..4 children");
   for(var child:children){test.assertTrue(child.getSize()==2,"Child size is halved");child.discard();}
  }test.succeed();
 }
 @GameTest(template="empty")
 public static void medicalSlimeHealsOnceWithoutSplitting(GameTestHelper test){
  var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"MedicalSlime"));player.setPos(pos.getCenter());player.setHealth(10);
  var slime=PlanarSlimes.TYPES.get("med").get().create(level);slime.setSize(2,true);slime.setPos(pos.getCenter());level.addFreshEntity(slime);slime.playerTouch(player);
  test.assertTrue(player.getHealth()==13&&slime.isRemoved(),"Size-two medical slime heals three and is consumed");slime.playerTouch(player);test.assertTrue(player.getHealth()==13,"Consumed slime cannot heal twice");
  test.assertTrue(level.getEntitiesOfClass(PlanarSlime.class,slime.getBoundingBox().inflate(2),e->!e.isRemoved()).isEmpty(),"Healing does not create children");test.succeed();
 }
}
