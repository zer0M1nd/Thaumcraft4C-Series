package tbmodern;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import tc4revival.aspect.*;
import thaumcraft.api.ThaumcraftApiHelper;

public final class EntityDeconstructor extends BlockEntity {
    private UUID owner;
    private int crystals,progress;
    public EntityDeconstructor(BlockPos pos,BlockState state) { super(Content.DECONSTRUCTOR.get(),pos,state); }
    public int crystals() { return crystals; }
    public static void tick(Level level,BlockPos pos,BlockState state,EntityDeconstructor device) {
        if(!(level instanceof ServerLevel server) || device.owner==null)return;
        int stability=Integer.bitCount(device.crystals & 63);
        var player=server.getServer().getPlayerList().getPlayer(device.owner);
        if(player==null || player.level()!=level || player.distanceToSqr(pos.getX()+.5,pos.getY(),pos.getZ()+.5)>=Math.pow(32+stability*8,2)) {
            device.progress=0;return;
        }
        var entities=level.getEntities(null,new AABB(pos));
        if(entities.isEmpty()) { device.progress=0;return; }
        var target=entities.get(0);if(!target.isAlive()) {device.progress=0;return;}
        AspectAmounts amounts;
        if(target instanceof ItemEntity item) {
            if(stability<6) {device.progress=0;return;}
            amounts=tc4revival.aspect.item.ItemStackAspects.resolve(item.getItem()).orElseGet(AspectAmounts::new);
        } else amounts=tc4revival.aspect.entity.EntityAspects.resolve(target).map(result->result.aspects()).orElseGet(AspectAmounts::new);
        if(amounts.isEmpty()) {device.progress=0;return;}
        device.progress+=target instanceof ItemEntity?1:1+stability/2;
        if(device.progress>=40) {
            device.progress=0;Aspect reward=null;
            if(target instanceof ItemEntity item) {
                var primals=TCAspects.catalog().toPrimals(amounts,AspectCatalog.Reduction.SUM);
                item.getItem().shrink(1);if(item.getItem().isEmpty())item.discard();else item.setItem(item.getItem());
                if(!primals.isEmpty() && level.random.nextInt(40)<primals.total())reward=primals.aspects().get(level.random.nextInt(primals.size()));
            } else {
                // Knowledge comes from actual damage, never cancelled or invulnerable hits.
                float health=target instanceof net.minecraft.world.entity.LivingEntity living?living.getHealth()+living.getAbsorptionAmount():0;
                if(!target.hurt(level.damageSources().magic(),2)
                        || target instanceof net.minecraft.world.entity.LivingEntity living && living.getHealth()+living.getAbsorptionAmount()>=health)return;
                if(target instanceof net.minecraft.server.level.ServerPlayer victim)ThaumcraftApiHelper.addWarpToPlayer(victim,1,true);
                reward=amounts.aspects().get(level.random.nextInt(amounts.size()));
            }
            if(reward!=null)
                ThaumcraftApiHelper.addResearchPoints(player,thaumcraft.api.aspects.Aspect.wrap(reward),(short)1);
        }
        if(level.getGameTime()%4==0)thaumcraft.api.fx.LegacyEffects.emit(level,"rune",new net.minecraft.world.phys.Vec3(pos.getX(),pos.getY()+(level.random.nextDouble()-.5),pos.getZ()),net.minecraft.world.phys.Vec3.ZERO,0xff8080,1,8,1,0,0);
    }
    private void sync() {setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag) {super.saveAdditional(tag);if(owner!=null)tag.putUUID("Owner",owner);tag.putInt("Crystals",crystals);}
    @Override public void load(CompoundTag tag) {super.load(tag);owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;crystals=tag.getInt("Crystals")&63;progress=0;}
    @Override public CompoundTag getUpdateTag() {return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    public static final class Device extends BaseEntityBlock {
        public Device() {super(Properties.of().strength(1).noOcclusion());}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new EntityDeconstructor(pos,state);}
        @Override public RenderShape getRenderShape(BlockState state) {return RenderShape.MODEL;}
        @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return Block.box(0,0,0,16,1.6,16);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
            return level.isClientSide?null:createTickerHelper(type,Content.DECONSTRUCTOR.get(),EntityDeconstructor::tick);
        }
        @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
            if(player instanceof net.minecraft.server.level.ServerPlayer server && level.getBlockEntity(pos) instanceof EntityDeconstructor device) {
                var stack=player.getItemInHand(hand);
                for(int i=0;i<6;i++)if(stack.is(Content.item(Content.CRYSTALS.get(i))) && (device.crystals&(1<<i))==0
                    && ThaumcraftApiHelper.isResearchComplete(server,"TB.EntityDecAdv")) {
                    device.crystals|=1<<i;device.progress=0;stack.shrink(1);break;
                }
                device.owner=player.getUUID();device.sync();
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tb.txt.linkEntDecon").withStyle(net.minecraft.ChatFormatting.AQUA,net.minecraft.ChatFormatting.ITALIC),false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}
