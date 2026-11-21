package ttmodern;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class KamiServerTests {
    @GameTest(template="wand_test_empty") public static void awakenedAxeMiningAndProtoclayPreserveModesAndProtection(GameTestHelper test){
        var world=test.getLevel();var origin=test.absolutePos(new net.minecraft.core.BlockPos(3,3,3));
        var p=new net.minecraftforge.common.util.FakePlayer(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTAxeRegression"));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.moveTo(origin.getCenter());
        var axe=new net.minecraft.world.item.ItemStack(Tinkerer.AWAKENED_AXE.get());
        axe.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,3);axe.getOrCreateTag().putInt("ToolMode",1);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,axe);
        var denied=origin.west();var allowed=origin.east();
        world.setBlockAndUpdate(denied,net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState());
        world.setBlockAndUpdate(allowed,net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState());
        Object protector=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void blocked(net.minecraftforge.event.level.BlockEvent.BreakEvent e){if(e.getPlayer()==p&&e.getPos().equals(denied))e.setCanceled(true);}};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(protector);
        try{
            test.assertTrue(AwakenedMining.mine(p,origin,net.minecraft.core.Direction.UP,axe,1,net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState())==1,"Native awakened axe still harvests its range through protected break events");
            test.assertTrue(world.isEmptyBlock(allowed)&&world.getBlockState(denied).is(net.minecraft.world.level.block.Blocks.OAK_PLANKS),"Protected planks stay intact");
            world.removeBlock(denied,false);
            for(int y=1;y<=3;y++)world.setBlockAndUpdate(origin.above(y),net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState());
            world.setBlockAndUpdate(origin.above(3).east(),net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState());
            axe.getOrCreateTag().putInt("ToolMode",2);
            test.assertTrue(AwakenedMining.mine(p,origin,net.minecraft.core.Direction.NORTH,axe,2,net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState())==4,"Tree mode includes connected diagonal branches");
            p.getInventory().setItem(1,new net.minecraft.world.item.ItemStack(Tinkerer.AWAKENED_PICK.get()));
            p.getInventory().setItem(2,new net.minecraft.world.item.ItemStack(Tinkerer.AWAKENED_SHOVEL.get()));
            p.getInventory().setItem(3,new net.minecraft.world.item.ItemStack(Tinkerer.PROTOCLAY.get()));
            test.assertTrue(ProtoclayItem.select(p,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())&&p.getMainHandItem().is(Tinkerer.AWAKENED_PICK.get()),"Protoclay switches away from native axe");
            test.assertTrue(ProtoclayItem.select(p,net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState())&&p.getMainHandItem()==axe,"Protoclay selects the same axe stack again");
            test.assertTrue(AwakenedModes.mode(axe)==2&&axe.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS)==3&&!axe.isDamageableItem(),"Mode and enchantment survive tool switches without durability");
            test.assertTrue(p.getInventory().countItem(Tinkerer.AWAKENED_AXE.get())==1&&p.getInventory().countItem(Tinkerer.AWAKENED_PICK.get())==1,"Tool switching preserves inventory exactly once");
            test.succeed();
        }finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protector);p.discard();}
    }
    @GameTest(template="wand_test_empty") public static void awakenedAxeRetainsNativeEnchantmentsWithoutDurabilityOrStripping(GameTestHelper test){
        var normal=new net.minecraft.world.item.ItemStack(Tinkerer.ICHOR_AXE.get());
        var awakened=new net.minecraft.world.item.ItemStack(Tinkerer.AWAKENED_AXE.get());
        for(var enchantment:net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS){
            test.assertTrue(enchantment.canEnchant(normal)==enchantment.canEnchant(awakened),"Ordinary and awakened Ichor axes accept the same enchantments: "+net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(enchantment));
            test.assertTrue(enchantment.canApplyAtEnchantingTable(normal)==enchantment.canApplyAtEnchantingTable(awakened),"Native enchanting-table rules remain unchanged");
        }
        test.assertTrue(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS.canEnchant(awakened),"The awakened axe retains native anvil damage enchantments");
        test.assertTrue(!awakened.isDamageableItem()&&!net.minecraft.world.item.enchantment.Enchantments.UNBREAKING.canEnchant(awakened)&&!net.minecraft.world.item.enchantment.Enchantments.MENDING.canEnchant(awakened)&&!tc4revival.content.TCEnchantments.REPAIR.get().canEnchant(awakened),"Indestructible tools reject durability enchantments");
        test.assertTrue(awakened.canPerformAction(net.minecraftforge.common.ToolActions.AXE_DIG)&&!awakened.canPerformAction(net.minecraftforge.common.ToolActions.AXE_STRIP)&&!awakened.canPerformAction(net.minecraftforge.common.ToolActions.AXE_SCRAPE)&&!awakened.canPerformAction(net.minecraftforge.common.ToolActions.AXE_WAX_OFF),"Awakened modes do not enable vanilla axe transformations");
        test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererKamiWandRegistrationAndAssembly(GameTestHelper test){
        var rod=thaumcraft.api.wands.WandRod.get(Tinkerer.ID+":ichorcloth");var cap=thaumcraft.api.wands.WandCap.get(Tinkerer.ID+":ichor");
        test.assertTrue(rod.capacity==1000&&rod.glowing()&&rod.regenerates==null&&!rod.regenerateAll,"KAMI rod capacity and visual flag must not introduce free regeneration");
        test.assertTrue(Math.abs(cap.cost-.7f)<.0001f,"Ichor cap costs 70 percent vis");
        var grid=new java.util.ArrayList<WandAssembly.Ingredient>(java.util.Collections.nCopies(9,null));
        grid.set(2,new WandAssembly.Ingredient(Tinkerer.ID+":ichor_cap",0,null));grid.set(6,grid.get(2));grid.set(4,new WandAssembly.Ingredient(Tinkerer.ID+":ichorcloth_rod",0,null));
        var result=WandAssembly.match(grid,false,key->true).orElseThrow();test.assertTrue(result.unlocked()&&result.costPerPrimal()==100&&result.rod()==rod&&result.cap()==cap,"Existing assembly recognizes registered addon items");
        test.assertTrue(!WandAssembly.match(grid,false,key->false).orElseThrow().unlocked(),"Assembly requires both component researches");
        var wand=TCItems.CASTING_WAND.get().create(rod,cap,false,true);test.assertTrue(WandParts.rod(wand.getTag())==rod&&WandParts.cap(wand.getTag())==cap,"Serialized wand retains addon components");
        for(String aspect:CropTraits.PRIMALS)test.assertTrue(wand.getTag().getInt(aspect)==TCItems.CASTING_WAND.get().capacity(wand),"Filled KAMI wand stores its full capacity for every primal");test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererIchorEquipmentProtection(GameTestHelper test){
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTIchor"));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(Tinkerer.ICHOR_HELM.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new net.minecraft.world.item.ItemStack(Tinkerer.ICHOR_CHEST.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,new net.minecraft.world.item.ItemStack(Tinkerer.ICHOR_LEGS.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new net.minecraft.world.item.ItemStack(Tinkerer.ICHOR_BOOTS.get()));
        for(var slot:net.minecraft.world.entity.EquipmentSlot.values())if(slot.getType()==net.minecraft.world.entity.EquipmentSlot.Type.ARMOR)player.getAttributes().addTransientAttributeModifiers(player.getItemBySlot(slot).getAttributeModifiers(slot));
        test.assertTrue(player.getArmorValue()==20,"Full Ichorcloth shows twenty armor points");
        for(float damage:new float[]{1,20,100}){
            var event=new net.minecraftforge.event.entity.living.LivingHurtEvent(player,test.getLevel().damageSources().mobAttack(net.minecraft.world.entity.EntityType.COW.create(test.getLevel())),damage);IchorArmorProtection.hurt(event);
            float actual=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(event.getAmount(),player.getArmorValue(),0);
            test.assertTrue(Math.abs(actual-damage*.15f)<.001f,"Fixed full-set 85 percent reduction: input="+damage+", result="+actual+", adjusted="+event.getAmount());
        }
        for(var stack:player.getArmorSlots()){
            var armor=(IchorArmorItem)stack.getItem();test.assertTrue(!stack.isDamageableItem()&&armor.getVisDiscount(stack,player,null)==(armor.getType()==net.minecraft.world.item.ArmorItem.Type.BOOTS?3:4)&&armor.getWarp(stack,player)==3,"Armor is indestructible and exposes vis discount and warp");
        }
        for(var item:java.util.List.of(Tinkerer.ICHOR_PICK.get(),Tinkerer.ICHOR_SHOVEL.get(),Tinkerer.ICHOR_AXE.get(),Tinkerer.ICHOR_SWORD.get()))test.assertTrue(!new net.minecraft.world.item.ItemStack(item).isDamageableItem(),"Ichorium tools must not consume durability");
        test.succeed();
    }
}
