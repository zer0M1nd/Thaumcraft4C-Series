package tbmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.content.*;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.wands.ItemFocusBasic;

/** Four independent legacy focus behaviors; shared dispatch only handles payment and permissions. */
public final class Foci extends ItemFocusBasic {
    private final String kind;
    public Foci(String kind) { this.kind = kind; }
    private static Aspect aspect(String tag) { return TCAspects.catalog().require(tag); }
    private static Map<Aspect,Integer> cost(String tag, int value) { return Map.of(aspect(tag), value); }
    private int upgrade(ItemStack focus, String key) { return upgradeLevel(focus, FocusUpgrade.require("thaumicbases:" + key).id); }
    @Override public int color(ItemStack focus) { return switch(kind) { case "activation" -> 0xaaaaaa; case "drain" -> 0x191770; case "experience" -> 0xf8ffdc; default -> 0x64379d; }; }
    @Override public String sortingPrefix() { return switch(kind) { case "activation" -> "AC"; case "drain" -> "DR"; case "experience" -> "XP"; default -> "FX"; }; }
    @Override public Map<Aspect,Integer> costs(ItemStack focus) {
        return switch(kind) { case "activation" -> Map.of(aspect("ordo"),5,aspect("terra"),10); case "drain" -> cost("aqua",5); case "experience" -> Map.of(aspect("ordo"),500,aspect("perditio"),500); default -> cost("ordo",5); };
    }
    @Override public boolean costPerTick(ItemStack focus) { return !kind.equals("activation"); }
    @Override public Animation animation(ItemStack focus) { return kind.equals("activation") ? Animation.WAVE : Animation.CHARGE; }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) {
        var result = new ArrayList<FocusUpgrade>(); result.add(FocusUpgrade.FRUGAL);
        if (!kind.equals("drain")) result.add(FocusUpgrade.POTENCY);
        List<String> extra = List.of();
        if (rank == 3) extra = switch(kind) { case "drain" -> List.of("aquatic","netheric"); case "experience" -> List.of("vaporizing","decomposing"); case "flux" -> List.of("warping","crystalization"); default -> List.of(); };
        if (rank == 5) {
            if (kind.equals("experience")) extra = upgrade(focus,"decomposing") > 0 ? List.of("decomposing") : upgrade(focus,"vaporizing") > 0 ? List.of("vaporizing") : List.of("vaporizing","decomposing");
            if (kind.equals("flux")) extra = upgrade(focus,"warping") > 0 ? List.of("calming") : upgrade(focus,"crystalization") > 0 ? List.of("crystalization") : List.of("warping","crystalization");
        }
        for (String key : extra) result.add(FocusUpgrade.require("thaumicbases:"+key));
        return List.copyOf(result);
    }
    @Override public boolean canApplyUpgrade(ItemStack focus, Player player, FocusUpgrade upgrade, int rank) { return upgradesForRank(focus,rank).contains(upgrade); }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        if (kind.equals("drain") || kind.equals("flux")) { player.startUsingItem(hand); return InteractionResultHolder.consume(wand); }
        if (player instanceof ServerPlayer server) {
            if (kind.equals("activation")) activate(wand,server,hand,hit);
            else experience(wand,server,hit);
        }
        return InteractionResultHolder.sidedSuccess(wand,level.isClientSide);
    }
    private static boolean permitted(ServerPlayer player, BlockPos pos) {
        return player.serverLevel().hasChunkAt(pos) && !player.serverLevel().isOutsideBuildHeight(pos)
            && player.serverLevel().mayInteract(player,pos) && player.mayBuild()
            && player.mayUseItemAt(pos,Direction.UP,player.getMainHandItem());
    }
    private static boolean mayBreak(ServerPlayer player, BlockPos pos) {
        return permitted(player,pos) && net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(player.serverLevel(),player.gameMode.getGameModeForPlayer(),player,pos) >= 0;
    }
    private void activate(ItemStack wand, ServerPlayer player, InteractionHand hand, BlockHitResult hit) {
        ItemStack focus = attached(wand); var tag = focus.getOrCreateTag();
        if (player.isShiftKeyDown()) {
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                tag.putLong("Target",hit.getBlockPos().asLong()); tag.putString("Dimension",player.level().dimension().location().toString());
                attach(wand,focus); WandInventory.consume(player,cost("ordo",100)); player.swing(hand,true);
            }
            return;
        }
        if (!tag.contains("Target")) return;
        BlockPos target = BlockPos.of(tag.getLong("Target"));
        // The old focus charges before its range/dimension validation.
        if (!WandInventory.consume(player,costs(focus))) return;
        double range = 32D * (((CastingWandItem)wand.getItem()).focusPotency(wand,player) + 1);
        if (player.distanceToSqr(target.getX(),target.getY(),target.getZ()) > range*range) { player.displayClientMessage(Component.translatable("tb.txt.tooFar"),false); return; }
        if (!tag.getString("Dimension").equals(player.level().dimension().location().toString())) { player.displayClientMessage(Component.translatable("tb.txt.wrongDimension"),false); return; }
        if (!permitted(player,target)) return;
        var remoteHit = new BlockHitResult(target.getCenter(),Direction.DOWN,target,false);
        var event = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,hand,target,remoteHit);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled() || event.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DENY) return;
        player.level().getBlockState(target).use(player.level(),player,hand,remoteHit);
    }
    private static Map<Aspect,Integer> primals(double raw) {
        int amount = (int)Math.min(Integer.MAX_VALUE, Math.max(0, Math.floor(raw)));
        Map<Aspect,Integer> result = new LinkedHashMap<>(); for (Aspect aspect : thaumcraft.api.aspects.AspectSelection.select(TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.FOCUS_COST)) result.put(aspect,amount); return result;
    }
    private void experience(ItemStack wand, ServerPlayer player, BlockHitResult hit) {
        ServerLevel level = player.serverLevel(); ItemStack focus = attached(wand);
        int decompose = upgrade(focus,"decomposing"), vaporize = upgrade(focus,"vaporizing"), potency = tc4revival.content.FocusPotency.discrete(((CastingWandItem)wand.getItem()).focusPotency(wand,player),level.random);
        if (decompose > 0) {
            Vec3 point = player.getEyePosition().add(player.getLookAngle());
            var entities = level.getEntities(player,new AABB(point,point).inflate(.3),entity->thaumcraft.api.wands.WandActors.canDecompose(player,entity));
            if (!entities.isEmpty()) {
                Entity entity = entities.get(level.random.nextInt(entities.size()));
                if (entity instanceof LivingEntity living) {
                    if (entity instanceof Player || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                        || entity.getType().is(TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("forge","bosses")))
                        || entity.getType().is(TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("thaumicbases","decomposition_blacklist")))) return;
                    double health = living.getHealth();
                    if (health > 0 && WandInventory.consume(player,primals(health*100/decompose))) {
                        boolean hurt = living.hurt(level.damageSources().fellOutOfWorld(),Float.MAX_VALUE);
                        if (hurt && !living.isAlive()) net.minecraft.world.entity.ExperienceOrb.award(level,living.position(),(int)Math.min(Integer.MAX_VALUE,Math.floor(health*3)));
                        return;
                    }
                }
            }
        }
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos(); if (!permitted(player,pos)) return;
        var state = level.getBlockState(pos); float hardness = state.getDestroySpeed(level,pos);
        if (!(hardness > 0) || !Float.isFinite(hardness) || !mayBreak(player,pos)) return;
        if (!WandInventory.consume(player,primals(hardness*100))) return;
        int experience = state.getExpDrop(level,level.random,pos,potency,0); if (experience <= 0) experience = potency+1;
        var aspects = vaporize > 0 && (vaporize > 1 || level.random.nextBoolean()) ? ItemStackAspects.resolve(new ItemStack(state.getBlock())).orElse(null) : null;
        if (!level.removeBlock(pos,false)) return;
        level.levelEvent(2001,pos,Block.getId(state));
        if (aspects != null) for (Aspect aspect : aspects.aspects()) {
            int amount = aspects.amount(aspect); if (amount <= 0) continue;
            ItemStack crystal = TCItems.ESSENCE_CRYSTAL.get().of(aspect,1);
            TCItems.ESSENCE_CRYSTAL.get().setStoredAspects(crystal,new AspectAmounts().add(aspect,amount)); Block.popResource(level,pos,crystal);
        }
        net.minecraft.world.entity.ExperienceOrb.award(level,pos.getCenter(),Math.min(Integer.MAX_VALUE/2,experience)*2); player.swing(player.getUsedItemHand(),true);
    }
    @Override public void usingTick(ItemStack wand, Player player, int remaining) {
        if (!(player instanceof ServerPlayer server)) return;
        ItemStack focus = attached(wand);
        if (!WandInventory.consume(player,costs(focus))) { player.stopUsingItem(); return; }
        if (kind.equals("drain")) drain(server,focus); else if (kind.equals("flux")) flux(server,focus,wand);
    }
    private static void beam(ServerLevel level, Player player, Vec3 end, int color) {
        thaumcraft.api.fx.LegacyEffects.wandBeam(player,end,0,color,1,false);
    }
    private void drain(ServerPlayer player, ItemStack focus) {
        ServerLevel level = player.serverLevel(); Vec3 eye = player.getEyePosition();
        var hit = level.clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(player.getBlockReach())),ClipContext.Block.OUTLINE,ClipContext.Fluid.SOURCE_ONLY,player));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos(); var state = level.getBlockState(pos);
        boolean water = state.is(Blocks.WATER), lava = state.is(Blocks.LAVA);
        if (!water && !lava || !state.getFluidState().isSource() || !permitted(player,pos)) return;
        int aquatic = upgrade(focus,"aquatic"); boolean nether = upgrade(focus,"netheric") > 0;
        if (lava && !nether) return;
        beam(level,player,hit.getLocation(),water?0x5555ff:0xff5555);
        if (player.tickCount % 5 != 0 && !(water && aquatic > 0)) return;
        if (!mayBreak(player,pos) || lava && !WandInventory.consume(player,cost("ignis",3))) return;
        // removeBlock restores a block's FluidState, which leaves source fluids in place.
        // Match the legacy flag 2: synchronize the removal without refilling it immediately.
        if (!level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS)) return;
        level.levelEvent(2001,pos,Block.getId(state));
        if (water && aquatic > 0 && level.random.nextDouble() < .4) Block.popResource(level,pos,TCItems.ESSENCE_CRYSTAL.get().of(aspect(level.random.nextDouble()<.1?"venenum":"aqua"),1));
    }
    private void flux(ServerPlayer player, ItemStack focus, ItemStack wand) {
        ServerLevel level = player.serverLevel(); Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        beam(level,player,eye.add(look.scale(2)),0xff00ff);
        float potency = ((CastingWandItem)wand.getItem()).focusPotency(wand,player);
        int interval = Math.max(1, (int)Math.ceil(10 - potency * 2));
        if (player.tickCount % interval != 0) return;
        // Both flux blocks have empty selection shapes. Traverse every intersected voxel;
        // integer-distance sampling misses thin intersections at oblique angles.
        BlockPos pos = BlockGetter.traverseBlocks(eye,eye.add(look.scale(player.getBlockReach())),level,(world,cell)-> {
            if (!world.hasChunkAt(cell)) return null;
            var block = world.getBlockState(cell);
            return block.is(TCArcane.FLUX_GAS.get()) || block.is(TCArcane.FLUX_GOO.get()) ? cell.immutable() : null;
        },world->null);
        if (pos != null) {
            var state = level.getBlockState(pos);
            if (!mayBreak(player,pos) || !level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS)) return;
            level.levelEvent(2001,pos,Block.getId(state));
            if (upgrade(focus,"calming") > 0) { ThaumcraftApiHelper.addWarpToPlayer(player,-1,true); if(level.random.nextDouble()<=.4)ThaumcraftApiHelper.addStickyWarpToPlayer(player,-1); }
            else if (upgrade(focus,"warping") > 0) { ThaumcraftApiHelper.addWarpToPlayer(player,1,true); if(level.random.nextDouble()<=.1)ThaumcraftApiHelper.addStickyWarpToPlayer(player,1); }
            int crystalization = upgrade(focus,"crystalization");
            if (crystalization > 0) {
                var all = TCAspects.catalog().all(); Aspect aspect = crystalization == 1 ? aspect("potentia") : all.get(level.random.nextInt(all.size()));
                Block.popResource(level,pos,TCItems.ESSENCE_CRYSTAL.get().of(aspect,1));
            }
            return;
        }
    }
    @Override public void appendHoverText(ItemStack focus, Level level, List<Component> lines, TooltipFlag flags) {
        super.appendHoverText(focus,level,lines,flags);
        if (kind.equals("activation") && focus.hasTag() && focus.getTag().contains("Target")) {
            BlockPos pos=BlockPos.of(focus.getTag().getLong("Target")); lines.add(Component.literal("X: "+pos.getX()+" Y: "+pos.getY()+" Z: "+pos.getZ())); lines.add(Component.literal(focus.getTag().getString("Dimension")));
        }
    }
}
