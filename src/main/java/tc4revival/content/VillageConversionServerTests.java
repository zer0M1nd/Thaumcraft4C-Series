package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class VillageConversionServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=180)
    public static void villageInfectionAndGoldenAppleCure(GameTestHelper test) {
        var level=test.getLevel();
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var zombies=new java.util.ArrayList<ZombieVillager>();
        var expected=new java.util.ArrayList<CompoundTag>();
        var roles=LegacyVillagerTrades.Role.values();
        var difficulty=level.getDifficulty();
        try {
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.HARD,true);
            for (int i=0;i<roles.length;i++) {
                var pos=test.absolutePos(new BlockPos(2+i*3,3,3));
                var villager=TCVillagers.create(roles[i],level);
                villager.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);villager.setNoAi(true);villager.setNoGravity(true);
                var offers=villager.getOffers();offers.get(0).increaseUses();
                expected.add(offers.createTag());level.addFreshEntity(villager);
                var attacker=EntityType.ZOMBIE.create(level);attacker.moveTo(villager.position());
                // Real lethal damage enters Zombie.killedEntity and the Forge conversion events.
                test.assertTrue(villager.hurt(level.damageSources().mobAttack(attacker),1000),"Lethal zombie damage accepted");
                var infected=level.getEntitiesOfClass(ZombieVillager.class,new AABB(pos).inflate(1));
                test.assertTrue(infected.size()==1&&villager.isRemoved(),"Actual infection replaces original villager");
                var zombie=infected.get(0);zombie.setNoAi(true);zombie.setNoGravity(true);
                test.assertTrue(zombie.getVillagerData().getProfession()==villager.getVillagerData().getProfession(),"Profession survives infection");
                zombie.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200));
                var apples=new ItemStack(Items.GOLDEN_APPLE,2);player.setItemInHand(InteractionHand.MAIN_HAND,apples);
                test.assertTrue(zombie.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction()&&zombie.isConverting()&&apples.getCount()==1,"Weakness and real apple interaction start cure");
                var saved=zombie.saveWithoutId(new CompoundTag());
                test.assertTrue(saved.getInt("ConversionTime")>=3600&&saved.getInt("ConversionTime")<=6000,"Vanilla cure delay was started");
                test.assertTrue(saved.getCompound("Offers").equals(expected.get(i)),"Trade payload survives infection and save");
                // Exercise persisted conversion restoration, shortening only the test's remaining wait.
                saved.putInt("ConversionTime",1);zombie.load(saved);zombies.add(zombie);
            }
        } finally { level.getServer().setDifficulty(difficulty,true); }
        test.startSequence().thenWaitUntil(()->test.assertTrue(zombies.stream().allMatch(z->z.isRemoved()),"Cures finish through server ticks"))
                .thenExecute(()->{
                    for(int i=0;i<roles.length;i++) {
                        var pos=zombies.get(i).position();
                        var found=level.getEntitiesOfClass(Villager.class,new AABB(pos,pos).inflate(1));
                        test.assertTrue(found.size()==1,"Exactly one cured villager");var cured=found.get(0);
                        test.assertTrue(TCVillagers.role(cured)==roles[i],"Cure retains TC profession and merchant hooks");
                        test.assertTrue(cured.getOffers().createTag().equals(expected.get(i)),"Uses, prices and stock persist across cure");
                        test.assertTrue(cured.showProgressBar()&&cured.getVillagerData().getLevel()==1,"Cure keeps level and modern experience bar");
                        var saved=cured.saveWithoutId(new CompoundTag());var reloaded=EntityType.VILLAGER.create(level);reloaded.load(saved);
                        test.assertTrue(TCVillagers.role(reloaded)==roles[i]&&reloaded.getOffers().createTag().equals(expected.get(i)),"Cured villager NBT restores profession and offers");
                        cured.discard();
                    }
                    player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                    tc4revival.TC4Revival.LOGGER.info("Village conversion test passed: lethal infection, apple interaction, persisted cure, both professions and offers");
                }).thenSucceed();
    }
}
