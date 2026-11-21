package emtmodern.test;

import emtmodern.ElectroMagicTools;
import emtmodern.item.InfusedQuantumChestplate;
import ic2.api.item.ElectricItem;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class QuantumChestServerTests {
    public static ItemStack chest(){return new ItemStack(ElectroMagicTools.CONTENT.get("infused_quantum_chestplate").get());}
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void fourAttachmentsChargeAndPersistence(GameTestHelper test){
        var player=GogglesServerTests.player(test);player.getAbilities().instabuild=false;player.setShiftKeyDown(true);
        for(var attachment:InfusedQuantumChestplate.ATTACHMENTS){
            var armor=chest();player.setItemSlot(EquipmentSlot.CHEST,armor);ElectricItem.manager.charge(armor,50000,Integer.MAX_VALUE,true,false);
            ItemStack part=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(attachment.item())));part.setHoverName(net.minecraft.network.chat.Component.literal("Installed attachment"));part.getOrCreateTag().putString("OwnerData","retained");
            if(!attachment.chargeKey().isEmpty())ElectricItem.manager.charge(part,12345,Integer.MAX_VALUE,true,false);
            double expectedCharge=attachment.chargeKey().isEmpty()?0:ElectricItem.manager.getCharge(part);player.setItemInHand(InteractionHand.MAIN_HAND,part);
            test.assertTrue(InfusedQuantumChestplate.install(player,armor),"Supported sneaking attachment installs");test.assertTrue(player.getMainHandItem().isEmpty(),"Install consumes exactly one attachment");
            test.assertTrue(InfusedQuantumChestplate.attachment(armor).number()==attachment.number(),"Installed attachment kind retained");
            armor=ItemStack.of(armor.save(new net.minecraft.nbt.CompoundTag()));player.setItemSlot(EquipmentSlot.CHEST,armor);
            ElectricItem.manager.discharge(armor,800,Integer.MAX_VALUE,true,false,false);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));test.assertTrue(!InfusedQuantumChestplate.detach(player,armor),"Occupied hand cannot detach and overwrite an item");player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            player.setShiftKeyDown(false);test.assertTrue(!InfusedQuantumChestplate.detach(player,armor),"Non-sneaking packet cannot detach");player.setShiftKeyDown(true);
            test.assertTrue(InfusedQuantumChestplate.detach(player,armor),"Detach restores stored attachment after save/load");
            test.assertTrue(player.getMainHandItem().is(ForgeRegistries.ITEMS.getValue(new ResourceLocation(attachment.item()))),"Detach restores the corresponding attachment item");
            if(!attachment.chargeKey().isEmpty())test.assertTrue(ElectricItem.manager.getCharge(player.getMainHandItem())==(int)expectedCharge,"Attachment retains its original integer EU independently of chest use");
            test.assertTrue(!player.getMainHandItem().hasCustomHoverName(),"Author's newly constructed attachment rule retained");
            test.assertTrue(ElectricItem.manager.getCharge(armor)==49200,"Chest energy remains independent");test.assertTrue(InfusedQuantumChestplate.attachment(armor)==null,"No duplicate attachment remains after removal");
        }
        var legacy=chest();legacy.getOrCreateTag().putString("useother","NW");legacy.getTag().putInt("NWCharge",4321);InfusedQuantumChestplate.migrate(legacy.getTag());player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        test.assertTrue(InfusedQuantumChestplate.detach(player,legacy),"Legacy attachment data still detaches");test.assertTrue(ElectricItem.manager.getCharge(player.getMainHandItem())==4321,"Legacy integer EU field migrates without loss");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void chestFlightArmorAndExtinguishing(GameTestHelper test){
        var level=test.getLevel();level.setWeatherParameters(10000,0,false,false);level.setRainLevel(0);
        var player=GogglesServerTests.player(test);player.getAbilities().instabuild=false;player.setOnGround(false);player.setPos(test.absolutePos(new net.minecraft.core.BlockPos(3,10,3)).getCenter());
        var armor=chest();player.setItemSlot(EquipmentSlot.CHEST,armor);var item=(InfusedQuantumChestplate)armor.getItem();ElectricItem.manager.charge(armor,1000000,Integer.MAX_VALUE,true,false);
        player.setDeltaMovement(0,0,0);player.fallDistance=30;
        test.assertTrue(InfusedQuantumChestplate.useJetpack(player,false,armor,true,false,true),"Charged jetpack activates");test.assertTrue(Math.abs(player.getDeltaMovement().y-.2)<.0001&&player.getDeltaMovement().horizontalDistanceSqr()>0,"Jetpack lifts and adds forward thrust");
        test.assertTrue(ElectricItem.manager.getCharge(armor)==999992&&player.fallDistance==0,"Normal jetpack spends 8 EU and resets falling distance");
        player.setDeltaMovement(0,-.1,0);InfusedQuantumChestplate.useJetpack(player,true,armor,false,false,false);test.assertTrue(Math.abs(player.getDeltaMovement().y+.025)<.0001,"Hover descent matches original negative .025");test.assertTrue(ElectricItem.manager.getCharge(armor)==999982,"Hover spends 10 EU");
        player.setSecondsOnFire(20);armor.getOrCreateTag().putByte("wing",(byte)0);item.tick(armor,level,player,false,false,false,false);test.assertTrue(!player.isOnFire(),"Chest extinguishes even without a flight attachment");
        for(int wing=2;wing<=4;wing++){
            var profile=emtmodern.item.WingFlight.PROFILES.get(wing==2?"thaumium":wing==3?"nano":"quantum");armor.getTag().putByte("wing",(byte)wing);armor.getTag().putByte("f",(byte)0);armor.getTag().putBoolean("isHolding",false);player.setDeltaMovement(.1,0,.1);
            for(int i=0;i<7;i++)InfusedQuantumChestplate.useWings(player,armor,profile,true);
            double charge=ElectricItem.manager.getCharge(armor);player.fallDistance=9;InfusedQuantumChestplate.useWings(player,armor,profile,false);
            test.assertTrue(Math.abs(player.getDeltaMovement().y-profile.lift()*7)<.0001,"Embedded wings flap on current key release with no standalone-wing delay");test.assertTrue(Math.abs(ElectricItem.manager.getCharge(armor)-(charge-profile.lift()*7*10*profile.energy()))<.001,"Embedded wings spend chest EU only");test.assertTrue(player.fallDistance==0,"Embedded wings clear fall distance");
        }
        double before=ElectricItem.manager.getCharge(armor);float damage=ic2.core.item.armor.ItemArmorElectric.damageArmor(player,player.damageSources().generic(),10);test.assertTrue(Math.abs(damage-5.6)<.001,"Chest protects 44 percent via real IC2 armor pipeline");test.assertTrue(Math.abs(ElectricItem.manager.getCharge(armor)-(before-88000))<.01,"Protection costs 20000 EU per absorbed damage point");
        ElectricItem.manager.discharge(armor,Double.MAX_VALUE,Integer.MAX_VALUE,true,false,false);player.setDeltaMovement(0,0,0);test.assertTrue(!InfusedQuantumChestplate.useJetpack(player,false,armor,true,false,false),"Empty jetpack cannot fly");test.succeed();
    }
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void shieldSurvivesExplosionAndNeverDrops(GameTestHelper test){
        var pos=test.absolutePos(new net.minecraft.core.BlockPos(3,2,3));var level=test.getLevel();var state=ElectroMagicTools.SHIELD.get().defaultBlockState();level.setBlockAndUpdate(pos,state);
        level.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,2,net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        test.assertTrue(level.getBlockState(pos).is(state.getBlock()),"Shield survives real vanilla explosion");
        var player=GogglesServerTests.player(test);test.assertTrue(!state.canEntityDestroy(level,pos,player),"Entities cannot destroy shield");
        test.assertTrue(net.minecraft.world.level.block.Block.getDrops(state,level,pos,null,player,new ItemStack(Items.DIAMOND_PICKAXE)).isEmpty(),"Shield drops nothing even with a tool");
        ItemStack silk=new ItemStack(Items.DIAMOND_PICKAXE);silk.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,1);test.assertTrue(net.minecraft.world.level.block.Block.getDrops(state,level,pos,null,player,silk).isEmpty(),"Silk touch cannot bypass no-drops rule");
        var recipe=level.getRecipeManager().byKey(new ResourceLocation("emt","shield")).orElseThrow();test.assertTrue(recipe.getResultItem(level.registryAccess()).getCount()==6,"Arcane shield recipe yields original six blocks");test.succeed();
    }
}
