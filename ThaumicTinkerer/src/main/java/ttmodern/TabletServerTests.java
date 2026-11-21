package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class TabletServerTests {
    private static TabletEntity tablet(GameTestHelper test,BlockPos pos){test.getLevel().setBlock(pos,Tinkerer.TABLET.get().defaultBlockState().setValue(TabletBlock.FACING,Direction.EAST),3);return (TabletEntity)test.getLevel().getBlockEntity(pos);}
    private static void ticks(TabletEntity tablet,int count){for(int i=0;i<count;i++)tablet.tick();}
    @GameTest(template="wand_test_empty")
    public static void tinkererTabletPlacementPowerAndInventory(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var tablet=tablet(test,pos);var target=pos.east();
        level.removeBlock(target,false);level.setBlock(target.below(),Blocks.STONE.defaultBlockState(),3);tablet.items.setStackInSlot(0,new ItemStack(Items.COBBLESTONE,3));tablet.configure(false,true);
        try{
            ticks(tablet,60);test.assertTrue(level.isEmptyBlock(target)&&tablet.items.getStackInSlot(0).getCount()==3,"Unpowered redstone mode does not place or consume");
            level.setBlock(pos.north(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);ticks(tablet,16);
            test.assertTrue(level.getBlockState(target).is(Blocks.COBBLESTONE)&&tablet.items.getStackInSlot(0).getCount()==2,"Powered native placement consumes exactly one");
            var saved=tablet.saveWithoutMetadata();var copy=new TabletEntity(pos,tablet.getBlockState());copy.load(saved);
            test.assertTrue(!copy.leftClick()&&copy.redstone()&&copy.items.getStackInSlot(0).getCount()==2,"Inventory and configuration persist");
            test.assertTrue(tc4revival.research.ResearchDefinitions.server().pages().require(Tinkerer.ID+":ANIMATION_TABLET").size()==2,"Both research pages loaded");test.succeed();
        }finally{tablet.items.setStackInSlot(0,ItemStack.EMPTY);level.removeBlock(pos,false);level.removeBlock(pos.north(),false);level.removeBlock(target,false);level.removeBlock(target.below(),false);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererTabletMiningDropsDurabilityAndProtection(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var tablet=tablet(test,pos);var target=pos.east();
        tablet.items.setStackInSlot(0,new ItemStack(Items.IRON_PICKAXE));level.setBlock(target,Blocks.STONE.defaultBlockState(),3);
        var guard=new MiningGuard(target);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(guard);
        try{
            ticks(tablet,70);test.assertTrue(level.getBlockState(target).is(Blocks.STONE)&&tablet.items.getStackInSlot(0).getDamageValue()==0,"Protection cancellation prevents destruction and tool damage");
            guard.deny=false;ticks(tablet,70);
            test.assertTrue(level.isEmptyBlock(target)&&tablet.items.getStackInSlot(0).getDamageValue()==1,"Native mining completes and damages tool once");
            int drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(target).inflate(1)).stream().filter(e->e.getItem().is(Items.COBBLESTONE)).mapToInt(e->e.getItem().getCount()).sum();
            test.assertTrue(drops==1,"Native harvest generates exactly one cobblestone");
            level.setBlock(target,Blocks.BEDROCK.defaultBlockState(),3);ticks(tablet,70);test.assertTrue(level.getBlockState(target).is(Blocks.BEDROCK),"Unbreakable block preserved");test.succeed();
        }finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(guard);tablet.items.setStackInSlot(0,ItemStack.EMPTY);level.removeBlock(pos,false);level.removeBlock(target,false);level.getEntitiesOfClass(ItemEntity.class,new AABB(target).inflate(1)).forEach(Entity::discard);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererTabletArcaneTransaction(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));level.setBlock(pos,tc4revival.content.TCArcane.BLOCK.get().defaultBlockState(),3);
        var table=(tc4revival.content.ArcaneWorkbenchBlockEntity)level.getBlockEntity(pos);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TabletCraft"));player.moveTo(pos.getCenter());
        try{
            Item[] ingredients={Items.GOLD_INGOT,Items.IRON_INGOT,Items.GOLD_INGOT,Items.IRON_INGOT,tc4revival.content.TCItems.GOLEM_CORE_BLANK.get(),Items.IRON_INGOT};
            for(int i=0;i<6;i++)table.setItem(i,new ItemStack(ingredients[i]));
            var wand=tc4revival.content.TCItems.CASTING_WAND.get().create(tc4revival.content.WandParts.Rod.SILVERWOOD,tc4revival.content.WandParts.Cap.GOLD,false,true);table.setItem(10,wand);
            var menu=new tc4revival.content.ArcaneWorkbenchMenu(1,player.getInventory(),table);player.containerMenu=menu;
            test.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Tablet crafting requires research");
            thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,Tinkerer.ID+":ANIMATION_TABLET");menu.refresh();menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
            test.assertTrue(menu.getCarried().is(Tinkerer.TABLET.get().asItem()),"Real arcane crafting produces tablet");
            for(int i=0;i<6;i++)test.assertTrue(table.getItem(i).isEmpty(),"Each ingredient consumed once");
            String[] tags={"aer","ordo","ignis"};int[] remaining={7500,8500,9000};
            for(int i=0;i<3;i++)test.assertTrue(tc4revival.content.TCItems.CASTING_WAND.get().vis(wand,tc4revival.aspect.TCAspects.catalog().require(tags[i]))==remaining[i],"Exact tablet vis cost: "+tags[i]);test.succeed();
        }finally{player.containerMenu=player.inventoryMenu;level.removeBlock(pos,false);}
    }
    public static final class MiningGuard {
        private final BlockPos target;public boolean deny=true;MiningGuard(BlockPos target){this.target=target;}
        @net.minecraftforge.eventbus.api.SubscribeEvent public void TabletServerTests_breaking_1(net.minecraftforge.event.level.BlockEvent.BreakEvent event){if(deny&&event.getPos().equals(target))event.setCanceled(true);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererTabletEntityUseAttackAndMenu(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var tablet=tablet(test,pos);var target=pos.east();level.removeBlock(target,false);
        var cow=EntityType.COW.create(level);cow.moveTo(target.getX()+.5,target.getY(),target.getZ()+.5,0,0);cow.setNoAi(true);level.addFreshEntity(cow);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TabletMenu"));player.moveTo(pos.getCenter());
        try{
            tablet.configure(false,false);tablet.items.setStackInSlot(0,new ItemStack(Items.BUCKET));ticks(tablet,16);
            test.assertTrue(tablet.items.getStackInSlot(0).is(Items.MILK_BUCKET),"Right-click entity returns replacement item to machine");
            ticks(tablet,14);tablet.configure(true,false);tablet.items.setStackInSlot(0,new ItemStack(Items.IRON_SWORD));ticks(tablet,16);
            test.assertTrue(cow.getHealth()<=4&&tablet.items.getStackInSlot(0).getDamageValue()==1,"Sword attack uses full native weapon attributes and durability");
            var menu=new TabletMenu(1,player.getInventory(),tablet);test.assertTrue(!menu.quickMoveStack(player,0).isEmpty()&&tablet.items.getStackInSlot(0).isEmpty(),"Menu returns tool without duplication");
            player.getInventory().setItem(9,new ItemStack(Items.COBBLESTONE,32));test.assertTrue(!menu.quickMoveStack(player,1).isEmpty()&&tablet.items.getStackInSlot(0).getCount()==32,"Menu accepts general block stacks");
            test.assertTrue(menu.clickMenuButton(player,1)&&!tablet.leftClick()&&menu.clickMenuButton(player,2)&&tablet.redstone(),"Mode buttons update server state");
            player.moveTo(pos.getCenter().add(20,0,0));test.assertTrue(!menu.clickMenuButton(player,0)&&menu.quickMoveStack(player,0).isEmpty(),"Distant access denied");test.succeed();
        }finally{cow.discard();tablet.items.setStackInSlot(0,ItemStack.EMPTY);level.removeBlock(pos,false);}
    }
}
