package heartwork;

import com.mojang.authlib.GameProfile;
import heartwork.content.*;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder("heartwork") @PrefixGameTestTemplate(false)
public class HeartworkServerTests {
    private static ItemStack item(String id) { return new ItemStack(Heartwork.item(id)); }
    private static Player player(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"HeartworkTest"));
        player.getAbilities().instabuild=false;
        player.moveTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,2,1))));
        return player;
    }
    private static CraftingContainer grid() {
        return new TransientCraftingContainer(new AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(Player p,int i) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player p) { return true; }
        },3,3);
    }

    @GameTest(template="empty") public static void hammerCraftingConservesTools(GameTestHelper h) {
        var input=grid();input.setItem(0,item("hammer"));input.getItem(0).getOrCreateTag().putString("Owner","test");
        input.setItem(1,new ItemStack(Items.COAL));
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();
        var output=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(output.is(Heartwork.item("coal_powder")) && output.getCount()==2,"Coal crushing retains original 2 powder output");
        var remaining=recipe.getRemainingItems(input);
        h.assertTrue(remaining.get(0).is(Heartwork.item("hammer")) && remaining.get(0).getCount()==1,"Exactly one hammer remains");
        h.assertTrue(remaining.get(0).getTag().getString("Owner").equals("test"),"Hammer metadata retained");
        h.assertTrue(remaining.get(1).isEmpty(),"Coal consumed");h.succeed();
    }

    @GameTest(template="empty") public static void countedFurnaceRecipesAndNetworkKeepYield(GameTestHelper h) {
        for (String[] pair:new String[][]{{"dough","biscuit","2"},{"dough_cookie","minecraft:cookie","4"}}) {
            var input=new SimpleContainer(item(pair[0]));
            var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,h.getLevel()).orElseThrow();
            var result=recipe.assemble(input,h.getLevel().registryAccess());
            h.assertTrue(result.is(Heartwork.item(pair[1])) && result.getCount()==Integer.parseInt(pair[2]),"Original furnace output count for "+pair[0]);
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                Heartwork.SMELTING.get().toNetwork(buffer,recipe);
                var decoded=Heartwork.SMELTING.get().fromNetwork(recipe.getId(),buffer);
                h.assertTrue(ItemStack.matches(result,decoded.assemble(input,h.getLevel().registryAccess())),"Furnace recipe network roundtrip");
            } finally { buffer.release(); }
        }
        h.succeed();
    }

    @GameTest(template="empty") public static void eatingStackedMealsReturnsOneContainer(GameTestHelper h) {
        for (var element:Heartwork.CONTENT.getAsJsonArray("items")) {
            var definition=element.getAsJsonObject();
            if (!definition.has("remainder")) continue;
            var p=player(h);p.getFoodData().setFoodLevel(1);
            var stack=item(definition.get("id").getAsString());stack.setCount(3);
            var result=stack.finishUsingItem(h.getLevel(),p);
            h.assertTrue(result.getCount()==2 && result.getItem()==stack.getItem(),"Stacked food remains after eating one");
            int count=0;var container=Heartwork.item(definition.get("remainder").getAsString());
            for (int i=0;i<p.getInventory().getContainerSize();i++) if(p.getInventory().getItem(i).is(container)) count+=p.getInventory().getItem(i).getCount();
            h.assertTrue(count==1,"Exactly one container returned for "+definition.get("id"));
            var last=item(definition.get("id").getAsString());p.getFoodData().setFoodLevel(1);
            h.assertTrue(last.finishUsingItem(h.getLevel(),p).is(container),"Last food transforms into container");
        }
        h.succeed();
    }

    @GameTest(template="empty") public static void swissRepairKeepsProgressAndRejectsExtraInput(GameTestHelper h) {
        var input=grid();input.setItem(1,item("hone"));
        var knife=item("swiss");knife.setDamageValue(128);knife.getOrCreateTag().putInt("Exp",77);
        knife.getOrCreateTag().putBoolean("active",true);knife.setHoverName(net.minecraft.network.chat.Component.literal("Owner's knife"));
        input.setItem(3,knife);
        var recipe=h.getLevel().getRecipeManager().byKey(new ResourceLocation("heartwork:swiss_repair")).orElseThrow();
        var crafting=(CraftingRecipe)recipe;
        h.assertTrue(crafting.matches(input,h.getLevel()),"Valid original diagonal sharpening grid");
        var result=crafting.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(result.getDamageValue()==0 && result.getTag().getInt("Exp")==77 && SwissItem.active(result),"Sharpening retains growth and active state");
        h.assertTrue(result.getHoverName().getString().equals("Owner's knife") && knife.getDamageValue()==128,"Preview preserves original and custom name");
        var saved=ItemStack.of(result.save(new CompoundTag()));
        h.assertTrue(saved.getTag().getInt("Exp")==77,"Progress survives persistence");
        input.setItem(0,new ItemStack(Items.DIAMOND));
        h.assertTrue(!crafting.matches(input,h.getLevel()),"Extra ingredients cannot be silently consumed");h.succeed();
    }

    @GameTest(template="empty") public static void swissGrowthAndSelfRepairUseOriginalThresholds(GameTestHelper h) {
        var p=player(h);var stack=item("swiss");var swiss=(SwissItem)stack.getItem();
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.setShiftKeyDown(true);
        swiss.use(h.getLevel(),p,InteractionHand.MAIN_HAND);p.setShiftKeyDown(false);
        h.assertTrue(SwissItem.active(stack),"Real use unfolds knife");
        stack.getOrCreateTag().putInt("Exp",49);
        swiss.mineBlock(stack,h.getLevel(),Blocks.STONE.defaultBlockState(),h.absolutePos(new BlockPos(1,1,1)),p);
        h.assertTrue(SwissItem.proficiency(stack)==2 && stack.getDamageValue()==4,"49 to 50 uses upgrades to tier 2 after paying old tier cost");
        swiss.mineBlock(stack,h.getLevel(),Blocks.STONE.defaultBlockState(),h.absolutePos(new BlockPos(1,1,1)),p);
        h.assertTrue(stack.getDamageValue()==6,"Tier 2 costs two durability");
        p.tickCount=400;swiss.inventoryTick(stack,h.getLevel(),p,0,true);
        h.assertTrue(stack.getDamageValue()==5,"One durability repairs every 400 ticks");h.succeed();
    }

    @GameTest(template="empty") public static void matureCropsHarvestAndResetOnce(GameTestHelper h) {
        var p=player(h);
        // An empty structure does not clear terrain; provide actual growing light first.
        for (int i=0;i<4;i++) {
            var pos=h.absolutePos(new BlockPos(1+i,2,1));
            h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            h.getLevel().setBlock(pos.below(),Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);
            h.getLevel().setBlock(pos.above(),Blocks.GLOWSTONE.defaultBlockState(),3);
        }
        h.runAfterDelay(5, () -> {
        int index=0;
        for(var element:Heartwork.CONTENT.getAsJsonArray("blocks")) {
            var data=element.getAsJsonObject();if(!data.get("kind").getAsString().equals("crop"))continue;
            var crop=(HeartworkCrop)Heartwork.BLOCK_MAP.get(data.get("id").getAsString()).get();
            var pos=h.absolutePos(new BlockPos(1+index++,2,1));
            h.getLevel().setBlock(pos,crop.getStateForAge(7),3);
            h.assertTrue(h.getLevel().getBlockState(pos).is(crop),"Crop remains planted in illuminated farmland");
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
            int xp=p.totalExperience;
            h.assertTrue(crop.use(h.getLevel().getBlockState(pos),h.getLevel(),pos,p,InteractionHand.MAIN_HAND,hit).consumesAction(),"Mature crop harvest action");
            h.assertTrue(crop.getAge(h.getLevel().getBlockState(pos))==0 && p.totalExperience==xp+5,"Harvest resets age and grants original 5 XP");
            h.assertTrue(!crop.use(h.getLevel().getBlockState(pos),h.getLevel(),pos,p,InteractionHand.MAIN_HAND,hit).consumesAction(),"No duplicate harvest of immature crop");
        }
        h.succeed();
        });
    }
}
