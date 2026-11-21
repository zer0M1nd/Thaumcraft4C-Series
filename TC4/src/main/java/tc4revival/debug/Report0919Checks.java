package tc4revival.debug;

import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.util.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import tc4revival.aspect.*;
import tc4revival.content.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
public class Report0919Checks {
    private record Pending(ServerLevel level,List<BlockSnapshot> backups,BlockPos node,BlockPos chest,int start) {}
    private static Pending pending;
    private static boolean sawChestOpen;
    private static tc4revival.aspect.recipe.RecipeAspectData lastData;
    private static tc4revival.aspect.item.ItemBaseAspectSession lastSession;
    public static int verify(CommandSourceStack source) {
        if(pending!=null){source.sendFailure(Component.literal("0919_1 check already running"));return 0;}
        var level=source.getLevel();var node=new BlockPos(348,202,348);var chest=node.east(3);
        var positions=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(node.offset(-2,-1,-2),node.offset(5,2,2)))positions.add(p.immutable());
        var backups=positions.stream().map(p->BlockSnapshot.create(level.dimension(),level,p)).toList();
        boolean branch=tc4revival.config.TCConfig.SERVER.branchFeatures.get();
        try {
            tc4revival.config.TCConfig.SERVER.branchFeatures.set(true);
            for(var p:positions) {
                boolean wall=p.getY()==node.getY()-1||p.getX()==node.getX()-2||p.getX()==node.getX()+5||Math.abs(p.getZ()-node.getZ())==2;
                level.setBlock(p,wall?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
            }
            var amounts=new AspectAmounts().add(TCAspects.catalog().require("aqua"),37);
            level.setBlock(node,Blocks.WATER.defaultBlockState(),3);
            require(AuraNodeGeneration.place(level,node,new AuraNodeGeneration.Result(amounts,AuraNodeType.NORMAL,null)),"Underwater generation rejected");
            var block=level.getBlockState(node);var tile=(AuraNodeBlockEntity)level.getBlockEntity(node);
            require(block.getValue(AuraNodeBlock.WATERLOGGED)&&block.getFluidState().isSource(),"Missing contained water source");
            var bucket=TCArcane.AURA_NODE.get().pickupBlock(level,node,block);
            require(bucket.is(Items.WATER_BUCKET)&&level.getBlockEntity(node)==tile&&!level.getBlockState(node).getValue(AuraNodeBlock.WATERLOGGED),"Bucket destroyed or replaced node");
            require(TCArcane.AURA_NODE.get().placeLiquid(level,node,level.getBlockState(node),Fluids.WATER.getSource(false)),"Cannot waterlog existing node");
            require(level.getBlockEntity(node)==tile&&tile.aspects().amount(TCAspects.catalog().require("aqua"))==37,"Waterlogging lost node NBT");
            TCArcane.AURA_NODE.get().pickupBlock(level,node,level.getBlockState(node));
            // Real server fluid ticks, not an artificial call to a block replacement predicate.
            level.setBlock(node.above(),Blocks.WATER.defaultBlockState(),3);
            level.setBlock(chest,TCArcane.HUNGRY_CHEST.get().defaultBlockState(),3);
            var hungry=(HungryChestBlockEntity)level.getBlockEntity(chest);
            var rest=GolemItemTransfer.insert(new net.minecraftforge.items.wrapper.InvWrapper(hungry),new ItemStack(Items.IRON_INGOT,4),true);
            require(rest.isEmpty()&&hungry.getItem(0).getCount()==4,"Automation insertion failed");
            labels(level);
            var balanced=tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("thaumcraft:balanced_shard")))).orElseThrow();
            require(balanced.size()==8,"Balanced shard still truncated: "+balanced.view());
            var session=tc4revival.aspect.item.ItemAspects.baseSession();
            require(session==tc4revival.aspect.item.ItemAspects.baseSession(),"Item queries rebuild their session");
            var data=tc4revival.aspect.recipe.RecipeAspects.data();
            if(lastData!=null&&lastData!=data)require(session!=lastSession,"Reload retained stale calculation session");
            lastData=data;lastSession=session;
            var pearl=new ItemStack(Items.ENDER_PEARL);long start=System.nanoTime();
            for(int i=0;i<1000;i++)tc4revival.aspect.item.ItemStackAspects.resolve(pearl).orElseThrow();
            tc4revival.TC4Revival.LOGGER.info("0919_1 aspect query sample: 1000 repeated pearl resolutions in {} ms",(System.nanoTime()-start)/1_000_000d);
            var jar=new EssentiaJarBlockEntity(BlockPos.ZERO,TCArcane.ESSENTIA_JAR.get().defaultBlockState());
            require(jar.renderExtendedTube(),"Jar tube extension missing");
            var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try { new tc4revival.network.InfusionRiskReply(node,null,true).encode(buf);var read=tc4revival.network.InfusionRiskReply.decode(buf);require(read.repeat()&&read.risk()==null,"Repeat hint packet lost"); }finally{buf.release();}
            sawChestOpen=false;pending=new Pending(level,backups,node,chest,level.getServer().getTickCount());
            source.sendSuccess(()->Component.literal("0919_1 synchronous checks passed; observing real fluid and chest ticks for 100 ticks."),false);return 1;
        }catch(RuntimeException e) {
            if(level.getBlockEntity(chest) instanceof HungryChestBlockEntity c)c.clearContent();
            backups.forEach(b->b.restore(true,false));source.sendFailure(Component.literal("0919_1 FAILED: "+e));tc4revival.TC4Revival.LOGGER.error("0919_1 FAILED",e);return 0;
        }finally { tc4revival.config.TCConfig.SERVER.branchFeatures.set(branch); }
    }
    private static void labels(ServerLevel level) {
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"LabelAudit"));
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.JAR_LABEL.get(),4));int slot=player.getInventory().selected;
        require(JarLabelItem.mark(player,InteractionHand.MAIN_HAND,slot,"aqua",false),"Single label rejected");
        require(player.getMainHandItem().getCount()==3,"Single action changed whole stack");
        int marked=0,total=0;for(var s:player.getInventory().items)if(s.getItem() instanceof JarLabelItem item){total+=s.getCount();if(item.storedAspects(s).contains(TCAspects.catalog().require("aqua")))marked+=s.getCount();}
        require(marked==1&&total==4,"Single marking duplicates or loses labels");
        require(JarLabelItem.mark(player,InteractionHand.MAIN_HAND,slot,"ignis",true)&&player.getMainHandItem().getCount()==3,"Bulk labeling failed");
        require(!JarLabelItem.mark(player,InteractionHand.MAIN_HAND,(slot+1)%9,"aer",true),"Changed selected slot accepted");
        require(!JarLabelItem.mark(player,InteractionHand.MAIN_HAND,slot,"missing:aspect",true),"Unknown aspect accepted");
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        require(!JarLabelItem.mark(player,InteractionHand.MAIN_HAND,slot,"alienis",true),"Undiscovered aspect accepted");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        require(!JarLabelItem.mark(player,InteractionHand.MAIN_HAND,slot,"aqua",true),"Non-label item modified");
        player.discard();
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||pending==null)return;
        var p=pending;int elapsed=event.getServer().getTickCount()-p.start();
        try {
            if(p.level().getBlockEntity(p.chest()) instanceof HungryChestBlockEntity c&&c.lid>0)sawChestOpen=true;
            if(elapsed==4)require(sawChestOpen,"Chest did not animate automation insertion");
            if(elapsed<100)return;
            require(p.level().getBlockEntity(p.node()) instanceof AuraNodeBlockEntity n&&n.aspects().amount(TCAspects.catalog().require("aqua"))==37,"Flowing water destroyed/cleared node");
            require(p.level().getBlockEntity(p.chest()) instanceof HungryChestBlockEntity c&&c.lid==0&&c.getItem(0).getCount()==4,"Chest did not close or lost items");
            tc4revival.TC4Revival.LOGGER.info("0919_1 PASSED: 100 real server ticks; water flow/node persistence, bucket waterlogging, chest automation animation, label transactions/security, eight shard aspects, tube extension and repeat hint packet.");
            restore();
        }catch(RuntimeException e){tc4revival.TC4Revival.LOGGER.error("0919_1 FAILED",e);restore();}
    }
    private static void restore(){var p=pending;pending=null;if(p!=null){if(p.level().getBlockEntity(p.chest()) instanceof HungryChestBlockEntity c)c.clearContent();p.backups().forEach(b->b.restore(true,false));}}
    @SubscribeEvent public static void stopping(net.minecraftforge.event.server.ServerStoppingEvent event){restore();lastData=null;lastSession=null;}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
