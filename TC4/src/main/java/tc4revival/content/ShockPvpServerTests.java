package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ShockPvpServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void shockPvpProtectsFirstTargetAndSkipsProtectedChainPlayers(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(2,12,2));var forced=new ArrayList<net.minecraft.world.level.ChunkPos>();for(int x=base.getX()>>4;x<=(base.getX()+11)>>4;x++)for(int z=base.getZ()>>4;z<=(base.getZ()+5)>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new net.minecraft.world.level.ChunkPos(x,z));
        var caster=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ShockPvpCaster"));caster.moveTo(Vec3.atBottomCenterOf(base));caster.setYRot(0);caster.setXRot(0);
        class TargetPlayer extends FakePlayer {
            int hits;float damage;
            TargetPlayer(){super(level,new GameProfile(UUID.randomUUID(),"ShockPvpTarget"));}
            @Override public boolean hurt(DamageSource source,float amount){hits++;damage=amount;return true;}
        }
        test.runAfterDelay(20,()->{boolean previous=level.getServer().isPvpAllowed();var targets=new ArrayList<Entity>();try {
            for(boolean pvp:new boolean[]{false,true})for(boolean chain:new boolean[]{false,true}) {
                level.getServer().setPvpAllowed(pvp);var player=new TargetPlayer();player.moveTo(Vec3.atBottomCenterOf(base.offset(chain?3:0,0,4)));test.assertTrue(level.addFreshEntity(player),"Player fixture added to world query");targets.add(player);
                var first=EntityType.COW.create(level);var near=EntityType.COW.create(level);var far=EntityType.COW.create(level);
                if(chain){first.moveTo(Vec3.atBottomCenterOf(base.south(4)));near.moveTo(Vec3.atBottomCenterOf(base.offset(6,0,4)));far.moveTo(Vec3.atBottomCenterOf(base.offset(9,0,4)));for(var cow:List.of(first,near,far)){cow.setNoAi(true);level.addFreshEntity(cow);targets.add(cow);}}
                test.assertTrue(FocusTargeting.pointed(caster,0,20,1.1)==(chain?first:player),"Actual aiming chooses intended first target");
                var core=new ItemStack(TCItems.SHOCK_FOCUS.get());if(chain)TCItems.SHOCK_FOCUS.get().applyUpgrade(core,FocusUpgrade.CHAIN_LIGHTNING,3);var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,core);caster.setItemInHand(InteractionHand.MAIN_HAND,wand);WandCooldowns.clear(caster);wand.use(level,caster,InteractionHand.MAIN_HAND);TCItems.CASTING_WAND.get().onUseTick(level,caster,wand,100);
                test.assertTrue(player.hits==(pvp?1:0)&&(!pvp||player.damage==4),"PVP switch gates both direct and chained player hurt calls");
                if(chain){test.assertTrue(first.getHealth()==4&&near.getHealth()==6,"Initial six damage and next mob four damage");test.assertTrue(far.getHealth()==(pvp?10:6),"Protected player does not consume a chain hop");}
                test.assertTrue(wand.getTag().getInt("aer")==(chain?2460:2475)&&wand.getTag().getInt("aqua")==(chain?2490:2500),"Casting pays the installed core cost even when PVP prevents damage");caster.stopUsingItem();targets.forEach(Entity::discard);targets.clear();
            }
            tc4revival.TC4Revival.LOGGER.info("PASS shockPvpProtectsFirstTargetAndSkipsProtectedChainPlayers: four direct/chain/PVP cases, protected target skipping and cost");test.succeed();
        }finally{level.getServer().setPvpAllowed(previous);targets.forEach(Entity::discard);caster.stopUsingItem();WandCooldowns.clear(caster);forced.forEach(c->level.setChunkForced(c.x,c.z,false));}});
    }
}
