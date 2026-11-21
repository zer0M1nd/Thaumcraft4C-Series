package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.gametest.*;
import top.theillusivec4.curios.api.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class Report0922ServerTests {
    @GameTest(template="wand_test_empty")
    public static void tinkererReport0922FireFacesFollowSupportAndNeighbors(GameTestHelper test){
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var faces=java.util.List.of(net.minecraft.world.level.block.FireBlock.NORTH,net.minecraft.world.level.block.FireBlock.SOUTH,net.minecraft.world.level.block.FireBlock.EAST,net.minecraft.world.level.block.FireBlock.WEST,net.minecraft.world.level.block.FireBlock.UP);
        for(var entry:Tinkerer.FIRES.entrySet()){
            for(var direction:Direction.values())world.setBlockAndUpdate(pos.relative(direction),Blocks.AIR.defaultBlockState());
            world.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            world.setBlockAndUpdate(pos,entry.getValue().get().defaultBlockState().setValue(ImbuedFireBlock.AGE,7));
            var state=world.getBlockState(pos);
            test.assertTrue(faces.stream().noneMatch(state::getValue),"Supported fire uses full floor model: "+entry.getKey());
            world.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());
            world.setBlockAndUpdate(pos.north(),Blocks.OAK_PLANKS.defaultBlockState());
            world.setBlockAndUpdate(pos.above(),Blocks.OAK_PLANKS.defaultBlockState());
            state=world.getBlockState(pos);
            test.assertTrue(state.getValue(net.minecraft.world.level.block.FireBlock.NORTH)&&state.getValue(net.minecraft.world.level.block.FireBlock.UP),"Wall and ceiling flames attach to fuel");
            test.assertTrue(!state.getValue(net.minecraft.world.level.block.FireBlock.SOUTH)&&state.getValue(ImbuedFireBlock.AGE)==7,"Empty side stays clear and visual updates preserve age");
            world.setBlockAndUpdate(pos.north(),Blocks.AIR.defaultBlockState());
            test.assertTrue(!world.getBlockState(pos).getValue(net.minecraft.world.level.block.FireBlock.NORTH),"Removing fuel removes attached face");
        }
        test.succeed();
    }
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper test){
        var p=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TT0922"));
        p.moveTo(test.absolutePos(new BlockPos(2,2,2)).getCenter());return p;
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererReport0922UniversalCuriosRemainFunctional(GameTestHelper test){
        var p=player(test);var curios=CuriosApi.getCuriosInventory(p).resolve().orElseThrow();curios.reset();
        var slots=curios.getStacksHandler("necklace").orElseThrow().getStacks();
        var nitor=new ItemStack(Tinkerer.BRIGHT_NITOR.get());var clay=new ItemStack(Tinkerer.PROTOCLAY.get());
        var tag=net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse("curios:curio"));
        test.assertTrue(nitor.is(tag)&&clay.is(tag),"Both items accept universal Curios slots");
        slots.setStackInSlot(0,nitor);var context=new SlotContext("necklace",p,0,false,true);
        var light=p.blockPosition().above();test.getLevel().setBlockAndUpdate(light,Blocks.AIR.defaultBlockState());
        CuriosApi.getCurio(nitor).resolve().orElseThrow().curioTick(context);
        test.assertTrue(test.getLevel().getBlockState(light).is(Tinkerer.NITOR_GAS.get()),"Equipped nitor illuminates while absent from main inventory");
        slots.setStackInSlot(0,clay);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Tinkerer.AWAKENED_PICK.get()));p.getInventory().setItem(1,new ItemStack(Tinkerer.AWAKENED_SHOVEL.get()));
        test.assertTrue(ProtoclayItem.select(p,Blocks.DIRT.defaultBlockState())&&p.getMainHandItem().is(Tinkerer.AWAKENED_SHOVEL.get()),"Equipped protoclay switches to the specialized tool");
        test.assertTrue(p.getInventory().countItem(Tinkerer.AWAKENED_PICK.get())==1,"Previous tool preserved exactly once");
        test.getLevel().removeBlock(light,false);p.discard();test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererReport0922BedrockClickPriority(GameTestHelper test){
        var world=test.getLevel();var p=player(test);var floor=new BlockPos(p.getBlockX(),world.getMinBuildHeight(),p.getBlockZ());var original=world.getBlockState(floor);boolean enabled=TinkererConfig.BEDROCK_ENABLED.get();
        try{
            TinkererConfig.BEDROCK_ENABLED.set(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Tinkerer.AWAKENED_PICK.get()));p.moveTo(floor.above().getCenter());
            world.setBlockAndUpdate(floor,Blocks.BEDROCK.defaultBlockState());var hit=new BlockHitResult(floor.getCenter(),Direction.UP,floor,false);var context=new UseOnContext(p,InteractionHand.MAIN_HAND,hit);
            p.setShiftKeyDown(false);Tinkerer.AWAKENED_PICK.get().useOn(context);test.assertTrue(world.getBlockState(floor).is(Blocks.BEDROCK),"Ordinary right click leaves bedrock unchanged");
            int mode=AwakenedModes.mode(p.getMainHandItem());
            p.setShiftKeyDown(true);Tinkerer.AWAKENED_PICK.get().useOn(context);test.assertTrue(world.getBlockState(floor).is(Blocks.BEDROCK)&&AwakenedModes.mode(p.getMainHandItem())!=mode,"Sneak right-click only changes mining mode");
            p.setShiftKeyDown(false);
            for(boolean creative:new boolean[]{false,true}){
                p.getAbilities().instabuild=creative;world.setBlockAndUpdate(floor,Blocks.BEDROCK.defaultBlockState());
                var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(p,floor,Direction.UP,net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
                BedrockTravel.click(event);test.assertTrue(event.isCanceled()&&world.getBlockState(floor).is(Tinkerer.BEDROCK_PORTAL.get()),"Left click creates a portal before normal mining in survival and creative");
            }
            test.succeed();
        }finally{TinkererConfig.BEDROCK_ENABLED.set(enabled);world.setBlockAndUpdate(floor,original);p.discard();}
    }
    @GameTest(template="wand_test_empty") public static void tinkererReport0922FireItemsPlaceTheirOwnElement(GameTestHelper test){
        var p=player(test);var floor=test.absolutePos(new BlockPos(2,1,2));test.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        for(var entry:Tinkerer.FIRES.entrySet()){
            var at=floor.above();test.getLevel().removeBlock(at,false);var stack=new ItemStack(entry.getValue().get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
            stack.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(floor.getCenter().add(0,.5,0),Direction.UP,floor,false)));
            test.assertTrue(test.getLevel().getBlockState(at).is(entry.getValue().get()),"BlockItem placement preserves elemental fire: "+entry.getKey());
        }p.discard();test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererReport0922BedrockAreaProtectsShellAndNeverDropsSilkBedrock(GameTestHelper test){
        var world=test.getLevel().getServer().getLevel(BedrockTravel.DIMENSION);test.assertTrue(world!=null,"Mining dimension loaded");
        var pos=new BlockPos(test.absolutePos(new BlockPos(2,2,2)).getX(),100,test.absolutePos(new BlockPos(2,2,2)).getZ());world.getChunkAt(pos);
        var p=new net.minecraftforge.common.util.FakePlayer(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TTBedrock0922"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.moveTo(pos.above().getCenter());
        var tool=new ItemStack(Tinkerer.AWAKENED_PICK.get());tool.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,1);tool.getOrCreateTag().putInt("ToolMode",1);p.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var saved=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        try{
            for(var at:AwakenedArea.select("pick",1,pos,Direction.UP)){saved.put(at,world.getBlockState(at));world.setBlock(at,Blocks.BEDROCK.defaultBlockState(),2);}
            saved.put(pos,world.getBlockState(pos));world.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),2);
            var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(p,pos,Direction.UP);BedrockTravel.click(event);
            test.assertTrue(world.isEmptyBlock(pos)&&AwakenedArea.select("pick",1,pos,Direction.UP).stream().allMatch(world::isEmptyBlock),"Left click instantly removes the entire bedrock area");
            test.assertTrue(world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(5)).stream().noneMatch(e->e.getItem().is(Blocks.BEDROCK.asItem())),"Silk touch never yields bedrock");
            for(int y:new int[]{0,1,254,255}){var shell=new BlockPos(pos.getX(),y,pos.getZ());saved.put(shell,world.getBlockState(shell));world.setBlock(shell,Blocks.BEDROCK.defaultBlockState(),2);test.assertTrue(!BedrockTravel.breakBedrock(p,shell)&&world.getBlockState(shell).is(Blocks.BEDROCK),"Area helper protects shell Y="+y);}
        }finally{saved.forEach((at,state)->world.setBlock(at,state,2));p.discard();}test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererReport0922OriginalAuthorArmorDiscounts(GameTestHelper test){
        var p=player(test);
        for(var item:java.util.List.of(Tinkerer.ICHOR_HELM.get(),Tinkerer.ICHOR_CHEST.get(),Tinkerer.ICHOR_LEGS.get(),Tinkerer.ICHOR_BOOTS.get(),Tinkerer.AWAKENED_HELM.get(),Tinkerer.AWAKENED_CHEST.get(),Tinkerer.AWAKENED_LEGS.get(),Tinkerer.AWAKENED_BOOTS.get())){
            var stack=new ItemStack(item);var armor=(IchorArmorItem)item;
            var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();item.appendHoverText(stack,test.getLevel(),lines,net.minecraft.world.item.TooltipFlag.Default.NORMAL);
            test.assertTrue(lines.stream().noneMatch(line->line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text&&text.getKey().equals("ttmodern.vis_discount")),"No duplicate manual vis tooltip");
            double value=stack.getAttributeModifiers(armor.getEquipmentSlot()).get(tc4revival.content.TCAttributes.VIS_COST.get()).stream().mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount).sum();
            test.assertTrue(Math.abs(value+armor.getVisDiscount(stack,p,null)/100d)<.00001,"Real synchronized attribute agrees with the discount API");
            test.assertTrue(armor.getVisDiscount(stack,p,null)==(armor instanceof AwakenedArmorItem?7:armor.getType()==ArmorItem.Type.BOOTS?3:4),"Normal discounts stay 3/4; all awakened pieces provide 7 percent");
        }
        p.discard();test.succeed();
    }
}
