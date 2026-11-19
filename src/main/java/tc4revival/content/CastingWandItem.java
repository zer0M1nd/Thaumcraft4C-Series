package tc4revival.content;

import java.util.Map;
import thaumcraft.api.wands.WandCasting;
import thaumcraft.api.wands.SpellParameters;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

/** Casting implements block interaction before focus use, as in TC4. */
public  class CastingWandItem extends Item {
    public CastingWandItem() { super(new Properties().stacksTo(1)); }
    public WandParts.Rod rod(ItemStack stack) { return WandParts.rod(stack.getTag()); }
    public WandParts.Cap cap(ItemStack stack) { return WandParts.cap(stack.getTag()); }
    public boolean isStaff(ItemStack stack) { return rod(stack).staff(); }
    public boolean isSceptre(ItemStack stack) { return WandParts.sceptre(stack.getTag()); }
    public int focusCooldownMillis(ItemStack stack, ItemStack focus, int normal) { return normal; }
    public int continuousFocusCalls(ItemStack stack,ItemStack focus) { return 1; }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.UNCOMMON; }
    public int capacity(ItemStack stack) { return WandVis.capacity(rod(stack).capacity(),isSceptre(stack)); }
    public int vis(ItemStack stack, Aspect aspect) { return Math.min(capacity(stack),WandVis.stored(stack.getTag(), aspect)); }
    public float consumptionModifier(ItemStack stack, Player player, Aspect aspect, boolean crafting) {
        var focus = WandFocusItem.attached(stack);
        int frugal = focus.isEmpty() ? 0 : ((WandFocusItem) focus.getItem()).upgradeLevel(focus, 1);
        return (float)thaumcraft.api.wands.FocusUpgradeType.costMultiplier(focus,player,aspect)*WandVis.modifier(cap(stack).cost(aspect.tag()), WandInventory.discount(player, aspect),
                frugal, !focus.isEmpty(), crafting, isSceptre(stack));
    }
    public boolean consumeForPlayer(ItemStack stack, Player player, Map<Aspect, Integer> costs, boolean wholeVis, boolean crafting, boolean commit) {
        costs=SpellParameters.costs(stack,costs);
        if (!thaumcraft.api.wands.WandAutomation.allowed(stack,player)
                ||!thaumcraft.api.wands.MagicOperationRules.triggerAllows(stack,player,costs,commit)) return false;
        try (var automation = thaumcraft.api.wands.WandAutomation.open(stack,player)) {
        return consume(stack, costs, aspect -> consumptionModifier(stack, player, aspect, crafting), wholeVis, commit, player.level());
            }
    }
    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flags) {
        tc4revival.client.WandTooltip.append(stack, lines, flags);
        if (thaumcraft.api.wands.WandAutomation.installed(stack)) {
            var core = thaumcraft.api.wands.WandAutomation.core(stack);
            lines.add(net.minecraft.network.chat.Component.translatable("thaumcraft.wand.automation_only").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            if (!core.isEmpty()) lines.add(core.getHoverName());
        }
    }
    /** May be overridden for per-stack policies; initial create(..., charged=true) is independent. */
    public boolean rechargeable(ItemStack stack) { return rod(stack).rechargeable && thaumcraft.api.wands.WandRechargePolicies.allows(stack); }
    /** Accepted centivis, honoring addVis overrides. Simulation must be side-effect free. */
    public int receiveVis(ItemStack stack, Aspect aspect, int amount, boolean simulate) {
        if (amount < 0) throw new IllegalArgumentException("Negative vis transfer");
        if (!rechargeable(stack) || !aspect.isPrimal()) return 0;
        return Math.max(0, amount - Math.max(0, addVis(stack, aspect, amount, false, !simulate)));
    }
    public int addVis(ItemStack stack, Aspect aspect, int amount, boolean wholeVis, boolean commit) {
        if (amount < 0) throw new IllegalArgumentException("Negative vis transfer");
        if (!rechargeable(stack) || !aspect.isPrimal()) return amount;
        var tag = stack.hasTag() ? stack.getTag().copy() : new net.minecraft.nbt.CompoundTag();
        int remainder = WandVis.add(tag, aspect, amount, capacity(stack), wholeVis, commit);
        if (commit && aspect.isPrimal()) stack.setTag(tag);
        return remainder;
    }
    public ItemStack create(WandParts.Rod rod, WandParts.Cap cap, boolean sceptre, boolean charged) {
        var stack = new ItemStack(this);
        stack.getOrCreateTag().putString("rod", rod.tag);
        stack.getOrCreateTag().putString("cap", cap.tag);
        int oldDamage = rod == WandParts.Rod.WOOD && cap == WandParts.Cap.IRON && !sceptre ? 0
                : (int) (rod.craftCost * cap.craftCost * (sceptre ? 1.5f : 1));
        stack.getOrCreateTag().putInt("tc4_metadata", oldDamage);
        if (sceptre) stack.getOrCreateTag().putByte("sceptre", (byte) 1);
        if (charged) for (var aspect : TCAspects.catalog().primals()) stack.getOrCreateTag().putInt(aspect.tag(), capacity(stack));
        return stack;
    }
    public int legacyMetadata(ItemStack stack) { return stack.hasTag() ? stack.getTag().getInt("tc4_metadata") : 0; }
    public int focusUpgrade(ItemStack stack, FocusUpgrade upgrade) {
        var focus = WandFocusItem.attached(stack);
        if (focus.isEmpty()) return 0;
        return ((WandFocusItem) focus.getItem()).upgradeLevel(focus, upgrade.id);
    }
    /** Installed focus levels plus the wielder's equipment attribute, without rounding. */
    public float focusPotency(ItemStack stack, Player player) {
        return focusUpgrade(stack, FocusUpgrade.POTENCY) + FocusPotency.bonus(player);
    }
    @Override public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier>
            getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack) {
        if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND && isStaff(stack))
            return com.google.common.collect.ImmutableMultimap.of(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 6,
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        return super.getAttributeModifiers(slot, stack);
    }
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack) {
        var rod = rod(stack); var cap = cap(stack);
        return net.minecraft.network.chat.Component.translatable("item.Wand.name",
                net.minecraft.network.chat.Component.translatable("item.Wand." + cap.tag + ".cap"),
                net.minecraft.network.chat.Component.translatable("item.Wand." + rod.tag.replace("_staff", "") + ".rod"),
                net.minecraft.network.chat.Component.translatable("item.Wand." + (rod.staff() ? "staff" : WandParts.sceptre(stack.getTag()) ? "sceptre" : "wand") + ".obj"));
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new tc4revival.client.WandItemExtension());
    }
    public boolean consume(ItemStack stack, Map<Aspect, Integer> costs, java.util.function.ToDoubleFunction<Aspect> modifiers,
            boolean wholeVis, boolean commit, Level level) {
        costs=SpellParameters.costs(stack,costs);
        if(!thaumcraft.api.wands.MagicOperationRules.allows(new thaumcraft.api.wands.MagicOperationRules.Context(
            thaumcraft.api.wands.MagicOperationRules.Kind.USE,"thaumcraft:vis",null,level,null,null,stack,ItemStack.EMPTY,"",costs,!commit),false))return false;
        var tag = stack.hasTag() ? stack.getTag().copy() : new net.minecraft.nbt.CompoundTag();
        for(var aspect:TCAspects.catalog().primals())if(tag.getInt(aspect.tag())>capacity(stack))tag.putInt(aspect.tag(),capacity(stack));
        boolean write = commit && !level.isClientSide;
        boolean accepted = WandVis.consume(tag, costs, modifiers, wholeVis, write);
        if (write && accepted) stack.setTag(tag);
        return accepted;
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // stopUsingItem (damage, changed aim, hand replacement) does not call releaseUsing.
        // A block target belongs only to the current use session, never to the next cast.
        if (entity instanceof Player holder && (!holder.isUsingItem() || holder.getUseItem()!=stack))
            clearObjectInUse(stack,level,holder);
        if (!level.isClientSide && entity instanceof Player player) {
            var tag = stack.hasTag() ? stack.getTag().copy() : new net.minecraft.nbt.CompoundTag();
            // Regeneration chooses an aspect on a scratch tag, then uses the same acceptance policy as devices.
            if (rechargeable(stack)) WandRegeneration.tick(tag, player.tickCount, TCAspects.catalog(), level.random::nextInt, rod(stack), capacity(stack))
                    .ifPresent(id -> receiveVis(stack, TCAspects.catalog().require(id), 100, false));
            thaumcraft.api.wands.WandRod.tick(stack,player);
            thaumcraft.api.wands.FocusUpgradeType.tick(stack,player);
        }
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!thaumcraft.api.wands.WandAutomation.allowed(stack,context.getPlayer())) return InteractionResult.FAIL;
        try (var automation = thaumcraft.api.wands.WandAutomation.open(stack,context.getPlayer())) {
        var addon=thaumcraft.api.wands.WandTriggerRegistry.perform(context);
        if(addon!=InteractionResult.PASS)return addon;
        var level = context.getLevel(); var position = context.getClickedPos();
        // TC4 gives the clicked block and tile first refusal before structure triggers.
        if (level.getBlockState(position).getBlock() instanceof WandInteractable block) {
            var result = block.onWandUseFirst(stack, context);
            if (result != InteractionResult.PASS) return result;
        }
        if (level.getBlockEntity(position) instanceof WandInteractable blockEntity) {
            var result = blockEntity.onWandUseFirst(stack, context);
            if (result != InteractionResult.PASS) return result;
        }
        var advanced=AdvancedAlchemyStructure.form(stack,context);
        if (advanced!=InteractionResult.PASS) return advanced;
        var thaumatorium=ThaumatoriumBlock.form(stack,context);
        if (thaumatorium!=InteractionResult.PASS) return thaumatorium;
        var furnace=InfernalFurnaceStructure.form(stack,context);
        if (furnace!=InteractionResult.PASS) return furnace;
        var altar=InfusionAltar.form(stack,context);
        if (altar!=InteractionResult.PASS) return altar;
        var jar=NodeJarCapture.form(stack,context);
        if (jar!=InteractionResult.PASS) return jar;
        var crucible = CrucibleBlock.form(stack,context);
        if (crucible != InteractionResult.PASS) return crucible;
        var conversion = ArcaneWorkbenchBlock.form(stack, context);
        if (conversion != InteractionResult.PASS) return conversion;
        var recovery=OwnedDeviceRecovery.recover(context);
        if (recovery!=InteractionResult.PASS) return recovery;
        if (!WandFocusItem.attached(stack).isEmpty()) return InteractionResult.PASS;
        if (context.getPlayer() instanceof ServerPlayer player)
            return WandBookCreation.create(player, context.getClickedPos());
        return InteractionResult.PASS;
            }
    }
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        if (!thaumcraft.api.wands.WandAutomation.allowed(player.getItemInHand(hand),player)) return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));
        try (var automation = thaumcraft.api.wands.WandAutomation.open(player.getItemInHand(hand),player)) {
        var stack = player.getItemInHand(hand);
        clearObjectInUse(stack,level,player);
        var hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        // Nodes can be waterlogged. Ignore water for their interaction, while retaining
        // the fluid-aware ray for foci and all other legacy wand targets.
        var solidHit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.NONE);
        if (solidHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && level.hasChunkAt(solidHit.getBlockPos())
                && level.getBlockEntity(solidHit.getBlockPos()) instanceof AuraNodeBlockEntity) hit = solidHit;
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && level.hasChunkAt(hit.getBlockPos())) {
            var position = hit.getBlockPos();
            if (level.getBlockState(position).getBlock() instanceof WandInteractable block) {
                var result = block.onWandUse(level, stack, player);
                if (result.isPresent()) return net.minecraft.world.InteractionResultHolder.consume(result.get());
            }
            if (level.getBlockEntity(position) instanceof WandInteractable blockEntity) {
                var result = blockEntity.onWandUse(level, stack, player);
                if (result.isPresent()) return net.minecraft.world.InteractionResultHolder.consume(result.get());
            }
        }
        try(var cast=WandCasting.open(new WandCasting.Request(player,stack,hand,WandCasting.Action.USE))){
            var focus=cast.focus();
            if (!focus.isEmpty() && focus.getItem() instanceof WandFocusItem item && WandCooldowns.begin(player, focusCooldownMillis(stack,focus,item.cooldownMillis(focus))))
                return item.cast(stack, level, player, hand, hit);
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        }
            }
    }
    public void setObjectInUse(ItemStack stack, Level level, net.minecraft.core.BlockPos position) {
        new WandUseTarget(position.getX(), position.getY(), position.getZ(), level.dimension().location().toString()).write(stack.getOrCreateTag());
    }
    private java.util.Optional<WandInteractable> objectInUse(ItemStack stack, Level level) {
        return WandUseTarget.read(stack.getTag(), level.dimension().location().toString()).flatMap(target -> {
            var position = new net.minecraft.core.BlockPos(target.x(), target.y(), target.z());
            if (!level.hasChunkAt(position)) return java.util.Optional.empty();
            return level.getBlockEntity(position) instanceof WandInteractable value ? java.util.Optional.of(value) : java.util.Optional.empty();
        });
    }
    private void clearObjectInUse(ItemStack stack,Level level,Player player) {
        if (stack.getTag()==null || !stack.getTag().contains("IIUX")) return;
        try { objectInUse(stack,level).ifPresent(target->target.onWandRelease(stack,level,player,0)); }
        finally { WandUseTarget.clear(stack.getTag()); }
    }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack,ItemStack newStack,boolean slotChanged) {
        if(slotChanged || oldStack.getItem()!=newStack.getItem())return true;
        var oldTag=oldStack.hasTag()?oldStack.getTag().copy():new net.minecraft.nbt.CompoundTag();
        var newTag=newStack.hasTag()?newStack.getTag().copy():new net.minecraft.nbt.CompoundTag();
        for(var aspect:tc4revival.aspect.TCAspects.catalog().primals()){oldTag.remove(aspect.tag());newTag.remove(aspect.tag());}
        WandUseTarget.clear(oldTag);WandUseTarget.clear(newTag);
        return !oldTag.equals(newTag);
    }
    @Override public int getUseDuration(ItemStack stack) { return Integer.MAX_VALUE; }
    @Override public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) { return net.minecraft.world.item.UseAnim.NONE; }
    @Override public void onUseTick(Level level, net.minecraft.world.entity.LivingEntity entity, ItemStack stack, int remainingTicks) {
        if(entity instanceof Player holder&&WandCasting.holdMode(holder,stack,remainingTicks))return;
        if (!(entity instanceof Player) || !thaumcraft.api.wands.WandAutomation.allowed(stack,(Player)entity)) return;
        try (var automation = thaumcraft.api.wands.WandAutomation.open(stack,(Player)entity)) {
        if (!(entity instanceof Player player)) return;
        var target = objectInUse(stack, level);
        if (target.isPresent()) target.get().onWandUseTick(stack, player, remainingTicks);
        else { try(var cast=WandCasting.open(player,stack,WandCasting.Action.HOLD_TICK)){
            var focus = cast.focus();
            if (!focus.isEmpty() && focus.getItem() instanceof WandFocusItem item && WandCooldowns.begin(player, focusCooldownMillis(stack,focus,item.cooldownMillis(focus))))
                for(int i=0;i<Math.max(1,Math.min(16,continuousFocusCalls(stack,focus)));i++) {
                    if(!player.isUsingItem() || player.getUseItem()!=stack)break;
                    item.usingTick(stack, player, remainingTicks);
                }
        }}
            }
    }
    @Override public void releaseUsing(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity, int remainingTicks) {
        if(entity instanceof Player holder&&WandCasting.releaseMode(holder,stack)){WandUseTarget.clear(stack.getTag());return;}
        if (!(entity instanceof Player) || !thaumcraft.api.wands.WandAutomation.allowed(stack,(Player)entity)) { WandUseTarget.clear(stack.getTag()); return; }
        try (var automation = thaumcraft.api.wands.WandAutomation.open(stack,(Player)entity)) {
        try {
            if (entity instanceof Player player) {
                var target = objectInUse(stack, level);
                if (target.isPresent()) target.get().onWandRelease(stack, level, player, remainingTicks);
                else {try(var cast=WandCasting.open(player,stack,WandCasting.Action.RELEASE)){
                    var focus = cast.focus();
                    if (!focus.isEmpty() && focus.getItem() instanceof WandFocusItem item) item.release(stack, level, player, remainingTicks);
                }}
            }
        } finally { WandUseTarget.clear(stack.getTag()); }
            }
    }
    @Override public boolean onEntitySwing(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        var player = entity instanceof Player value ? value : null;
        if (!thaumcraft.api.wands.WandAutomation.allowed(stack,player)) return true;
        try (var automation = thaumcraft.api.wands.WandAutomation.open(stack,player);var cast=WandCasting.open(player,stack,WandCasting.Action.SWING)) {
        var focus = cast.focus();
        // LocalPlayer.swing sends its animation packet during Item.use prediction,
        // before Minecraft sends the use-item packet. An animation must not claim
        // the server's cast cooldown, otherwise the actual cast is rejected.
        return !focus.isEmpty() && focus.getItem() instanceof WandFocusItem item && item.swing(stack, entity);
            }
    }
    @Override public boolean onBlockStartBreak(ItemStack stack, net.minecraft.core.BlockPos position, Player player) {
        if (!thaumcraft.api.wands.WandAutomation.allowed(stack,player)) return true;
        try (var automation = thaumcraft.api.wands.WandAutomation.open(stack,player);var cast=WandCasting.open(player,stack,WandCasting.Action.START_BREAK)) {
        var focus = cast.focus();
        return !focus.isEmpty() && focus.getItem() instanceof WandFocusItem item && item.startBreak(stack, position, player);
            }
    }
}
