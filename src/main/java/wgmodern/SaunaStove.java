package wgmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class SaunaStove extends BaseEntityBlock {
    private static final VoxelShape SHAPE=Shapes.or(Block.box(0,0,0,16,12,16),Block.box(0,12,0,16,16,3),Block.box(0,12,13,16,16,16),Block.box(0,12,3,3,16,13),Block.box(13,12,3,16,16,13));
    public SaunaStove(){super(Properties.copy(Blocks.OAK_PLANKS).strength(2.5f,6).noOcclusion().lightLevel(s->8));}
    @Override public String getDescriptionId(){return "tile.WG_WoodenDevice.saunaStove";}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return createTickerHelper(type,Content.SAUNA_TILE.get(),Tile::tick);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){if(l.getBlockEntity(pos) instanceof Tile t&&FluidUtil.interactWithFluidHandler(p,hand,t.tank))return InteractionResult.sidedSuccess(l.isClientSide);return InteractionResult.PASS;}
    @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource r){if(l.getBlockEntity(p) instanceof Tile t&&t.fuel>0&&r.nextInt(7)==0)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->WGEffects.ember(p));}
    public static class Tile extends BlockEntity {
        public final FluidTank tank=new FluidTank(4000,f->f.getFluid()==net.minecraft.world.level.material.Fluids.WATER){@Override protected void onContentsChanged(){changed();}};
        private LazyOptional<IFluidHandler> capability=LazyOptional.of(()->tank);
        public int fuel;private final Set<BlockPos> area=new HashSet<>(),checked=new HashSet<>();private final ArrayDeque<BlockPos> open=new ArrayDeque<>();private boolean checking;
        public Tile(BlockPos p,BlockState s){super(Content.SAUNA_TILE.get(),p,s);}
        public static void tick(Level l,BlockPos p,BlockState s,Tile tile){if(l.isClientSide){if(tile.fuel>0&&l.getGameTime()%10==0)tile.fuel--;return;}if(tile.fuel>0&&l.getGameTime()%10==0){tile.fuel--;tile.setChanged();if(tile.area.isEmpty()&&!tile.checking)tile.recheck();}if(!tile.checking&&tile.fuel<=0&&tile.tank.getFluidAmount()>=1000){tile.tank.drain(1000,IFluidHandler.FluidAction.EXECUTE);tile.fuel=1200;tile.recheck();tile.changed();}if(tile.checking)tile.scan();if(tile.fuel>0&&!tile.checking)for(var player:l.getEntitiesOfClass(ServerPlayer.class,new AABB(p).inflate(8,6,8)))if(tile.contains(player)){if(!player.isInWater())player.addEffect(new MobEffectInstance(tc4revival.content.TCEffects.WARP_WARD.get(),20,0,true,false));if(l.random.nextInt(100)==0&&player.getFoodData().getFoodLevel()>6)player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel()-1);if(l.random.nextInt(200)==0)thaumcraft.api.ThaumcraftApiHelper.addWarpToPlayer(player,-1,true);if(l.getGameTime()%20==0)WGNetwork.sauna(player);}}
        public boolean contains(Player player){var box=player.getBoundingBox();for(var p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(area.contains(p))return true;return false;}
        public void recheck(){checking=true;area.clear();checked.clear();open.clear();for(var d:Direction.values())if(d!=Direction.DOWN)open.add(worldPosition.relative(d));}
        public void scan(){for(int i=0;i<30&&!open.isEmpty()&&area.size()<1200;i++){var pos=open.removeFirst();if(!checked.add(pos)||!level.hasChunkAt(pos)||!passable(pos))continue;int dx=pos.getX()-worldPosition.getX(),dz=pos.getZ()-worldPosition.getZ(),dy=pos.getY()-worldPosition.getY();boolean water=false;for(int y=-6;y<6;y++)if(level.getFluidState(new BlockPos(pos.getX(),worldPosition.getY()+y,pos.getZ())).is(FluidTags.WATER)){water=true;break;}if(dx*dx+dz*dz>=64||dy<=-6||dy>=(water?2:6))continue;area.add(pos.immutable());for(var d:Direction.values()){var next=pos.relative(d);if(!checked.contains(next))open.add(next);}}if(area.size()>=1200||open.isEmpty()){checking=false;open.clear();}}
        private boolean passable(BlockPos pos){return level.getBlockState(pos).getCollisionShape(level,pos).isEmpty();}
        public Set<BlockPos> steamArea(){return Set.copyOf(area);}
        private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){if(cap==ForgeCapabilities.FLUID_HANDLER&&side!=Direction.UP)return capability.cast();return super.getCapability(cap,side);}
        @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
        @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(()->tank);}
        @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("fuel",fuel);tag.put("tank",tank.writeToNBT(new CompoundTag()));}
        @Override public void load(CompoundTag tag){super.load(tag);fuel=Math.max(0,tag.getInt("fuel"));tank.readFromNBT(tag.getCompound("tank"));}
        @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    }
}
