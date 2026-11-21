package emtmodern.test;

import emtmodern.ElectroMagicTools;
import emtmodern.item.*;
import ic2.api.item.ElectricItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class HammerServerTests {
    public static ItemStack stack(String id){return new ItemStack(ElectroMagicTools.CONTENT.get(id).get());}
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void hammerFollowupDamageUsesLegacyInvulnerabilityAndRealEu(GameTestHelper test){
        var player=GogglesServerTests.player(test);var level=test.getLevel();String[] ids={"tainted_mjolnir","mjolnir","supercharged_mjolnir"};float[] damages={7,12,19};
        var critChance=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib","crit_chance"));
        var critDamage=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib","crit_damage"));
        boolean criticalIntegration=critChance!=null&&critDamage!=null&&player.getAttribute(critChance)!=null&&player.getAttribute(critDamage)!=null;
        // Optional AttributesLib can randomly amplify either hit. Exercise both deterministic
        // ordinary and critical damage through the real event pipeline, without changing it.
        for(float multiplier:criticalIntegration?new float[]{1,1.5f}:new float[]{1})for(int i=0;i<ids.length;i++){
            if(criticalIntegration){player.getAttribute(critChance).setBaseValue(1);player.getAttribute(critDamage).setBaseValue(multiplier);}
            var tool=stack(ids[i]);player.setItemInHand(InteractionHand.MAIN_HAND,tool);var target=EntityType.COW.create(level);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);
            if(i==2)ElectricItem.manager.charge(tool,5000,Integer.MAX_VALUE,true,false);
            boolean firstHit=target.hurt(level.damageSources().playerAttack(player),7);
            float firstHealth=target.getHealth();int immunity=target.invulnerableTime;
            tool.hurtEnemy(target,player);
            float expected=100-damages[i]*multiplier;
            test.assertTrue(Math.abs(firstHealth-(100-7*multiplier))<.001,"Initial hit follows configured critical multiplier: "+multiplier+" health="+firstHealth);
            test.assertTrue(Math.abs(target.getHealth()-expected)<.001,"Legacy second hit is adjusted by vanilla damage immunity: "+ids[i]+" multiplier="+multiplier+" firstHit="+firstHit+" firstHealth="+firstHealth+" immunity="+immunity+" after="+target.getHealth()+" expected="+expected);
            if(i==2)test.assertTrue(ElectricItem.manager.getCharge(tool)==0,"Supercharged follow-up consumes 5000 real EU");else test.assertTrue(tool.getDamageValue()==1,"Non-electric melee consumes one durability");
        }
        var empty=stack("supercharged_mjolnir");var victim=EntityType.COW.create(level);float health=victim.getHealth();empty.getItem().hurtEnemy(empty,victim,player);test.assertTrue(victim.getHealth()==health,"Empty electric hammer has no additional damage");
        for(String id:java.util.List.of("mjolnir","supercharged_mjolnir"))test.assertTrue(level.getRecipeManager().byKey(new ResourceLocation("emt",id)).isPresent(),"Original hammer infusion loads: "+id);
        test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void hammerStormAndTaintedBombsUseExactLegacyPatterns(GameTestHelper test){
        var level=test.getLevel();var player=GogglesServerTests.player(test);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(test.absolutePos(new BlockPos(3,40,3)).getCenter());var area=player.getBoundingBox().inflate(20);
        var broken=stack("tainted_mjolnir");player.setItemInHand(InteractionHand.MAIN_HAND,broken);broken.getItem().use(level,player,InteractionHand.MAIN_HAND);
        var bombs=level.getEntitiesOfClass(tc4revival.content.AlumentumEntity.class,area);test.assertTrue(bombs.size()==6&&broken.getDamageValue()==20,"Tainted hammer creates six alumentum and costs 20 durability");
        for(var offset:BrokenThorHammer.BOMB_OFFSETS)test.assertTrue(bombs.stream().anyMatch(e->e.position().distanceToSqr(player.position().add(offset[0],offset[1],offset[2]))<.0001&&e.getDeltaMovement().lengthSqr()==0),"Legacy stationary bomb offset");bombs.forEach(Entity::discard);
        var charged=stack("supercharged_mjolnir");player.setItemInHand(InteractionHand.MAIN_HAND,charged);ElectricItem.manager.charge(charged,80000,Integer.MAX_VALUE,true,false);charged.getItem().use(level,player,InteractionHand.MAIN_HAND);
        var bolts=level.getEntitiesOfClass(LightningBolt.class,area);var guardians=level.getEntitiesOfClass(ArcherEntity.class,area);
        test.assertTrue(bolts.size()==24&&guardians.size()==4&&ElectricItem.manager.getCharge(charged)==5000,"Storm creates 24 bolts/four guardians and consumes 75000 EU");
        for(var offset:ElectricThorHammer.LIGHTNING_OFFSETS)test.assertTrue(bolts.stream().anyMatch(e->e.position().distanceToSqr(player.position().add(offset[0],0,offset[1]))<.0001),"Legacy lightning offset");
        bolts.forEach(Entity::discard);guardians.forEach(Entity::discard);charged.getItem().use(level,player,InteractionHand.MAIN_HAND);
        bolts=level.getEntitiesOfClass(LightningBolt.class,area);test.assertTrue(bolts.size()==1&&player.hasEffect(net.minecraft.world.effect.MobEffects.HARM)&&ElectricItem.manager.getCharge(charged)==5000,"Insufficient power strikes user and applies instant harm without charge debit");bolts.forEach(Entity::discard);
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);charged.getItem().use(level,player,InteractionHand.MAIN_HAND);test.assertTrue(level.getEntitiesOfClass(LightningBolt.class,area).isEmpty(),"Original creative right click does nothing");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void archerUsesOriginalAttributesAndEnchantedArrowAttack(GameTestHelper test){
        var level=test.getLevel();var archer=ElectroMagicTools.ARCHER_ENTITY.get().create(level);archer.setPos(test.absolutePos(new BlockPos(2,40,2)).getCenter());level.addFreshEntity(archer);
        test.assertTrue(archer.getMaxHealth()==40&&Math.abs(archer.getAttributeValue(Attributes.MOVEMENT_SPEED)-.3)<.00001&&archer.getMainHandItem().isEmpty(),"Hammer summon retains 40 HP/.3 speed and no spawn-equipment initialization");
        var bow=new ItemStack(Items.BOW);bow.enchant(Enchantments.POWER_ARROWS,2);bow.enchant(Enchantments.PUNCH_ARROWS,1);bow.enchant(Enchantments.FLAMING_ARROWS,1);archer.setItemSlot(EquipmentSlot.MAINHAND,bow);
        var target=EntityType.ZOMBIE.create(level);target.setPos(archer.position().add(0,0,8));archer.performRangedAttack(target,1);
        var arrows=level.getEntitiesOfClass(Arrow.class,archer.getBoundingBox().inflate(4));test.assertTrue(arrows.size()==1&&arrows.get(0).getOwner()==archer&&arrows.get(0).getKnockback()==1&&arrows.get(0).isOnFire()&&arrows.get(0).getBaseDamage()>2,"Actual arrow inherits Power/Punch/Flame with guardian owner");arrows.forEach(Entity::discard);
        archer.die(level.damageSources().generic());test.assertTrue(archer.isRemoved(),"Guardian death follows immediate legacy explosion/removal");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void taintedHammerLootIsWeightedAndDisabledByResearchConfig(GameTestHelper test){
        var level=test.getLevel();var table=level.getServer().getLootData().getLootTable(new ResourceLocation("minecraft","chests/simple_dungeon"));var data=Deserializers.createLootTableSerializer().create().toJson(table);
        test.assertTrue(data.contains("emt:tainted_mjolnir")&&data.contains("emt:one_ring")&&data.contains("minecraft:saddle"),"Both added weighted entries preserve original dungeon rewards");
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.ZERO).create(LootContextParamSets.CHEST);boolean old=HammerConfig.DISABLE_MJOLNIR.get();int weight=HammerConfig.BROKEN_WEIGHT.get();
        try{HammerConfig.DISABLE_MJOLNIR.set(false);HammerConfig.BROKEN_WEIGHT.set(25);int count=0;for(int seed=1;seed<=128;seed++)for(var drop:table.getRandomItems(params,seed))if(drop.is(stack("tainted_mjolnir").getItem()))count++;test.assertTrue(count>0,"Seeded dungeon generation yields tainted hammers");
            HammerConfig.DISABLE_MJOLNIR.set(true);for(int seed=1;seed<=32;seed++)test.assertTrue(table.getRandomItems(params,seed).stream().noneMatch(s->s.is(stack("tainted_mjolnir").getItem())),"Disabled Mjolnir removes live dungeon weighting");
        }finally{HammerConfig.DISABLE_MJOLNIR.set(old);HammerConfig.BROKEN_WEIGHT.set(weight);}test.succeed();
    }
}
