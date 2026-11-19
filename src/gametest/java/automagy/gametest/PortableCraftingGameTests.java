package automagy.gametest;

import automagy.content.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;

@GameTestHolder("automagy")
@PrefixGameTestTemplate(false)
public final class PortableCraftingGameTests {
 private static ItemStack equip(ServerPlayer player,boolean storage){
  var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,true);
  var focus=new ItemStack(Content.CRAFTING_FOCUS.get());
  if(storage&&!Content.CRAFTING_FOCUS.get().applyUpgrade(focus,FocusUpgrade.require("automagy:storage"),1))throw new IllegalStateException("Storage upgrade rejected");
  WandFocusItem.attach(wand,focus);player.getInventory().selected=0;player.getInventory().setItem(0,wand);return wand;
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void ordinaryCraftConsumesIngredientsWithoutVis(GameTestHelper helper){
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AutomagyTest"));
  try{
   var wand=equip(player,false);var ordo=tc4revival.aspect.TCAspects.catalog().require("ordo");int before=TCItems.CASTING_WAND.get().vis(wand,ordo);
   var menu=new PortableCraftingMenu(1,player.getInventory(),0);menu.getSlot(0).set(new ItemStack(Items.OAK_LOG,2));menu.broadcastChanges();
   helper.assertTrue(menu.getSlot(9).getItem().is(Items.OAK_PLANKS)&&menu.getSlot(9).getItem().getCount()==4,"Log recipe output missing");
   menu.quickMoveStack(player,9);
   helper.assertTrue(player.getInventory().countItem(Items.OAK_PLANKS)==4,"Shift crafting lost or duplicated planks");
   helper.assertTrue(menu.getSlot(0).getItem().getCount()==1,"Craft did not consume exactly one log");
   helper.assertTrue(TCItems.CASTING_WAND.get().vis(wand,ordo)==before,"Ordinary crafting consumed vis");
   menu.removed(player);
   var leftovers=player.level().getEntitiesOfClass(HoveringItemEntity.class,player.getBoundingBox().inflate(3));
   helper.assertTrue(leftovers.size()==1&&leftovers.get(0).getItem().is(Items.OAK_LOG)&&leftovers.get(0).getItem().getCount()==1&&player.getInventory().countItem(Items.OAK_LOG)==0,"Closing must toss unused ingredient, not insert into inventory");
   leftovers.forEach(net.minecraft.world.entity.Entity::discard);
  }finally{player.discard();}helper.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void storedIngredientsSurviveReopenWithoutDuplication(GameTestHelper helper){
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AutomagyTest"));
  try{
   equip(player,true);var first=new PortableCraftingMenu(1,player.getInventory(),0);first.getSlot(3).set(new ItemStack(Items.DIAMOND,3));first.removed(player);
   helper.assertTrue(player.getInventory().countItem(Items.DIAMOND)==0,"Storage also returned a duplicate ingredient");
   var second=new PortableCraftingMenu(2,player.getInventory(),0);
   helper.assertTrue(second.getSlot(3).getItem().is(Items.DIAMOND)&&second.getSlot(3).getItem().getCount()==3,"Stored ingredients did not reload");
   second.removed(player);helper.assertTrue(player.getInventory().countItem(Items.DIAMOND)==0,"Repeated close duplicated storage");
  }finally{player.discard();}helper.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void fullInventoryDoesNotPartiallyTakeOutput(GameTestHelper helper){
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AutomagyTest"));
  try{
   equip(player,false);for(int i=1;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));player.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,63));
   var menu=new PortableCraftingMenu(1,player.getInventory(),0);menu.getSlot(0).set(new ItemStack(Items.OAK_LOG));menu.broadcastChanges();
   helper.assertTrue(menu.quickMoveStack(player,9).isEmpty(),"Partial output transfer was allowed");
   helper.assertTrue(player.getInventory().countItem(Items.OAK_PLANKS)==63&&menu.getSlot(0).getItem().is(Items.OAK_LOG),"Failed shift craft changed inventory");
  }finally{player.discard();}helper.succeed();
 }
 private static void withArcaneRecipe(GameTestHelper helper,java.util.function.Consumer<ServerPlayer> action){
  var manager=helper.getLevel().getRecipeManager();var saved=new java.util.ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(manager.getRecipes());
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AutomagyTest"));
  try{
   var json=com.google.gson.JsonParser.parseString("{\"type\":\"thaumcraft:arcane\",\"vis\":{\"ordo\":4},\"pattern\":[\"C\"],\"key\":{\"C\":{\"item\":\"minecraft:coal\"}},\"result\":{\"item\":\"minecraft:emerald\"}}").getAsJsonObject();
   var all=new java.util.ArrayList<>(saved);all.add(new ArcaneRecipe.Serializer().fromJson(net.minecraft.resources.ResourceLocation.parse("automagy:test_payment"),json));manager.replaceRecipes(all);action.accept(player);
  }finally{manager.replaceRecipes(saved);player.discard();}
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void combinedArcaneFeeMustBeAffordableBeforeOutput(GameTestHelper helper){
  withArcaneRecipe(helper,player->{
   var wand=equip(player,false);var casting=TCItems.CASTING_WAND.get();var ordo=tc4revival.aspect.TCAspects.catalog().require("ordo");
   int recipe=(int)(400*casting.consumptionModifier(wand,player,ordo,true)),extra=(int)(700*casting.consumptionModifier(wand,player,ordo,false));
   int available=Math.max(recipe,extra);wand.getOrCreateTag().putInt("ordo",available);
   var menu=new PortableCraftingMenu(1,player.getInventory(),0);menu.getSlot(0).set(new ItemStack(Items.COAL));menu.broadcastChanges();
   helper.assertTrue(menu.getSlot(9).getItem().isEmpty(),"Individually affordable payments incorrectly created output");
   helper.assertTrue(menu.unavailablePreview().is(Items.EMERALD),"Unavailable arcane recipe lost its display-only preview");
   helper.assertTrue(!menu.getSlot(47).isActive()&&!menu.getSlot(47).mayPickup(player)&&menu.quickMoveStack(player,47).isEmpty(),"Preview became an extractable inventory slot");
   menu.clicked(47,0,net.minecraft.world.inventory.ClickType.PICKUP,player);
   menu.clicked(47,0,net.minecraft.world.inventory.ClickType.CLONE,player);
   helper.assertTrue(menu.getCarried().isEmpty()&&player.getInventory().countItem(Items.EMERALD)==0&&menu.getSlot(0).getItem().is(Items.COAL),"Clicking preview created output or consumed input");
   wand.getOrCreateTag().putInt("ordo",recipe+extra+100);menu.broadcastChanges();
   helper.assertTrue(menu.unavailablePreview().isEmpty()&&menu.getSlot(9).getItem().is(Items.EMERALD),"Affordable result retained a ghost preview");
   wand.getOrCreateTag().putInt("ordo",available);menu.broadcastChanges();
   helper.assertTrue(casting.vis(wand,ordo)==available,"Preview consumed vis");menu.getSlot(0).set(ItemStack.EMPTY);menu.removed(player);
  });helper.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void gratisStillPaysRecipeButSkipsEmptyAdditionalCharge(GameTestHelper helper){
  withArcaneRecipe(helper,player->{
   var wand=equip(player,false);var focus=WandFocusItem.attached(wand);
   helper.assertTrue(Content.CRAFTING_FOCUS.get().applyUpgrade(focus,FocusUpgrade.require("automagy:gratis"),1),"Gratis upgrade rejected");WandFocusItem.attach(wand,focus);
   var casting=TCItems.CASTING_WAND.get();var ordo=tc4revival.aspect.TCAspects.catalog().require("ordo");int before=casting.vis(wand,ordo),expected=(int)(400*casting.consumptionModifier(wand,player,ordo,true));
   var menu=new PortableCraftingMenu(1,player.getInventory(),0);menu.getSlot(0).set(new ItemStack(Items.COAL));menu.broadcastChanges();
   helper.assertTrue(menu.getSlot(9).getItem().is(Items.EMERALD),"Gratis prevented affordable arcane output");menu.quickMoveStack(player,9);
   helper.assertTrue(player.getInventory().countItem(Items.EMERALD)==1&&menu.getSlot(0).getItem().isEmpty(),"Arcane craft lost output or did not consume coal");
   helper.assertTrue(before-casting.vis(wand,ordo)==expected,"Gratis charged wrong amount");menu.removed(player);
  });helper.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void staffAllowsOrdinaryButRejectsArcaneCrafting(GameTestHelper helper){
  withArcaneRecipe(helper,player->{
   var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD_STAFF,WandParts.Cap.GOLD,false,true);
   WandFocusItem.attach(wand,new ItemStack(Content.CRAFTING_FOCUS.get()));player.getInventory().selected=0;player.getInventory().setItem(0,wand);
   var menu=new PortableCraftingMenu(1,player.getInventory(),0);menu.getSlot(0).set(new ItemStack(Items.COAL));menu.broadcastChanges();
   helper.assertTrue(menu.usingStaff()&&menu.getSlot(9).getItem().isEmpty(),"Staff produced arcane output");
   menu.getSlot(0).set(new ItemStack(Items.OAK_LOG));menu.broadcastChanges();
   helper.assertTrue(menu.getSlot(9).getItem().is(Items.OAK_PLANKS),"Staff cannot craft ordinary recipe");menu.quickMoveStack(player,9);
   helper.assertTrue(player.getInventory().countItem(Items.OAK_PLANKS)==4,"Staff ordinary craft lost output");menu.removed(player);
  });helper.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void replacingWandInvalidatesMenuWithoutDuplicatingStorage(GameTestHelper helper){
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AutomagyTest"));
  try{
   var wand=equip(player,true);var menu=new PortableCraftingMenu(1,player.getInventory(),0);menu.getSlot(2).set(new ItemStack(Items.DIAMOND,3));
   // A network-equivalent copy is not the original server inventory owner.
   var copy=wand.copy();player.getInventory().setItem(0,copy);
   helper.assertTrue(!menu.stillValid(player),"Menu accepted replacement owner stack");
   helper.assertTrue(menu.quickMoveStack(player,2).isEmpty(),"Invalid menu allowed stored ingredient extraction");
   menu.removed(player);menu.removed(player);
   helper.assertTrue(player.getInventory().countItem(Items.DIAMOND)==0,"Invalidated storage returned duplicate ingredients");
   var reopened=new PortableCraftingMenu(2,player.getInventory(),0);
   helper.assertTrue(reopened.getSlot(2).getItem().getCount()==3,"Replacement copy lost persisted contents");reopened.removed(player);
  }finally{player.discard();}helper.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void cakeReturnsExactlyThreeBuckets(GameTestHelper helper){
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AutomagyTest"));
  try{
   equip(player,false);var menu=new PortableCraftingMenu(1,player.getInventory(),0);
   var ingredients=new Item[]{Items.MILK_BUCKET,Items.MILK_BUCKET,Items.MILK_BUCKET,Items.SUGAR,Items.EGG,Items.SUGAR,Items.WHEAT,Items.WHEAT,Items.WHEAT};
   for(int i=0;i<9;i++)menu.getSlot(i).set(new ItemStack(ingredients[i]));menu.broadcastChanges();
   helper.assertTrue(menu.getSlot(9).getItem().is(Items.CAKE),"Cake preview missing");menu.quickMoveStack(player,9);menu.removed(player);
   helper.assertTrue(player.getInventory().countItem(Items.CAKE)==1&&player.getInventory().countItem(Items.BUCKET)==3,"Cake output or container remainder incorrect");
   helper.assertTrue(player.getInventory().countItem(Items.MILK_BUCKET)==0&&player.getInventory().countItem(Items.WHEAT)==0,"Cake retained consumed ingredients");
  }finally{player.discard();}helper.succeed();
 }
 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void transposingSwapsInventoryAndPouchWithoutLosingNbt(GameTestHelper helper){
  for(boolean pouchMode:new boolean[]{false,true}){
   var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AutomagyTest"));
   try{
    var wand=equip(player,false);var old=new ItemStack(TCItems.FIRE_FOCUS.get());old.getOrCreateTag().putString("TestMarker","original");WandFocusItem.attach(wand,old);
    // Use an empty wand to stop before GUI/network opening, after the real key swap path.
    wand.getOrCreateTag().putInt("ordo",0);
    var replacement=new ItemStack(Content.CRAFTING_FOCUS.get());
    helper.assertTrue(Content.CRAFTING_FOCUS.get().applyUpgrade(replacement,FocusUpgrade.require("automagy:transposing"),1),"Transposing upgrade rejected");
    replacement.getOrCreateTag().putString("TestMarker","replacement");
    ItemStack pouch=ItemStack.EMPTY;
    if(pouchMode){pouch=new ItemStack(TCItems.FOCUS_POUCH.get());var contents=FocusPouchItem.contents(pouch);contents.set(4,replacement);FocusPouchItem.save(pouch,contents);player.getInventory().setItem(1,pouch);}
    else player.getInventory().setItem(1,replacement);
    CraftingFocusKey.press(player);
    helper.assertTrue(ItemStack.isSameItemSameTags(WandFocusItem.attached(wand),replacement),"Key did not attach transposing focus with its NBT");
    helper.assertTrue(ItemStack.isSameItemSameTags(pouchMode?FocusPouchItem.contents(pouch).get(4):player.getInventory().getItem(1),old),"Swap lost original focus or its NBT");
    helper.assertTrue(player.containerMenu==player.inventoryMenu,"Empty wand opened a crafting menu");
   }finally{player.discard();}
  }helper.succeed();
 }

 @GameTest(template="empty",templateNamespace="automagy",timeoutTicks=20)
 public static void hoveringLeftoversPersistRefreshOnTouchAndResumeFalling(GameTestHelper helper){
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AutomagyTest"));HoveringItemEntity hover=null;
  try{
   for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
   hover=HoveringItemEntity.toss(player,new ItemStack(Items.DIAMOND,3),true);hover.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);hover.setNoPickUpDelay();
   var saved=new net.minecraft.nbt.CompoundTag();hover.addAdditionalSaveData(saved);saved.putInt("HoverTicks",5);hover.readAdditionalSaveData(saved);
   helper.assertTrue(hover.hoverTicks()==5&&hover.isNoGravity()&&hover.getItem().getCount()==3,"Hover save/reload lost lifetime or contents");
   hover.playerTouch(player);helper.assertTrue(hover.hoverTicks()==200&&hover.isAlive(),"Full-inventory touch did not extend hover lifetime");
   hover.addAdditionalSaveData(saved);saved.putInt("HoverTicks",1);hover.readAdditionalSaveData(saved);var box=hover.getBoundingBox().inflate(1);hover.tick();
   var falling=player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,box,e->!(e instanceof HoveringItemEntity)&&e.getItem().is(Items.DIAMOND));
   helper.assertTrue(hover.isRemoved()&&falling.size()==1&&falling.get(0).getItem().getCount()==3&&!falling.get(0).isNoGravity(),"Hover expiry lost/duplicated contents or retained no-gravity");falling.forEach(net.minecraft.world.entity.Entity::discard);
  }finally{if(hover!=null&&!hover.isRemoved())hover.discard();player.discard();}helper.succeed();
 }

}
