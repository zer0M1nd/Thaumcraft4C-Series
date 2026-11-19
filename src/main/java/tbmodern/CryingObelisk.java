package tbmodern;

import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=ThaumicBases.ID)
public final class CryingObelisk extends Block {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,2);
    public CryingObelisk() { super(Properties.copy(Blocks.CRYING_OBSIDIAN));registerDefaultState(stateDefinition.any().setValue(PART,0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(PART); }
    private static boolean material(BlockState state) { return state.is(Blocks.CRYING_OBSIDIAN) || state.getBlock() instanceof CryingObelisk; }
    private static int part(BlockGetter level,BlockPos pos) {
        if(material(level.getBlockState(pos.above())) && !material(level.getBlockState(pos.above(2))) && !material(level.getBlockState(pos.below())))return 1;
        if(material(level.getBlockState(pos.below())) && !material(level.getBlockState(pos.below(2))) && !material(level.getBlockState(pos.above())))return 2;
        return 0;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(PART,part(context.getLevel(),context.getClickedPos())); }
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        int part=part(level,pos);
        return part==0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : state.setValue(PART,part);
    }
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state) { return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CRYING_OBSIDIAN); }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState previous,boolean moving) {
        if(!level.isClientSide)level.scheduleTick(pos,this,1);
    }
    @Override public void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random) { reconcile(level,pos); }
    private static boolean assembling;
    public static void reconcile(Level level,BlockPos pos) {
        if(level.isClientSide || assembling || !level.hasChunkAt(pos) || !material(level.getBlockState(pos)))return;
        assembling=true;
        try {
            int part=part(level,pos);
            if(part==0) {
                if(level.getBlockState(pos).getBlock() instanceof CryingObelisk)level.setBlock(pos,Blocks.CRYING_OBSIDIAN.defaultBlockState(),UPDATE_ALL);
                return;
            }
            BlockPos bottom=part==1?pos:pos.below();
            var block=Content.block("crying_obelisk");
            // Commit both halves before shape updates, so neither transient half falls apart.
            level.setBlock(bottom,block.defaultBlockState().setValue(PART,1),UPDATE_CLIENTS|UPDATE_KNOWN_SHAPE);
            level.setBlock(bottom.above(),block.defaultBlockState().setValue(PART,2),UPDATE_CLIENTS|UPDATE_KNOWN_SHAPE);
            level.updateNeighborsAt(bottom,block);level.updateNeighborsAt(bottom.above(),block);
        } finally { assembling=false; }
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void placed(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if(event.getLevel() instanceof Level level)reconcile(level,event.getPos());
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void neighbors(net.minecraftforge.event.level.BlockEvent.NeighborNotifyEvent event) {
        if(event.getLevel() instanceof Level level && !level.isClientSide) {
            reconcile(level,event.getPos());reconcile(level,event.getPos().above());reconcile(level,event.getPos().below());
        }
    }
    private record PendingMigration(net.minecraft.server.level.ServerLevel level,BlockPos pos) {}
    private static final java.util.Queue<PendingMigration> MIGRATIONS=new java.util.concurrent.ConcurrentLinkedQueue<>();
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void loaded(net.minecraftforge.event.level.ChunkEvent.Load event) {
        if(!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level))return;
        var chunk=event.getChunk();var sections=chunk.getSections();
        // The chunk's completion future is not resolved during this event. Only read the
        // supplied sections here; world reads or a synchronous execute() can deadlock loading.
        for(int index=0;index<sections.length;index++) {
            var section=sections[index];
            if(!section.maybeHas(state->state.getBlock() instanceof CryingObelisk))continue;
            int yBase=chunk.getSectionYFromSectionIndex(index)<<4;
            for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++)
                if(section.getBlockState(x,y,z).getBlock() instanceof CryingObelisk)
                    MIGRATIONS.add(new PendingMigration(level,new BlockPos(chunk.getPos().getMinBlockX()+x,yBase+y,chunk.getPos().getMinBlockZ()+z)));
        }
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void migrate(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END || !(event.level instanceof net.minecraft.server.level.ServerLevel level))return;
        for(int count=MIGRATIONS.size();count>0;count--) {
            var pending=MIGRATIONS.poll();if(pending==null)break;
            if(pending.level()!=level){MIGRATIONS.add(pending);continue;}
            if(level.getChunkSource().getChunkNow(pending.pos().getX()>>4,pending.pos().getZ()>>4)!=null)reconcile(level,pending.pos());
        }
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {MIGRATIONS.clear();}
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void interact(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var level=event.getLevel();var pos=event.getPos();
        if(!level.getBlockState(pos).is(Blocks.CRYING_OBSIDIAN) || part(level,pos)==0)return;
        if(!level.isClientSide) {
            reconcile(level,pos);
            level.getBlockState(pos).use(level,event.getEntity(),event.getHand(),event.getHitVec());
        }
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=ThaumicBases.ID)
    public static class LegacyMappings {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void missing(net.minecraftforge.registries.MissingMappingsEvent event) {
            for(var mapping:event.getMappings(net.minecraftforge.registries.ForgeRegistries.Keys.ITEMS,ThaumicBases.ID))
                if(mapping.getKey().getPath().equals("crying_obelisk"))mapping.remap(net.minecraft.world.item.Items.CRYING_OBSIDIAN);
        }
    }
    @Override public boolean isBed(BlockState state,BlockGetter level,BlockPos pos,Entity entity) { return part(level,pos)>0; }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(part(level,pos)==0)return InteractionResult.PASS;
        if(player instanceof ServerPlayer server) {
            server.setRespawnPosition(level.dimension(),pos,player.getYRot(),false,false);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tb.txt.spawnSet").withStyle(net.minecraft.ChatFormatting.AQUA,net.minecraft.ChatFormatting.ITALIC),false);
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.PORTAL_TRAVEL,net.minecraft.sounds.SoundSource.BLOCKS,1,2);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public Optional<Vec3> getRespawnPosition(BlockState state,EntityType<?> type,LevelReader level,BlockPos pos,float yaw,LivingEntity entity) {
        if(part(level,pos)==0)return Optional.empty();
        for(int dy=0;dy>-3;dy--)for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++) {
            BlockPos target=pos.offset(dx,dy,dz);
            if(!level.hasChunkAt(target) || !level.getWorldBorder().isWithinBounds(target))continue;
            Vec3 spawn=net.minecraft.world.entity.vehicle.DismountHelper.findSafeDismountLocation(type,level,target,true);
            if(spawn!=null)return Optional.of(spawn);
        }
        return Optional.empty();
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if(part(level,pos)==0)return;
        for(int i=0;i<10;i++)level.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,pos.getX()+.5+(random.nextDouble()*2-1),pos.getY()-random.nextDouble()*3,pos.getZ()+.5+(random.nextDouble()*2-1),0,random.nextDouble()*3,0);
    }
}
