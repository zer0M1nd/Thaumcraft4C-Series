package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class PrimalCrusherServerTests {
    @GameTest(template="wand_test_empty")
    public static void primalCrusherLegacyMiningLoot(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3)); var player=player(test,pos);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var cinnabar=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:cinnabar_ore"));
        var ores=java.util.List.of(Blocks.IRON_ORE,Blocks.GOLD_ORE,cinnabar);
        var clusters=java.util.List.of(ClusterType.IRON,ClusterType.GOLD,ClusterType.CINNABAR);
        for (var item:java.util.List.of(TCItems.PRIMAL_CRUSHER.get(),TCItems.ELEMENTAL_PICKAXE.get())) {
            var tool=new ItemStack(item);
            // Above-one probability deterministically exercises the real registered loot modifier.
            tool.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE,20);
            for (int index=0;index<ores.size();index++) {
                var state=ores.get(index).defaultBlockState();
                var drops=net.minecraft.world.level.block.Block.getDrops(state,level,pos,null,player,tool);
                test.assertTrue(drops.size()==1 && drops.get(0).is(TCItems.cluster(clusters.get(index))) && drops.get(0).getCount()>=1,
                        "Both TC tools convert old ore through registered global loot modifier: "+state);
            }
            tool.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,1);
            var silk=net.minecraft.world.level.block.Block.getDrops(Blocks.IRON_ORE.defaultBlockState(),level,pos,null,player,tool);
            test.assertTrue(silk.size()==1 && silk.get(0).is(Items.IRON_ORE),
                    "Silk Touch preserves the intact ore");
            var fresh=new ItemStack(item); boolean ore=false,cluster=false;
            for (int sample=0;sample<128;sample++) {
                var drops=net.minecraft.world.level.block.Block.getDrops(Blocks.IRON_ORE.defaultBlockState(),level,pos,null,player,fresh);
                test.assertTrue(drops.size()==1 && drops.get(0).getCount()==1,"Old iron ore produces exactly one result");
                ore|=drops.get(0).is(Items.RAW_IRON); cluster|=drops.get(0).is(TCItems.cluster(ClusterType.IRON));
                test.assertTrue(drops.get(0).is(Items.RAW_IRON) || drops.get(0).is(TCItems.cluster(ClusterType.IRON)),"Unconverted ore yields raw iron");
            }
            test.assertTrue(ore && cluster,"Ordinary fortune-zero mining reaches both legacy outcomes");
        }
        var ordinary=net.minecraft.world.level.block.Block.getDrops(Blocks.IRON_ORE.defaultBlockState(),level,pos,null,player,new ItemStack(Items.DIAMOND_PICKAXE));
        test.assertTrue(ordinary.size()==1 && ordinary.get(0).is(Items.RAW_IRON),"Ordinary Minecraft tools retain modern drops");
        var modern=net.minecraft.world.level.block.Block.getDrops(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),level,pos,null,player,player.getMainHandItem());
        test.assertTrue(modern.size()==1 && modern.get(0).is(Items.RAW_IRON),"New-version ore is not silently added to old-content conversion");
        pass(test,"legacy-native-clusters-registered-loot-silk-modern-boundaries");
    }
    @GameTest(template="wand_test_empty")
    public static void primalCrusherNativeClusterAreaHarvest(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3)); var player=player(test,pos);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var previous=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for (var target:ElementalShovelLayout.plane(pos,Direction.UP,0,0)) {
            previous.put(target,level.getBlockState(target)); level.setBlock(target,Blocks.IRON_ORE.defaultBlockState(),3);
        }
        try {
            var tool=player.getMainHandItem(); tool.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE,20);
            TCItems.PRIMAL_CRUSHER.get().onBlockStartBreak(tool,pos,player); level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            TCItems.PRIMAL_CRUSHER.get().mineBlock(tool,level,Blocks.IRON_ORE.defaultBlockState(),pos,player);
            var following=level.getEntitiesOfClass(FollowingItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(4));
            // Fortune XX guarantees conversion, but modern iron loot retains its fortune count.
            test.assertTrue(following.size()==8
                            && following.stream().allMatch(drop->drop.getItem().is(TCItems.cluster(ClusterType.IRON))
                                    && drop.getItem().getCount()>=1 && drop.getItem().getCount()<=21),
                    "Actual eight-neighbor harvest carries converted clusters into following items: following="+following.stream().map(e->e.getItem().toString()).toList()
                            +", remaining="+previous.keySet().stream().filter(p->!p.equals(pos)&&!level.isEmptyBlock(p)).map(p->p+"="+level.getBlockState(p)).toList()
                            +", ordinary="+level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(4)).stream().map(e->e.getType()+":"+e.getItem()).toList());
            following.forEach(net.minecraft.world.entity.Entity::discard);
        } finally { previous.forEach((target,state)->level.setBlock(target,state,3)); }
        pass(test,"native-cluster-area-harvest");
    }
    private static FakePlayer player(GameTestHelper test,BlockPos pos) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"CrusherTest"));
        player.setPos(pos.getX()+.5,pos.getY()+3,pos.getZ()+.5); player.setXRot(90);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.PRIMAL_CRUSHER.get())); return player;
    }
    @GameTest(template="wand_test_empty")
    public static void primalCrusherPlaneProtectionAndDurability(GameTestHelper test) {
        var level=test.getLevel(); var center=test.absolutePos(new BlockPos(3,4,3)); var player=player(test,center);
        var tool=player.getMainHandItem(); var crusher=TCItems.PRIMAL_CRUSHER.get();
        for (var pos:ElementalShovelLayout.plane(center,Direction.UP,0,0)) level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
        level.setBlock(center.east(),Blocks.BEDROCK.defaultBlockState(),3); level.setBlock(center.west(),Blocks.OAK_LOG.defaultBlockState(),3);
        crusher.onBlockStartBreak(tool,center,player); level.setBlock(center,Blocks.AIR.defaultBlockState(),3);
        var protectedPos=center.north();
        java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> reject=event->{if(event.getLevel()==level && event.getPos().equals(protectedPos)) event.setCanceled(true);};
        MinecraftForge.EVENT_BUS.addListener(reject);
        try { crusher.mineBlock(tool,level,Blocks.STONE.defaultBlockState(),center,player); }
        finally { MinecraftForge.EVENT_BUS.unregister(reject); }
        test.assertTrue(level.getBlockState(center.east()).is(Blocks.BEDROCK) && level.getBlockState(center.west()).is(Blocks.OAK_LOG) && level.getBlockState(protectedPos).is(Blocks.STONE),"Unbreakable, ineffective and protected blocks remain intact");
        test.assertTrue(tool.getDamageValue()==6,"Eligible neighbors cost durability before protected harvest rejection; center has no extra cost");
        var drops=level.getEntitiesOfClass(FollowingItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(4));
        test.assertTrue(drops.stream().mapToInt(item->item.getItem().getCount()).sum()==5 && drops.stream().allMatch(FollowingItemEntity::hasFollowingTarget),"Five successful stone drops follow player without duplication");
        drops.forEach(net.minecraft.world.entity.Entity::discard);
        player.setShiftKeyDown(true); crusher.mineBlock(tool,level,Blocks.STONE.defaultBlockState(),center,player);
        test.assertTrue(tool.getDamageValue()==7 && level.getBlockState(protectedPos).is(Blocks.STONE),"Sneaking uses one-block tool wear without area harvesting");
        tool.setDamageValue(20); player.tickCount=20; crusher.inventoryTick(tool,level,player,8,false);
        test.assertTrue(tool.getDamageValue()==19 && crusher.warp(tool,player)==2,"Unselected inventory tool self-repairs and supplies two warp");
        pass(test,"plane-protection-durability-repair");
    }
    @GameTest(template="wand_test_empty")
    public static void primalCrusherRecipeAndHarvestRules(GameTestHelper test) {
        var level=test.getLevel(); var crusher=TCItems.PRIMAL_CRUSHER.get(); var tool=new ItemStack(crusher);
        test.assertTrue(tool.getMaxDamage()==500 && crusher.isValidRepairItem(tool,new ItemStack(TCItems.PRIMAL_CHARM.get())),"Durability and primal charm repair material match old tool");
        test.assertTrue(crusher.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()) && crusher.isCorrectToolForDrops(Blocks.GLASS.defaultBlockState()) && !crusher.isCorrectToolForDrops(Blocks.OAK_LOG.defaultBlockState()) && !crusher.isCorrectToolForDrops(Blocks.WHEAT.defaultBlockState()),"Broad harvest rule excludes wood and plants but allows glass and obsidian");
        test.assertTrue(tool.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()) && tool.isCorrectToolForDrops(Blocks.GLASS.defaultBlockState()) && !tool.isCorrectToolForDrops(Blocks.OAK_LOG.defaultBlockState()),"Forge stack-aware harvest entry uses the same broad rule");
        test.assertTrue(crusher.getDestroySpeed(tool,Blocks.DIRT.defaultBlockState())==8 && crusher.getDestroySpeed(tool,Blocks.STONE.defaultBlockState())==8 && crusher.getDestroySpeed(tool,Blocks.GLASS.defaultBlockState())==1,"Harvest eligibility and fast mining remain distinct");
        var ingredients=new java.util.ArrayList<ItemStack>();
        for (String path:java.util.List.of("primal_charm","void_pickaxe","void_shovel","primal_charm","elemental_pickaxe","elemental_shovel")) {
            var ingredient=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:"+path)));
            if (ingredient.isDamageableItem()) ingredient.setDamageValue(40); ingredients.add(ingredient);
        }
        var center=new ItemStack(TCItems.PRIMORDIAL_PEARL.get());
        var input=new InfusionRecipe.Input(center,ingredients,key->key.equals("PRIMALCRUSHER"));
        var recipe=InfusionRecipe.find(level,input).orElseThrow();
        test.assertTrue(recipe.getId().getPath().endsWith("primal_crusher") && recipe.assemble(input,level.registryAccess()).is(crusher),"Worn component tools are accepted by the actual infusion recipe");
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,ingredients,key->false),level),"Research is required");
        pass(test,"recipe-and-harvest-rules");
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Primal crusher test passed: {}",name); test.succeed(); }
}
