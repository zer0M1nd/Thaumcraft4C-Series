package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class RobeAcquisitionServerTests {
    @GameTest(template="wand_test_empty")
    public static void ordinaryRobeEquipmentAndRepairUseLegacyParameters(GameTestHelper test) {
        var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RobeEquipment"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        RobeArmorItem[] robes={TCItems.ROBE_CHEST.get(),TCItems.ROBE_LEGS.get(),TCItems.ROBE_BOOTS.get()};
        int[] durability={400,375,325},defense={3,2,1},discount={2,2,1};
        for(int i=0;i<robes.length;i++) {
            var robe=robes[i]; var stack=new ItemStack(robe); stack.setDamageValue(10);
            test.assertTrue(stack.getMaxDamage()==durability[i] && robe.getDefense()==defense[i] && robe.getEnchantmentValue()==25 && robe.getToughness()==0,"Registered SPECIAL material parameters");
            test.assertTrue(stack.getRarity()==Rarity.UNCOMMON && RunicGear.charge(stack)==0 && robe.visDiscount(stack,player,null)==discount[i],"Legacy rarity, zero intrinsic runic shield and discount");
            test.assertTrue(robe.isValidRepairItem(stack,new ItemStack(TCItems.material(MaterialType.FABRIC))) && !robe.isValidRepairItem(stack,new ItemStack(Items.LEATHER)),"Only enchanted fabric is the legacy repair material");
            player.setItemSlot(robe.getEquipmentSlot(),stack);
            player.doTick(); // Vanilla equipment update installs the actual worn discount.
            player.tickCount=40; VisRepair.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));
            test.assertTrue(stack.getDamageValue()==10,"Ordinary robes do not repair without Repair enchantment");
            stack.enchant(TCEnchantments.REPAIR.get(),2);
            var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);
            player.getInventory().setItem(35,wand); var before=wand.copy();
            VisRepair.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));
            test.assertTrue(stack.getDamageValue()==8,"Equipped robe repairs exactly enchantment rank");
            String id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(robe).toString();
            var base=tc4revival.aspect.item.ItemAspects.baseSession().base(id).orElseThrow();
            var primals=TCAspects.catalog().toPrimals(base,tc4revival.aspect.AspectCatalog.Reduction.SUM);
            test.assertTrue(!primals.isEmpty(),"Real robe aspect definition supplies repair payment");
            for(var aspect:TCAspects.catalog().primals()) {
                int baseCost=(int)Math.sqrt(primals.amount(aspect)*2.0)*2;
                int paid=(int)(baseCost*(1-discount[i]/100f));
                test.assertTrue(TCItems.CASTING_WAND.get().vis(before,aspect)-TCItems.CASTING_WAND.get().vis(wand,aspect)==paid,"Base-aspect repair cost still receives worn equipment discount through old consumeAllVis");
            }
            player.setItemSlot(robe.getEquipmentSlot(),ItemStack.EMPTY);
            player.doTick();player.tickCount=40; // Remove the discount before the hotbar-only checkpoint.
            player.getInventory().setItem(0,stack); before=wand.copy();
            VisRepair.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));
            test.assertTrue(stack.getDamageValue()==6 && !ItemStack.matches(before,wand),"Repairable ordinary robes also repair in hotbar");
            player.getInventory().setItem(0,ItemStack.EMPTY); player.getInventory().setItem(9,stack); before=wand.copy();
            VisRepair.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));
            test.assertTrue(stack.getDamageValue()==6 && ItemStack.matches(before,wand),"Stored robe does not trigger repair");
            player.getInventory().setItem(9,ItemStack.EMPTY);
        }
        tc4revival.TC4Revival.LOGGER.info("PASS ordinaryRobeEquipmentAndRepairUseLegacyParameters: three pieces, worn/hotbar/storage and real aspect payment");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void enchantedFabricAndRobesUseActualWorkbenchTransactions(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(2,2,2));
        level.setBlock(pos,TCArcane.BLOCK.get().defaultBlockState(),3);
        var table=(ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
        Item[] wool={Items.WHITE_WOOL,Items.ORANGE_WOOL,Items.MAGENTA_WOOL,Items.LIGHT_BLUE_WOOL,
                Items.YELLOW_WOOL,Items.LIME_WOOL,Items.PINK_WOOL,Items.GRAY_WOOL,Items.LIGHT_GRAY_WOOL,
                Items.CYAN_WOOL,Items.PURPLE_WOOL,Items.BLUE_WOOL,Items.BROWN_WOOL,Items.GREEN_WOOL,Items.RED_WOOL,Items.BLACK_WOOL};
        var fabric=TCItems.material(MaterialType.FABRIC);
        for(int index=0;index<19;index++) {
            var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RobeAcquire"));
            player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            for(int cell=0;cell<11;cell++)table.setItem(cell,ItemStack.EMPTY);
            String shape=index<16?" S SCS S ":index==16?"I IIIIIII":index==17?"IIII II I":"   I II I";
            Item output=index<16?fabric:index==16?TCItems.ROBE_CHEST.get():index==17?TCItems.ROBE_LEGS.get():TCItems.ROBE_BOOTS.get();
            Map<String,Integer> costs=index<16?Map.of("aer",1,"terra",1,"ignis",1,"aqua",1,"ordo",1,"perditio",1):index==16?Map.of("aer",5):index==17?Map.of("aqua",5):Map.of("terra",3);
            for(int cell=0;cell<9;cell++) {
                char symbol=shape.charAt(cell);
                if(symbol!=' ') table.setItem(cell,new ItemStack(symbol=='S'?Items.STRING:symbol=='C'?wool[index]:fabric,2));
            }
            var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);
            table.setItem(10,wand);
            var menu=new ArcaneWorkbenchMenu(1,player.getInventory(),table); player.containerMenu=menu;
            try {
                var before=table.saveWithoutMetadata();
                menu.clicked(0,0,ClickType.PICKUP,player);
                test.assertTrue(menu.getCarried().isEmpty() && before.equals(table.saveWithoutMetadata()),"Research gate blocks materials and vis: "+index);
                KnowledgeSavedData.get(player.server).update(player.getUUID(),bound->0,k->{k.rememberResearch("ENCHFABRIC");return null;});
                var empty=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,false);
                table.setItem(10,empty); menu.refresh(); before=table.saveWithoutMetadata();
                menu.clicked(0,0,ClickType.PICKUP,player);
                test.assertTrue(menu.getCarried().isEmpty() && !menu.preview().isEmpty() && before.equals(table.saveWithoutMetadata()),"Insufficient vis shows preview without transaction");
                table.setItem(10,wand); menu.refresh();
                menu.clicked(0,0,ClickType.PICKUP,player);
                test.assertTrue(menu.getCarried().is(output) && menu.getCarried().getCount()==1,"Actual loaded recipe output: "+index);
                for(var aspect:TCAspects.catalog().primals()) test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,aspect)==TCItems.CASTING_WAND.get().capacity(wand)-100*costs.getOrDefault(aspect.tag(),0),"Exact legacy vis cost "+index+" "+aspect.tag());
                for(int cell=0;cell<9;cell++) test.assertTrue(table.getItem(cell).getCount()==(shape.charAt(cell)==' '?0:1),"Ingredients debit once");
                menu.setCarried(ItemStack.EMPTY); menu.clicked(0,0,ClickType.QUICK_MOVE,player);
                test.assertTrue(player.getInventory().countItem(output)==1,"Shift-click crafts remaining set exactly once");
                for(int cell=0;cell<9;cell++)test.assertTrue(table.getItem(cell).isEmpty(),"Second craft exhausts ingredients");
                for(var aspect:TCAspects.catalog().primals()) test.assertTrue(TCItems.CASTING_WAND.get().vis(wand,aspect)==TCItems.CASTING_WAND.get().capacity(wand)-200*costs.getOrDefault(aspect.tag(),0),"Shift craft pays once");
            } finally {player.containerMenu=player.inventoryMenu;}
        }
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        tc4revival.TC4Revival.LOGGER.info("PASS enchantedFabricAndRobesUseActualWorkbenchTransactions: 16 wool variants, 3 robes, 38 crafts with research and vis gates");
        test.succeed();
    }
}
