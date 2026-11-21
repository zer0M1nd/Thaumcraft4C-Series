package tc4revival.content;

import java.util.*;
import java.util.function.Consumer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class HellbatTargetServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void hellbatTargetExclusionRangePvpAndPayment(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(5,20,5));var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"BatTargets"));owner.moveTo(Vec3.atBottomCenterOf(base));owner.setYRot(0);owner.setXRot(0);
        var forced=new ArrayList<ChunkPos>();for(int x=(base.getX()-2)>>4;x<=(base.getX()+2)>>4;x++)for(int z=base.getZ()>>4;z<=(base.getZ()+36)>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new ChunkPos(x,z));
        test.runAfterDelay(20,()->{
            var actors=new ArrayList<Entity>();var summoned=new ArrayList<FireBatEntity>();var saved=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();boolean pvp=level.getServer().isPvpAllowed();
            Consumer<EntityJoinLevelEvent> capture=e->{if(e.getLevel()==level&&e.getEntity() instanceof FireBatEntity bat&&bat.summoned()&&bat.distanceToSqr(owner)<9)summoned.add(bat);};MinecraftForge.EVENT_BUS.addListener(capture);
            try {
                for(int scene=0;scene<7;scene++) {
                    LivingEntity target=null;boolean expected=scene==2||scene==5;
                    level.getServer().setPvpAllowed(scene==5);
                    if(scene==1||scene==2){var blocker=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);blocker.moveTo(owner.getEyePosition().add(0,-.4,3));level.addFreshEntity(blocker);actors.add(blocker);}
                    if(scene>=2){target=scene==4||scene==5?new FakePlayer(level,new GameProfile(UUID.randomUUID(),"BatPvpTarget")):EntityType.COW.create(level);target.moveTo(owner.position().add(0,0,scene==2?31:scene==3?34:6));test.assertTrue(level.addFreshEntity(target),"Target fixture joins world");actors.add(target);}
                    if(scene==6)for(int y=0;y<4;y++){var pos=base.offset(0,y,3);saved.put(pos,level.getBlockState(pos));level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);}
                    var stack=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(stack,new ItemStack(TCItems.HELLBAT_FOCUS.get()));owner.setItemInHand(InteractionHand.MAIN_HAND,stack);var before=stack.copy();WandCooldowns.clear(owner);
                    stack.use(level,owner,InteractionHand.MAIN_HAND);
                    test.assertTrue(summoned.size()==(expected?1:0),"Summon eligibility scene="+scene);
                    if(expected)test.assertTrue(summoned.get(0).getTarget()==target,"Excluded fire bat does not steal intended target");
                    for(var aspect:tc4revival.aspect.TCAspects.catalog().primals()){int cost=expected?switch(aspect.tag()){case "ignis"->200;case "aer","perditio"->100;default->0;}:0;test.assertTrue(TCItems.CASTING_WAND.get().vis(before,aspect)-TCItems.CASTING_WAND.get().vis(stack,aspect)==cost,"Rejected target never pays: scene="+scene);}
                    summoned.forEach(Entity::discard);summoned.clear();actors.forEach(Entity::discard);actors.clear();saved.forEach((p,s)->level.setBlock(p,s,2));saved.clear();
                }
                tc4revival.TC4Revival.LOGGER.info("PASS hellbatTargetExclusionRangePvpAndPayment: seven no-target/same-type/31-34-range/PVP/wall scenes");test.succeed();
            } finally {MinecraftForge.EVENT_BUS.unregister(capture);level.getServer().setPvpAllowed(pvp);summoned.forEach(Entity::discard);actors.forEach(Entity::discard);saved.forEach((p,s)->level.setBlock(p,s,2));WandCooldowns.clear(owner);forced.forEach(c->level.setChunkForced(c.x,c.z,false));}
        });
    }

    @GameTest(template="wand_test_empty")
    public static void hellbatUpgradeChoicesAndVampireResearchGate(GameTestHelper test) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"BatResearch"));var focus=TCItems.HELLBAT_FOCUS.get();var core=new ItemStack(focus);
        for(int rank=0;rank<=6;rank++) {
            List<FocusUpgrade> expected=switch(rank){case 1,2,4->List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY);case 3->List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,FocusUpgrade.BAT_BOMBS,FocusUpgrade.DEVIL_BATS);case 5->List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,FocusUpgrade.VAMPIRE_BATS);default->List.of();};
            test.assertTrue(focus.availableUpgrades(core,rank).equals(expected),"Legacy rank choices "+rank);
            for(var upgrade:expected)test.assertTrue(focus.canApplyUpgrade(core,player,upgrade,rank)==(upgrade!=FocusUpgrade.VAMPIRE_BATS),"Only vampire requires extra research");
        }
        var knowledge=tc4revival.research.KnowledgeSavedData.get(player.server);knowledge.update(player.getUUID(),bound->0,k->{k.rememberResearch("FOCUSHELLBAT");return null;});
        test.assertTrue(!focus.canApplyUpgrade(core,player,FocusUpgrade.VAMPIRE_BATS,5),"Base focus research does not substitute VAMPBAT");
        knowledge.update(player.getUUID(),bound->0,k->{k.rememberResearch("VAMPBAT");return null;});
        test.assertTrue(focus.canApplyUpgrade(core,player,FocusUpgrade.VAMPIRE_BATS,5),"Actual server knowledge unlocks vampire choice");
        tc4revival.TC4Revival.LOGGER.info("PASS hellbatUpgradeChoicesAndVampireResearchGate: all ranks and absent/base/vampire knowledge");test.succeed();
    }
}
