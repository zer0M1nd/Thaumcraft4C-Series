package tc4revival.research;

import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.BlockScanTargets;

/** Server-owned thaumometer aiming: entity precedence, ten-block entity ray, five-block block ray. */
public  class ScanTargetSelector {
    public static final double ENTITY_RANGE = 10;
    public static final double BLOCK_RANGE = 5;
    private ScanTargetSelector() {}
    /** Moving block collections can supply the actual surface under the lens. */
    public interface EntitySurface {
        boolean supports(Entity entity);
        Optional<Selection> select(ServerPlayer observer,Entity entity);
    }
    private static final java.util.Map<ResourceLocation,EntitySurface> SURFACES = new java.util.TreeMap<>();
    public static synchronized void registerEntitySurface(ResourceLocation id,EntitySurface surface) {
        if(SURFACES.putIfAbsent(java.util.Objects.requireNonNull(id),java.util.Objects.requireNonNull(surface))!=null)
            throw new IllegalArgumentException("Duplicate scan surface "+id);
    }
    private static EntitySurface surface(Entity entity) {
        for(var surface:SURFACES.values())if(surface.supports(entity))return surface;
        return null;
    }

    public record Selection(ScanTarget target, Component name, Vec3 runeOrigin, int runeLifetime,
            Optional<tc4revival.content.NodeJarData> node) {
        public Selection(ScanTarget target, Component name, Vec3 runeOrigin, int runeLifetime) {
            this(target,name,runeOrigin,runeLifetime,Optional.empty());
        }
    }

    public static Optional<ScanTarget> resolve(ServerPlayer player) {
        return select(player).map(Selection::target);
    }

    /** The lens and the active use share the same authoritative selection, including entity precedence. */
    public static Optional<Selection> select(ServerPlayer player) {
        requireThread(player);
        if (!player.isAlive() || player.isRemoved() || player.isSpectator()) return Optional.empty();
        var entity = pointedEntity(player);
        // The selected entity blocks fallback even if it is unscannable or has already been scanned.
        if (entity.isPresent()) {
            var value = entity.get();
            var surface=surface(value);
            if(surface!=null)return surface.select(player,value);
            var name = value instanceof ItemEntity item ? item.getItem().getHoverName() : value.getName();
            return ScanTargetResolver.entity(value).map(target -> new Selection(target, name,
                    value.position().add(-0.5, value.getEyeHeight() / 2.0f, -0.5), (int) (value.getBbHeight() * 15)));
        }
        var eye = player.getEyePosition();
        var end = eye.add(player.getLookAngle().scale(BLOCK_RANGE));
        var hit = trace(player.serverLevel(), player, eye, end, false);
        if (hit.position() == null) return Optional.empty();
        var position = hit.position();
        var state = tc4revival.content.WardedBlock.queryOriginal(player.serverLevel(), position);
        var mapped = BlockScanTargets.target(state);
        Component name = state.getBlock().getName();
        if (mapped.isPresent() && mapped.get().modernItem() != null) {
            var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(mapped.get().modernItem()));
            if (item != null && item != net.minecraft.world.item.Items.AIR) name = new ItemStack(item).getHoverName();
        }
        Component displayName = name;
        var node = player.serverLevel().getBlockEntity(position) instanceof tc4revival.content.ScannableNode value
                ? Optional.of(new tc4revival.content.NodeJarData(value.aspects(),value.nodeType(),value.nodeModifier(),value.nodeId()))
                : Optional.<tc4revival.content.NodeJarData>empty();
        return ScanTargetResolver.block(player.serverLevel(), position).map(target -> new Selection(target,
                displayName, Vec3.atLowerCornerOf(position).add(0, 0.25, 0), 15, node));
    }

    public static Optional<Entity> pointedEntity(ServerPlayer player) {
        requireThread(player);
        var eye = player.getEyePosition();
        var direction = player.getLookAngle().scale(ENTITY_RANGE);
        var end = eye.add(direction);
        if (!finite(eye) || !finite(end)) return Optional.empty();
        var candidates = new ArrayList<EntityScanRay.Candidate<Entity>>();
        // Item entities must remain scannable even though native attack targeting excludes them.
        for (var entity : player.serverLevel().getEntities(player, player.getBoundingBox().expandTowards(direction),
                entity -> !entity.isRemoved() && !entity.isSpectator())) {
            var surface=surface(entity);
            if(surface!=null) {
                var selection=surface.select(player,entity);
                selection.ifPresent(hit->candidates.add(new EntityScanRay.Candidate<>(entity,
                        new net.minecraft.world.phys.AABB(hit.runeOrigin(),hit.runeOrigin()).inflate(.001),0,1,true)));
                continue;
            }
            float distance = player.distanceTo(entity);
            if (distance < 0.5f) continue;
            boolean visible = !trace(player.serverLevel(), player, eye, entity.getEyePosition(), true).blocked();
            candidates.add(new EntityScanRay.Candidate<>(entity, entity.getBoundingBox(), entity.getPickRadius(), distance, visible));
        }
        return EntityScanRay.select(eye, end, candidates);
    }

    private record Trace(boolean blocked, BlockPos position) {}
    private static Trace trace(ServerLevel level, Entity observer, Vec3 from, Vec3 to, boolean entitySight) {
        if (!finite(from) || !finite(to)) return new Trace(true, null);
        if(!level.hasChunksAt(BlockPos.containing(from),BlockPos.containing(to)))return new Trace(true,null);
        var context=new net.minecraft.world.level.ClipContext(from,to,entitySight?net.minecraft.world.level.ClipContext.Block.COLLIDER:net.minecraft.world.level.ClipContext.Block.OUTLINE,
                entitySight || observer.isShiftKeyDown()?net.minecraft.world.level.ClipContext.Fluid.NONE:net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY,observer) {
            @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(net.minecraft.world.level.block.state.BlockState state,BlockGetter world,BlockPos pos) {
                if(!entitySight) {
                    if(world.getBlockEntity(pos) instanceof tc4revival.content.ScannableNode)return Shapes.block();
                    if(state.getBlock() instanceof tc4revival.content.FiniteFluidBlock)return observer.isShiftKeyDown()?Shapes.empty():state.getShape(world,pos,CollisionContext.of(observer));
                }
                return super.getBlockShape(state,world,pos);
            }
        };
        var hit=level.clip(context);
        boolean blocked=hit.getType()!=net.minecraft.world.phys.HitResult.Type.MISS;
        return new Trace(blocked,blocked?hit.getBlockPos():null);
    }
    private static boolean finite(Vec3 value) { return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z); }
    private static void requireThread(ServerPlayer player) {
        if (player.getServer() == null || !player.getServer().isSameThread()) throw new IllegalStateException("Scan aiming must run on the owning server thread");
    }
}
