package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Persistent altar encounter state, and the obelisk's eldritch aura. */
public  class EldritchMonumentBlockEntity extends BlockEntity implements WandInteractable {
    private int eyes,spawnType,counter;
    private boolean open,spawner,spawnedClerics;
    private int pendingMazeWidth,pendingMazeHeight,pendingMazeBoss;
    private long pendingMazeSeed;
    public void deferNaturalMaze(int width,int height,long seed) {
        deferNaturalMaze(width,height,seed,Math.floorMod((int)seed,4));
    }
    public void deferNaturalMaze(int width,int height,long seed,int boss) {
        pendingMazeBoss=boss;
        pendingMazeWidth=width;pendingMazeHeight=height;pendingMazeSeed=seed;setChanged();
    }
    public EldritchMonumentBlockEntity(BlockPos pos,BlockState state) { super(TCEldritch.MONUMENT_ENTITY.get(),pos,state); }
    public int eyes() { return eyes; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean value) { open=value; sync(); }
    public void configureSpawner(boolean enabled,int type) { spawner=enabled; spawnType=type==1 ? 1 : 0; setChanged(); }
    public void insertEye(ItemStack stack) {
        if (!(level instanceof ServerLevel server) || eyes>=4 || !stack.is(TCEldritch.ELDRITCH_EYE.get())) return;
        if (eyes>=2) configureSpawner(true,1);
        eyes++; stack.shrink(1); sync();
        EldritchMazeRuntime.request(server,worldPosition);
        server.playSound(null,worldPosition,TCSounds.CRYSTAL.get(),SoundSource.BLOCKS,.2f,1);
    }
    @Override public net.minecraft.world.InteractionResult onWandUseFirst(ItemStack wand,net.minecraft.world.item.context.UseOnContext context) {
        if (!(getBlockState().getBlock() instanceof EldritchMonumentBlock block) || block.kind()!=EldritchMonumentBlock.Kind.ALTAR
                || eyes!=4 || open || !(wand.getItem() instanceof CastingWandItem casting)) return net.minecraft.world.InteractionResult.PASS;
        if (!(level instanceof ServerLevel server) || !(context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player))
            return net.minecraft.world.InteractionResult.PASS;
        if (!tc4revival.research.KnowledgeSavedData.get(server.getServer()).snapshot(player.getUUID())
                .map(knowledge->knowledge.researchMarkers().contains("OCULUS")).orElse(false)) return net.minecraft.world.InteractionResult.PASS;
        BlockPos nodePos=worldPosition.above();
        if (!(server.getBlockEntity(nodePos) instanceof AuraNodeBlockEntity node) || node.nodeType()!=AuraNodeType.DARK
                || !server.mayInteract(player,worldPosition) || !server.mayInteract(player,nodePos)
                || !player.mayUseItemAt(nodePos,Direction.UP,wand) || !EldritchMazeRuntime.request(server,worldPosition)) return net.minecraft.world.InteractionResult.PASS;
        var costs=new java.util.LinkedHashMap<tc4revival.aspect.Aspect,Integer>();
        for (var aspect:thaumcraft.api.aspects.AspectSelection.select(tc4revival.aspect.TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.STRUCTURE_COST)) costs.put(aspect,100);
        if (!thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:eldritch_obelisk",casting,wand,player,costs,true,true,false)) return net.minecraft.world.InteractionResult.PASS;
        BlockState old=server.getBlockState(nodePos); CompoundTag saved=node.saveWithFullMetadata();
        boolean committed=false;
        try {
            if (!server.setBlock(nodePos,TCEldritch.PORTAL.get().defaultBlockState(),2)
                    || !(server.getBlockEntity(nodePos) instanceof EldritchPortalBlockEntity)
                    || !TCEldritch.isDevice(server.getBlockState(nodePos.above()).getBlock())
                    || server.getBlockEntity(worldPosition)!=this
                    || !thaumcraft.api.wands.MagicOperationRules.payStructure("thaumcraft:eldritch_obelisk",casting,wand,player,costs,true,true,true)) return net.minecraft.world.InteractionResult.PASS;
            committed=true; setOpen(true);
            server.updateNeighborsAt(nodePos,TCEldritch.PORTAL.get());
            server.playSound(null,worldPosition,TCSounds.WAND.get(),SoundSource.BLOCKS,1,1);
            return net.minecraft.world.InteractionResult.SUCCESS;
        } finally {
            if (!committed) {
                server.setBlock(nodePos,old,2);
                if (server.getBlockEntity(nodePos) instanceof AuraNodeBlockEntity restored) { restored.load(saved); restored.sync(); }
                server.updateNeighborsAt(nodePos,old.getBlock());
            }
        }
    }
    @Override public java.util.Optional<ItemStack> onWandUse(net.minecraft.world.level.Level level,ItemStack wand,net.minecraft.world.entity.player.Player player) { return java.util.Optional.empty(); }
    @Override public void onWandUseTick(ItemStack wand,net.minecraft.world.entity.player.Player player,int remainingTicks) {}
    @Override public void onWandRelease(ItemStack wand,net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,int remainingTicks) {}
    public void tick() {
        if(pendingMazeWidth>0 && level instanceof ServerLevel server
                && server.hasChunksAt(worldPosition.offset(-16,0,-16),worldPosition.offset(16,0,16))) {
            // Worldgen workers cannot mutate server-owned maze SavedData or aura state.
            // Natural starts are spaced beyond the maximum maze overlap radius.
            AuraNodeGeneration.createRandom(server,worldPosition.above(),net.minecraft.util.RandomSource.create(pendingMazeSeed),false,true,false);
            EldritchMazeRuntime.schedule(server,worldPosition.getX()>>4,worldPosition.getZ()>>4,pendingMazeWidth,pendingMazeHeight,pendingMazeSeed,pendingMazeBoss);
            pendingMazeWidth=0;setChanged();
        }
        if (!(getBlockState().getBlock() instanceof EldritchMonumentBlock block)) return;
        if (block.kind()==EldritchMonumentBlock.Kind.OBELISK) {
            if (level instanceof ServerLevel server) {
                if(++counter%20!=0)return;
                var area=new AABB(worldPosition.getX()+.5,worldPosition.getY(),worldPosition.getZ()+.5,
                        worldPosition.getX()+.5,worldPosition.getY(),worldPosition.getZ()+.5).inflate(6);
                for (LivingEntity entity:server.getEntitiesOfClass(LivingEntity.class,area,e->e instanceof EldritchMob)) {
                    entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,40,0,true,true));
                    entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION,40,0,true,true));
                }
            } else if (level!=null && level.isClientSide) {
                var area=new AABB(worldPosition.getX()+.5,worldPosition.getY(),worldPosition.getZ()+.5,
                        worldPosition.getX()+.5,worldPosition.getY(),worldPosition.getZ()+.5).inflate(6);
                for (LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,area,e->e instanceof EldritchMob))
                    tc4revival.client.EldritchWisps.homing(level,worldPosition.getX()+.5,worldPosition.getY()+1+level.random.nextFloat()*3,worldPosition.getZ()+.5,entity);
            }
        } else if (block.kind()==EldritchMonumentBlock.Kind.ALTAR && level instanceof ServerLevel server
                && spawner && counter++>=80 && counter%40==0) {
            if (spawnType==1) spawnGuardian(server);
            else if (!spawnedClerics) spawnClerics(server);
            else spawnGuards(server);
        }
    }
    private boolean supported(ServerLevel server,BlockPos feet) {
        BlockPos below=feet.below(); return server.getBlockState(below).isFaceSturdy(server,below,Direction.UP);
    }
    private boolean clear(ServerLevel server,Mob mob) {
        return server.isUnobstructed(mob) && server.noCollision(mob) && !server.containsAnyLiquid(mob.getBoundingBox());
    }
    private void initialize(ServerLevel server,Mob mob,int radius) {
        mob.finalizeSpawn(server,server.getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.EVENT,null,null);
        mob.restrictTo(worldPosition,radius);
    }
    private void spawnClerics(ServerLevel server) {
        int successes=0;
        for (int dx:new int[]{-2,2}) for (int dz:new int[]{-2,2}) {
            BlockPos feet=worldPosition.offset(dx,0,dz); if (!supported(server,feet)) continue;
            CultistClericEntity cleric=TCEntities.CULTIST_CLERIC.get().create(server); if (cleric==null) continue;
            cleric.setPos(feet.getX()+.5,feet.getY(),feet.getZ()+.5); if (!clear(server,cleric)) continue;
            cleric.restrictTo(worldPosition,8); initialize(server,cleric,8);
            if (server.addFreshEntity(cleric)) { cleric.spawnAnim(); cleric.setRitualist(true); successes++; }
        }
        if (successes>2) { spawnedClerics=true; setChanged(); }
    }
    private BlockPos wanderingPosition(ServerLevel server) {
        var random=server.random;
        int x=(4+random.nextInt(7))*(random.nextInt(3)-1);
        int y=random.nextInt(4)*(random.nextInt(3)-1);
        int z=(4+random.nextInt(7))*(random.nextInt(3)-1);
        return worldPosition.offset(x,y,z);
    }
    private void spawnGuards(ServerLevel server) {
        var area=new AABB(worldPosition).inflate(24,16,24);
        if (server.getEntitiesOfClass(CultistClericEntity.class,area).isEmpty()) { spawner=false; setChanged(); return; }
        if (server.getEntitiesOfClass(CultistEntity.class,area).size()>=8) return;
        CultistKnightEntity knight=TCEntities.CULTIST_KNIGHT.get().create(server);
        BlockPos feet=wanderingPosition(server); if (knight==null || !supported(server,feet)) return;
        knight.setPos(feet.getX(),feet.getY(),feet.getZ()); if (!clear(server,knight)) return;
        initialize(server,knight,16); if (server.addFreshEntity(knight)) knight.spawnAnim();
    }
    private void spawnGuardian(ServerLevel server) {
        EldritchGuardianEntity guardian=TCEntities.ELDRITCH_GUARDIAN.get().create(server);
        BlockPos feet=wanderingPosition(server); if (guardian==null || !supported(server,feet)) return;
        guardian.setPos(feet.getX(),feet.getY(),feet.getZ());
        if (!guardian.checkSpawnRules(server,MobSpawnType.NATURAL) || !clear(server,guardian)) return;
        initialize(server,guardian,16); if (server.addFreshEntity(guardian)) guardian.spawnAnim();
    }
    public void sync() { setChanged(); if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putInt("Counter",counter); tag.putByte("eyes",(byte)eyes); tag.putBoolean("open",open);
        tag.putBoolean("spawner",spawner); tag.putBoolean("spawnedClerics",spawnedClerics); tag.putByte("spawntype",(byte)spawnType);
        tag.putInt("PendingMazeBoss",pendingMazeBoss);tag.putInt("PendingMazeWidth",pendingMazeWidth);tag.putInt("PendingMazeHeight",pendingMazeHeight);tag.putLong("PendingMazeSeed",pendingMazeSeed);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); counter=Math.max(0,tag.getInt("Counter")); eyes=Math.max(0,Math.min(4,tag.getByte("eyes"))); open=tag.getBoolean("open");
        spawner=tag.getBoolean("spawner"); spawnedClerics=tag.getBoolean("spawnedClerics"); spawnType=tag.getByte("spawntype")==1 ? 1 : 0;
        pendingMazeBoss=Math.floorMod(tag.getInt("PendingMazeBoss"),4);pendingMazeWidth=tag.getInt("PendingMazeWidth");pendingMazeHeight=tag.getInt("PendingMazeHeight");pendingMazeSeed=tag.getLong("PendingMazeSeed");
        if(pendingMazeWidth<11||pendingMazeWidth>21||pendingMazeHeight<11||pendingMazeHeight>21)pendingMazeWidth=0;
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).expandTowards(0,4,0); }
}
