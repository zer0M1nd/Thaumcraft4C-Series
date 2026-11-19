package thmodern.node;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.*;
import tc4revival.research.KnowledgeSavedData;
import thmodern.Horizons;

public final class SyntheticNodeEntity extends VisNetworkNodeBlockEntity implements WandDevice,ScannableNode,RevealedAspectContainer {
    private AspectAmounts capacity=new AspectAmounts(),charge=new AspectAmounts(),fractions=new AspectAmounts();
    public SyntheticNodeEntity(BlockPos pos,BlockState state){super(Horizons.SYNTHETIC_NODE_ENTITY.get(),pos,state);}
    @Override public int range(){return 8;}
    @Override public boolean source(){return false;}
    @Override public AspectAmounts aspects(){return charge.copy();}
    @Override public AspectAmounts revealedAspects(){return aspects();}
    public AspectAmounts capacity(){return capacity.copy();}
    @Override public AuraNodeType nodeType(){return AuraNodeType.NORMAL;}
    @Override public String nodeId(){return "SYNTHETIC";}
    public boolean addEssence(ItemStack stack){
        if(level==null||level.isClientSide||!stack.is(TCItems.WISP_ESSENCE.get()))return false;
        var aspects=TCItems.WISP_ESSENCE.get().storedAspects(stack).aspects();
        if(aspects.size()!=1)return false;
        var aspect=aspects.get(0);
        int accepted=Math.min(stack.getCount(),(Integer.MAX_VALUE-capacity.amount(aspect))/4);
        if(accepted<=0)return false;
        capacity.add(aspect,accepted*4);charge.add(aspect,0);fractions.add(aspect,0);stack.shrink(accepted);sync();return true;
    }
    public void dropEssences(){
        if(level==null||level.isClientSide)return;
        for(var aspect:capacity.aspects())for(int count=capacity.amount(aspect)/4;count>0;){
            var stack=TCItems.WISP_ESSENCE.get().of(aspect);int n=Math.min(count,stack.getMaxStackSize());stack.setCount(n);
            net.minecraft.world.level.block.Block.popResource(level,worldPosition,stack);count-=n;
        }
        capacity=new AspectAmounts();charge=new AspectAmounts();fractions=new AspectAmounts();setChanged();
    }
    public void tick(){
        if(level==null||level.isClientSide)return;tickNetwork();boolean changed=false;
        for(var aspect:capacity.byAmount()){
            if(charge.amount(aspect)>=capacity.amount(aspect))continue;
            var primals=TCAspects.catalog().toPrimals(new AspectAmounts().add(aspect,1),AspectCatalog.Reduction.SUM);
            int received=10;
            for(var primal:primals.aspects())received=Math.min(received,VisNetwork.drain(level,worldPosition,primal,10));
            if(received<=0||primals.isEmpty())continue;
            int fractional=fractions.amount(aspect)+received;
            int whole=Math.min(capacity.amount(aspect)-charge.amount(aspect),fractional/100);
            charge.add(aspect,whole);fractions.remove(aspect);fractions.add(aspect,fractional%100);changed=true;
        }
        if(changed){setChanged();if(level.getGameTime()%20==0)sync();}
    }
    @Override public void onWandUseTick(ItemStack stack,Player player,int remainingTicks){
        if(!lookingAt(player)||level==null||level.isClientSide||remainingTicks%5!=0
                ||!(stack.getItem() instanceof CastingWandItem wand)||!(player instanceof ServerPlayer server))return;
        var research=KnowledgeSavedData.get(server.getServer()).snapshot(player.getUUID()).map(v->v.researchMarkers()).orElse(java.util.Set.of());
        int rate=1+(research.contains("NODETAPPER1")?1:0)+(research.contains("NODETAPPER2")?1:0);
        boolean preserve=!player.isShiftKeyDown()&&research.contains("NODEPRESERVE")
                &&WandParts.rod(stack.getTag())!=WandParts.Rod.WOOD&&WandParts.cap(stack.getTag())!=WandParts.Cap.IRON;
        int floor=preserve?1:0;
        var candidates=charge.primals().stream().filter(a->charge.amount(a)>floor&&wand.vis(stack,a)<wand.capacity(stack)).toList();
        if(candidates.isEmpty())return;
        var aspect=candidates.get(level.random.nextInt(candidates.size()));
        int wanted=Math.min(rate,charge.amount(aspect)-floor),left=wand.addVis(stack,aspect,wanted,true,true);
        if(left<wanted){charge.reduce(aspect,wanted-left);sync();}
    }
    private void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);AspectNbt.write(tag,"Capacity",capacity);AspectNbt.write(tag,"Charge",charge);AspectNbt.write(tag,"Fractions",fractions);}
    @Override public void load(CompoundTag tag){
        super.load(tag);capacity=AspectNbt.read(tag,"Capacity",TCAspects.catalog()).amounts();
        var savedCharge=AspectNbt.read(tag,"Charge",TCAspects.catalog()).amounts();var savedFraction=AspectNbt.read(tag,"Fractions",TCAspects.catalog()).amounts();
        charge=new AspectAmounts();fractions=new AspectAmounts();
        for(var aspect:capacity.aspects()){
            if(capacity.amount(aspect)<=0){capacity.remove(aspect);continue;}
            charge.add(aspect,Math.max(0,Math.min(capacity.amount(aspect),savedCharge.amount(aspect))));
            fractions.add(aspect,Math.max(0,Math.min(99,savedFraction.amount(aspect))));
        }
    }
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
