package tbmodern;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.util.FakePlayerFactory;

public final class SpikeBlock extends Block {
    public static final BooleanProperty BLOODY=BooleanProperty.create("bloody");
    private static final GameProfile PROFILE=new GameProfile(UUID.fromString("ade919b4-0572-4875-80be-fb80bf8f20ca"),"[TB]Spikes");
    private final String kind;
    public SpikeBlock(String kind) { super(Properties.of().strength(1).noOcclusion());this.kind=kind;registerDefaultState(stateDefinition.any().setValue(BLOODY,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(BLOODY); }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Block.box(0,0,0,16,15,16); }
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) { return !level.isEmptyBlock(pos.below()); }
    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        return direction==Direction.DOWN && !canSurvive(state,level,pos)?Blocks.AIR.defaultBlockState():super.updateShape(state,direction,neighbor,level,pos,neighborPos);
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if(!(level instanceof ServerLevel server))return;
        if(!kind.equals("iron") && level.hasNeighborSignal(pos))return;
        if(kind.equals("thaumic") && entity instanceof ItemEntity)return;
        if(kind.equals("void")) {
            if(entity instanceof ItemEntity || entity instanceof ExperienceOrb)return;
            if(entity instanceof Player player && player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof tc4revival.content.VoidArmorItem)return;
            var fake=FakePlayerFactory.get(server,PROFILE);fake.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
            entity.hurt(level.damageSources().playerAttack(fake),20);
        } else entity.hurt(level.damageSources().cactus(),kind.equals("iron")?8:14);
        if(!state.getValue(BLOODY) && entity instanceof LivingEntity living && living.getHealth()<=0)level.setBlock(pos,state.setValue(BLOODY,true),3);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        ItemStack held=player.getItemInHand(hand);
        if(!state.getValue(BLOODY) || !held.is(Content.item("thaumcraft:enchanted_fabric")))return InteractionResult.PASS;
        if(!level.isClientSide) {
            held.shrink(1);var cloth=new ItemStack(Content.item("bloody_cloth"));
            if(!player.addItem(cloth))player.drop(cloth,false);
            level.setBlock(pos,state.setValue(BLOODY,false),3);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
