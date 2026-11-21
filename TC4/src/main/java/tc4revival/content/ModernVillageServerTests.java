package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class ModernVillageServerTests {
    private static void pass(GameTestHelper t,String name){tc4revival.TC4Revival.LOGGER.info("Modern village PASS {}",name);t.succeed();}
    @GameTest(template="wand_test_empty",timeoutTicks=2400)
    public static void modernWizardWorkstationLifecycle(GameTestHelper t){workstation(t,LegacyVillagerTrades.Role.WIZARD);}
    @GameTest(template="wand_test_empty",timeoutTicks=2400)
    public static void modernBankerWorkstationLifecycle(GameTestHelper t){workstation(t,LegacyVillagerTrades.Role.BANKER);}
    private static void workstation(GameTestHelper t,LegacyVillagerTrades.Role role) {
        var level=t.getLevel();level.setDayTime(2000);
        var pos=t.absolutePos(new BlockPos(3,2,3));
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.setBlock(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
        var block=role==LegacyVillagerTrades.Role.WIZARD?TCArcane.BLOCK.get():TCArcane.WAND_PEDESTAL.get();
        level.setBlock(pos,block.defaultBlockState(),3);
        var v=EntityType.VILLAGER.create(level);v.moveTo(pos.getX()+1.5,pos.getY(),pos.getZ()+.5);level.addFreshEntity(v);
        t.assertTrue(v.getVillagerData().getProfession()==VillagerProfession.NONE,"Birth does not assign TC profession");
        for(int i=0;i<20;i++)t.assertTrue(v.getBreedOffspring(level,v).getVillagerData().getProfession()==VillagerProfession.NONE,"Children start unemployed");
        t.startSequence().thenWaitUntil(()->t.assertTrue(TCVillagers.role(v)==role,"Vanilla AI finds workstation: "+role))
        .thenExecute(()->{
            t.assertTrue(v.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElseThrow().pos().equals(pos),"Actual JOB_SITE binding");
            t.assertTrue(v.getOffers().size()==2&&v.showProgressBar(),"Two novice trades and vanilla XP bar");
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        }).thenWaitUntil(()->t.assertTrue(v.getVillagerData().getProfession()==VillagerProfession.NONE,"Untraded resident loses destroyed workplace"))
        .thenExecute(()->level.setBlock(pos,block.defaultBlockState(),3))
        .thenWaitUntil(()->t.assertTrue(TCVillagers.role(v)==role,"Reacquire profession through restored workplace"))
        .thenExecute(()->{
            var offer=v.getOffers().get(0);
            for(int i=0;i<5;i++)v.notifyTrade(offer);
            t.assertTrue(v.getVillagerXp()>=10,"Successful trade grants real villager XP");
        }).thenWaitUntil(()->t.assertTrue(v.getVillagerData().getLevel()>=2,"Vanilla level up after trades"))
        .thenExecute(()->{
            t.assertTrue(v.getOffers().size()==4,"Second level adds two independent offers");
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        }).thenIdle(60).thenExecute(()->{
            t.assertTrue(TCVillagers.role(v)==role,"Traded profession remains locked after workstation removal");
            var offer=v.getOffers().get(0);while(!offer.isOutOfStock())offer.increaseUses();
            int uses=offer.getUses();v.setTradingPlayer(null);
            t.assertTrue(uses>0,"Exhausted offer fixture");
            level.setDayTime(6000);
            level.setBlock(pos,block.defaultBlockState(),3);v.moveTo(pos.getX()+1.5,pos.getY(),pos.getZ()+.5);
        }).thenWaitUntil(()->t.assertTrue(v.getOffers().get(0).getUses()==0,"Vanilla working AI restocks at workstation"))
        .thenExecute(()->{v.discard();pass(t,"workstation-lifecycle-"+role);});
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void modernVillageLevelsPricesAndMigration(GameTestHelper t) {
        var level=t.getLevel();
        for(var role:LegacyVillagerTrades.Role.values()) {
            var v=TCVillagers.create(role,level);v.setNoAi(true);
            for(int n=1;n<=5;n++) {
                v.setVillagerData(v.getVillagerData().setLevel(n));v.setOffers(null);
                var offers=v.getOffers();t.assertTrue(offers.size()==2,"Two randomly selected candidates per level");
                t.assertTrue(offers.stream().allMatch(o->o.getXp()>0&&o.getPriceMultiplier()>0),"Modern XP and demand pricing");
                var offer=offers.get(0);while(!offer.isOutOfStock())offer.increaseUses();
                v.restock();t.assertTrue(offer.getUses()==0&&offer.getDemand()>0,"Vanilla restock resets uses and updates demand");
            }
            var old=new MerchantOffers();old.add(new MerchantOffer(new ItemStack(TCItems.material(MaterialType.GOLD_COIN),20),new ItemStack(Items.EMERALD),7,0,0));
            v.setOffers(old);v.setVillagerData(v.getVillagerData().setLevel(1));
            VillageWorkstations.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(v));
            t.assertTrue(v.getOffers().size()==2&&v.getOffers().stream().allMatch(o->o.getXp()>0),"Legacy zero-XP trades migrate once");
            var saved=v.saveWithoutId(new CompoundTag());var loaded=EntityType.VILLAGER.create(level);loaded.load(saved);
            t.assertTrue(loaded.getPersistentData().getBoolean("TC4ModernTrades")&&loaded.getOffers().createTag().equals(v.getOffers().createTag()),"Migration and offers persist");
        }
        pass(t,"levels-prices-migration");
    }
    @GameTest(template="wand_test_empty")
    public static void modernVillageReloadOverride(GameTestHelper t) {
        var v=TCVillagers.create(LegacyVillagerTrades.Role.WIZARD,t.getLevel());
        t.assertTrue(v.getOffers().size()==2&&v.getOffers().stream().allMatch(o->o.getBaseCostA().is(Items.STICK)&&o.getMaxUses()==17&&o.getXp()==9),"Actual data pack replaces novice trades after /reload");
        pass(t,"datapack-reload");
    }
}
