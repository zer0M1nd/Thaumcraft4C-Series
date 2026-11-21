package ttmodern;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class KamiFocusServerTests {
    private static ItemStack wand(ItemStack focus,boolean charged){var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.THAUMIUM,false,charged);WandFocusItem.attach(wand,focus);return wand;}
    @GameTest(template="wand_test_empty") public static void tinkererXpFocusProportionalCostsAndLeastAspect(GameTestHelper test){
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTExperience"));player.giveExperiencePoints(100);var focus=new ItemStack(Tinkerer.XP_DRAIN_FOCUS.get());var wand=wand(focus,true);var item=TCItems.CASTING_WAND.get();var aspects=TCAspects.catalog().primals();var first=aspects.get(0);var second=aspects.get(1);int capacity=item.capacity(wand);wand.getTag().putInt(first.tag(),capacity-50);wand.getTag().putInt(second.tag(),capacity-1000);
        long before=XpDrainFocus.experience(player);Tinkerer.XP_DRAIN_FOCUS.get().usingTick(wand,player,0);test.assertTrue(item.vis(wand,second)==capacity-500&&item.vis(wand,first)==capacity-50&&XpDrainFocus.experience(player)==before-15,"Exactly 500 centivis goes to the least charged aspect for 15 XP");
        wand.getTag().putInt(second.tag(),capacity);before=XpDrainFocus.experience(player);Tinkerer.XP_DRAIN_FOCUS.get().usingTick(wand,player,0);test.assertTrue(item.vis(wand,first)==capacity&&XpDrainFocus.experience(player)==before-2,"Partial 50 centivis fill costs rounded proportional XP");
        before=XpDrainFocus.experience(player);Tinkerer.XP_DRAIN_FOCUS.get().usingTick(wand,player,0);test.assertTrue(XpDrainFocus.experience(player)==before,"A full wand never spends XP");
        player.experienceLevel=0;player.experienceProgress=0;player.totalExperience=10000;
        wand.getTag().putInt(first.tag(),0);Tinkerer.XP_DRAIN_FOCUS.get().usingTick(wand,player,0);test.assertTrue(item.vis(wand,first)==0,"Stale lifetime total cannot pay for XP missing from level and progress");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererXpFocusUpgradesAndInfusionStorageProtection(GameTestHelper test){
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTExperienceUpgrade"));player.giveExperiencePoints(100);var focus=new ItemStack(Tinkerer.XP_DRAIN_FOCUS.get());Tinkerer.XP_DRAIN_FOCUS.get().applyUpgrade(focus,FocusUpgrade.FRUGAL,1);Tinkerer.XP_DRAIN_FOCUS.get().applyUpgrade(focus,FocusUpgrade.POTENCY,2);var wand=wand(focus,false);long before=XpDrainFocus.experience(player);Tinkerer.XP_DRAIN_FOCUS.get().usingTick(wand,player,0);int sum=TCAspects.catalog().primals().stream().mapToInt(aspect->TCItems.CASTING_WAND.get().vis(wand,aspect)).sum();test.assertTrue(sum==600&&before-XpDrainFocus.experience(player)==14,"Potency increases delivered vis and frugal lowers the independent XP cost");
        var talisman=new ItemStack(Tinkerer.XP_TALISMAN.get());var ingredient=new EmptyXpTalismanIngredient();test.assertTrue(ingredient.test(talisman),"Empty XP talisman accepted");XpTalismanItem.setXp(talisman,10);test.assertTrue(!ingredient.test(talisman),"Upgrade refuses stored XP");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererRecallChargeHotbarAndVisTransaction(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,1,2));level.setBlock(pos,Tinkerer.WARP_GATE.get().defaultBlockState(),3);for(int y=1;y<=3;y++)level.removeBlock(pos.above(y),false);var gate=(WarpGateEntity)level.getBlockEntity(pos);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTRecall")){
            @Override public void teleportTo(double x,double y,double z){setPos(x,y,z);}
            @Override public void closeContainer(){}
        };player.setPos(pos.getX()+5,pos.getY()+1,pos.getZ()+.5);var focus=new ItemStack(Tinkerer.RECALL_FOCUS.get());var wand=wand(focus,true);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);var pearl=new ItemStack(Tinkerer.SKY_PEARL.get());SkyPearlItem.bind(pearl,level,pos);player.getInventory().setItem(2,pearl);var start=player.position();var aer=TCAspects.catalog().require("aer");int before=TCItems.CASTING_WAND.get().vis(wand,aer);
        Tinkerer.RECALL_FOCUS.get().usingTick(wand,player,Integer.MAX_VALUE-60);test.assertTrue(player.position().equals(start),"Recall does not finish before 61 ticks");gate.locked(true);Tinkerer.RECALL_FOCUS.get().usingTick(wand,player,Integer.MAX_VALUE-61);test.assertTrue(player.position().equals(start)&&TCItems.CASTING_WAND.get().vis(wand,aer)==before,"Locked destination costs no vis");gate.locked(false);
        pearl.getTag().putString("Dimension","minecraft:the_nether");Tinkerer.RECALL_FOCUS.get().usingTick(wand,player,Integer.MAX_VALUE-61);test.assertTrue(player.position().equals(start),"Cross-dimension pearl rejected");SkyPearlItem.bind(pearl,level,pos);
        Tinkerer.RECALL_FOCUS.get().usingTick(wand,player,Integer.MAX_VALUE-61);test.assertTrue(player.distanceToSqr(pos.getX()+.5,pos.getY()+1.6,pos.getZ()+.5)<.001&&TCItems.CASTING_WAND.get().vis(wand,aer)<before&&player.getInventory().getItem(2)==pearl,"Successful recall spends vis and retains bound pearl");player.getInventory().setItem(2,ItemStack.EMPTY);player.getInventory().setItem(10,pearl);test.assertTrue(RecallFocus.pearl(player).isEmpty(),"Pearls outside hotbar are not selected");level.removeBlock(pos,false);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererShadowbeamReflectionAndOcclusion(GameTestHelper test){
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(2,1,2));var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTShadow"));player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);player.setYRot(0);player.setXRot(0);
        for(int z=0;z<=6;z++)for(int y=0;y<=3;y++)level.removeBlock(origin.offset(0,y,z),false);for(int y=0;y<=3;y++)level.setBlock(origin.offset(0,y,3),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var front=net.minecraft.world.entity.EntityType.COW.create(level);var hidden=net.minecraft.world.entity.EntityType.COW.create(level);front.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+1.7);hidden.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+4.5);front.setNoAi(true);hidden.setNoAi(true);level.addFreshEntity(front);level.addFreshEntity(hidden);float original=front.getHealth(),hiddenHealth=hidden.getHealth();
        var points=ShadowbeamFocus.trace(level,player,0);test.assertTrue(front.getHealth()==original-TinkererConfig.SHADOW_DAMAGE.get()&&hidden.getHealth()==hiddenHealth,"Beam damages exposed entity once, reflects instead of crossing a solid wall");test.assertTrue(points.size()<=451&&points.stream().anyMatch(p->p.z<player.getZ()),"Trace is bounded and returns behind the caster after reflection");var v=new net.minecraft.world.phys.Vec3(.1,.2,.3);for(var face:net.minecraft.core.Direction.values())test.assertTrue(Math.abs(ShadowbeamFocus.reflect(v,face).length()-v.length())<1e-9,"Reflection preserves speed on every axis");front.discard();hidden.discard();for(int y=0;y<=3;y++)level.removeBlock(origin.offset(0,y,3),false);test.succeed();
    }
}
