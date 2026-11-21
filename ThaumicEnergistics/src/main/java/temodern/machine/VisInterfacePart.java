package temodern.machine;

import java.util.UUID;
import appeng.api.config.*;
import appeng.api.networking.*;
import appeng.api.networking.ticking.*;
import appeng.api.parts.*;
import appeng.parts.*;
import appeng.api.implementations.items.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tc4revival.aspect.Aspect;
import thaumcraft.api.aspects.EssentiaBuffer;
import thaumcraft.api.visnet.*;

/** Each cable face owns a real TC endpoint, independently of its shared host block entity. */
public final class VisInterfacePart extends AEBasePart implements VisEndpoint,VisSupply,IGridTickable {
    public static final ResourceLocation[] MODELS=java.util.stream.Stream.of("input","output","creative").map(s->ResourceLocation.parse("thaumicenergistics:part/vis_"+s)).toArray(ResourceLocation[]::new);
    private final boolean creative;
    private boolean live,output;
    private Vec3 parentPoint;
    private int networkTicks;
    private UUID identity=UUID.randomUUID(),linked;
    private final EssentiaBuffer buffer=new EssentiaBuffer(60000,Aspect::isPrimal,this::save);
    public VisInterfacePart(IPartItem<?> item,boolean creative){
        super(item);this.creative=creative;
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(1).addService(IGridTickable.class,this);
    }
    private void save(){if(getHost()!=null)getHost().markForSave();}
    @Override public IManagedGridNode node(){return getMainNode();}
    @Override public UUID visIdentity(){return identity;}
    @Override public boolean visInput(){return !output;}
    @Override public Level level(){return getLevel();}
    @Override public Key key(){return new Key(getBlockEntity().getBlockPos(),ResourceLocation.parse("thaumicenergistics:vis/"+getSide().getName()));}
    @Override public Vec3 point(){return Vec3.atCenterOf(key().position()).add(Vec3.atLowerCornerOf(getSide().getNormal()).scale(.45));}
    @Override public boolean valid(){return live&&!getBlockEntity().isRemoved()&&getHost().getPart(getSide())==this;}
    @Override public int range(){return 8;}
    @Override public boolean source(){return creative||output;}
    @Override public int takeVis(Aspect aspect,int amount){return node().isActive()&&visInput()?buffer.extract(aspect,amount,false):0;}
    @Override public int drain(Aspect aspect,int amount){
        if(!source()||!node().isActive()||amount<=0||!aspect.isPrimal())return 0;
        if(creative)return amount;
        var supply=VisSupply.find(node().getGrid(),linked,this);return supply==null?0:supply.takeVis(aspect,amount);
    }
    @Override public void addToWorld(){super.addToWorld();live=true;if(!level().isClientSide)VisNetHandler.register(this);}
    @Override public void removeFromWorld(){if(!level().isClientSide)VisNetHandler.unregister(this);live=false;super.removeFromWorld();}
    @Override public TickingRequest getTickingRequest(IGridNode node){return new TickingRequest(1,1,false,false);}
    @Override public TickRateModulation tickingRequest(IGridNode ignored,int elapsed){
        if(networkTicks++%40==0)VisNetHandler.register(this);
        if(node().isActive()&&visInput())for(var aspect:tc4revival.aspect.TCAspects.catalog().primals()){
            int wanted=Math.min(100,10000-buffer.contents().amount(aspect));if(wanted<=0)continue;
            if(creative){buffer.insert(aspect,wanted,false);continue;}
            if(VisNetHandler.root(this) instanceof VisSupply supply&&supply.node().getGrid()==node().getGrid())continue;
            var power=node().getGrid().getEnergyService();
            if(power.extractAEPower(4,Actionable.SIMULATE,PowerMultiplier.CONFIG)>=4){
                int received=VisNetHandler.consume(this,thaumcraft.api.aspects.Aspect.wrap(aspect),wanted);
                if(received>0){power.extractAEPower(4,Actionable.MODULATE,PowerMultiplier.CONFIG);buffer.insert(aspect,received,false);}
            }
        }
        return TickRateModulation.SAME;
    }
    @Override public boolean useStandardMemoryCard(){return false;}
    @Override public boolean onPartActivate(Player player,InteractionHand hand,Vec3 pos){return memoryCard(player,player.getItemInHand(hand));}
    @Override public boolean onPartShiftActivate(Player player,InteractionHand hand,Vec3 pos){return memoryCard(player,player.getItemInHand(hand));}
    public boolean memoryCard(Player player,ItemStack stack){
        if(!(stack.getItem() instanceof IMemoryCard card))return false;
        if(level().isClientSide)return true;
        var message=MemoryCardMessages.INVALID_MACHINE;
        if(player.isShiftKeyDown()){
            output=false;linked=null;var tag=new CompoundTag();tag.putUUID("source",identity);
            card.setMemoryCardContents(stack,"gui.thaumicenergistics.vis_link",tag);message=MemoryCardMessages.SETTINGS_SAVED;
        }else if(card.getData(stack).isEmpty()){
            output=false;linked=null;message=MemoryCardMessages.SETTINGS_LOADED;
        }else if(card.getSettingsName(stack).equals("gui.thaumicenergistics.vis_link")&&card.getData(stack).hasUUID("source")){
            var id=card.getData(stack).getUUID("source");
            if(!creative&&VisSupply.find(node().getGrid(),id,this)!=null){linked=id;output=true;message=MemoryCardMessages.SETTINGS_LOADED;}
        }
        if(message!=MemoryCardMessages.INVALID_MACHINE){VisNetHandler.refresh(this);VisNetHandler.register(this);save();getHost().markForUpdate();}
        card.notifyUser(player,message);return true;
    }
    @Override public void writeToNBT(CompoundTag tag){super.writeToNBT(tag);tag.putUUID("identity",identity);if(linked!=null)tag.putUUID("linked",linked);tag.putBoolean("output",output);tag.put("vis",buffer.save());}
    @Override public void readFromNBT(CompoundTag tag){super.readFromNBT(tag);if(tag.hasUUID("identity"))identity=tag.getUUID("identity");linked=tag.hasUUID("linked")?tag.getUUID("linked"):null;output=tag.getBoolean("output");buffer.load(tag.getCompound("vis"));}
    @Override public void parentChanged(VisEndpoint parent){parentPoint=parent==null?null:parent.point();if(getHost()!=null)getHost().markForUpdate();}
    @Override public boolean requireDynamicRender(){return false;}
    @Override @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void renderDynamic(float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){

    }
    @Override public void writeToStream(net.minecraft.network.FriendlyByteBuf data){super.writeToStream(data);data.writeBoolean(output);data.writeBoolean(parentPoint!=null);if(parentPoint!=null){data.writeDouble(parentPoint.x);data.writeDouble(parentPoint.y);data.writeDouble(parentPoint.z);}}
    @Override public boolean readFromStream(net.minecraft.network.FriendlyByteBuf data){boolean changed=super.readFromStream(data);boolean old=output;output=data.readBoolean();var oldParent=parentPoint;parentPoint=data.readBoolean()?new Vec3(data.readDouble(),data.readDouble(),data.readDouble()):null;return changed||old!=output||!java.util.Objects.equals(oldParent,parentPoint);}
    @Override public void getBoxes(IPartCollisionHelper boxes){
        // AE2's collision helper measures depth from the opposite end to model JSON.
        boxes.addBox(6,6,14,10,10,16);
        boxes.addBox(4,4,12.5,12,12,14);
        boxes.addBox(5,5,11,11,11,12.5);
    }
    @Override public IPartModel getStaticModels(){return new PartModel(MODELS[creative?2:output?1:0]);}
}
