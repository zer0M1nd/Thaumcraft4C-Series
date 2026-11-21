package thmodern.vat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
import thmodern.creature.NightmareLanding;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class NightmareServerTests {
    @GameTest(template="empty")
    public static void nightmareLandingRequiresGroundAndRiderClearance(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(3,3,3));var level=test.getLevel();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){level.setBlock(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);for(int y=0;y<4;y++)level.setBlock(pos.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
        test.assertTrue(NightmareLanding.safe(level,pos),"Three-by-three ground with four-high air fits horse and rider");level.setBlock(pos.above(3),Blocks.STONE.defaultBlockState(),3);test.assertTrue(!NightmareLanding.safe(level,pos),"Low ceiling rejected");level.setBlock(pos.above(3),Blocks.AIR.defaultBlockState(),3);level.setBlock(pos,Blocks.LAVA.defaultBlockState(),3);test.assertTrue(!NightmareLanding.safe(level,pos),"Liquid landing rejected");test.succeed();
    }
    @GameTest(template="empty")
    public static void nightmareConversionRetainsEnderBoostOnlyOnce(GameTestHelper test){
        var source=Horizons.ENDERSTEED.get().create(test.getLevel());var recipe=VatInfusionRecipes.all().stream().filter(r->r.id.getPath().equals("nightmare")).findFirst().orElseThrow();var converted=CreatureTransformation.prepare(source,recipe);
        test.assertTrue(converted.getType()==Horizons.NIGHTMARE.get()&&converted.fireImmune()&&converted.getMaxHealth()==source.getMaxHealth(),"Nightmare inherits existing ender boosts without multiplying again");
        var tag=new CompoundTag();converted.save(tag);var loaded=Horizons.NIGHTMARE.get().create(test.getLevel());loaded.load(tag);test.assertTrue(loaded.getMaxHealth()==source.getMaxHealth(),"Second save/load retains same stats");test.succeed();
    }
    @GameTest(template="empty",timeoutTicks=600)
    public static void nightmareTransfersOneHorseAndKeepsRiderMounted(GameTestHelper test){
        var source=test.getLevel();var destination=source.getServer().getLevel(net.minecraft.world.level.Level.NETHER);test.assertTrue(destination!=null,"Nether is available");
        var pos=test.absolutePos(new BlockPos(3,3,3));var horse=Horizons.NIGHTMARE.get().create(source);horse.moveTo(pos.getCenter());horse.setTamed(true);horse.equipSaddle(net.minecraft.sounds.SoundSource.NEUTRAL);source.addFreshEntity(horse);
        double scale=net.minecraft.world.level.dimension.DimensionType.getTeleportationScale(source.dimensionType(),destination.dimensionType());var arrival=BlockPos.containing(horse.getX()*scale,Math.max(16,Math.min(110,horse.getY())),horse.getZ()*scale);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){destination.getChunk(arrival.offset(x,0,z));destination.setBlock(arrival.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);for(int y=0;y<4;y++)destination.setBlock(arrival.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
        var player=new thmodern.testing.NetworkTestPlayer(source,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"NightmareRider"));player.moveTo(horse.position());player.startRiding(horse,true);var uuid=horse.getUUID();
        test.assertTrue(horse.getControllingPassenger()==player&&horse.isSaddled()&&horse.isAlive()&&player.isAlive(),"Mounted transfer preconditions: controller="+(horse.getControllingPassenger()==player)+" saddle="+horse.isSaddled()+" horseAlive="+horse.isAlive()+" playerAlive="+player.isAlive());test.assertTrue(thmodern.creature.NightmareLanding.find(destination,horse.getX()*scale,horse.getY(),horse.getZ()*scale)!=null,"Prepared landing is safe");test.assertTrue(horse.travel(player),"Mounted nightmare transfers successfully");var moved=destination.getEntity(uuid);
        test.assertTrue(source.getEntity(uuid)==null&&moved instanceof thmodern.creature.NightmareHorse&&player.level()==destination&&player.getVehicle()==moved,"One replacement horse in target dimension with same rider");
        test.assertTrue(((thmodern.creature.NightmareHorse)moved).isSaddled(),"Horse saddle inventory survives dimension transfer");player.stopRiding();player.teleportTo(source,pos.getX(),pos.getY(),pos.getZ(),0,0);moved.discard();test.succeed();
    }
}
