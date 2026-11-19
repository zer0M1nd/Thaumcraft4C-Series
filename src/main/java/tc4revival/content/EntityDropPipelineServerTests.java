package tc4revival.content;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class EntityDropPipelineServerTests {
    @GameTest(template="wand_test_empty")
    public static void entityDropPipelineActualDeath(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DropPipeline"));
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(Enchantments.MOB_LOOTING,9);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);
        for(var type:List.of(EntityType.ZOMBIE,EntityType.ZOMBIE_VILLAGER,EntityType.ZOMBIFIED_PIGLIN,TCEntities.INHABITED_ZOMBIE.get(),EntityType.HUSK,EntityType.DROWNED)) {
            var mob=(net.minecraft.world.entity.LivingEntity)type.create(level);mob.moveTo(Vec3.atBottomCenterOf(pos));level.addFreshEntity(mob);
            mob.hurt(level.damageSources().playerAttack(player),1000);
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4));
            int brains=drops.stream().filter(e->e.getItem().is(TCItems.ZOMBIE_BRAIN.get())).mapToInt(e->e.getItem().getCount()).sum();
            test.assertTrue(brains==(type==TCEntities.INHABITED_ZOMBIE.get()?0:1),"Real death includes modern zombie variants: "+type);
            drops.forEach(Entity::discard);mob.discard();
            level.getEntitiesOfClass(AspectOrbEntity.class,new AABB(pos).inflate(4)).forEach(Entity::discard);
            level.getEntitiesOfClass(EldritchCrabEntity.class,new AABB(pos).inflate(4)).forEach(Entity::discard);
        }
        passed(test,"actual-death-looting-and-legacy-identity");
    }
    @GameTest(template="wand_test_empty")
    public static void entityDropPipelineCombinedRewards(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        var mob=EntityType.ZOMBIE.create(level);mob.moveTo(Vec3.atBottomCenterOf(pos));ChampionMobs.make(mob,ChampionMobs.Kind.BOLD,false);
        // Loot tables use the actual killer equipment and kill credit, not just
        // the synthetic LivingDropsEvent looting integer. Keep a non-FakePlayer
        // killer because champion rewards deliberately exclude FakePlayer kills.
        var killer=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CombinedLoot"));
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(Enchantments.MOB_LOOTING,9);
        killer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);
        mob.setLastHurtByMob(killer);
        var dissolve=new net.minecraft.world.damagesource.DamageSource(
                level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(LiquidDeathDrops.DISSOLVE),killer);
        int crystals=0;
        for(int sample=0;sample<16;sample++) {
            var drops=new ArrayList<ItemEntity>();var ordinary=new ItemEntity(level,pos.getX(),pos.getY(),pos.getZ(),new ItemStack(Items.ROTTEN_FLESH));drops.add(ordinary);
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(mob,dissolve,drops,9,true));
            // Other installed mods may append their own gems/essences to this same event.
            // Validate exact count/order for our rewards while preserving the original object.
            test.assertTrue(drops.stream().filter(drop->drop==ordinary).count()==1,
                    "Original drop survives exactly once");
            var rewards=ownRewards(drops);
            test.assertTrue(rewards.size()>=2 && rewards.get(0).getItem().getItem() instanceof LootBagItem
                    && rewards.get(1).getItem().is(TCItems.ZOMBIE_BRAIN.get())
                    && rewards.get(1).getItem().getCount()==1,
                    "Champion bag precedes one brain before dissolution: "+rewards.stream().map(drop->drop.getItem().toString()).toList());
            test.assertTrue(drops.indexOf(ordinary)<drops.indexOf(rewards.get(0)),"Original precedes TC rewards");
            test.assertTrue(rewards.stream().skip(2).allMatch(drop->drop.getItem().is(TCItems.ESSENCE_CRYSTAL.get())),
                    "Only dissolution crystals append after common material reward");crystals+=rewards.size()-2;
            level.getEntitiesOfClass(ExperienceOrb.class,new AABB(pos).inflate(3)).forEach(Entity::discard);
        }
        test.assertTrue(crystals>0,"Unified handler still produces dissolution crystals");
        var villager=EntityType.VILLAGER.create(level);villager.moveTo(Vec3.atBottomCenterOf(pos));
        var coins=new ArrayList<ItemEntity>();MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(villager,level.damageSources().playerAttack(killer),coins,9,false));
        coins=new ArrayList<>(ownRewards(coins));
        test.assertTrue(coins.size()==1 && coins.get(0).getItem().is(TCItems.material(MaterialType.GOLD_COIN)) && coins.get(0).getY()==villager.getEyeY(),"Villager coin branch needs no recent player credit and uses eye-height drop");
        var brainy=TCEntities.BRAINY_ZOMBIE.get().create(level);var excluded=new ArrayList<ItemEntity>();
        MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(brainy,level.damageSources().generic(),excluded,9,true));
        test.assertTrue(ownRewards(excluded).isEmpty(),"Brainy zombie retains its own custom drops without a second generic brain reward");
        var normal=EntityType.ZOMBIE.create(level);MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(normal,level.damageSources().generic(),excluded,9,false));
        test.assertTrue(ownRewards(excluded).isEmpty(),"Ordinary zombie material reward requires recent player credit");
        passed(test,"ordered-combined-rewards-and-exclusions");
    }
    private static List<ItemEntity> ownRewards(List<ItemEntity> drops) {
        return drops.stream().filter(drop->net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(drop.getItem().getItem()).getNamespace().equals("thaumcraft")).toList();
    }
    private static void passed(GameTestHelper test,String name) { tc4revival.TC4Revival.LOGGER.info("Entity drop pipeline PASS: {}",name);test.succeed(); }
}

