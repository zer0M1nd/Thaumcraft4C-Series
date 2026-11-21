package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class EarthShockServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void earthShockVisibilityUsesFeetRatherThanEyes(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(3,18,3));var chunk=new ChunkPos(base);boolean forced=level.setChunkForced(chunk.x,chunk.z,true);
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"EarthSight"));owner.moveTo(Vec3.atCenterOf(base.above(10)));
        test.runAfterDelay(20,()->{try {
            for(boolean high:new boolean[]{false,true}) {
                var wall=base.east(2).above(high?1:0);level.setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
                var target=EntityType.COW.create(level);target.moveTo(Vec3.atCenterOf(base.east(4)));target.setNoAi(true);level.addFreshEntity(target);
                try {
                    var orb=new ShockOrbEntity(level,owner,6,5);orb.setPos(Vec3.atCenterOf(base));
                    test.assertTrue(FocusTargeting.visible(orb,target.getEyePosition())!=high,"Fixture makes eye-based ray disagree with old position-based visibility");
                    orb.onHit(new BlockHitResult(orb.position(),Direction.UP,base.below(),false));
                    test.assertTrue(target.getMaxHealth()-target.getHealth()==(high?5:0),"Low wall blocks foot ray, high wall leaves foot ray clear");
                }finally{target.discard();level.setBlock(wall,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);for(var p:BlockPos.betweenClosed(base.offset(-6,-1,-6),base.offset(6,7,6)))if(level.getBlockState(p).is(TCArcane.SHOCK_FIELD.get()))level.setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);}
            }
            tc4revival.TC4Revival.LOGGER.info("PASS earthShockVisibilityUsesFeetRatherThanEyes: low/high obstacle cases with inverse eye-ray control");test.succeed();
        }finally{if(forced)level.setChunkForced(chunk.x,chunk.z,false);}});
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void earthShockUpgradesDamageCubeAndPaymentMatchLegacy(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(12,20,12));var center=Vec3.atCenterOf(base);var forced=new ArrayList<ChunkPos>();for(int x=(base.getX()-12)>>4;x<=(base.getX()+12)>>4;x++)for(int z=(base.getZ()-12)>>4;z<=(base.getZ()+12)>>4;z++)if(level.setChunkForced(x,z,true))forced.add(new ChunkPos(x,z));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"EarthShock"));player.moveTo(center);var targets=new ArrayList<LivingEntity>();var shots=new ArrayList<ShockOrbEntity>();var item=TCItems.SHOCK_FOCUS.get();var wandItem=TCItems.CASTING_WAND.get();
        test.runAfterDelay(20,()->{try {
            // The production flat world has stone above the small template; keep foot rays unobstructed.
            for(var cell:BlockPos.betweenClosed(base.offset(-12,-1,-12),base.offset(12,5,12)))level.setBlock(cell,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
            var critChance=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new net.minecraft.resources.ResourceLocation("attributeslib","crit_chance"));
            if(critChance!=null&&player.getAttribute(critChance)!=null)player.getAttribute(critChance).setBaseValue(0);
            for(int enlarge=0;enlarge<=2;enlarge++)for(int potency:new int[]{0,2}) {
                int area=4+enlarge*2;float damage=(float)(5+potency*1.33);var core=new ItemStack(item);item.applyUpgrade(core,FocusUpgrade.EARTH_SHOCK,3);if(enlarge>0)item.applyUpgrade(core,FocusUpgrade.ENLARGE,4);if(enlarge>1)item.applyUpgrade(core,FocusUpgrade.ENLARGE,5);if(potency>0){item.applyUpgrade(core,FocusUpgrade.POTENCY,1);item.applyUpgrade(core,FocusUpgrade.POTENCY,2);}
                var wand=wandItem.create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,core);player.setItemInHand(InteractionHand.MAIN_HAND,wand);var before=wand.copy();WandCooldowns.clear(player);wand.use(level,player,InteractionHand.MAIN_HAND);
                var found=level.getEntitiesOfClass(ShockOrbEntity.class,new AABB(base).inflate(4));test.assertTrue(found.size()==1,"Actual earthshock wand creates one orb");var orb=found.get(0);shots.add(orb);orb.setPos(center);test.assertTrue(orb.getOwner()==player,"Caster attribution retained");
                for(var offset:new Vec3[]{new Vec3(1,0,0),new Vec3(area-.1,0,area-.1),new Vec3(area+1,0,0)}){var cow=EntityType.COW.create(level);cow.moveTo(center.add(offset));cow.setNoAi(true);level.addFreshEntity(cow);targets.add(cow);}
                test.assertTrue(level.getEntitiesOfClass(LivingEntity.class,new AABB(base).inflate(12)).containsAll(targets),"All targets visible to entity query");
                orb.onHit(new BlockHitResult(center,Direction.UP,base.below(),false));
                for(int i=0;i<3;i++)test.assertTrue(Math.abs(targets.get(i).getMaxHealth()-targets.get(i).getHealth()-(i<2?damage:0))<.0001,"Actual earthshock cube includes diagonal beyond spherical radius, excludes outside box; enlarge="+enlarge+" potency="+potency+" target="+i+" health="+targets.get(i).getHealth()+" max="+targets.get(i).getMaxHealth()+" expectedDamage="+(i<2?damage:0));
                test.assertTrue(orb.isRemoved(),"Impact consumes orb");
                for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())test.assertTrue(wandItem.vis(before,aspect)-wandItem.vis(wand,aspect)==(aspect.tag().equals("aer")?75:aspect.tag().equals("terra")?25:0),"Earthshock pays both advertised aspects");
                targets.forEach(Entity::discard);targets.clear();
            }
            tc4revival.TC4Revival.LOGGER.info("PASS earthShockUpgradesDamageCubeAndPaymentMatchLegacy: six enlargement/potency casts, cube corners, outer boundary, damage and legacy debit");test.succeed();
        }finally{targets.forEach(Entity::discard);shots.forEach(Entity::discard);WandCooldowns.clear(player);forced.forEach(c->level.setChunkForced(c.x,c.z,false));}});
    }
}
