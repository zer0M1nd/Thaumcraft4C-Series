package tc4revival.content;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class VoidRobeServerTests {
    private static  class RobePlayer extends FakePlayer {
        RobePlayer(ServerLevel level) { super(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"VoidRobeTest")); }
        float armorStage(DamageSource source,float amount) { return getDamageAfterArmorAbsorb(source,amount); }
    }
    @GameTest(template="wand_test_empty")
    public static void voidRobesUseMagicProtectionWithoutFortressSetBonuses(GameTestHelper test) {
        var player=new RobePlayer(test.getLevel());
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TCItems.VOID_ROBE_HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(TCItems.VOID_ROBE_CHEST.get()));
        player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(TCItems.VOID_ROBE_LEGS.get()));
        test.assertTrue(Math.abs(player.armorStage(player.damageSources().cactus(),10)-3.6f)<.0001,"Void robes have no fortress set multiplier");
        test.assertTrue(Math.abs(player.armorStage(player.damageSources().magic(),10)-(10-160/35f))<.0001,"Magic uses the special armor priority");
        test.assertTrue(player.armorStage(player.damageSources().onFire(),10)==10,"Void robes have no fortress fire override for bypass damage");
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TCItems.FORTRESS_HELMET.get()));
        test.assertTrue(Math.abs(player.armorStage(player.damageSources().cactus(),10)-3.6f)<.0001,"Mixed robe pieces cannot increase the fortress set count");
        test.assertTrue(TCItems.VOID_ROBE_HELMET.get().getMaxDamage()==110 && TCItems.VOID_ROBE_CHEST.get().getMaxDamage()==160 && TCItems.VOID_ROBE_LEGS.get().getMaxDamage()==150,"Use actual ConfigItems VOID material, not unused VOIDFORTRESS");
        pass(test,"mixed-special-armor");
    }
    @GameTest(template="wand_test_empty")
    public static void voidRobeRepairAndGearInterfacesFollowWornAndCarriedRules(GameTestHelper test) {
        var player=new RobePlayer(test.getLevel()); var helmet=new ItemStack(TCItems.VOID_ROBE_HELMET.get()); helmet.setDamageValue(12);
        var chest=new ItemStack(TCItems.VOID_ROBE_CHEST.get()); var legs=new ItemStack(TCItems.VOID_ROBE_LEGS.get());
        player.setItemSlot(EquipmentSlot.HEAD,helmet); player.setItemSlot(EquipmentSlot.CHEST,chest); player.setItemSlot(EquipmentSlot.LEGS,legs);
        player.doTick();player.doTick(); // Install worn attributes before the controlled repair checkpoints.
        helmet.setDamageValue(12); // Setup at tick zero can already repair; reset only the checkpoint input.
        player.tickCount=20; helmet.inventoryTick(test.getLevel(),player,39,false); TCItems.VOID_ROBE_HELMET.get().onArmorTick(helmet,test.getLevel(),player);
        test.assertTrue(helmet.getDamageValue()==11,"Equipped armor receives one repair, not two inventory passes");
        var carried=new ItemStack(TCItems.VOID_ROBE_CHEST.get()); carried.setDamageValue(8); carried.inventoryTick(test.getLevel(),player,8,false);
        test.assertTrue(carried.getDamageValue()==7,"Carried robes also repair"); player.tickCount=21;
        carried.inventoryTick(test.getLevel(),player,8,false); test.assertTrue(carried.getDamageValue()==7,"Repair is limited to each twentieth entity tick");
        test.assertTrue(WarpingGear.equipped(player)==6,"Full set contributes six equipment warp");
        int discount=0; for (var stack:player.getArmorSlots()) if (stack.getItem() instanceof VisDiscountGear gear) discount+=gear.visDiscount(stack,player,null);
        test.assertTrue(discount==15 && TCItems.VOID_ROBE_HELMET.get().showNodes(helmet,player) && !TCItems.VOID_ROBE_CHEST.get().showNodes(chest,player),"All pieces discount vis, only the hood reveals");
        pass(test,"repair-and-gear");
    }
    @GameTest(template="wand_test_empty")
    public static void voidRobeDyeWashAndInfusionPreserveLegacyInputs(GameTestHelper test) {
        var player=new RobePlayer(test.getLevel()); var robe=new ItemStack(TCItems.VOID_ROBE_CHEST.get()); robe.setDamageValue(19); robe.getOrCreateTag().putInt("Runic",4);
        var menu=new AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int index) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        };
        var grid=new TransientCraftingContainer(menu,2,2); grid.setItem(0,robe); grid.setItem(1,new ItemStack(Items.RED_DYE));
        var dye=(RobeDyeRecipe)test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("minecraft:armor_dye")).orElseThrow();
        test.assertTrue(dye.matches(grid,test.getLevel()),"Void robes must enter legacy robe dyeing"); var colored=dye.assemble(grid,test.getLevel().registryAccess());
        test.assertTrue(TCItems.VOID_ROBE_CHEST.get().getColor(colored)==0x8c3960 && colored.getDamageValue()==19 && colored.getTag().getInt("Runic")==4,"Old palette and mandatory base color preserve armor state");
        var pos=test.absolutePos(new BlockPos(2,2,2)); test.getLevel().setBlock(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,2),3);
        player.setItemInHand(InteractionHand.MAIN_HAND,colored);
        TCItems.VOID_ROBE_CHEST.get().onItemUseFirst(colored,new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
        test.assertTrue(TCItems.VOID_ROBE_CHEST.get().getColor(colored)==6961280 && test.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)==1 && colored.getDamageValue()==19,"Wash consumes one water level and only clears color");
        var recipe=(InfusionRecipe)test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:infusion/void_robe_helmet")).orElseThrow();
        var center=new ItemStack(TCItems.voidArmor(VoidEquipment.Armor.HELMET));
        var parts=List.of(new ItemStack(TCItems.GOGGLES.get()),new ItemStack(TCItems.material(MaterialType.FABRIC)),new ItemStack(TCItems.material(MaterialType.FABRIC)),
                new ItemStack(TCItems.material(MaterialType.SALIS_MUNDUS)),new ItemStack(TCItems.material(MaterialType.FABRIC)),new ItemStack(TCItems.material(MaterialType.FABRIC)));
        test.assertTrue(recipe.matches(new InfusionRecipe.Input(center,parts,key->true),test.getLevel()),"Actual hood recipe must match its legacy components");
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,parts,key->false),test.getLevel()),"Hood infusion is research gated");
        center.setDamageValue(1); test.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,parts,key->true),test.getLevel()),"Base crafting input is not a damage wildcard");
        pass(test,"dye-wash-and-infusion");
    }
    @GameTest(template="wand_test_empty")
    public static void loadedRobeDyeAndWashCoverAllPiecesAndLegacyAliases(GameTestHelper test) {
        var player=new RobePlayer(test.getLevel());
        var menu=new AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
        };
        var grid=new TransientCraftingContainer(menu,3,3);
        // Default robe mixed with each old sheep colour, in modern DyeColor ID order.
        int[] expected={0xb49bbf,0xac615f,0x8e42ac,0x6868ac,0xb2975f,0x94a661,0xb9609b,0x5b4266,
                0x81688c,0x5b5b8c,0x743b99,0x4e4299,0x734862,0x7f6f6d,0x8c3960,0x41284c};
        Item[] pieces={TCItems.ROBE_CHEST.get(),TCItems.ROBE_LEGS.get(),TCItems.ROBE_BOOTS.get(),
                TCItems.VOID_ROBE_HELMET.get(),TCItems.VOID_ROBE_CHEST.get(),TCItems.VOID_ROBE_LEGS.get()};
        Item[] aliases={Items.BONE_MEAL,Items.INK_SAC,Items.LAPIS_LAZULI,Items.COCOA_BEANS};
        int[] aliasColors={0,15,11,12};
        var pos=test.absolutePos(new BlockPos(2,2,2));
        for(var item:pieces) {
            var robe=new ItemStack(item); robe.setDamageValue(7); robe.getOrCreateTag().putInt("Runic",4);
            robe.setHoverName(net.minecraft.network.chat.Component.literal("Retained robe name"));
            var original=robe.copy(); var dyeable=(DyeableLeatherItem)item;
            for(int index=0;index<20;index++) {
                grid.clearContent(); grid.setItem(4,robe);
                grid.setItem(8,new ItemStack(index<16?DyeItem.byColor(DyeColor.byId(index)):aliases[index-16]));
                var recipe=test.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,test.getLevel()).orElseThrow();
                test.assertTrue(recipe instanceof RobeDyeRecipe,"Loaded crafting lookup must select robe dye serializer");
                var result=recipe.assemble(grid,test.getLevel().registryAccess());
                test.assertTrue(dyeable.getColor(result)==expected[index<16?index:aliasColors[index-16]],"Legacy colour mismatch: "+item+" dye "+index);
                test.assertTrue(result.getDamageValue()==7 && result.getTag().getInt("Runic")==4 && result.getHoverName().equals(robe.getHoverName()),"Dye preserves unrelated NBT");
                test.assertTrue(ItemStack.matches(robe,original),"Crafting must not mutate source");
            }
            grid.setItem(0,new ItemStack(item));
            test.assertTrue(test.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,test.getLevel()).isEmpty(),"Two robes must not craft");
            for(int level=1;level<=3;level++) for(boolean colored:new boolean[]{false,true}) {
                var washed=original.copy(); if(colored)dyeable.setColor(washed,0x123456);
                test.getLevel().setBlock(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,level),3);
                player.setItemInHand(InteractionHand.MAIN_HAND,washed);
                var result=item.onItemUseFirst(washed,new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
                var state=test.getLevel().getBlockState(pos);
                test.assertTrue(result.consumesAction() && (level==1?state.is(Blocks.CAULDRON):state.getValue(LayeredCauldronBlock.LEVEL)==level-1),"Wash consumes exactly one water level, even undyed");
                test.assertTrue(dyeable.getColor(washed)==6961280 && ItemStack.matches(washed,original),"Wash only removes color");
            }
            for(var block:List.of(Blocks.CAULDRON,Blocks.LAVA_CAULDRON)) {
                var untouched=original.copy(); dyeable.setColor(untouched,0x123456); var before=untouched.copy();
                test.getLevel().setBlock(pos,block.defaultBlockState(),3); player.setItemInHand(InteractionHand.MAIN_HAND,untouched);
                test.assertTrue(item.onItemUseFirst(untouched,new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)))==InteractionResult.PASS && ItemStack.matches(untouched,before),"Non-water cauldrons do not wash");
            }
        }
        pass(test,"loaded-dye-120-and-wash-48");
    }
    @GameTest(template="wand_test_empty")
    public static void robeRedyeConsumesIngredientsAndKeepsVanillaLeather(GameTestHelper test) {
        var player=new RobePlayer(test.getLevel());
        var menu=new AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
        };
        var grid=new TransientCraftingContainer(menu,3,3);
        var recipe=(RobeDyeRecipe)test.getLevel().getRecipeManager().byKey(ResourceLocation.parse("minecraft:armor_dye")).orElseThrow();
        for(var item:List.of(TCItems.ROBE_CHEST.get(),TCItems.ROBE_LEGS.get(),TCItems.ROBE_BOOTS.get(),
                TCItems.VOID_ROBE_HELMET.get(),TCItems.VOID_ROBE_CHEST.get(),TCItems.VOID_ROBE_LEGS.get())) {
            var robe=new ItemStack(item); var dyeable=(DyeableLeatherItem)item;
            dyeable.setColor(robe,0x123456); robe.setDamageValue(9); robe.getOrCreateTag().putInt("Runic",3);
            var original=robe.copy();
            for(int offset=0;offset<9;offset++) {
                grid.clearContent(); grid.setItem(offset,robe.copy());
                grid.setItem((offset+1)%9,new ItemStack(Items.RED_DYE,5));
                grid.setItem((offset+2)%9,new ItemStack(Items.WHITE_DYE,4));
                grid.setItem((offset+3)%9,new ItemStack(Items.BLACK_DYE,3));
                test.assertTrue(recipe.matches(grid,test.getLevel()),"Multidye is shapeless");
                var output=recipe.assemble(grid,test.getLevel().registryAccess());
                test.assertTrue(dyeable.getColor(output)==0x816e78 && output.getDamageValue()==9 && output.getTag().getInt("Runic")==3 && output.getCount()==1,"Existing colour participates in three-dye mixture and preserves NBT");
                test.assertTrue(ItemStack.matches(robe,original),"Preview must not mutate robe");
                var resultInventory=new ResultContainer(); resultInventory.setItem(0,output.copy());
                var slot=new ResultSlot(player,grid,resultInventory,0,0,0);
                var taken=slot.remove(1); slot.onTake(player,taken);
                test.assertTrue(grid.getItem(offset).isEmpty() && grid.getItem((offset+1)%9).getCount()==4 && grid.getItem((offset+2)%9).getCount()==3 && grid.getItem((offset+3)%9).getCount()==2,"Actual vanilla result-slot callback consumes one of each occupied ingredient");
                test.assertTrue(ItemStack.matches(taken,output),"Taking result preserves assembled output");
            }
            grid.clearContent(); grid.setItem(4,robe);
            for(int i=0;i<9;i++) if(i!=4) grid.setItem(i,new ItemStack(Items.RED_DYE));
            test.assertTrue(dyeable.getColor(recipe.assemble(grid,test.getLevel().registryAccess()))==0x913538,"Full grid supports eight dyes");
            grid.clearContent(); grid.setItem(0,robe);
            test.assertTrue(!recipe.matches(grid,test.getLevel()),"Robe alone cannot craft");
            grid.setItem(1,new ItemStack(Items.DIRT));
            test.assertTrue(!recipe.matches(grid,test.getLevel()),"Non-dye ingredient rejected");
        }
        var vanilla=new net.minecraft.world.item.crafting.ArmorDyeRecipe(ResourceLocation.parse("thaumcraft:test_vanilla_dye"),net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        for(var item:List.of(Items.LEATHER_HELMET,Items.LEATHER_CHESTPLATE,Items.LEATHER_LEGGINGS,Items.LEATHER_BOOTS)) for(boolean colored:new boolean[]{false,true}) {
            grid.clearContent(); var leather=new ItemStack(item); leather.setDamageValue(8);
            if(colored)((DyeableLeatherItem)item).setColor(leather,0x123456);
            grid.setItem(0,leather); grid.setItem(1,new ItemStack(Items.BLUE_DYE)); grid.setItem(8,new ItemStack(Items.RED_DYE));
            test.assertTrue(recipe.matches(grid,test.getLevel()) && ItemStack.matches(recipe.assemble(grid,test.getLevel().registryAccess()),vanilla.assemble(grid,test.getLevel().registryAccess())),"Ordinary leather keeps current vanilla mixing");
            grid.setItem(1,new ItemStack(Items.LAPIS_LAZULI));
            test.assertTrue(!recipe.matches(grid,test.getLevel()),"Legacy raw-dye aliases only apply to TC4 robes");
        }
        pass(test,"redye-54-result-slots-full-grid-and-leather-8");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Void robe system test passed: {}",name); test.succeed(); }
}
