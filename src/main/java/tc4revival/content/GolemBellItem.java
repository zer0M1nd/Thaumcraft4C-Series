package tc4revival.content;

import java.util.ArrayList;
import java.util.function.IntFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

public  class GolemBellItem extends Item {
    private final Function<GolemMaterial,Item> placers;
    private final IntFunction<Item> cores,upgrades;
    public GolemBellItem(Properties properties,Function<GolemMaterial,Item> placers,IntFunction<Item> cores,IntFunction<Item> upgrades) {
        super(properties.stacksTo(1)); this.placers=placers; this.cores=cores; this.upgrades=upgrades;
    }
    public static GolemEntity selected(ItemStack stack,Level level) {
        if (!stack.hasTag() || !stack.getTag().contains("golemid")) return null;
        var target=level.getEntity(stack.getTag().getInt("golemid"));
        return target instanceof GolemEntity golem && golem.isAlive()?golem:null;
    }
    private static void clearSelection(CompoundTag tag) {
        for (String key:new String[]{"golemid","markers","golemhomex","golemhomey","golemhomez","golemhomeface","golemhomeanchor","MovingMarkerFrames"}) tag.remove(key);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand) {
        if (!(target instanceof GolemEntity golem)) return InteractionResult.PASS;
        if (!golem.level().isClientSide) {
            var tag=stack.getOrCreateTag(); clearSelection(tag);
            golem.markers().inDimension(golem.level().dimension().location());
            tag.put("markers",golem.markers().save()); tag.putInt("golemid",golem.getId());
            tag.putInt("golemhomex",golem.home().getX()); tag.putInt("golemhomey",golem.home().getY()); tag.putInt("golemhomez",golem.home().getZ());
            tag.putInt("golemhomeface",golem.homeFacing().get3DDataValue());
            tone(golem.level(),golem.blockPosition());
        } else player.swing(hand);
        return InteractionResult.sidedSuccess(golem.level().isClientSide);
    }
    public static void resetMarkers(ItemStack stack,Player player) {
        var golem=selected(stack,player.level()); if (golem==null || player.level().isClientSide) return;
        stack.getOrCreateTag().put("markers",new ListTag()); golem.markers().replace(java.util.List.of());
        tone(player.level(),player.blockPosition());
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var player=context.getPlayer(); if (player==null) return InteractionResult.PASS;
        var hit=getPlayerPOVHitResult(context.getLevel(),player,ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType()==HitResult.Type.BLOCK && !context.getLevel().isClientSide) changeMarker(stack,player,hit.getBlockPos(),hit.getDirection());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);
        // Water surfaces use Item.use rather than the solid-block use-on entry point.
        if(hit.getType()!=HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).isSource())
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        if(!level.isClientSide)changeMarker(stack,player,hit.getBlockPos(),hit.getDirection());
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    public static void changeMarker(ItemStack stack,Player player,BlockPos pos,net.minecraft.core.Direction face) {
        var anchor=player.level() instanceof net.minecraft.server.level.ServerLevel level&&thaumcraft.api.golems.GolemLocations.enabled()?thaumcraft.api.golems.GolemLocations.bind(level,pos,face):null;
        changeMarker(stack,player,pos,face,anchor);
    }
    public static void changeMarker(ItemStack stack,Player player,BlockPos pos,net.minecraft.core.Direction face,thaumcraft.api.golems.GolemAnchor anchor) {
        Level level=player.level(); if (level.isClientSide) return;
        var stored=new GolemMarkers(); stored.load(stack.getOrCreateTag().getList("markers",Tag.TAG_COMPOUND));
        // Bell storage keeps markers from other dimensions; only entity getMarkers prunes them.
        var markers=new ArrayList<GolemMarker>();
        for (Tag value:stored.save()) GolemMarker.load((CompoundTag)value).ifPresent(markers::add);
        var golem=selected(stack,level); boolean colored=golem!=null && golem.configuration().upgrades(4)>0;
        int index=-1,color=0;
        if (!colored) index=find(markers,new GolemMarker(pos,level.dimension().location(),face,(byte)-1,anchor));
        else for (int candidate=-1;candidate<16;candidate++) {
            color=candidate; index=find(markers,new GolemMarker(pos,level.dimension().location(),face,(byte)candidate,anchor)); if (index>=0) break;
        }
        if (index>=0) {
            markers.remove(index);
            if (colored && !player.isShiftKeyDown() && ++color<=15) markers.add(new GolemMarker(pos,level.dimension().location(),face,(byte)color,anchor));
        } else markers.add(new GolemMarker(pos,level.dimension().location(),face,(byte)-1,anchor));
        stored.replace(markers); stack.getOrCreateTag().put("markers",stored.save());
        if (stack.getOrCreateTag().contains("golemid")) {
            if (golem==null) clearSelection(stack.getOrCreateTag()); else golem.markers().replace(markers);
        }
        tone(level,pos);
    }
    private static int find(java.util.List<GolemMarker> markers,GolemMarker query) {
        for (int index=0;index<markers.size();index++) {
            var saved=markers.get(index);
            if(query.anchor()!=null&&saved.anchor()!=null?query.dimension().equals(saved.dimension())&&query.anchor().equals(saved.anchor())&&query.color()==saved.color():query.equals(saved))return index;
        }
        return -1;
    }
    private static void tone(Level level,BlockPos pos) { level.playSound(null,pos,SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.PLAYERS,.7f,1+level.random.nextFloat()*.1f); }
    @Override public boolean onLeftClickEntity(ItemStack stack,Player player,Entity target) {
        if (target instanceof TravelingTrunkEntity trunk) return recoverTrunk(player,trunk);
        if (!(target instanceof GolemEntity golem) || !golem.isAlive()) return false;
        if (golem.level().isClientSide) { golem.spawnAnim(); return true; }
        var config=golem.configuration(); var portable=new ItemStack(placers.apply(config.material())); var tag=portable.getOrCreateTag();config.saveExtension(tag);
        tag.putBoolean("AxisAligned",config.axisAligned());
        var detached=new ArrayList<ItemStack>();
        if (config.advanced()) tag.putByte("advanced",(byte)1);
        if (player.isShiftKeyDown()) {
            if (config.core()!=null) detached.add(new ItemStack(cores.apply(config.core().ordinal())));
            for (byte upgrade:config.upgrades()) if (upgrade>=0 && golem.getRandom().nextBoolean()) detached.add(new ItemStack(upgrades.apply(upgrade)));
        } else {
            if (golem.hasCustomName()) portable.setHoverName(golem.getCustomName());
            if (!config.decorations().isEmpty()) tag.putString("deco",config.decorations());
            if (config.core()!=null) tag.putByte("core",(byte)config.core().ordinal());
            tag.putByteArray("upgrades",config.upgrades());
            golem.markers().inDimension(golem.level().dimension().location()); tag.put("markers",golem.markers().save());
            var filters=new CompoundTag(); golem.filters().save(filters); tag.put("Inventory",filters.getList("Inventory",Tag.TAG_COMPOUND));
            tag.putByteArray("colors",filters.getByteArray("colors"));
            var configuration=new CompoundTag();config.save(configuration);tag.putByte("toggles",configuration.getByte("toggles"));
        }
        if (golem.spawnAtLocation(portable,.5f)==null) return true;
        for (var component:detached) golem.spawnAtLocation(component,.5f);
        golem.dropCargo(); golem.playSound(TCSounds.ZAP.get(),.5f,1); golem.discard(); return true;
    }
    private boolean recoverTrunk(Player player,TravelingTrunkEntity trunk) {
        if (!trunk.isAlive() || !trunk.mayOpen(player)) return false;
        if (trunk.level().isClientSide) { trunk.spawnAnim(); return true; }
        var portable=new ItemStack(TCItems.TRAVELING_TRUNK.get());
        boolean dismantle=player.isShiftKeyDown();
        if (!dismantle) {
            if (trunk.hasCustomName()) portable.setHoverName(trunk.getCustomName());
            portable.getOrCreateTag().putByte("upgrade",(byte)trunk.upgrade());
            if (trunk.upgrade()==4) portable.getOrCreateTag().put("inventory",trunk.saveInventory());
        }
        if (trunk.spawnAtLocation(portable,.5f)==null) return true;
        if (dismantle && trunk.upgrade()>=0 && trunk.getRandom().nextBoolean()) trunk.spawnAtLocation(new ItemStack(upgrades.apply(trunk.upgrade())),.5f);
        if (dismantle || trunk.upgrade()!=4) trunk.dropInventory();
        trunk.playSound(TCSounds.ZAP.get(),.5f,1); trunk.discard(); return true;
    }
}
