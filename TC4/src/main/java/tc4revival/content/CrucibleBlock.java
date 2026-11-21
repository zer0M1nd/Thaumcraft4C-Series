package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public  class CrucibleBlock extends BaseEntityBlock {
    public static final TagKey<Block> BELLOWS=TagKey.create(Registries.BLOCK,new ResourceLocation("thaumcraft","bellows"));
    private static final VoxelShape COLLISION=Shapes.or(box(0,0,0,16,5,16),box(0,0,0,2,13.6,16),box(14,0,0,16,13.6,16),
            box(0,0,0,16,13.6,2),box(0,0,14,16,13.6,16));
    public CrucibleBlock() { super(Properties.of().strength(3,17).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.block(); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return COLLISION; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new CrucibleBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,TCArcane.CRUCIBLE_ENTITY.get(),(world,pos,block,crucible)->{
            if (world.isClientSide) tc4revival.client.CrucibleEffects.tick(crucible); else crucible.tick();
        });
    }
    public static InteractionResult form(ItemStack wand,UseOnContext context) {
        var level=context.getLevel(); var pos=context.getClickedPos(); var state=level.getBlockState(pos);
        if (!state.is(Blocks.CAULDRON) && !state.is(Blocks.WATER_CAULDRON)) return InteractionResult.PASS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        if (!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,context.getClickedFace(),wand)) return InteractionResult.FAIL;
        if (!level.setBlock(pos,TCArcane.CRUCIBLE.get().defaultBlockState(),3)) return InteractionResult.FAIL;
        if(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)crucible.claimAutomation(player);
        level.playSound(null,pos,TCSounds.WAND.get(),SoundSource.BLOCKS,1,1);
        level.blockEvent(pos,TCArcane.CRUCIBLE.get(),1,1); return InteractionResult.CONSUME;
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) return;
        if (entity instanceof ItemEntity item && !(item instanceof SpecialItemEntity) && crucible.contents().boiling() && crucible.contents().water()>0) crucible.dissolve(item);
        else if (entity.tickCount%10==0) {
            if (entity instanceof LivingEntity && crucible.contents().boiling() && crucible.contents().water()>0) {
                entity.hurt(level.damageSources().inFire(),1);
                level.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.4f,2+level.random.nextFloat()*.4f);
            }
        }
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) return InteractionResult.PASS;
        crucible.claimAutomation(player);
        var held=player.getItemInHand(hand); if (held.isEmpty()) return InteractionResult.PASS;
        if (held.is(Items.WATER_BUCKET)) {
            // Match vanilla cauldrons: empty the whole bucket and spill excess water.
            // Generic fluid containers below still transfer exactly the accepted amount.
            if (crucible.contents().water()>=CrucibleContents.WATER_CAPACITY) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            crucible.fluids().fill(new FluidStack(Fluids.WATER,net.minecraftforge.fluids.FluidType.BUCKET_VOLUME),FluidAction.EXECUTE);
            player.setItemInHand(hand,net.minecraft.world.item.ItemUtils.createFilledResult(held,player,new ItemStack(Items.BUCKET)));
        } else if (held.is(Items.POTION) && PotionUtils.getPotion(held)==Potions.WATER) {
            // Forge defines a bottle as 250 mB. Indivisible containers require room for the whole bottle.
            var water=new FluidStack(Fluids.WATER,net.minecraftforge.fluids.FluidType.BUCKET_VOLUME/4);
            if (crucible.fluids().fill(water,FluidAction.SIMULATE)!=water.getAmount()) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            crucible.fluids().fill(water,FluidAction.EXECUTE);
            player.setItemInHand(hand,net.minecraft.world.item.ItemUtils.createFilledResult(held,player,new ItemStack(Items.GLASS_BOTTLE)));
        } else {
            var result=FluidUtil.tryEmptyContainerAndStow(held,crucible.fluids(),
                    new net.minecraftforge.items.wrapper.InvWrapper(player.getInventory()),Integer.MAX_VALUE,player,!level.isClientSide);
            if (!result.isSuccess()) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            player.setItemInHand(hand,result.getResult());
        }
        player.containerMenu.broadcastChanges();
        level.playSound(null,pos,SoundEvents.GENERIC_SWIM,SoundSource.BLOCKS,.33f,1+(level.random.nextFloat()-level.random.nextFloat())*.3f);
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!next.is(state.getBlock())&&level.getBlockEntity(pos) instanceof CrucibleBlockEntity host)
            host.extensions().beforeRemove(moving?thaumcraft.api.devices.DeviceExtensions.Removal.MOVED:thaumcraft.api.devices.DeviceExtensions.Removal.REPLACED);

        if (!next.is(this) && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) { crucible.automation().drop(); crucible.empty(); }
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level,pos,state,placer,stack);
        if(placer instanceof Player player && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)crucible.claimAutomation(player);
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if (random.nextInt(10)==0 && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible && crucible.contents().boiling() && crucible.contents().water()>0)
            level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),SoundEvents.LAVA_POP,SoundSource.BLOCKS,.1f+random.nextFloat()*.1f,1.2f+random.nextFloat()*.2f,false);
    }
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) return 0;
        long amount=crucible.contents().aspects().total();
        return net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.floor((float)amount/100.0f*14.0f)+(amount>0 ? 1 : 0),0,15);
    }
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int parameter) {
        if (id==1 || id==2) { if (level.isClientSide && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) tc4revival.client.CrucibleEffects.event(crucible,id,parameter); return true; }
        return super.triggerEvent(state,level,pos,id,parameter);
    }
}
