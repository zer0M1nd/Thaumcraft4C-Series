package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class HellbatNaturalTargetServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void naturalFireBatSkipsInvulnerablePlayersAndKeepsTwelveRange(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(5,20,5));var point=Vec3.atBottomCenterOf(base);
        var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();for(int x=base.getX()>>4;x<=(base.getX()+15)>>4;x++)for(int z=base.getZ()>>4;z<=base.getZ()>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new net.minecraft.world.level.ChunkPos(x,z));
        var near=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"BatCreative"));var far=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"BatSurvival"));
        var bat=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);bat.moveTo(point);
        test.runAfterDelay(20,()->{
            try {
                near.setGameMode(GameType.CREATIVE);near.moveTo(point.add(3,0,0));far.setGameMode(GameType.SURVIVAL);far.moveTo(point.add(6,0,0));level.addFreshEntity(near);level.addFreshEntity(far);
                test.assertTrue(level.players().containsAll(List.of(near,far)),"Both fixture players belong to actual level player query");
                bat.customServerAiStep();test.assertTrue(bat.getTarget()==far,"Closer creative player cannot hide vulnerable survival target");
                far.getAbilities().invulnerable=true;bat.customServerAiStep();test.assertTrue(bat.getTarget()==null,"No vulnerable player means no target, even if survival has invulnerability ability");
                far.getAbilities().invulnerable=false;far.moveTo(point.add(13,0,0));bat.customServerAiStep();test.assertTrue(bat.getTarget()==null,"Outside twelve-block acquisition range rejected");
                far.moveTo(point.add(11,0,0));bat.customServerAiStep();test.assertTrue(bat.getTarget()==far,"Inside twelve-block acquisition range selected");
                far.setGameMode(GameType.SPECTATOR);bat.customServerAiStep();test.assertTrue(bat.getTarget()==null,"Target changing to spectator is cleared and not reacquired");
                far.setGameMode(GameType.SURVIVAL);far.setShiftKeyDown(true);far.moveTo(point.add(10,0,0));bat.customServerAiStep();test.assertTrue(bat.getTarget()==null,"Sneaking contracts old detection radius to 9.6");
                far.moveTo(point.add(9,0,0));bat.customServerAiStep();test.assertTrue(bat.getTarget()==far,"Sneaking inside reduced radius remains visible");bat.setTarget(null);far.setShiftKeyDown(false);far.setInvisible(true);
                far.moveTo(point.add(1,0,0));bat.customServerAiStep();test.assertTrue(bat.getTarget()==null,"Unarmored invisibility uses minimum armor visibility .1");
                var slots=List.of(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET);
                for(var slot:slots)far.setItemSlot(slot,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
                far.moveTo(point.add(8,0,0));bat.customServerAiStep();test.assertTrue(bat.getTarget()==far,"Four occupied armor slots give invisible radius 8.4");bat.setTarget(null);
                far.moveTo(point.add(9,0,0));bat.customServerAiStep();test.assertTrue(bat.getTarget()==null,"Invisible target outside armored radius rejected");
                tc4revival.TC4Revival.LOGGER.info("PASS naturalFireBatSkipsInvulnerablePlayersAndKeepsTwelveRange: creative masking, invulnerability, spectator, sneaking/invisibility/armor and range");test.succeed();
            } finally {bat.discard();near.discard();far.discard();forced.forEach(c->level.setChunkForced(c.x,c.z,false));}
        });
    }
}
