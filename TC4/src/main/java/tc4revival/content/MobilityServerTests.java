package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class MobilityServerTests {
    private static  class MobilityPlayer extends FakePlayer {
        MobilityPlayer(ServerLevel level) { super(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"MobilityTest")); }
        float airAcceleration() { return getFlyingSpeed(); }
        void updateWater() { updateInWaterStateAndDoFluidPushing(); }
    }
    private static ItemStack jar(int fuel) {
        var jar=new ItemStack(TCItems.FILLED_ESSENTIA_JAR.get());
        TCItems.FILLED_ESSENTIA_JAR.get().setStoredAspects(jar,new AspectAmounts().add(TCAspects.get(LegacyAspect.ENERGY),fuel)); return jar;
    }
    private static ItemStack harness(int fuel) { var stack=new ItemStack(TCItems.HOVER_HARNESS.get()); HoverFuel.setJar(stack,jar(fuel)); return stack; }
    private static void tick(MobilityPlayer player) { MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,player)); }
    @GameTest(template="wand_test_empty")
    public static void hoverFuelKeepsLegacyIntervalsAndRemovableJar(GameTestHelper test) {
        for (boolean belt:new boolean[]{false,true}) {
            var armor=harness(2); var originalJar=HoverFuel.jar(armor); originalJar.getOrCreateTag().putString("AspectFilter","potentia"); HoverFuel.setJar(armor,originalJar);
            int interval=belt?288:360;
            for (int n=0;n<interval;n++) test.assertTrue(HoverFuel.expend(armor,belt),"Partial charge must not consume the jar's essentia");
            test.assertTrue(HoverFuel.fuel(armor)==2 && HoverFuel.expend(armor,belt) && HoverFuel.fuel(armor)==1,"Threshold tick consumes exactly one point");
            for (int n=0;n<interval;n++) HoverFuel.expend(armor,belt);
            test.assertTrue(!HoverFuel.expend(armor,belt) && HoverFuel.fuel(armor)==0,"Last point stops flight immediately");
            var empty=HoverFuel.jar(armor);
            test.assertTrue(empty.is(TCItems.FILLED_ESSENTIA_JAR.get()) && empty.getTag().getString("AspectFilter").equals("potentia"),"Empty jar and its filter survive consumption");
            var restored=ItemStack.of(armor.save(new CompoundTag())); test.assertTrue(!HoverFuel.jar(restored).isEmpty() && restored.getTag().getShort("charge")==0,"NBT round trip retains empty jar and charge");
        }
        var armor=harness(1); armor.getOrCreateTag().putShort("charge",(short)140); HoverFuel.setJar(armor,jar(8));
        test.assertTrue(armor.getTag().getShort("charge")==140 && !HoverFuel.accepts(new ItemStack(Items.COAL)),"Replacing fuel does not reset the harness counter or accept arbitrary energy items");
        pass(test,"fuel-intervals");
    }
    @GameTest(template="wand_test_empty")
    public static void hoverMenuWritesThroughAndLocksItsHost(GameTestHelper test) {
        var player=new MobilityPlayer(test.getLevel()); var armor=harness(5); player.setItemInHand(InteractionHand.MAIN_HAND,armor);
        var menu=new HoverHarnessMenu(1,player.getInventory(),InteractionHand.MAIN_HAND);
        menu.clicked(0,0,ClickType.PICKUP,player);
        test.assertTrue(HoverFuel.jar(armor).isEmpty() && HoverFuel.jarFuel(menu.getCarried())==5,"Taking a jar removes it from the actual item before close");
        menu.clicked(0,0,ClickType.PICKUP,player); test.assertTrue(HoverFuel.fuel(armor)==5 && menu.getCarried().isEmpty(),"Putting the jar back saves immediately");
        menu.clicked(28+player.getInventory().selected,0,ClickType.THROW,player);
        test.assertTrue(player.getMainHandItem()==armor,"Host cannot be thrown while its menu is open");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        menu.clicked(0,0,ClickType.PICKUP,player); menu.removed(player);
        test.assertTrue(menu.getCarried().isEmpty() && HoverFuel.fuel(armor)==5 && !menu.stillValid(player),"Detached host cannot yield a second fuel snapshot");
        pass(test,"container-ownership");
    }
    @GameTest(template="wand_test_empty")
    public static void hoverFlightValidatesEquipmentFuelAndRestoresPermissions(GameTestHelper test) {
        var player=new MobilityPlayer(test.getLevel()); HoverFlight.request(player);
        test.assertTrue(!player.getAbilities().flying,"Unauthorised toggle without armor is refused");
        var armor=harness(1); player.setItemSlot(EquipmentSlot.CHEST,armor); HoverFlight.request(player);
        test.assertTrue(HoverFlight.active(player) && player.getAbilities().flying && player.getAbilities().mayfly,"Equipped fuel grants server-owned flight");
        player.setOnGround(false); var mining=new PlayerEvent.BreakSpeed(player,Blocks.STONE.defaultBlockState(),2,null); MinecraftForge.EVENT_BUS.post(mining);
        test.assertTrue(mining.getNewSpeed()==10,"Airborne mining compensates the vanilla penalty");
        player.fallDistance=8; tick(player); test.assertTrue(player.fallDistance==0 && armor.getTag().getShort("charge")==1,"Actual tick listener consumes charge and cancels fall accumulation");
        armor.getOrCreateTag().putShort("charge",(short)360); tick(player);
        test.assertTrue(!HoverFlight.active(player) && !player.getAbilities().flying && !player.getAbilities().mayfly,"Exhaustion revokes both flight and its borrowed permission");
        HoverFlight.request(player); test.assertTrue(!HoverFlight.active(player),"Empty jar cannot restart flight");
        HoverFuel.setJar(armor,jar(2)); HoverFlight.request(player); player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY); tick(player);
        test.assertTrue(!player.getAbilities().mayfly,"Removing armor releases flight permissions");
        var restored=new MobilityPlayer(test.getLevel()); restored.getAbilities().mayfly=true; restored.getAbilities().flying=true;
        restored.getPersistentData().putBoolean("TC4HoverPreviousMayfly",false); tick(restored);
        test.assertTrue(!restored.getAbilities().mayfly && !restored.getAbilities().flying,"Crash-loaded borrowed permission is revoked even without armor");
        pass(test,"server-flight-authority");
    }
    @GameTest(template="wand_test_empty")
    public static void mobilityMovementAndRecipesUseActualEngineHooks(GameTestHelper test) {
        var player=new MobilityPlayer(test.getLevel()); var boots=new ItemStack(TCItems.TRAVELLER_BOOTS.get()); player.setItemSlot(EquipmentSlot.FEET,boots);
        player.setOnGround(false); player.zza=1;
        test.assertTrue(Math.abs(player.airAcceleration()-.05f)<1e-6,"Air acceleration accessor uses the migrated traveller rule");
        player.setDeltaMovement(Vec3.ZERO); MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(player));
        test.assertTrue(Math.abs(player.getDeltaMovement().y-.275f)<1e-6,"Actual jump event adds the original vertical boost");
        player.fallDistance=.1f; TCItems.TRAVELLER_BOOTS.get().onArmorTick(boots,test.getLevel(),player);
        test.assertTrue(player.fallDistance<0 && boots.getMaxDamage()==350,"Boot fall reduction is not clamped and durability is 350");
        var armor=harness(2); test.assertTrue(armor.getMaxDamage()==400 && TCEnchantments.HASTE.get().canEnchant(armor) && !TCItems.HOVER_HARNESS.get().allowHotbarRepair(),"Harness durability and enchantment interfaces match the old special armor");
        var recipe=(InfusionRecipe)test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:infusion/traveller_boots")).orElseThrow();
        var components=List.of(new ItemStack(TCItems.shard(ShardType.AIR)),new ItemStack(TCItems.shard(ShardType.AIR)),new ItemStack(TCItems.material(MaterialType.FABRIC)),new ItemStack(TCItems.material(MaterialType.FABRIC)),new ItemStack(Items.FEATHER),new ItemStack(Items.PUFFERFISH));
        test.assertTrue(recipe.matches(new InfusionRecipe.Input(new ItemStack(Items.LEATHER_BOOTS),components,key->true),test.getLevel()),"Former wildcard raw fish accepts pufferfish in the actual recipe");
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(new ItemStack(Items.LEATHER_BOOTS),components,key->false),test.getLevel()),"Recipe still requires research");
        for (String name:new String[]{"hover_harness","hover_girdle"}) test.assertTrue(test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:infusion/"+name)).isPresent(),"Companion infusion recipe must load");
        pass(test,"movement-and-acquisition");
    }
    @GameTest(template="wand_test_empty")
    public static void travellerMovementRespectsForwardGroundFlightAndYaw(GameTestHelper test) {
        var player=new MobilityPlayer(test.getLevel());var boots=new ItemStack(TCItems.TRAVELLER_BOOTS.get());
        int cases=0;
        for(float yaw:new float[]{0,90})for(boolean ground:new boolean[]{false,true})for(boolean flying:new boolean[]{false,true})for(float forward:new float[]{-1,0,1}) {
            player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);player.setOnGround(ground);player.getAbilities().flying=flying;player.zza=forward;player.setYRot(yaw);
            float vanilla=player.airAcceleration();
            player.setItemSlot(EquipmentSlot.FEET,boots);player.setDeltaMovement(Vec3.ZERO);player.fallDistance=.1f;
            TCItems.TRAVELLER_BOOTS.get().onArmorTick(boots,test.getLevel(),player);
            boolean accelerate=!flying && forward>0 && ground;
            double expectedX=accelerate && yaw==90?-.055:0,expectedZ=accelerate && yaw==0?.055:0;
            test.assertTrue(Math.abs(player.getDeltaMovement().x-expectedX)<1e-6 && Math.abs(player.getDeltaMovement().z-expectedZ)<1e-6,"Ground acceleration follows yaw only while walking forward without ability flight");
            float expectedAir=!ground && !flying && forward>0?.05f:vanilla;
            test.assertTrue(Math.abs(player.airAcceleration()-expectedAir)<1e-6,"Actual air accessor preserves vanilla outside boot acceleration conditions");
            test.assertTrue(Math.abs(player.fallDistance+.15f)<1e-6,"Old fall reduction remains unconditional for positive fall distance, including ability flight");
            cases++;
        }
        test.assertTrue(cases==24,"Complete movement state matrix");pass(test,"traveller-24-movement-boundaries");
    }
    @GameTest(template="wand_test_empty")
    public static void waterBootsAndEquippedGirdleUseActualWorldAndSlots(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new net.minecraft.core.BlockPos(3,3,3));
        var player=new MobilityPlayer(level);player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());player.updateWater();
        test.assertTrue(player.isInWater(),"Real water block sets engine immersion state");
        var boots=new ItemStack(TCItems.TRAVELLER_BOOTS.get());player.setItemSlot(EquipmentSlot.FEET,boots);player.setOnGround(true);player.zza=1;player.setDeltaMovement(Vec3.ZERO);
        TCItems.TRAVELLER_BOOTS.get().onArmorTick(boots,level,player);
        test.assertTrue(Math.abs(player.getDeltaMovement().z-.055/4)<1e-6,"Real water reduces ground acceleration to one quarter");
        var curios=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElseThrow();curios.reset();
        var belt=new ItemStack(TCItems.HOVER_GIRDLE.get());curios.getStacksHandler("belt").orElseThrow().getStacks().setStackInSlot(0,belt);
        test.assertTrue(HoverFlight.girdle(player),"Actual equipped belt is detected");
        player.fallDistance=.1f;top.theillusivec4.curios.api.CuriosApi.getCurio(belt).resolve().orElseThrow().curioTick(new top.theillusivec4.curios.api.SlotContext("belt",player,0,false,true));
        test.assertTrue(Math.abs(player.fallDistance+.23f)<1e-6,"Belt capability preserves unclamped .33 fall subtraction");
        var armor=harness(2);armor.getOrCreateTag().putShort("charge",(short)288);player.setItemSlot(EquipmentSlot.CHEST,armor);HoverFlight.request(player);tick(player);
        test.assertTrue(HoverFuel.fuel(armor)==1 && armor.getTag().getShort("charge")==0,"Real equipped belt selects the 288 threshold through actual flight tick");
        player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);tick(player);level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());pass(test,"water-and-real-belt");
    }
    @GameTest(template="wand_test_empty")
    public static void outerLandsSuppressesHarnessAndIndependentFlightAtLegacyInterval(GameTestHelper test) {
        var outer=test.getLevel().getServer().getLevel(TCEldritch.OUTER_LANDS);test.assertTrue(outer!=null,"Actual outer-lands dimension is loaded");
        var player=new MobilityPlayer(outer);var armor=harness(2);player.setItemSlot(EquipmentSlot.CHEST,armor);player.tickCount=19;HoverFlight.request(player);tick(player);
        test.assertTrue(HoverFlight.active(player) && player.getAbilities().flying,"Restriction waits for the twentieth tick");
        int charge=armor.getTag().getShort("charge");player.tickCount=20;tick(player);
        test.assertTrue(!HoverFlight.active(player) && !player.getAbilities().flying && !player.getAbilities().mayfly && armor.getTag().getByte("hover")==0 && armor.getTag().getShort("charge")==charge,"Restriction revokes borrowed flight and does not spend another fuel tick");
        var independent=new MobilityPlayer(outer);independent.getAbilities().mayfly=true;independent.getAbilities().flying=true;independent.tickCount=20;tick(independent);
        test.assertTrue(!independent.getAbilities().flying && independent.getAbilities().mayfly,"Legacy rule stops unarmored flight while preserving independently granted permission");
        independent.setGameMode(net.minecraft.world.level.GameType.CREATIVE);independent.getAbilities().flying=true;tick(independent);
        test.assertTrue(independent.getAbilities().flying,"Creative flight is exempt");
        independent.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);tick(independent);test.assertTrue(independent.getAbilities().flying,"Modern spectator travel remains available");
        pass(test,"outer-lands-flight-restriction");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Mobility system test passed: {}",name); test.succeed(); }
}
