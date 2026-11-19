package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.util.FakePlayer;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class FortressServerTests {
    private static  class ArmorPlayer extends FakePlayer {
        ArmorPlayer(ServerLevel level) { super(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FortressTest")); }
        float armorStage(DamageSource source,float damage) { return getDamageAfterArmorAbsorb(source,damage); }
    }
    @GameTest(template="wand_test_empty")
    public static void fortressArmorUsesActualArmorStageAndDurabilityCaps(GameTestHelper test) {
        var player=new ArmorPlayer(test.getLevel());
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TCItems.FORTRESS_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(TCItems.FORTRESS_CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(TCItems.FORTRESS_LEGGINGS.get()));
        float damage=player.armorStage(player.damageSources().cactus(),10);
        test.assertTrue(Math.abs(damage-2)<.0001,"Three pieces give old 80 percent physical reduction; actual="+damage
                +", applies="+FortressProtection.applies(player)+", defense="+TCItems.FORTRESS_HELMET.get().getDefense()+"/"+TCItems.FORTRESS_CHESTPLATE.get().getDefense()+"/"+TCItems.FORTRESS_LEGGINGS.get().getDefense()
                +", slots="+java.util.stream.StreamSupport.stream(player.getArmorSlots().spliterator(),false).toList());
        test.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==3,"Absorbed damage drives armor wear");
        var head=player.getItemBySlot(EquipmentSlot.HEAD); head.getOrCreateTag().putInt("mask",0);
        test.assertTrue(Math.abs(player.armorStage(player.damageSources().cactus(),10)-1.68f)<.0001,"Mask presence adds the original set bonus");
        test.assertTrue(Math.abs(player.armorStage(player.damageSources().inFire(),10))<.0001,"Full masked fortress set normalizes fire absorption");
        test.assertTrue(Math.abs(player.armorStage(player.damageSources().magic(),10)-(10-10*16/35f*1.3f))<.0001,"Magic is protected despite bypass armor");
        test.assertTrue(player.armorStage(player.damageSources().wither(),10)==10,"Legacy wither bypasses armor without being magic");
        int before=head.getDamageValue(); player.armorStage(player.damageSources().fall(),10);
        test.assertTrue(head.getDamageValue()==before,"Fall never damages fortress pieces");
        player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY); player.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);
        head.setDamageValue(head.getMaxDamage());
        test.assertTrue(Math.abs(player.armorStage(player.damageSources().cactus(),20)-19.96f)<.0001,"Last durability unit caps absorption before break");
        pass(test,"armor-stage-and-caps");
    }
    @GameTest(template="wand_test_empty")
    public static void fortressUpgradesPreserveHelmetStateAndGateResearch(GameTestHelper test) {
        var level=test.getLevel(); var helmet=new ItemStack(TCItems.FORTRESS_HELMET.get()); helmet.setDamageValue(71);
        helmet.getOrCreateTag().putInt("mask",2); helmet.getOrCreateTag().putInt("Runic",3);
        var goggles=new ItemStack(TCItems.GOGGLES.get()); goggles.setDamageValue(35);
        var recipe=(InfusionRecipe)level.getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:infusion/fortress_goggles")).orElseThrow();
        var parts=List.of(new ItemStack(Items.SLIME_BALL),goggles);
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(helmet,parts,key->false),level),"Upgrade requires research");
        var input=new InfusionRecipe.Input(helmet,parts,key->true);
        test.assertTrue(recipe.matches(input,level),"Damaged helmet and goggles retain wildcard matching");
        var result=recipe.assemble(input,level.registryAccess());
        test.assertTrue(result.getDamageValue()==71 && result.getTag().getInt("mask")==2 && result.getTag().getInt("Runic")==3
                && FortressArmorItem.hasGoggles(result),"Goggles patch must preserve wear and existing upgrades");
        test.assertTrue(!FortressArmorItem.hasGoggles(helmet),"Recipe output must not mutate its input");
        test.assertTrue(TCItems.FORTRESS_HELMET.get().showNodes(result,null) && TCItems.FORTRESS_HELMET.get().showIngamePopups(result,null),"Goggles provide both revealing interfaces");
        result.getOrCreateTag().putInt("mask",0);
        test.assertTrue(TCItems.FORTRESS_HELMET.get().protectsFromWarp(result),"Grinning mask connects to timed warp protection");
        pass(test,"infusion-state-and-research");
    }
    @GameTest(template="wand_test_empty")
    public static void fortressMasksUseDirectAttackBeforeRunicAbsorption(GameTestHelper test) {
        var level=test.getLevel(); var attacker=new ArmorPlayer(level); var victim=new ArmorPlayer(level);
        var leech=new ItemStack(TCItems.FORTRESS_HELMET.get()); leech.getOrCreateTag().putInt("mask",2); attacker.setItemSlot(EquipmentSlot.HEAD,leech); attacker.setHealth(10);
        var angry=new ItemStack(TCItems.FORTRESS_HELMET.get()); angry.getOrCreateTag().putInt("mask",1); victim.setItemSlot(EquipmentSlot.HEAD,angry);
        var shield=RunicShieldEvents.state(victim); shield.equipment(new RunicShieldState.Equipment(20,0,0,0,0));
        for (int i=1;i<=20;i++) shield.recharge(i,0,()->true);
        var event=new net.minecraftforge.event.entity.living.LivingHurtEvent(victim,level.damageSources().playerAttack(attacker),12);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        test.assertTrue(attacker.getHealth()==11 && attacker.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)
                && attacker.getEffect(net.minecraft.world.effect.MobEffects.WITHER).getDuration()==80,"Both direct-hit mask effects must trigger");
        test.assertTrue(event.getAmount()==0 && shield.charge()==8,"Mask effects must precede complete absorption by the runic shield");
        var arrow=new net.minecraft.world.entity.projectile.Arrow(level,attacker);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingHurtEvent(victim,level.damageSources().arrow(arrow,attacker),12));
        test.assertTrue(attacker.getHealth()==11,"Owned projectile must not be treated as the player direct source");
        pass(test,"direct-mask-effects");
    }
    @GameTest(template="wand_test_empty")
    public static void voidRobesKeepLegacyMagicPriorityAndOrdinaryArmorMixing(GameTestHelper test) {
        var player=new ArmorPlayer(test.getLevel());
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TCItems.VOID_ROBE_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(TCItems.VOID_ROBE_CHEST.get()));
        player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(TCItems.VOID_ROBE_LEGS.get()));
        player.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.DIAMOND_BOOTS));
        test.assertTrue(Math.abs(player.armorStage(player.damageSources().cactus(),10)-2.4f)<1e-5,"VOID material's sixteen robe points plus three ordinary boot points use old linear armor");
        int bootsBefore=player.getItemBySlot(EquipmentSlot.FEET).getDamageValue();
        test.assertTrue(Math.abs(player.armorStage(player.damageSources().magic(),10)-(10-160/35f))<1e-5,"Magic bypasses ordinary boots, but robes still absorb at defense/35");
        test.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).getDamageValue()==bootsBefore,"Bypassed ordinary boots do not wear from magic");
        test.assertTrue(player.armorStage(player.damageSources().wither(),10)==10,"Unblockable nonmagic damage bypasses robes");
        int before=player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();player.armorStage(player.damageSources().fall(),10);
        test.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==before,"Fall cannot damage void robes");
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS}) {
            var stack=player.getItemBySlot(slot);var item=(VoidRobeArmorItem)stack.getItem();
            test.assertTrue(item.warp(stack,player)==2 && item.visDiscount(stack,player,null)==5,"Each robe contributes two warp and five percent discount");
            test.assertTrue(item.showNodes(stack,player)==(slot==EquipmentSlot.HEAD),"Only the void hood reveals nodes");
        }
        pass(test,"void-robe-damage-and-mixed-armor");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Fortress system test passed: {}",name); test.succeed(); }
}
