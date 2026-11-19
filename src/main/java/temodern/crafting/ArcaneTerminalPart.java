package temodern.crafting;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;
import appeng.parts.reporting.ItemTerminalPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ArcaneTerminalPart extends ItemTerminalPart implements WorkHost, appeng.api.networking.ticking.IGridTickable {
    public static final ResourceLocation FACE=ResourceLocation.fromNamespaceAndPath(temodern.TE.ID,"part/arcane_terminal");
    public static final ResourceLocation FACE_OFF=ResourceLocation.fromNamespaceAndPath(temodern.TE.ID,"part/arcane_terminal_off");
    public static final ResourceLocation BASE=ResourceLocation.fromNamespaceAndPath(temodern.TE.ID,"part/arcane_terminal_base");
    private static final IPartModel MODEL=new PartModel(BASE,FACE,MODEL_STATUS_HAS_CHANNEL);
    private static final IPartModel POWERED=new PartModel(BASE,FACE,MODEL_STATUS_ON);
    private static final IPartModel OFF=new PartModel(BASE,FACE_OFF,MODEL_STATUS_OFF);
    private final Workstation work=new Workstation(this);
    private java.util.UUID linkedVis;
    public ArcaneTerminalPart(IPartItem<?> item){super(item);getMainNode().addService(appeng.api.networking.ticking.IGridTickable.class,this);}
    @Override public net.minecraft.world.level.Level workLevel(){return getLevel();}
    @Override public Workstation workstation(){return work;}
    @Override public String kind(){return "arcane_terminal";}
    @Override public net.minecraft.core.BlockPos workPosition(){return getBlockEntity().getBlockPos();}
    @Override public void markWorkChanged(){if(getHost()!=null)getHost().markForSave();}
    @Override public appeng.api.networking.IManagedGridNode node(){return getMainNode();}
    @Override public appeng.api.networking.security.IActionSource action(){return appeng.api.networking.security.IActionSource.ofMachine(this);}
    @Override public boolean consumeNetworkVis(java.util.Map<tc4revival.aspect.Aspect,Integer> costs,boolean simulate){return false;}
    @Override public boolean removed(){return getHost()==null||getHost().getPart(getSide())!=this;}
    @Override public appeng.menu.locator.MenuLocator locator(){return appeng.menu.locator.MenuLocators.forPart(this);}
    @Override public boolean useStandardMemoryCard(){return false;}
    public boolean memoryCard(Player player,ItemStack stack){
        if(!(stack.getItem() instanceof appeng.api.implementations.items.IMemoryCard card))return false;
        if(getLevel().isClientSide)return true;
        var message=appeng.api.implementations.items.MemoryCardMessages.INVALID_MACHINE;
        var data=card.getData(stack);
        if(data.isEmpty()){
            linkedVis=null;markWorkChanged();message=appeng.api.implementations.items.MemoryCardMessages.SETTINGS_LOADED;
        }else if(card.getSettingsName(stack).equals("gui.thaumicenergistics.vis_link")&&data.hasUUID("source")){
            var id=data.getUUID("source");
            if(temodern.machine.VisSupply.find(node().getGrid(),id,this)!=null){
                linkedVis=id;markWorkChanged();message=appeng.api.implementations.items.MemoryCardMessages.SETTINGS_LOADED;
            }
        }
        card.notifyUser(player,message);return true;
    }
    @Override public boolean onPartActivate(Player player,InteractionHand hand,Vec3 position){
        if(memoryCard(player,player.getItemInHand(hand)))return true;
        if(player instanceof ServerPlayer server)ArcaneTerminalMenu.open(server,this);return true;
    }
    @Override public boolean onPartShiftActivate(Player player,InteractionHand hand,Vec3 position){return memoryCard(player,player.getItemInHand(hand));}
    @Override public appeng.api.networking.ticking.TickingRequest getTickingRequest(appeng.api.networking.IGridNode node){return new appeng.api.networking.ticking.TickingRequest(5,20,false,false);}
    @Override public appeng.api.networking.ticking.TickRateModulation tickingRequest(appeng.api.networking.IGridNode node,int elapsed){
        return work.chargeWand(temodern.machine.VisSupply.find(node.getGrid(),linkedVis,this))
                ?appeng.api.networking.ticking.TickRateModulation.URGENT:appeng.api.networking.ticking.TickRateModulation.IDLE;
    }
    @Override public void returnToMainMenu(Player player,appeng.menu.ISubMenu menu){if(player instanceof ServerPlayer server)ArcaneTerminalMenu.open(server,this);}
    @Override public void readFromNBT(CompoundTag tag){super.readFromNBT(tag);work.load(tag);linkedVis=tag.hasUUID("linkedVis")?tag.getUUID("linkedVis"):null;}
    @Override public void writeToNBT(CompoundTag tag){super.writeToNBT(tag);work.save(tag);if(linkedVis!=null)tag.putUUID("linkedVis",linkedVis);}
    @Override public void addAdditionalDrops(java.util.List<ItemStack> drops,boolean wrenched){super.addAdditionalDrops(drops,wrenched);for(int i=0;i<work.inventory.getContainerSize();i++)if(!work.inventory.getItem(i).isEmpty())drops.add(work.inventory.getItem(i).copy());}
    @Override public void clearContent(){super.clearContent();work.inventory.clearContent();}
    @Override public IPartModel getStaticModels(){return isActive()?MODEL:isPowered()?POWERED:OFF;}
}
