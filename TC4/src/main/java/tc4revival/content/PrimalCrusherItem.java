package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.*;

public  class PrimalCrusherItem extends DiggerItem implements RepairableItem,WarpingGear {
    private static final Tier TIER=new Tier() {
        public int getUses() { return 500; }
        public float getSpeed() { return 8; }
        public float getAttackDamageBonus() { return 4; }
        public int getLevel() { return 5; }
        public int getEnchantmentValue() { return 20; }
        public Ingredient getRepairIngredient() { return Ingredient.of(TCItems.PRIMAL_CHARM.get()); }
    };
    private final java.util.Map<Player,Direction> sides=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    public PrimalCrusherItem() { super(3.5f,-2.8f,TIER,BlockTags.MINEABLE_WITH_PICKAXE,new Properties().rarity(Rarity.EPIC)); }
    @Override public int warp(ItemStack stack,Player player) { return 2; }
    @Override public boolean canPerformAction(ItemStack stack,ToolAction action) { return action==ToolActions.PICKAXE_DIG || ToolActions.DEFAULT_SHOVEL_ACTIONS.contains(action); }
    @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return Items.DIAMOND_SHOVEL.useOn(context);
    }
    @Override public boolean isCorrectToolForDrops(BlockState state) {
        return !wood(state) && !state.is(BlockTags.LEAVES) && !(state.getBlock() instanceof BushBlock)
                && !(state.getBlock() instanceof SugarCaneBlock) && !(state.getBlock() instanceof CocoaBlock)
                && !(state.getBlock() instanceof ManaPodBlock);
    }
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state) { return isCorrectToolForDrops(state); }
    private static boolean wood(BlockState state) {
        var block=state.getBlock();
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS) || state.is(BlockTags.WOODEN_BUTTONS)
                || state.is(BlockTags.WOODEN_PRESSURE_PLATES) || block instanceof SignBlock || block instanceof BannerBlock
                || block instanceof WallBannerBlock || block instanceof ChestBlock) return true;
        if (state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.BOOKSHELF) || state.is(Blocks.JUKEBOX)
                || state.is(Blocks.NOTE_BLOCK) || state.is(Blocks.DAYLIGHT_DETECTOR)) return true;
        // TC4 wood machinery uses the corresponding modern mineable tag.
        var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block);
        return id!=null && id.getNamespace().equals("thaumcraft") && state.is(BlockTags.MINEABLE_WITH_AXE);
    }
    private static boolean effective(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.RAILS) || state.is(Blocks.ICE) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.FARMLAND) || state.is(Blocks.SOUL_SAND)
                || state.is(TCArcane.TAINT_SOIL.get()) || state.is(TCArcane.FLESH_BLOCK.get()) || state.is(TCArcane.TAINT_CRUST.get())
                || state.is(TCArcane.TAINT_FIBRES.get());
    }
    @Override public float getDestroySpeed(ItemStack stack,BlockState state) { return effective(state)?8:1; }
    @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos pos,Player player) {
        var eye=player.getEyePosition();
        var hit=player.level().clip(new ClipContext(eye,eye.add(player.getLookAngle().scale(10)),ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,player));
        if (hit.getType()==HitResult.Type.BLOCK) sides.put(player,hit.getDirection());
        return false;
    }
    @Override public boolean mineBlock(ItemStack stack,Level level,BlockState state,BlockPos pos,LivingEntity user) {
        if (user.isShiftKeyDown()) return super.mineBlock(stack,level,state,pos,user);
        if (user instanceof ServerPlayer player && effective(state)) {
            for (var target:ElementalShovelLayout.plane(pos,sides.getOrDefault(player,Direction.getNearest(player.getLookAngle().x,player.getLookAngle().y,player.getLookAngle().z)),0,player.getYRot())) {
                if (!level.hasChunkAt(target) || !level.mayInteract(player,target)) continue;
                var candidate=level.getBlockState(target);
                if (candidate.getDestroySpeed(level,target)<0 || !effective(candidate)) continue;
                stack.hurtAndBreak(1,user,owner->owner.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                ElementalHarvest.harvest(player,target,2);
            }
        }
        return true;
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
        super.inventoryTick(stack,level,entity,slot,selected); VoidEquipment.repair(stack,entity);
    }
}
