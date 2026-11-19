package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ShockChainServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void shockChainUsesNearestUnvisitedTargetsAndLegacyPayment(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(3,12,3));var forced=new ArrayList<ChunkPos>();for(int x=base.getX()>>4;x<=(base.getX()+46)>>4;x++)for(int z=base.getZ()>>4;z<=(base.getZ()+6)>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new ChunkPos(x,z));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ShockChain"));player.moveTo(Vec3.atBottomCenterOf(base));player.setYRot(0);player.setXRot(0);var targets=new ArrayList<LivingEntity>();var walls=new ArrayList<BlockPos>();
        var focusItem=TCItems.SHOCK_FOCUS.get();var wandItem=TCItems.CASTING_WAND.get();
        // Forced chunks become visible to the entity section manager on world ticks.
        test.runAfterDelay(20, () -> {
        int cases=0;
        try {
            // Each six-block hop is visible to the cube query but blocked by a wall.
            for(int i=0;i<7;i++)for(int y=0;y<4;y++){var wall=base.offset(i*6+3,y,4);walls.add(wall);level.setBlock(wall,Blocks.STONE.defaultBlockState(),2);}
            for(int hopsMode=-1;hopsMode<=2;hopsMode++)for(int potency:new int[]{0,2}) {
                for(int i=0;i<8;i++){var cow=EntityType.COW.create(level);cow.moveTo(Vec3.atBottomCenterOf(base.offset(i*6,0,4)));cow.setNoAi(true);cow.setNoGravity(true);level.addFreshEntity(cow);targets.add(cow);}
                test.assertTrue(level.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(base).inflate(50)).containsAll(targets),"All eight fixture targets must be visible to world entity queries before casting");
                var core=new ItemStack(focusItem);if(potency>0){focusItem.applyUpgrade(core,FocusUpgrade.POTENCY,1);focusItem.applyUpgrade(core,FocusUpgrade.POTENCY,2);}if(hopsMode>=0)focusItem.applyUpgrade(core,FocusUpgrade.CHAIN_LIGHTNING,3);if(hopsMode>0)focusItem.applyUpgrade(core,FocusUpgrade.ENLARGE,4);if(hopsMode>1)focusItem.applyUpgrade(core,FocusUpgrade.ENLARGE,5);
                var wand=wandItem.create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,core);player.setItemInHand(InteractionHand.MAIN_HAND,wand);var before=wand.copy();WandCooldowns.clear(player);wand.use(level,player,InteractionHand.MAIN_HAND);wandItem.onUseTick(level,player,wand,100);
                int additional=hopsMode<0?0:2+hopsMode*2; if(hopsMode==2)tc4revival.TC4Revival.LOGGER.info("Shock diagnostic: health={}, neighbors={}",targets.stream().map(LivingEntity::getHealth).toList(),level.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(base).inflate(50)).stream().map(e->e.getType()+":"+e.blockPosition()).toList());
                for(int i=0;i<8;i++){float expected=i==0?(hopsMode<0?4:6)+potency:i<=additional?4+potency:0;test.assertTrue(targets.get(i).getMaxHealth()-targets.get(i).getHealth()==expected,"Chain damage and unvisited nearest order index="+i+" enlarge="+hopsMode+" potency="+potency);}
                for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())test.assertTrue(wandItem.vis(before,aspect)-wandItem.vis(wand,aspect)==(aspect.tag().equals("aer")?(hopsMode<0?25:40):aspect.tag().equals("aqua")&&hopsMode>=0?10:0),"Installed chain mode pays its advertised cost once per cast");
                player.stopUsingItem();targets.forEach(Entity::discard);targets.clear();cases++;
            }
            tc4revival.TC4Revival.LOGGER.info("PASS shockChainUsesNearestUnvisitedTargetsAndLegacyPayment: {} ordinary/chain/enlarge/potency casts, walls between hops",cases);test.succeed();
        }finally{targets.forEach(Entity::discard);walls.forEach(p->level.setBlock(p,Blocks.AIR.defaultBlockState(),2));player.stopUsingItem();WandCooldowns.clear(player);forced.forEach(c->level.setChunkForced(c.x,c.z,false));}
        });
    }
}

