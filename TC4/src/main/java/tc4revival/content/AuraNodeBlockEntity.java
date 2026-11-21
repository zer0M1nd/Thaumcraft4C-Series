package tc4revival.content;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;

/** Ordinary node charge, exhaustion, regeneration, stabilization and wand tapping. */
public  class AuraNodeBlockEntity extends BlockEntity implements WandInteractable, RevealedAspectContainer, ScannableNode {
    private AspectAmounts aspects = new AspectAmounts(), base = new AspectAmounts();
    private AuraNodeType type = AuraNodeType.NORMAL;
    private AuraNodeModifier modifier;
    private String nodeId = "";
    private record Lease(thaumcraft.api.nodes.NodeApi.Influence value,long expires){}
    private final java.util.SortedMap<net.minecraft.resources.ResourceLocation,Lease> influences=new java.util.TreeMap<>();
    private thaumcraft.api.nodes.NodeApi.Influence influence(){
        thaumcraft.api.nodes.NodeApi.Influence best=null;long now=level==null?0:level.getGameTime();
        for(var lease:influences.values())if(lease.expires>now&&(best==null||lease.value.priority()>best.priority()))best=lease.value;return best;
    }
    public AspectAmounts portableCharge(){var values=aspects.copy();if(!influences.isEmpty())for(var a:values.aspects())if(values.amount(a)>base.amount(a))values.reduce(a,values.amount(a)-base.amount(a));return values;}
    public AspectAmounts permanentBase(){return base.copy();}
    public AuraNodeType permanentType(){return type;}
    public AuraNodeModifier permanentModifier(){return modifier;}
    private static void validateCapacity(AspectAmounts values){for(int n:values.view().values())if(n<0||n>1000000)throw new IllegalArgumentException("Node capacity outside 0..1000000");}
    public void editPermanent(AspectAmounts values,AuraNodeType kind,AuraNodeModifier quality){validateCapacity(values);base=values.copy();type=kind;modifier=quality;clampCharge();nodeChanged();}
    private void clampCharge(){var capacities=base();for(var aspect:aspects.aspects()){int n=capacities.amount(aspect);if(n==0&&!capacities.contains(aspect))aspects.remove(aspect);else if(aspects.amount(aspect)>n)aspects.reduce(aspect,aspects.amount(aspect)-n);}for(var aspect:capacities.aspects())if(!aspects.contains(aspect))aspects.add(aspect,0);}
    public void addCharge(Aspect aspect,int amount){if(amount<0||amount>Math.max(0,base().amount(aspect)-aspects.amount(aspect)))throw new IllegalArgumentException("Charge exceeds capacity");aspects.add(aspect,amount);sync();}
    public void putInfluence(net.minecraft.resources.ResourceLocation id,thaumcraft.api.nodes.NodeApi.Influence value,int ticks){
        if(ticks<1||ticks>1200)throw new IllegalArgumentException("Lease must last 1..1200 ticks");java.util.Objects.requireNonNull(value);java.util.Objects.requireNonNull(id);
        if(!influences.containsKey(id)&&influences.size()>=64)throw new IllegalStateException("Too many node devices");
        var before=influence();influences.put(id,new Lease(value,level.getGameTime()+ticks));if(!java.util.Objects.equals(before,influence())){clampCharge();nodeChanged();}
    }
    public void expireInfluences(){if(influences.values().removeIf(lease->lease.expires<=level.getGameTime())){clampCharge();nodeChanged();}}
    public void removeInfluence(net.minecraft.resources.ResourceLocation id){if(influences.remove(id)!=null){clampCharge();nodeChanged();}}
    private int ticks, regeneration = -1, rechargeWait, lock;
    private long lastActive;
    private boolean catchUp;
    private UUID drainer;
    private int drainColor = 0xffffff;
    public AuraNodeBlockEntity(BlockPos pos, BlockState state) { this(TCArcane.AURA_NODE_ENTITY.get(), pos, state); }
    protected AuraNodeBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    public AspectAmounts aspects() { return aspects.copy(); }
    @Override public AspectAmounts revealedAspects() { return aspects(); }
    public AspectAmounts base() { var result=base.copy();var effect=influence();if(effect!=null){result=new AspectAmounts();for(var a:base.aspects())result.add(a,(int)Math.min(16000000,Math.floor(base.amount(a)*effect.capacityMultiplier())));}return result; }
    public AuraNodeType nodeType() { var effect=influence();return effect!=null&&effect.type()!=null?effect.type():type; }
    public AuraNodeModifier nodeModifier() { var effect=influence();return effect!=null&&effect.overrideModifier()?effect.modifier():modifier; }
    public String nodeId() {
        if (nodeId.isEmpty() && level != null) nodeId = level.dimension().location() + ":" + worldPosition.getX() + ":" + worldPosition.getY() + ":" + worldPosition.getZ();
        return nodeId;
    }
    public UUID drainer() { return drainer; }
    public int drainColor() { return drainColor; }
    public void configure(AspectAmounts values, AuraNodeType type, AuraNodeModifier modifier, String id, boolean empty) {
        base = values.copy(); aspects = values.copy(); this.type = type; this.modifier = modifier; nodeId = id;
        if (empty) for (var aspect : aspects.aspects()) aspects.reduce(aspect, aspects.amount(aspect));
        nodeChanged();
    }
    public boolean take(Aspect aspect, int amount) {
        if (amount < 0 || !aspects.contains(aspect) || !aspects.reduce(aspect, amount)) return false;
        setChanged(); return true;
    }
    public void sync() { setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2); }
    private void nodeChanged() { regeneration = -1; sync(); }
    private int baseRegeneration() { return nodeModifier() == null ? 600 : switch (nodeModifier()) { case BRIGHT -> 400; case PALE -> 900; case FADING -> 0; }; }
    private int period() { int raw=baseRegeneration() * (lock == 1 ? 2 : lock == 2 ? 20 : 1);var effect=influence();return raw==0?0:Math.max(1,(int)(raw/(effect==null?1:effect.regenerationMultiplier()))); }
    private boolean recoverOne() {
        var available = aspects.aspects().stream().filter(aspect -> aspects.amount(aspect) < base().amount(aspect)).toList();
        if (available.isEmpty()) return false;
        aspects.add(available.get(level.random.nextInt(available.size())), 1); return true;
    }
    public int lockState(){return lock;}
    public int rechargeWaitTicks(){return rechargeWait;}
    public void tick() {
        tickNode();
        thaumcraft.api.nodes.NodeObservation.afterTick(this);
    }
    private void tickNode() {
        if (level == null) return;
        if (level.isClientSide) { hungryEntities(); if (++ticks%50==0 && nodeType()==AuraNodeType.DARK) tc4revival.client.SinisterStoneView.observe(level,worldPosition); return; }
        expireInfluences();
        nodeId(); ticks++;
        if (ticks == 1 || ticks % 50 == 0) {
            int next = level.hasChunkAt(worldPosition.below()) && !level.hasNeighborSignal(worldPosition.below())
                    && level.getBlockEntity(worldPosition.below()) instanceof NodeStabilizerBlockEntity stabilizer ? stabilizer.lockStrength() : 0;
            if (next != lock) { lock = next; regeneration = -1; }
        }
        boolean changed = hungryEntities();
        changed |= bullyNearby();
        if (regeneration < 0) regeneration = period();
        if (catchUp) {
            catchUp = false;
            long now = System.currentTimeMillis();
            long elapsed = Math.max(0, now - lastActive);
            long recovered = regeneration > 0 ? Math.min(base.total(), elapsed / (regeneration * 75L)) : 0;
            // Stop once full instead of spending unbounded time on an old/corrupt timestamp.
            for (long i = 0; i < recovered; i++) { if (!recoverOne()) break; changed = true; }
            // Consume this interval even if the node was already full. Otherwise a drain followed
            // by another chunk unload replays the same offline recovery before the periodic tick.
            lastActive = now;
            setChanged();
        } else if (lastActive <= 0) {
            lastActive = System.currentTimeMillis();
            setChanged();
        }
        if (ticks % 1200 == 0) {
            for (var aspect : aspects.aspects()) {
                if (aspects.amount(aspect) > 0) continue;
                setBase(aspect, (short) (base.amount(aspect) - 1)); changed = true;
                if (level.random.nextInt(20) == 0 || base.amount(aspect) <= 0) {
                    aspects.remove(aspect);
                    if (level.random.nextInt(5) == 0) {
                        if (modifier == AuraNodeModifier.BRIGHT) modifier = null;
                        else if (modifier == null) modifier = AuraNodeModifier.PALE;
                        if (modifier == AuraNodeModifier.PALE && level.random.nextInt(5) == 0) modifier = AuraNodeModifier.FADING;
                    }
                    regeneration = -1; break;
                }
                regeneration = -1;
            }
            if (aspects.isEmpty()) {
                if (getBlockState().getBlock() instanceof ObsidianTotemBlock)
                    level.setBlock(worldPosition,TCArcane.OBSIDIAN_TOTEM.get().at(level,worldPosition),3);
                else if (getBlockState().getBlock() instanceof SilverwoodNodeLogBlock)
                    level.setBlock(worldPosition,TCArcane.SILVERWOOD_LOG.get().defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,getBlockState().getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)),3);
                else level.removeBlock(worldPosition,false);
                return;
            }
        }
        if (rechargeWait > 0) rechargeWait--;
        if (regeneration > 0 && rechargeWait == 0 && ticks % regeneration == 0) { lastActive = System.currentTimeMillis(); changed |= recoverOne(); setChanged(); }
        if (ticks % 100 == 0) {
            if (nodeType() == AuraNodeType.UNSTABLE && level.random.nextBoolean()) {
                if (lock == 0) {
                    var primals = aspects.primals();
                    if (!primals.isEmpty()) {
                        var aspect = primals.get(level.random.nextInt(primals.size()));
                        if (take(aspect, 1)) { level.addFreshEntity(new AspectOrbEntity(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, aspect, 1)); changed = true; }
                    }
                } else if (level.random.nextInt(10000 / lock) == 42) { type = AuraNodeType.NORMAL; changed = true; }
            }
            if (modifier == AuraNodeModifier.FADING && lock > 0 && level.random.nextInt(12500 / lock) == 69) { modifier = AuraNodeModifier.PALE; changed = true; }
        }
        if (drainer != null) {
            var player = level.getPlayerByUUID(drainer);
            if (player == null || !player.isUsingItem() || WandUseTarget.read(player.getUseItem().getTag(), level.dimension().location().toString())
                    .map(target -> target.x() != worldPosition.getX() || target.y() != worldPosition.getY() || target.z() != worldPosition.getZ()).orElse(true)) {
                drainer = null; changed = true;
            }
        }
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            if (ticks % 50 == 0) NodeBiomeEffects.tick(server,worldPosition,nodeType());
            if (ticks % 100 == 0 && nodeType() != AuraNodeType.PURE && nodeType() != AuraNodeType.TAINTED
                    && server.getBiomeManager().getNoiseBiomeAtPosition(worldPosition).is(TCBiomes.TAINTED_LAND)
                    && server.random.nextInt(500)==0) { type=AuraNodeType.TAINTED;nodeChanged();changed=true; }
        }
        if (changed) sync();
        if (nodeType() == AuraNodeType.PURE && ticks % 50 == 0 && level instanceof net.minecraft.server.level.ServerLevel server)
            PureNodeBiomeConversion.tick(server,worldPosition);
        if (nodeType() == AuraNodeType.HUNGRY && ticks % 50 == 0) consumeNearbyBlock();
    }
    private boolean hungryEntities() {
        if (nodeType() != AuraNodeType.HUNGRY || !tc4revival.config.TCConfig.SERVER.hardNodes.get()) return false;
        boolean changed = false; var center = net.minecraft.world.phys.Vec3.atCenterOf(worldPosition);
        for (var entity : level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(worldPosition).inflate(15))) {
            if (entity instanceof Player player && (player.getAbilities().instabuild || player.getAbilities().invulnerable || player.isSpectator())) continue;
            // Player movement is simulated by the owning client. ServerPlayer velocity otherwise
            // accumulates without travel friction and is later sent as an enormous knockback packet.
            if(level.isClientSide && (!(entity instanceof Player player) || !player.isLocalPlayer()))continue;
            var offset = center.subtract(entity.position()); double distance = offset.length();
            if (!level.isClientSide && entity.isAlive() && !entity.isInvulnerable() && distance * distance < 2) {
                // ItemEntity may clear its stack when it dies. TC4 scans one item, not the stack count.
                var swallowed = entity instanceof net.minecraft.world.entity.item.ItemEntity item
                        ? item.getItem().copyWithCount(1) : net.minecraft.world.item.ItemStack.EMPTY;
                entity.hurt(level.damageSources().fellOutOfWorld(), 1);
                if (!entity.isAlive()) {
                    var food = !swallowed.isEmpty()
                            ? tc4revival.aspect.item.ItemStackAspects.resolve(swallowed).orElseGet(AspectAmounts::new)
                            : tc4revival.aspect.entity.EntityAspects.resolve(entity).map(value -> value.aspects()).orElseGet(AspectAmounts::new);
                    food = TCAspects.catalog().toPrimals(food, tc4revival.aspect.AspectCatalog.Reduction.SUM);
                    if (!food.isEmpty()) {
                        var aspect = food.aspects().get(level.random.nextInt(food.size()));
                        if (aspects.amount(aspect) < base.amount(aspect)) { aspects.add(aspect, 1); changed = true; }
                        else if (level.random.nextInt((int) Math.min(Integer.MAX_VALUE, 1L + Math.max(0L, base.amount(aspect) * 2L))) < food.amount(aspect)
                                && base.amount(aspect) < Integer.MAX_VALUE) { base.add(aspect, 1); changed = true; }
                    }
                }
            }
            if (!(entity instanceof ServerPlayer) && distance > 1e-8 && distance < 15) {
                double pull = Math.pow(1 - distance / 15, 2) / distance;
                entity.setDeltaMovement(entity.getDeltaMovement().add(offset.x * pull * .15, offset.y * pull * .25, offset.z * pull * .15));
                entity.hasImpulse = true;
            }
        }
        return changed;
    }
    private void consumeNearbyBlock() {
        var random = level.random;
        var target = worldPosition.offset(random.nextInt(16) - random.nextInt(16), random.nextInt(16) - random.nextInt(16), random.nextInt(16) - random.nextInt(16));
        for (int x = Math.min(target.getX(), worldPosition.getX()) >> 4; x <= Math.max(target.getX(), worldPosition.getX()) >> 4; x++)
            for (int z = Math.min(target.getZ(), worldPosition.getZ()) >> 4; z <= Math.max(target.getZ(), worldPosition.getZ()) >> 4; z++)
                if (!level.hasChunk(x, z)) return;
        int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, target.getX(), target.getZ());
        target = new BlockPos(target.getX(), Math.max(level.getMinBuildHeight(), Math.min(target.getY(), surface)), target.getZ());
        var start = net.minecraft.world.phys.Vec3.atCenterOf(worldPosition); var end = net.minecraft.world.phys.Vec3.atCenterOf(target);
        var hit = level.clip(new net.minecraft.world.level.ClipContext(start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.ANY, null));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().equals(worldPosition) || start.distanceToSqr(net.minecraft.world.phys.Vec3.atLowerCornerOf(hit.getBlockPos())) >= 256) return;
        var state = level.getBlockState(hit.getBlockPos()); float hardness = state.getDestroySpeed(level, hit.getBlockPos());
        if (!state.isAir() && hardness >= 0 && hardness < 5) level.destroyBlock(hit.getBlockPos(), true);
    }
    private void setBase(Aspect aspect, int amount) {
        if (amount > base.amount(aspect)) base.merge(aspect, amount);
        else base.reduce(aspect, base.amount(aspect) - amount);
    }
    /** Pearl changes capacity without refilling existing aspects or resetting cached regeneration. */
    public void rejuvenate(boolean researched, net.minecraft.util.RandomSource random) {
        for (var aspect : aspects.aspects()) {
            int capacity = base.amount(aspect);
            if (aspect.isPrimal()) setBase(aspect, (short) (capacity - 2 + random.nextInt(researched ? 9 : 6)));
            else if (random.nextBoolean()) setBase(aspect, (short) (capacity - 1));
        }
        for (var aspect : TCAspects.catalog().primals()) {
            int candidate = random.nextInt(researched ? 4 : 3);
            if (candidate <= 0 || candidate <= base.amount(aspect)) continue;
            setBase(aspect, candidate);
            // Legacy addToContainer can also reduce a charge that exceeds its new base.
            int excess = Math.max(0, 1 + aspects.amount(aspect) - base.amount(aspect));
            aspects.add(aspect, 1 - excess);
        }
        if (modifier == AuraNodeModifier.FADING && random.nextBoolean()) modifier = AuraNodeModifier.PALE;
        else if (modifier == AuraNodeModifier.PALE && random.nextBoolean()) modifier = null;
        else if (modifier == null && random.nextInt(5) == 0) modifier = AuraNodeModifier.BRIGHT;
        sync();
    }
    private boolean bullyNearby() {
        if (!(getBlockState().getBlock() instanceof AuraNodeBlock) || lock == 1 || nodeModifier() == AuraNodeModifier.FADING) return false;
        boolean strong = nodeType() == AuraNodeType.HUNGRY || nodeModifier() == AuraNodeModifier.BRIGHT;
        int interval = nodeModifier() == null ? 2 : strong ? 1 : nodeModifier() == AuraNodeModifier.PALE ? 3 : 2;
        if (ticks % interval != 0) return false;
        var random = level.random;
        BlockPos target = worldPosition.offset(random.nextInt(5) - random.nextInt(5), random.nextInt(5) - random.nextInt(5), random.nextInt(5) - random.nextInt(5));
        if (nodeModifier() == AuraNodeModifier.PALE && random.nextBoolean()) return false;
        if (target.equals(worldPosition) || !level.hasChunkAt(target) || !(level.getBlockEntity(target) instanceof AuraNodeBlockEntity other) || !(other.getBlockState().getBlock() instanceof AuraNodeBlock) || other.lock > 0
                || other.aspects.isEmpty() || (other.aspects.total() + other.base.total()) / 2 >= (aspects.total() + base.total()) / 2) return false;
        var aspect = other.aspects.aspects().get(random.nextInt(other.aspects.size()));
        if (!other.take(aspect, 1)) return false;
        if (aspects.amount(aspect) < base.amount(aspect)) aspects.add(aspect, 1);
        else if (random.nextInt((int) Math.min(Integer.MAX_VALUE, 1L + Math.max(0, (long) (base.amount(aspect) / (strong ? 1.5 : 1))))) == 0) {
            if (base.amount(aspect) < Integer.MAX_VALUE) base.add(aspect, 1);
            if (nodeModifier() == AuraNodeModifier.PALE && random.nextInt(100) == 0) { modifier = null; regeneration = -1; }
            if (random.nextInt(3) == 0) other.setBase(aspect, (short) (other.base.amount(aspect) - 1));
        }
        other.rechargeWait = other.regeneration / 2; other.sync();
        tc4revival.network.FocusEffectPacket.nodeZap(level,target.getCenter(),worldPosition.getCenter());
        return true;
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand, UseOnContext context) { return InteractionResult.PASS; }
    @Override public Optional<ItemStack> onWandUse(Level level, ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof CastingWandItem wand)) return Optional.empty();
        wand.setObjectInUse(stack, level, worldPosition);
        player.startUsingItem(player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        return Optional.of(stack);
    }
    @Override public void onWandUseTick(ItemStack stack, Player player, int remainingTicks) {
        if (level == null || !(stack.getItem() instanceof CastingWandItem wand)) return;
        HitResult hit = player.pick(player.getBlockReach(), 1, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK || !block.getBlockPos().equals(worldPosition)) {
            onWandRelease(stack,level,player,remainingTicks);
            WandUseTarget.clear(stack.getTag());
            player.stopUsingItem(); return;
        }
        if (level.isClientSide || remainingTicks % 5 != 0 || !(player instanceof ServerPlayer server)) return;
        Set<String> research = KnowledgeSavedData.get(server.getServer()).snapshot(player.getUUID()).map(value -> value.researchMarkers()).orElse(Set.of());
        int rate = 1 + (research.contains("NODETAPPER1") ? 1 : 0) + (research.contains("NODETAPPER2") ? 1 : 0);
        boolean preserve = !player.isShiftKeyDown() && research.contains("NODEPRESERVE")
                && wand.rod(stack) != WandParts.Rod.WOOD && wand.cap(stack) != WandParts.Cap.IRON;
        var candidates = aspects.primals().stream().filter(aspect -> aspects.amount(aspect) > (preserve ? 1 : 0) && wand.vis(stack, aspect) < wand.capacity(stack)).toList();
        if (candidates.isEmpty()) { if (drainer != null) { drainer = null; sync(); } return; }
        var aspect = candidates.get(level.random.nextInt(candidates.size()));
        int amount = Math.min(rate, aspects.amount(aspect) - (preserve ? 1 : 0));
        int accepted = wand.receiveVis(stack, aspect, amount * 100, true);
        if (accepted <= 0) return;
        accepted = wand.receiveVis(stack, aspect, accepted, false);
        int leftover = amount - (accepted + 99) / 100;
        if (leftover < amount) { take(aspect, amount - leftover); drainColor = aspect.color(); drainer = player.getUUID(); sync(); }
    }
    @Override public void onWandRelease(ItemStack stack, Level level, Player player, int remainingTicks) { if (player.getUUID().equals(drainer)) { drainer = null; sync(); } }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putString("nodeId", nodeId()); tag.putLong("lastActive", lastActive);
        tag.putByte("type", (byte) type.ordinal()); tag.putByte("modifier", modifier == null ? -1 : (byte) modifier.ordinal());
        var persisted=portableCharge();
        AspectNbt.write(tag, persisted); AspectNbt.write(tag, "AspectsBase", base);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); influences.clear(); nodeId = tag.getString("nodeId"); lastActive = tag.getLong("lastActive");
        type = AuraNodeType.fromId(tag.getByte("type")); modifier = tag.contains("modifier") ? AuraNodeModifier.fromId(tag.getByte("modifier")) : null;
        aspects = AspectNbt.read(tag, TCAspects.catalog()).amounts(); base = AspectNbt.read(tag, "AspectsBase", TCAspects.catalog()).amounts();
        if (base.isEmpty() && tag.getShort("nodeVisBase") > 0) for (var aspect : aspects.aspects()) base.merge(aspect, tag.getShort("nodeVisBase"));
        regeneration = -1; catchUp = lastActive > 0 && baseRegeneration() > 0 && System.currentTimeMillis() - lastActive > baseRegeneration() * 75L;
        drainer = tag.hasUUID("Drainer") ? tag.getUUID("Drainer") : null; drainColor = tag.contains("draincolor") ? tag.getInt("draincolor") : 0xffffff;
    }
    @Override public CompoundTag getUpdateTag() {
        var tag = saveWithoutMetadata();AspectNbt.write(tag,aspects);tag.putByte("type",(byte)nodeType().ordinal());tag.putByte("modifier",nodeModifier()==null?-1:(byte)nodeModifier().ordinal());AspectNbt.write(tag,"AspectsBase",base()); if (drainer != null) tag.putUUID("Drainer", drainer); tag.putInt("draincolor", drainColor); return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
