package tc4revival.content;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class PortableLootServerTests {
    @GameTest(template="wand_test_empty")
    public static void portableTablesPreserveOnlyPortableData(GameTestHelper test) {
        var pos=test.absolutePos(new BlockPos(3,3,3));var air=TCAspects.catalog().require("aer");
        for(boolean voidJar:new boolean[]{false,true}) for(int amount:new int[]{0,1,64}) for(boolean labelled:new boolean[]{false,true}) {
            var state=(voidJar?TCArcane.VOID_JAR.get():TCArcane.ESSENTIA_JAR.get()).defaultBlockState();
            var jar=new EssentiaJarBlockEntity(pos,state);jar.addToContainer(air,amount);if(labelled)jar.label(air,Direction.SOUTH);
            var item=voidJar?(amount>0||labelled?TCItems.FILLED_VOID_JAR.get():TCItems.VOID_JAR.get())
                    :(amount>0||labelled?TCItems.FILLED_ESSENTIA_JAR.get():TCItems.ESSENTIA_JAR.get());
            var expected=new ItemStack(item);if(amount>0)AspectNbt.write(expected.getOrCreateTag(),new AspectAmounts().add(air,amount));
            if(labelled)expected.getOrCreateTag().putString("AspectFilter","aer");
            check(test,state,jar,expected);check(test,state,null,ItemStack.EMPTY);
        }
        var nodeState=TCArcane.NODE_JAR.get().defaultBlockState();
        for(var type:AuraNodeType.values()) for(var modifier:new AuraNodeModifier[]{null,AuraNodeModifier.BRIGHT,AuraNodeModifier.PALE,AuraNodeModifier.FADING}) {
            var node=new NodeJarBlockEntity(pos,nodeState);
            var payload=new NodeJarData(new AspectAmounts().add(air,17),type,modifier,"portable-node");node.configure(payload);
            var expected=new ItemStack(TCItems.NODE_JAR.get());expected.setTag(payload.write());check(test,nodeState,node,expected);
            node.drop=false;check(test,nodeState,node,ItemStack.EMPTY);
        }
        check(test,nodeState,null,ItemStack.EMPTY);

        for(boolean essentia:new boolean[]{false,true}) for(boolean linked:new boolean[]{false,true}) {
            var state=(essentia?TCArcane.ESSENTIA_MIRROR.get():TCArcane.ITEM_MIRROR.get()).defaultBlockState();
            var target=GlobalPos.of(Level.NETHER,new BlockPos(-31,79,2048));
            var expected=essentia?EssentiaMirrorItem.linkedStack(target,test.getLevel().dimension().location().toString())
                    :ItemMirrorItem.linkedStack(target,test.getLevel().dimension().location().toString());
            var data=expected.getTag().copy();data.putBoolean("linked",linked);data.putInt("instability",75);
            var cargo=new net.minecraft.nbt.ListTag();cargo.add(new ItemStack(Items.DIAMOND,64).save(new net.minecraft.nbt.CompoundTag()));data.put("Items",cargo);
            BlockEntity mirror=essentia?new EssentiaMirrorBlockEntity(pos,state):new ItemMirrorBlockEntity(pos,state);mirror.load(data);
            var plain=new ItemStack(essentia?TCItems.ESSENTIA_MIRROR.get():TCItems.ITEM_MIRROR.get());
            check(test,state,mirror,linked?expected:plain);check(test,state,null,plain);
        }
        for(int color=0;color<16;color++)check(test,TCArcane.candle(color).defaultBlockState(),null,new ItemStack(TCItems.candle(color)));
        check(test,TCArcane.WARDED_GLASS.get().defaultBlockState(),null,new ItemStack(TCItems.WARDED_GLASS.get()));
        for(var state:TCArcane.ARCANE_PRESSURE_PLATE.get().getStateDefinition().getPossibleStates())
            check(test,state,null,new ItemStack(TCItems.ARCANE_PRESSURE_PLATE.get()));
        tc4revival.TC4Revival.LOGGER.info("portableTablesPreserveOnlyPortableData PASS: jars, all node types/modifiers, all banner colors, remote links, missing entities, candles and warded blocks");
        test.succeed();
    }
    private static void check(GameTestHelper test,BlockState state,BlockEntity entity,ItemStack expected) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var before=entity==null?null:entity.saveWithoutMetadata();
        var builder=new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE,state)
                .withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos)).withParameter(LootContextParams.TOOL,ItemStack.EMPTY);
        if(entity!=null)builder.withParameter(LootContextParams.BLOCK_ENTITY,entity);
        var direct=level.getServer().getLootData().getLootTable(state.getBlock().getLootTable()).getRandomItems(builder.create(LootContextParamSets.BLOCK));
        var actual=Block.getDrops(state,level,pos,entity);
        for(var drops:List.of(direct,actual)) {
            test.assertTrue(expected.isEmpty()?drops.isEmpty():drops.size()==1&&drops.get(0).getCount()==1&&ItemStack.isSameItemSameTags(drops.get(0),expected),"Portable loot matches payload and quantity: "+state+" expected="+expected+"/"+expected.getTag()+" actual="+drops);
            if(!drops.isEmpty())drops.get(0).getOrCreateTag().putString("probe","independent output");
        }
        test.assertTrue(entity==null||before.equals(entity.saveWithoutMetadata()),"Queries and output mutation preserve source inventory and metadata");
        test.assertTrue(!expected.hasTag()||!expected.getTag().contains("probe"),"Expected and returned item tags remain independent");
    }
}
