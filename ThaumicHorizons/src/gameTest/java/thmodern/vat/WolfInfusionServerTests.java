package thmodern.vat;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import thmodern.creature.InfusedWolf;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class WolfInfusionServerTests {
    @GameTest(template="empty")
    public static void wolfConversionsRetainOwnerAndSitting(GameTestHelper test){
        var wolf=EntityType.WOLF.create(test.getLevel());var owner=UUID.randomUUID();wolf.setTame(true);wolf.setOwnerUUID(owner);wolf.setOrderedToSit(true);
        for(var key:java.util.List.of("seawolf","lunar_wolf","nether_hound")){
            var recipe=VatInfusionRecipes.all().stream().filter(r->r.id.getPath().equals(key)).findFirst().orElseThrow();var converted=(InfusedWolf)CreatureTransformation.prepare(wolf,recipe);
            test.assertTrue(converted.isTame()&&owner.equals(converted.getOwnerUUID())&&converted.isOrderedToSit(),"Conversion preserves tame owner and command: "+key);
            test.assertTrue(converted.fireImmune()==key.equals("nether_hound"),"Fire immunity only belongs to nether hound");
        }test.succeed();
    }
    @GameTest(template="empty")
    public static void lunarChargeUsesCentivisAndCapsAtWandCapacity(GameTestHelper test){
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"MoonTest"));var stack=thaumcraft.api.ThaumcraftApi.createWand("wood","iron",false,false);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        InfusedWolf.chargePlayer(player);var wand=(tc4revival.content.CastingWandItem)stack.getItem();
        for(var aspect:tc4revival.aspect.TCAspects.catalog().primals()){test.assertTrue(wand.vis(stack,aspect)==1,"One centivis per primal per pulse");wand.addVis(stack,aspect,wand.capacity(stack),false,true);}
        InfusedWolf.chargePlayer(player);for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())test.assertTrue(wand.vis(stack,aspect)==wand.capacity(stack),"Full wand remains capped");
        test.assertTrue(InfusedWolf.lunarInterval(1)==3&&InfusedWolf.lunarInterval(0)==7,"Moon phase controls three-to-seven tick interval");test.succeed();
    }
    @GameTest(template="empty")
    public static void seawolfRefreshesAirUnderwater(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(3,3,3));test.getLevel().setBlock(pos,Blocks.WATER.defaultBlockState(),3);test.getLevel().setBlock(pos.above(),Blocks.WATER.defaultBlockState(),3);
        var wolf=Horizons.SEAWOLF.get().create(test.getLevel());wolf.moveTo(pos.getCenter());wolf.setNoAi(true);wolf.tick();wolf.setAirSupply(2);wolf.aiStep();
        test.assertTrue(wolf.isInWater()&&wolf.getAirSupply()==300,"Underwater sea wolf replenishes air");test.succeed();
    }
    @GameTest(template="empty")
    public static void netherHoundFiresOnlyAtUnobstructedTarget(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(3,3,3));var wolf=Horizons.NETHER_HOUND.get().create(test.getLevel());wolf.moveTo(pos.getCenter());wolf.setYRot(0);wolf.setXRot(0);
        var cow=EntityType.COW.create(test.getLevel());cow.moveTo(pos.offset(0,0,4).getCenter());test.getLevel().addFreshEntity(cow);wolf.setTarget(cow);
        test.assertTrue(wolf.breatheFire(),"Visible selected target within seven blocks causes server ember spawn");
        var embers=test.getLevel().getEntitiesOfClass(tc4revival.content.EmberEntity.class,wolf.getBoundingBox().inflate(8));test.assertTrue(embers.size()==1&&embers.get(0).getOwner()==wolf,"One owned ember produced");
        test.getLevel().setBlock(pos.offset(0,1,2),Blocks.STONE.defaultBlockState(),3);test.getLevel().setBlock(pos.offset(0,0,2),Blocks.STONE.defaultBlockState(),3);
        test.assertTrue(!wolf.breatheFire(),"Solid obstruction prevents firing through wall");embers.forEach(net.minecraft.world.entity.Entity::discard);cow.discard();test.succeed();
    }
}
