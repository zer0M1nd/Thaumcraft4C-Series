package tc4revival.content;

import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Integrated rules and transformed vanilla entry points; authored for the deferred checkpoint. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class LegacyLootServerTests {
    private LegacyLootServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void enchantingRetainsLegacyEligibilityAndSymmetricConflicts(GameTestHelper test) {
        var pick=new ItemStack(TCItems.voidTool(VoidEquipment.Tool.PICKAXE));
        var hoe=new ItemStack(TCItems.voidTool(VoidEquipment.Tool.HOE));
        var axe=new ItemStack(TCItems.voidTool(VoidEquipment.Tool.AXE));
        var book=new ItemStack(Items.BOOK);
        var pickChoices=LegacyRandomEnchantments.candidates(pick,30);
        test.assertTrue(pickChoices.containsKey(151) && pickChoices.containsKey(34) && pickChoices.containsKey(35),"Repair and unbreaking are candidates before compatibility filtering");
        var hoeChoices=LegacyRandomEnchantments.candidates(hoe,30);
        test.assertTrue(hoeChoices.containsKey(32) && hoeChoices.containsKey(35) && hoeChoices.containsKey(151),"Modern hoes support efficiency, fortune and TC repair");
        test.assertTrue(!LegacyRandomEnchantments.candidates(axe,30).containsKey(16),"Native enchanting-table eligibility leaves axe damage enchants to the anvil");
        test.assertTrue(LegacyRandomEnchantments.candidates(book,30).containsKey(150) && LegacyRandomEnchantments.candidates(book,30).containsKey(151),"Books bypass equipment eligibility for both TC enchantments");
        for (var first:LegacyRandomEnchantments.Definition.values()) for (var second:LegacyRandomEnchantments.Definition.values())
            test.assertTrue(LegacyRandomEnchantments.compatible(first,second)==LegacyRandomEnchantments.compatible(second,first),"Both sides can veto compatibility, as in Forge 1.7.10");
        test.assertTrue(!LegacyRandomEnchantments.compatible(LegacyRandomEnchantments.Definition.SILK_TOUCH,LegacyRandomEnchantments.Definition.LOOTING),"Native loot-bonus enchantments reject silk touch");
        test.assertTrue(LegacyRandomEnchantments.compatible(LegacyRandomEnchantments.Definition.REPAIR,LegacyRandomEnchantments.Definition.UNBREAKING),"Repair accepts unbreaking");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void vanillaEnchantmentInstancesUseHasteWeightThree(GameTestHelper test) {
        test.assertTrue(new EnchantmentInstance(TCEnchantments.HASTE.get(),1).getWeight().asInt()==3,"The real vanilla weighted entry must receive Haste's legacy weight");
        test.assertTrue(new EnchantmentInstance(Enchantments.SHARPNESS,1).getWeight().asInt()==10 && new EnchantmentInstance(TCEnchantments.REPAIR.get(),1).getWeight().asInt()==2,"Other enchantment weights remain their registered values");
        test.assertTrue(TCEnchantments.HASTE.get().canEnchant(new ItemStack(Items.LEATHER_BOOTS)) && !TCEnchantments.HASTE.get().canEnchant(new ItemStack(Items.LEATHER_CHESTPLATE)),"Ordinary chest armor does not count as a hovering harness");
        var player=test.makeMockPlayer(); var boots=new ItemStack(Items.LEATHER_BOOTS); boots.enchant(TCEnchantments.HASTE.get(),3);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,boots); player.setYRot(0); player.zza=1;
        player.getAbilities().flying=false; player.hasImpulse=false; player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        HasteMovement.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(player));
        test.assertTrue(Math.abs(player.getDeltaMovement().z-.045)<.00001,"The actual worn-boots event adds the legacy forward acceleration");
        player.hasImpulse=true; player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        HasteMovement.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(player));
        test.assertTrue(Math.abs(player.getDeltaMovement().z-.0225)<.00001,"The legacy airborne/impulse flag halves acceleration");
        player.getAbilities().flying=true; player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        HasteMovement.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(player));
        test.assertTrue(player.getDeltaMovement().lengthSqr()==0,"Ability flight disables boots acceleration");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void firstBookEnchantIsSharedAcrossBagRaritiesButOutputsAreIndependent(GameTestHelper test) {
        var json=JsonParser.parseString("{\"entries\":[{\"stack\":{\"item\":\"minecraft:book\"},\"weights\":[10,10,10]}]}").getAsJsonObject();
        var pool=LegacyLootBags.decode(json); var random=new NoGearRandom();
        var first=pool.generate(0,random);
        test.assertTrue(first.is(Items.ENCHANTED_BOOK),"The first selected book changes the shared template into an enchanted book");
        var original=first.copy(); first.getOrCreateTag().putString("test_only_output","changed");
        random.intCalls=0; var second=pool.generate(2,random);
        test.assertTrue(random.intCalls>1,"Each selected book rolls its enchantments independently");
        test.assertTrue(second.is(Items.ENCHANTED_BOOK) && !net.minecraft.world.item.EnchantedBookItem.getEnchantments(second).isEmpty() && !second.getTag().contains("test_only_output"),"Later books are enchanted and do not share output NBT");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void preservedPotionDurationReachesVanillaPotionUtils(GameTestHelper test) {
        var plain=PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),Potions.REGENERATION);
        var legacy=plain.copy(); var effects=new ListTag(); effects.add(new MobEffectInstance(MobEffects.REGENERATION,675,0).save(new CompoundTag()));
        legacy.getOrCreateTag().put(LegacyPotionEffects.TAG,effects);
        test.assertTrue(PotionUtils.getMobEffects(legacy).size()==1 && PotionUtils.getMobEffects(legacy).get(0).getDuration()==Potions.REGENERATION.getEffects().get(0).getDuration(),"Modern base potion duration also applies to old marked loot");
        test.assertTrue(PotionUtils.getMobEffects(plain).get(0).getDuration()==Potions.REGENERATION.getEffects().get(0).getDuration(),"Unmarked modern potions keep their ordinary effects");
        PotionUtils.setCustomEffects(legacy,java.util.List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,0)));
        test.assertTrue(PotionUtils.getMobEffects(legacy).size()==2,"Explicit added custom effects are not lost by the legacy base override");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void equipmentQualitySixAndSharedAmuletTemplatesResolveRealItems(GameTestHelper test) {
        for (int slot=0;slot<=4;slot++) for (int quality=0;quality<=6;quality++)
            test.assertTrue(LegacyLootBags.gearItem(slot,quality)!=Items.AIR,"Every reachable gear slot and quality has an actual item");
        test.assertTrue(LegacyLootBags.gearItem(0,0)==Items.IRON_AXE && LegacyLootBags.gearItem(0,2)==Items.GOLDEN_AXE && LegacyLootBags.gearItem(0,6)==TCItems.voidTool(VoidEquipment.Tool.SWORD),"Weapon quality is the old mixed axe/sword sequence");
        var first=LegacyLootTemplates.prechargedAmulet(bound->0); var second=LegacyLootTemplates.prechargedAmulet(bound->4);
        test.assertTrue(!ItemStack.isSameItemSameTags(first,second),"Each reward rolls a separate amulet charge pattern");
        var amulet=(VisAmuletItem)first.getItem();
        for (var aspect:tc4revival.aspect.TCAspects.catalog().primals()) {
            int amount=amulet.vis(first,aspect); test.assertTrue(amount>=0 && amount<=400 && amount%100==0,"Each primal starts at zero through four whole vis");
        }
        first.getOrCreateTag().putString("test_only_output","changed");
        test.assertTrue(!LegacyLootTemplates.prechargedAmulet().getTag().contains("test_only_output"),"Loot templates never alias the returned stack");
        test.succeed();
    }
    private static  class NoGearRandom extends LegacyRandomSource {
        int intCalls;
        NoGearRandom() { super(17010); }
        @Override public float nextFloat() { return .5f; }
        @Override public int nextInt(int bound) { intCalls++; return super.nextInt(bound); }
    }
}
