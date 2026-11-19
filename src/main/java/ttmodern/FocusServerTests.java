package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.aspect.TCAspects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class FocusServerTests {
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test,BlockPos pos){
        var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TinkererFocus"));player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);return player;
    }
    private static ItemStack wand(WandFocusItem focus){var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,new ItemStack(focus));return wand;}
    @GameTest(template="wand_test_empty")
    public static void tinkererFlightPaysAndRespectsConfiguration(GameTestHelper test){
        var player=player(test,test.absolutePos(new BlockPos(2,2,2)));var focus=Tinkerer.FLIGHT_FOCUS.get();var stack=wand(focus);player.setItemInHand(InteractionHand.MAIN_HAND,stack);player.fallDistance=20;
        boolean enabled=TinkererConfig.FLIGHT.get();
        try {
            TinkererConfig.FLIGHT.set(true);focus.cast(stack,test.getLevel(),player,InteractionHand.MAIN_HAND,null);
            test.assertTrue(Math.abs(player.getDeltaMovement().z-2d/3)<.00001&&player.fallDistance==0,"Look-directed impulse and fall reset");
            test.assertTrue(TCItems.CASTING_WAND.get().vis(stack,TCAspects.catalog().require("aer"))==9985,"Flight debits 0.15 vis");
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);TinkererConfig.FLIGHT.set(false);focus.cast(stack,test.getLevel(),player,InteractionHand.MAIN_HAND,null);
            test.assertTrue(player.getDeltaMovement().lengthSqr()==0&&stack.getTag().getInt("aer")==9985,"Disabled flight has no impulse or charge");
            TinkererConfig.FLIGHT.set(true);stack.getOrCreateTag().putInt("aer",0);focus.cast(stack,test.getLevel(),player,InteractionHand.MAIN_HAND,null);
            test.assertTrue(player.getDeltaMovement().lengthSqr()==0,"No free flight on an empty wand");
            var discounted=wand(focus);var attached=WandFocusItem.attached(discounted);focus.applyUpgrade(attached,FocusUpgrade.FRUGAL,1);WandFocusItem.attach(discounted,attached);player.setItemInHand(InteractionHand.MAIN_HAND,discounted);focus.cast(discounted,test.getLevel(),player,InteractionHand.MAIN_HAND,null);
            test.assertTrue(discounted.getTag().getInt("aer")==9987,"Frugal upgrade uses common wand discount");test.succeed();
        }finally{TinkererConfig.FLIGHT.set(enabled);player.stopUsingItem();}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererTelekinesisOnlyChargesActiveItemPull(GameTestHelper test){
        var pos=test.absolutePos(new BlockPos(2,2,2));var player=player(test,pos);var focus=Tinkerer.TELEKINESIS_FOCUS.get();var stack=wand(focus);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var cargo=new ItemEntity(test.getLevel(),player.getX(),player.getY()+1,player.getZ()+3,new ItemStack(Items.APPLE));test.getLevel().addFreshEntity(cargo);
        try {
            focus.usingTick(stack,player,20);test.assertTrue(cargo.getDeltaMovement().z>0&&stack.getTag().getInt("aer")==9995&&stack.getTag().getInt("perditio")==9995,"Forward target pulls item and charges both primals");
            player.setShiftKeyDown(true);focus.usingTick(stack,player,19);test.assertTrue(cargo.getDeltaMovement().z<0&&stack.getTag().getInt("aer")==9990,"Sneaking pulls toward player");
            cargo.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);stack.getTag().putInt("perditio",0);focus.usingTick(stack,player,18);
            test.assertTrue(cargo.getDeltaMovement().lengthSqr()==0&&stack.getTag().getInt("aer")==9990,"One missing primal rejects entire payment and effect");
            cargo.discard();stack.getTag().putInt("perditio",10000);focus.usingTick(stack,player,17);test.assertTrue(stack.getTag().getInt("aer")==9990,"Empty area does not consume vis");test.succeed();
        }finally{cargo.discard();player.stopUsingItem();}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererSmeltingRequiresProgressAndExactPayment(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var player=player(test,pos.north(2));var focus=Tinkerer.SMELTING_FOCUS.get();var stack=wand(focus);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        // Optional mods can remove sand -> glass. The legacy focus follows the live furnace recipes.
        net.minecraft.world.level.block.Block input=null,output=null;
        for(var candidate:java.util.List.of(Blocks.SAND,Blocks.COBBLESTONE,Blocks.STONE,Blocks.NETHERRACK,Blocks.CLAY)){
            var inventory=new net.minecraft.world.SimpleContainer(new ItemStack(candidate));
            var result=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,inventory,level).map(r->r.assemble(inventory,level.registryAccess())).orElse(ItemStack.EMPTY);
            if(result.getItem() instanceof BlockItem block&&result.getCount()==1&&block.getBlock()!=candidate&&!block.getBlock().defaultBlockState().hasBlockEntity()){input=candidate;output=block.getBlock();break;}
        }
        test.assertTrue(input!=null,"Environment provides a valid block smelting fixture");level.setBlock(pos,input.defaultBlockState(),2);
        try {
            for(int i=0;i<20;i++)focus.heat(stack,player,pos);
            test.assertTrue(level.getBlockState(pos).is(input)&&stack.getTag().getInt("ignis")==10000,"Progress preflight does not charge");focus.heat(stack,player,pos);
            test.assertTrue(level.getBlockState(pos).is(output)&&stack.getTag().getInt("ignis")==9955&&stack.getTag().getInt("perditio")==9988,"Completed conversion pays once: block="+level.getBlockState(pos)+", vis="+stack.getTag());
            level.setBlock(pos,input.defaultBlockState(),2);for(int i=0;i<10;i++)focus.heat(stack,player,pos);focus.release(stack,level,player,0);
            for(int i=0;i<11;i++)focus.heat(stack,player,pos);test.assertTrue(level.getBlockState(pos).is(input),"Release clears incomplete target progress");
            stack.getTag().putInt("perditio",0);for(int i=0;i<30;i++)focus.heat(stack,player,pos);test.assertTrue(level.getBlockState(pos).is(input)&&stack.getTag().getInt("ignis")==9955,"Insufficient vis cannot replace block or partially charge");
            test.succeed();
        }finally{focus.release(stack,level,player,0);level.removeBlock(pos,false);}
    }
}
