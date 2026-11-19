package thaumicfutures.foci;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.*;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;

/** One successful placement per level tick. Queued jobs never outlive their held wand. */
@Mod.EventBusSubscriber(modid="thaumicfutures")
public final class BuilderPlacements {
    private record Key(UUID player,BlockPos pos) {}
    private record Job(UUID player,InteractionHand hand,ItemStack wand,BlockPos pos,BlockState before,Direction face,ItemStack material) {}
    private static final Map<ServerLevel,LinkedHashMap<Key,Job>> QUEUES=new WeakHashMap<>();
    private BuilderPlacements() {}
    public static void enqueue(ServerPlayer player,InteractionHand hand,ItemStack wand,BlockHitResult hit,List<BlockPos> positions,ItemStack material) {
        var level=player.serverLevel();var queue=QUEUES.computeIfAbsent(level,k -> new LinkedHashMap<>());
        for(var pos:positions) {
            if(queue.size()>=4096)break;
            if(level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos))queue.putIfAbsent(new Key(player.getUUID(),pos),new Job(player.getUUID(),hand,wand,pos.immutable(),level.getBlockState(pos),hit.getDirection(),material.copy()));
        }
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { if(event.getLevel() instanceof ServerLevel level)QUEUES.remove(level); }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !(event.level instanceof ServerLevel level))return;
        var queue=QUEUES.get(level);if(queue==null)return;
        int remaining=256;
        while(!queue.isEmpty() && remaining-->0) {
            var it=queue.entrySet().iterator();var job=it.next().getValue();it.remove();if(place(level,job))break;
        }
        if(queue.isEmpty())QUEUES.remove(level);
    }
    private static boolean place(ServerLevel level,Job job) {
        var player=level.getServer().getPlayerList().getPlayer(job.player);
        if(player==null || player.level()!=level || !player.isAlive() || player.isSpectator() || player.getItemInHand(job.hand)!=job.wand
                || player.distanceToSqr(Vec3.atCenterOf(job.pos))>96*96 || !level.hasChunkAt(job.pos) || !level.getWorldBorder().isWithinBounds(job.pos))return false;
        if(!(job.wand.getItem() instanceof CastingWandItem wand) || !(WandFocusItem.attached(job.wand).getItem() instanceof BuilderFocusItem focus))return false;
        var state=level.getBlockState(job.pos);var focusStack=WandFocusItem.attached(job.wand);
        if(!state.equals(job.before) || state.hasBlockEntity() || multiBlock(state.getBlock()) || state.getDestroySpeed(level,job.pos)<0 || !level.mayInteract(player,job.pos)
                || player.blockActionRestricted(level,job.pos,player.gameMode.getGameModeForPlayer()) || !(job.material.getItem() instanceof BlockItem item))return false;
        var hit=new BlockHitResult(Vec3.atCenterOf(job.pos),job.face,job.pos,false);
        if(net.minecraftforge.common.ForgeHooks.onRightClickBlock(player,job.hand,job.pos,hit).isCanceled())return false;
        int slot=-1;for(int i=0;i<36;i++)if(ItemStack.isSameItemSameTags(player.getInventory().getItem(i),job.material)) { slot=i;break; }
        if(!player.isCreative() && slot<0 || !wand.consumeForPlayer(job.wand,player,focus.costs(focusStack),false,false,false))return false;
        var context=new BlockPlaceContext(player,job.hand,job.material,hit) {
            @Override public BlockPos getClickedPos() { return job.pos; }
            @Override public boolean replacingClickedOnBlock() { return true; }
        };
        var replacement=item.getBlock().getStateForPlacement(context);
        // Multi-block and block-entity placement require their own item transactions; never create partial structures or copy inventories.
        if(replacement==null || replacement.equals(state) || replacement.hasBlockEntity() || multiBlock(item.getBlock()) || !replacement.canSurvive(level,job.pos)
                || !level.isUnobstructed(replacement,job.pos,net.minecraft.world.phys.shapes.CollisionContext.of(player)))return false;
        if(!state.isAir() && net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,job.pos)<0)return false;
        var drops=player.isCreative() || state.isAir()?List.<ItemStack>of():Block.getDrops(state,level,job.pos,null,player,new ItemStack(Items.DIAMOND_PICKAXE));
        var snapshot=BlockSnapshot.create(level.dimension(),level,job.pos);
        if(!level.setBlock(job.pos,replacement,2))return false;
        if(ForgeEventFactory.onBlockPlace(player,snapshot,job.face)) {
            level.restoringBlockSnapshots=true;
            try { snapshot.restore(true,false); } finally { level.restoringBlockSnapshots=false; }
            return false;
        }
        item.getBlock().setPlacedBy(level,job.pos,replacement,player,job.material);
        level.updateNeighborsAt(job.pos,replacement.getBlock());
        if(!player.isCreative()) {
            player.getInventory().getItem(slot).shrink(1);wand.consumeForPlayer(job.wand,player,focus.costs(focusStack),false,false,true);
            for(var drop:drops)if(!player.getInventory().add(drop))Block.popResource(level,job.pos,drop);
            player.getInventory().setChanged();
        }
        tc4revival.network.FocusEffectPacket.blockSparkle(level,job.pos,0xa0ffa0);
        var sound=replacement.getSoundType(level,job.pos,player);
        level.playSound(null,job.pos,sound.getPlaceSound(),net.minecraft.sounds.SoundSource.BLOCKS,.4f,1);
        return true;
    }
    private static boolean multiBlock(Block block) { return block instanceof DoublePlantBlock || block instanceof DoorBlock || block instanceof BedBlock; }
}
