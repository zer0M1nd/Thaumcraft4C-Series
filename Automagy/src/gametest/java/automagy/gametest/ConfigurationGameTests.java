package automagy.gametest;

import automagy.AutomagyConfig;
import automagy.content.*;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

@GameTestHolder("automagy")
@PrefixGameTestTemplate(false)
public final class ConfigurationGameTests {
    private static final BlockPos POS=new BlockPos(2,2,2);
    private static ItemStack water(){return PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.WATER);}
    private static FakePlayer player(GameTestHelper h,boolean creative){
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"ConfigTest"));
        player.getAbilities().instabuild=creative;
        player.getAbilities().mayBuild=true;
        player.setPos(Vec3.atCenterOf(h.absolutePos(POS)));
        return player;
    }
    private static ThirstyTankBlockEntity tank(GameTestHelper h){
        h.setBlock(POS,Content.THIRSTY_TANK.get());
        return (ThirstyTankBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
    }
    private static void use(GameTestHelper h,FakePlayer player,InteractionHand hand){
        var pos=h.absolutePos(POS);
        var state=h.getLevel().getBlockState(pos);
        h.assertTrue(state.use(h.getLevel(),player,hand,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)).consumesAction(),
                "Tank block did not consume the interaction");
    }
    private static long count(FakePlayer player,Item item){
        long count=0;
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);if(stack.is(item))count+=stack.getCount();
        }
        return count;
    }

    @GameTest(template="empty",templateNamespace="automagy",batch="configuration",timeoutTicks=20)
    public static void zeroBottleAmountRequiresWaterAndExchangesInBothGameModes(GameTestHelper h){
        int old=AutomagyConfig.WATER_BOTTLE.get();
        try{
            AutomagyConfig.WATER_BOTTLE.set(0);var tile=tank(h);
            for(boolean creative:new boolean[]{false,true})for(var hand:InteractionHand.values()){
                var player=player(h,creative);player.setItemInHand(hand,water());
                tile.tank.setFluid(FluidStack.EMPTY);use(h,player,hand);
                h.assertTrue(player.getItemInHand(hand).is(Items.POTION)&&tile.tank.isEmpty(),"Zero amount filled an empty tank or consumed a bottle");
                tile.tank.setFluid(new FluidStack(Fluids.LAVA,100));use(h,player,hand);
                h.assertTrue(player.getItemInHand(hand).is(Items.POTION)&&tile.tank.getFluidAmount()==100,"Zero amount accepted a non-water tank");
                tile.tank.setFluid(new FluidStack(Fluids.WATER,1));use(h,player,hand);
                h.assertTrue(player.getItemInHand(hand).is(Items.GLASS_BOTTLE)&&tile.tank.getFluidAmount()==1,"Zero amount did not exchange water bottle");
                use(h,player,hand);
                h.assertTrue(player.getItemInHand(hand).is(Items.POTION)&&PotionUtils.getPotion(player.getItemInHand(hand))==Potions.WATER
                        &&tile.tank.getFluidAmount()==1,"Zero amount did not fill glass bottle without draining");
            }
        }finally{AutomagyConfig.WATER_BOTTLE.set(old);}
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="automagy",batch="configuration",timeoutTicks=20)
    public static void positiveBottleAmountPreservesLegacyCreativeAndStackSemantics(GameTestHelper h){
        int old=AutomagyConfig.WATER_BOTTLE.get();
        try{
            AutomagyConfig.WATER_BOTTLE.set(250);var tile=tank(h);
            for(boolean creative:new boolean[]{false,true})for(var hand:InteractionHand.values()){
                var player=player(h,creative);tile.tank.setFluid(FluidStack.EMPTY);
                player.setItemInHand(hand,water());use(h,player,hand);
                h.assertTrue(tile.tank.getFluidAmount()==250,"Pouring did not fill exactly the configured amount");
                h.assertTrue(player.getItemInHand(hand).is(creative?Items.POTION:Items.GLASS_BOTTLE),"Creative positive pour must preserve its water bottle");
                player.setItemInHand(hand,new ItemStack(Items.GLASS_BOTTLE,3));use(h,player,hand);
                h.assertTrue(tile.tank.isEmpty()&&count(player,Items.GLASS_BOTTLE)==2&&count(player,Items.POTION)==1,
                        "Drawing must drain water and exchange one bottle even in creative");
            }
            var player=player(h,false);player.getInventory().setItem(8,new ItemStack(Items.GLASS_BOTTLE,10));
            player.setItemInHand(InteractionHand.MAIN_HAND,water());tile.tank.setFluid(FluidStack.EMPTY);use(h,player,InteractionHand.MAIN_HAND);
            h.assertTrue(count(player,Items.GLASS_BOTTLE)==11&&count(player,Items.POTION)==0,"Partial bottle stack did not preserve one-for-one exchange");
        }finally{AutomagyConfig.WATER_BOTTLE.set(old);}
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="automagy",batch="configuration",timeoutTicks=20)
    public static void bottleDisableCapabilityFallbackAndInsufficientSpaceAreSafe(GameTestHelper h){
        int old=AutomagyConfig.WATER_BOTTLE.get();
        try{
            var tile=tank(h);var player=player(h,false);
            AutomagyConfig.WATER_BOTTLE.set(-1);tile.tank.setFluid(new FluidStack(Fluids.WATER,1000));
            for(var stack:List.of(water(),new ItemStack(Items.GLASS_BOTTLE))){
                player.setItemInHand(InteractionHand.MAIN_HAND,stack.copy());use(h,player,InteractionHand.MAIN_HAND);
                h.assertTrue(ItemStack.matches(player.getMainHandItem(),stack)&&tile.tank.getFluidAmount()==1000,"Disabled bottles bypassed through a fluid capability");
            }
            int registered=FluidUtil.getFluidContained(water()).filter(f->f.getFluid().isSame(Fluids.WATER)).map(FluidStack::getAmount).orElse(0);
            h.assertTrue(TankBottles.amount(-2)==registered,"Forge fallback used a guessed bottle capacity");
            AutomagyConfig.WATER_BOTTLE.set(250);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));
            tile.tank.setFluid(new FluidStack(Fluids.WATER,249));use(h,player,InteractionHand.MAIN_HAND);
            h.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE)&&tile.tank.getFluidAmount()==249,"Insufficient water caused partial drain or bottle output");
            tile.tank.setFluid(new FluidStack(Fluids.WATER,15999));player.setItemInHand(InteractionHand.MAIN_HAND,water());use(h,player,InteractionHand.MAIN_HAND);
            h.assertTrue(player.getMainHandItem().is(Items.POTION)&&tile.tank.getFluidAmount()==15999,"Insufficient space caused partial fill or bottle consumption");
        }finally{AutomagyConfig.WATER_BOTTLE.set(old);}
        h.succeed();
    }

    @GameTest(template="empty",templateNamespace="automagy",batch="configuration",timeoutTicks=60)
    public static void advancedJarRollbackRestoresEveryOwnershipAndNodeField(GameTestHelper h){
        var player=player(h,false);var level=h.getLevel();var origin=h.absolutePos(POS);var center=origin.offset(1,0,1);
        var saved=new LinkedHashMap<BlockPos,CompoundTag>();
        for(int row=0;row<4;row++)for(int x=0;x<3;x++)for(int z=0;z<3;z++){
            var pos=origin.offset(x,2-row,z);
            if(row==2&&x==1&&z==1){
                level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),18);
                var node=(AuraNodeBlockEntity)level.getBlockEntity(pos);var air=TCAspects.catalog().require("aer");
                node.configure(new AspectAmounts().add(air,40),AuraNodeType.PURE,AuraNodeModifier.BRIGHT,"advanced-config-identity",false);node.take(air,9);
            }else if(row==0){
                level.setBlock(pos,TCArcane.SILVERWOOD_LOG.get().defaultBlockState(),18);
                h.assertTrue(WardedBlockEntity.protect(player,pos),"Could not prepare owned silverwood log");
            }else{
                level.setBlock(pos,TCArcane.WARDED_GLASS.get().defaultBlockState(),18);
                var glass=(OwnedDeviceBlockEntity)level.getBlockEntity(pos);glass.access().setOwner(player.getGameProfile().getName());
                glass.access().grant("OtherUser"+x+z,true);glass.changed();
            }
            saved.put(pos,level.getBlockEntity(pos).saveWithoutMetadata());
        }
        h.assertTrue(AdvancedNodeJarring.matches(level,origin,player),"Valid warded shell did not match");
        h.assertTrue(!AdvancedNodeJarring.matches(level,origin,player(h,false)),"Another owner could capture the shell");
        var payload=NodeJarCapture.payload(level,center);int[] charged={0};
        for(int stop=1;stop<=37;stop++){
            int fail=stop;int[] writes={0};
            h.assertTrue(!NodeJarCapture.convertValidated(level,origin,payload,()->{charged[0]++;return false;},(pos,state)->{
                boolean changed=level.setBlock(pos,state,18);return ++writes[0]!=fail&&changed;
            },()->AdvancedNodeJarring.matches(level,origin,player)),"Injected failure accepted at "+stop);
            h.assertTrue(AdvancedNodeJarring.matches(level,origin,player),"Rollback lost owned structure at "+stop);
            for(var entry:saved.entrySet())h.assertTrue(entry.getValue().equals(level.getBlockEntity(entry.getKey()).saveWithoutMetadata()),
                    "Rollback lost NBT at "+entry.getKey()+" / failure "+stop);
        }
        h.assertTrue(charged[0]==1,"Partial writes reached payment");
        h.assertTrue(NodeJarCapture.convertValidated(level,origin,payload,()->true,(pos,state)->level.setBlock(pos,state,18),
                ()->AdvancedNodeJarring.matches(level,origin,player)),"Final conversion failed");
        var jar=(NodeJarBlockEntity)level.getBlockEntity(center);
        h.assertTrue(jar.nodeId().equals("advanced-config-identity")&&jar.nodeModifier()==AuraNodeModifier.BRIGHT
                &&jar.nodeType()==AuraNodeType.PURE&&jar.aspects().amount(TCAspects.catalog().require("aer"))==31,"Successful jar changed node data");
        h.succeed();
    }
}
