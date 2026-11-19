package ttmodern;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.items.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.*;

/** Each tablet owns its fake player; neither inventories nor mining progress are shared. */
public final class TabletEntity extends BlockEntity {
    private boolean leftClick=true,redstone;
    private int swing,step;
    private FakePlayer actor;
    private BlockPos breaking;
    private BlockState breakingState;
    private ItemStack breakingTool=ItemStack.EMPTY;
    private float progress;
    public final ItemStackHandler items=new ItemStackHandler(1){@Override protected void onContentsChanged(int slot){changed();}};
    private LazyOptional<IItemHandler> capability=LazyOptional.of(()->items);
    public TabletEntity(BlockPos pos,BlockState state){super(Tinkerer.TABLET_ENTITY.get(),pos,state);}
    public boolean leftClick(){return leftClick;}
    public boolean redstone(){return redstone;}
    public void configure(boolean left,boolean power){if(leftClick!=left)stopBreaking();leftClick=left;redstone=power;changed();}
    public boolean idle(){return swing==0&&breaking==null;}
    public boolean trigger(){if(level==null||level.isClientSide||!idle()||items.getStackInSlot(0).isEmpty())return false;swing=3;step=3;changed();return true;}
    public void rotation(Direction direction){if(!direction.getAxis().isHorizontal())throw new IllegalArgumentException("Horizontal direction required");stopBreaking();if(level!=null)level.setBlock(worldPosition,getBlockState().setValue(TabletBlock.FACING,direction),3);}
    public float swing(float partial){return Math.max(0,Math.min(45,swing+step*partial));}
    public boolean usable(Player player){return level!=null&&!isRemoved()&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    private boolean powered(){return level.hasNeighborSignal(worldPosition)||level.hasNeighborSignal(worldPosition.above());}
    private FakePlayer actor(){
        if(actor==null){actor=new FakePlayer((ServerLevel)level,new GameProfile(java.util.UUID.fromString("a8f026a0-135b-11e4-9191-0800200c9a66"),"[ThaumcraftTablet]")){
            @Override public float getAttackStrengthScale(float partial){return 1;}
        };actor.setGameMode(GameType.SURVIVAL);}
        actor.moveTo(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,getBlockState().getValue(TabletBlock.FACING).toYRot(),-15);
        thaumcraft.api.wands.WandActors.bind(actor,new thaumcraft.api.wands.WandActors.ActorContext(actor,java.util.Optional.of(
            new thaumcraft.api.wands.WandActors.Origin(new net.minecraft.resources.ResourceLocation("thaumic_tinkerer","tablet"),
                net.minecraft.core.GlobalPos.of(level.dimension(),worldPosition),null,null))));
        actor.setOnGround(true);actor.getInventory().selected=0;actor.setItemInHand(InteractionHand.MAIN_HAND,items.getStackInSlot(0));return actor;
    }
    private HitResult target(){
        Direction face=getBlockState().getValue(TabletBlock.FACING);BlockPos front=worldPosition.relative(face);if(!level.hasChunkAt(front))return null;
        Vec3 start=worldPosition.getCenter();var shape=level.getBlockState(front).getShape(level,front);
        HitResult result=shape.isEmpty()?null:shape.clip(start,shape.bounds().getCenter().add(front.getX(),front.getY(),front.getZ()),front);
        double distance=result==null?Double.MAX_VALUE:start.distanceToSqr(result.getLocation());
        for(Entity entity:level.getEntities((Entity)null,new AABB(front),e->e.isAlive()&&e.isPickable()&&e!=actor)){
            var hit=entity.getBoundingBox().inflate(entity.getPickRadius()).clip(start,entity.getBoundingBox().getCenter());
            if(hit.isPresent()&&start.distanceToSqr(hit.get())<distance){result=new EntityHitResult(entity,hit.get());distance=start.distanceToSqr(hit.get());}
        }
        if(result==null&&!leftClick&&!level.getBlockState(front.below()).getShape(level,front.below()).isEmpty())
            result=new BlockHitResult(new Vec3(front.getX()+.5,front.getY()-1+level.getBlockState(front.below()).getShape(level,front.below()).bounds().maxY,front.getZ()+.5),Direction.UP,front.below(),false);
        return result;
    }
    public void tick(){
        if(level==null)return;
        if(level.isClientSide){animate();return;}
        if(items.getStackInSlot(0).isEmpty()){stopBreaking();if(swing!=0){swing=step=0;changed();}return;}
        var hit=target();
        if(breaking!=null){
            if(!(hit instanceof BlockHitResult block)||!block.getBlockPos().equals(breaking)||!level.getBlockState(breaking).equals(breakingState)||!ItemStack.isSameItemSameTags(items.getStackInSlot(0),breakingTool))stopBreaking();
            else advanceBreaking();
        }
        if(swing>0){animate();if(swing==45&&step<0)perform(hit);return;}
        if(redstone&&!powered()&&breaking==null)return;
        if(hit==null)return;
        swing=3;step=3;changed();
    }
    private void animate(){if(step==0)return;swing+=step;if(swing>=45){swing=45;step=-3;}else if(swing<=0){swing=step=0;}}
    private void perform(HitResult hit){
        var player=actor();var held=player.getMainHandItem();var modifiers=held.getAttributeModifiers(EquipmentSlot.MAINHAND);
        player.getAttributes().addTransientAttributeModifiers(modifiers);
        try{
            if(leftClick){
                if(hit instanceof EntityHitResult entity)player.attack(entity.getEntity());
                else if(hit instanceof BlockHitResult block&&breaking==null)startBreaking(player,block);
            }else{
                InteractionResult result=InteractionResult.PASS;
                if(hit instanceof EntityHitResult entity)result=player.interactOn(entity.getEntity(),InteractionHand.MAIN_HAND);
                else if(hit instanceof BlockHitResult block)result=player.gameMode.useItemOn(player,(ServerLevel)level,player.getMainHandItem(),InteractionHand.MAIN_HAND,block);
                if(result==InteractionResult.PASS)player.gameMode.useItem(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND);
            }
        }finally{player.getAttributes().removeAttributeModifiers(modifiers);storeInventory();}
    }
    private void storeInventory(){
        if(actor==null)return;
        items.setStackInSlot(0,actor.getMainHandItem());actor.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        for(int i=1;i<actor.getInventory().getContainerSize();i++){
            var stack=actor.getInventory().removeItemNoUpdate(i);if(!stack.isEmpty())Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,stack);
        }
        // A machine must not retain a stale menu opened by right-clicking a container.
        if(actor.containerMenu!=actor.inventoryMenu)actor.closeContainer();
    }
    private void startBreaking(FakePlayer player,BlockHitResult hit){
        var pos=hit.getBlockPos();var state=level.getBlockState(pos);
        if(state.isAir()||state.getDestroySpeed(level,pos)<0||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),player.getMainHandItem()))return;
        var event=net.minecraftforge.common.ForgeHooks.onLeftClickBlock(player,pos,hit.getDirection(),net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
        if(event.isCanceled()||event.getUseItem()==net.minecraftforge.eventbus.api.Event.Result.DENY)return;
        if(event.getUseBlock()!=net.minecraftforge.eventbus.api.Event.Result.DENY)state.attack(level,pos,player);
        if(!level.getBlockState(pos).equals(state))return;
        breaking=pos.immutable();breakingState=state;breakingTool=player.getMainHandItem().copy();progress=0;
    }
    private void advanceBreaking(){
        var player=actor();try{
            progress+=breakingState.getDestroyProgress(player,level,breaking);
            if(progress>=1){var pos=breaking;stopBreaking();player.gameMode.destroyBlock(pos);}
            else level.destroyBlockProgress(player.getId(),breaking,Math.min(9,(int)(progress*10)));
        }finally{storeInventory();}
    }
    public void stopBreaking(){if(breaking!=null&&level!=null&&actor!=null)level.destroyBlockProgress(actor.getId(),breaking,-1);breaking=null;breakingState=null;breakingTool=ItemStack.EMPTY;progress=0;}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Items",items.serializeNBT());tag.putBoolean("Left",leftClick);tag.putBoolean("Redstone",redstone);tag.putInt("Swing",swing);tag.putInt("Step",step);}
    @Override public void load(CompoundTag tag){super.load(tag);items.deserializeNBT(tag.getCompound("Items"));leftClick=!tag.contains("Left")||tag.getBoolean("Left");redstone=tag.getBoolean("Redstone");swing=Math.max(0,Math.min(45,tag.getInt("Swing")));step=swing==0?0:tag.getInt("Step")<0?-3:3;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ITEM_HANDLER&&!isRemoved()?capability.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(()->items);}
    @Override public void setRemoved(){stopBreaking();actor=null;super.setRemoved();}
}
