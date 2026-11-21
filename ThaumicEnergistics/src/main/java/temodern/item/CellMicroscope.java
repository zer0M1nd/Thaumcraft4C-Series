package temodern.item;

import java.util.*;
import appeng.api.storage.*;
import appeng.api.stacks.AEItemKey;
import appeng.api.implementations.blockentities.IChestOrDrive;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.research.*;
import tc4revival.network.*;

/** Scan the physical cells being looked at, not their network or an arbitrary client-supplied item. */
public final class CellMicroscope extends Item {
    public CellMicroscope() { super(new Properties().stacksTo(1)); }
    @Override public int getUseDuration(ItemStack stack){return 50;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.NONE;}
    private record Target(Object identity,List<MEStorage> inventories,net.minecraft.world.phys.Vec3 origin){}
    private static Optional<Target> target(ServerPlayer player){
        var entity=ScanTargetSelector.pointedEntity(player).orElse(null);
        if(entity instanceof ItemEntity drop){
            var cell=StorageCells.getCellInventory(drop.getItem(),null);
            if(cell!=null)return Optional.of(new Target(drop.getUUID(),List.of(cell),drop.position().add(-.5,0,-.5)));
        }
        var hit=player.serverLevel().clip(new net.minecraft.world.level.ClipContext(player.getEyePosition(),player.getEyePosition().add(player.getLookAngle().scale(10)),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.ANY,player));
        var block=hit;
        if(block.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK && player.serverLevel().hasChunkAt(block.getBlockPos())
                && player.serverLevel().getBlockEntity(block.getBlockPos()) instanceof IChestOrDrive drive){
            var cells=new ArrayList<MEStorage>();
            for(int i=0;i<drive.getCellCount();i++){var cell=drive.getOriginalCellInventory(i);if(cell!=null)cells.add(cell);}
            return Optional.of(new Target(player.serverLevel().getBlockEntity(block.getBlockPos()),cells,net.minecraft.world.phys.Vec3.atLowerCornerOf(block.getBlockPos())));
        }
        return Optional.empty();
    }
    private record Session(Object target,ItemStack tool,long started){}
    private static final Map<ServerPlayer,Session> sessions=new WeakHashMap<>();
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server){
            var selected=target(server);if(selected.isEmpty())return InteractionResultHolder.pass(stack);
            sessions.put(server,new Session(selected.get().identity,stack,level.getGameTime()));
        }
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){
        if(!(entity instanceof ServerPlayer player))return;
        var session=sessions.get(player);var current=target(player);
        if(session==null || session.tool!=stack || !player.isAlive() || player.isSpectator() || current.isEmpty()
                || !session.target.equals(current.get().identity)){sessions.remove(player);player.stopUsingItem();return;}
        if(level.getGameTime()%5==0)TCNetwork.send(player,new ScanEffectPacket(level.dimension().location(),current.get().origin,15,true));
        if(level.getGameTime()-session.started<45)return;
        sessions.remove(player);
        var seen=new HashSet<AEItemKey>();
        for(var inventory:current.get().inventories)for(var entry:inventory.getAvailableStacks())
            if(entry.getKey() instanceof AEItemKey key && seen.add(key))scanStoredItem(player,key.toStack());
        player.stopUsingItem();
    }
    /** Uses the main mod's actual scan identity, prerequisite, reward and notification transaction. */
    public static void scanStoredItem(ServerPlayer player,ItemStack stack){
        if(stack.isEmpty())return;
        var observation=new ItemEntity(player.serverLevel(),player.getX(),player.getY(),player.getZ(),stack.copyWithCount(1));
        var target=ScanTargetResolver.entity(observation).orElse(null);if(target==null)return;
        var data=KnowledgeSavedData.get(player.getServer());data.ensure(player.getUUID(),player.getRandom()::nextInt);
        if(!target.canTrack(data.snapshot(player.getUUID()).orElseThrow(),PlayerKnowledge.ScanMode.THAUMOMETER))return;
        var result=data.update(player.getUUID(),player.getRandom()::nextInt,knowledge->target.completeWithClue(knowledge,
                tc4revival.aspect.TCAspects.catalog(),PlayerKnowledge.ScanMode.THAUMOMETER,
                tc4revival.config.TCConfig.SERVER.aspectPoolSoftCap.get(),ResearchCatalogs.rules(),player.getRandom()::nextInt));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new thaumcraft.api.research.ScanEvent(player,target,result.scan()));
        TCNetwork.send(player,ScanFeedbackPacket.from(result.scan()));
        result.clue().ifPresent(key->TCNetwork.send(player,new CluePacket(key)));
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining){if(entity instanceof ServerPlayer player)sessions.remove(player);}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){if(entity instanceof ServerPlayer player)sessions.remove(player);return stack;}
}
