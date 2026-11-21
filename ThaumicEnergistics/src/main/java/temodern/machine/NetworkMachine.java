package temodern.machine;

import java.util.Map;
import java.util.concurrent.Future;
import com.google.common.collect.ImmutableSet;
import appeng.api.config.*;
import appeng.api.networking.*;
import appeng.api.networking.security.*;
import appeng.api.networking.crafting.*;
import appeng.api.storage.StorageHelper;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.content.EssentiaTransport;
import thaumcraft.api.aspects.EssentiaBuffer;
import thaumcraft.api.aspects.IEssentiaStorage;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.api.visnet.TileVisNode;
import temodern.essentia.*;

/** Addon-owned lifecycle; AE action sources, power and channels use the native grid. */
public final class NetworkMachine extends TileVisNode implements IInWorldGridNodeHost, IActionHost,
        VisSupply, IEssentiaStorage, IAspectSource, EssentiaTransport, ICraftingRequester, ICraftingSimulationRequester, temodern.crafting.WorkHost {
    @Override public net.minecraft.world.level.Level workLevel(){return getLevel();}
    @Override public net.minecraft.core.BlockPos workPosition(){return getBlockPos();}
    @Override public void markWorkChanged(){setChanged();}
    private final IManagedGridNode node = GridHelper.createManagedNode(this, (owner, gridNode) -> owner.setChanged())
            .setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(1);
    private final EssentiaBuffer fuel = new EssentiaBuffer(64, a -> a.tag().equals("ignis") || a.tag().equals("potentia"), this::setChanged);
    private final EssentiaBuffer vis = new EssentiaBuffer(90000, Aspect::isPrimal, this::setChanged);
    // Archived migration data only. Assemblers cannot spend or refill their former internal buffer.
    private CompoundTag legacyAssemblerVis=new CompoundTag();
    private final EssentiaBuffer pending = new EssentiaBuffer(Integer.MAX_VALUE, a -> true, this::setChanged);
    private final appeng.util.ConfigManager terminalConfig = new appeng.util.ConfigManager(this::setChanged);
    private final appeng.api.storage.MEStorage emptyTerminal = () -> Component.translatable("block.thaumicenergistics.arcane_terminal");
    private final ShaftDrive shaftDrive=new ShaftDrive();
    public long clientCrankUntil;
    private boolean visOutput;
    private java.util.UUID visIdentity=java.util.UUID.randomUUID();
    private java.util.UUID linkedVis;
    private double energy;
    private double burnRate = 5;
    private int burnSpeed = 100;
    private ICraftingLink job;
    private Future<ICraftingPlan> calculation;
    private String demand = "";
    private long nextRequest;
    public final temodern.crafting.Workstation workstation = new temodern.crafting.Workstation(this);
    private appeng.api.upgrades.IUpgradeInventory upgrades;
    @Override public appeng.api.upgrades.IUpgradeInventory getUpgrades(){
        if(!kind().equals("arcane_assembler"))return appeng.api.upgrades.UpgradeInventories.empty();
        if(upgrades==null)upgrades=appeng.api.upgrades.UpgradeInventories.forMachine(getBlockState().getBlock(),5,this::setChanged);
        return upgrades;
    }
    private final net.minecraftforge.common.util.LazyOptional<appeng.api.implementations.blockentities.ICraftingMachine> craftingCapability = net.minecraftforge.common.util.LazyOptional.of(() -> workstation);
    public NetworkMachine(BlockPos pos, BlockState state) {
        super(Machines.TYPE.get(), pos, state);
        terminalConfig.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        terminalConfig.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        terminalConfig.registerSetting(Settings.TYPE_FILTER, TypeFilter.ALL);
        terminalConfig.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
        if (kind().equals("essentia_vibration_chamber")) node.setFlags().setIdlePowerUsage(0);
        if (craftingProvider()) node.addService(ICraftingRequester.class, this);
        if(kind().equals("arcane_assembler"))node.addService(ICraftingProvider.class,workstation);
    }
    @Override public appeng.api.storage.MEStorage getInventory(){return node.isActive()&&node.getGrid()!=null?node.getGrid().getStorageService().getInventory():emptyTerminal;}
    @Override public appeng.api.util.IConfigManager getConfigManager(){return terminalConfig;}
    @Override public net.minecraft.world.item.ItemStack getMainMenuIcon(){return new net.minecraft.world.item.ItemStack(temodern.TE.ITEMS.getEntries().stream().filter(e->e.getId().getPath().equals(kind())).findFirst().orElseThrow().get());}
    @Override public void returnToMainMenu(net.minecraft.world.entity.player.Player player,appeng.menu.ISubMenu menu){if(player instanceof net.minecraft.server.level.ServerPlayer server)temodern.crafting.ArcaneTerminalMenu.open(server,this);}
    @Override public temodern.crafting.Workstation workstation(){return workstation;}
    @Override public boolean removed(){return isRemoved();}
    @Override public appeng.menu.locator.MenuLocator locator(){return appeng.menu.locator.MenuLocators.forBlockEntity(this);}
    public String kind() { return getBlockState().getBlock() instanceof MachineBlock block ? block.kind : ""; }
    private boolean generator() { return kind().equals("essentia_vibration_chamber"); }
    private boolean craftingProvider() { return kind().equals("infusion_provider") || kind().equals("essentia_provider"); }
    private boolean provider() { return kind().endsWith("provider"); }
    private boolean visInterface() { return kind().endsWith("vis_interface"); }
    public IManagedGridNode node() { return node; }
    public IActionSource action() { return IActionSource.ofMachine(this); }
    @Override public IActionSource getActionSource() { return action(); }
    @Override public IGridNode getGridNode(Direction side) { return node.getNode(); }
    @Override public IGridNode getActionableNode() { return node.getNode(); }
    @Override public int range() { return 8; }
    @Override public boolean source() { return kind().equals("creative_vis_interface") || kind().equals("vis_interface") && visOutput; }
    @Override public java.util.UUID visIdentity(){return visIdentity;}
    @Override public boolean visInput(){return visInterface()&&!visOutput;}
    public boolean visOutput() { return visOutput; }
    public void toggleVisOutput() { if(!kind().equals("vis_interface"))return; refreshNetwork();visOutput=!visOutput;refreshNetwork();setChanged(); }
    @Override protected int drainSource(Aspect aspect, int amount) {
        if(!source() || !node.isActive() || node.getGrid()==null)return 0;
        if(kind().equals("creative_vis_interface"))return amount;
        if(linkedVis==null)return 0;
        var supply=VisSupply.find(node.getGrid(),linkedVis,this);
        if(supply!=null)return supply.takeVis(aspect,amount);
        return 0;
    }
    public boolean memoryCard(net.minecraft.world.entity.player.Player player,net.minecraft.world.item.ItemStack stack){
        if(!(visInterface()||kind().equals("arcane_assembler")||kind().equals("arcane_terminal"))||!(stack.getItem() instanceof appeng.api.implementations.items.IMemoryCard card))return false;
        if(level.isClientSide)return true;
        var message=appeng.api.implementations.items.MemoryCardMessages.INVALID_MACHINE;
        if(player.isShiftKeyDown()&&visInterface()){
            refreshNetwork();visOutput=false;linkedVis=null;refreshNetwork();
            var data=new CompoundTag();data.putUUID("source",visIdentity);
            card.setMemoryCardContents(stack,"gui.thaumicenergistics.vis_link",data);
            message=appeng.api.implementations.items.MemoryCardMessages.SETTINGS_SAVED;setChanged();
        }else if(card.getSettingsName(stack).equals("gui.thaumicenergistics.vis_link")&&card.getData(stack).hasUUID("source")){
            var id=card.getData(stack).getUUID("source");
            if(VisSupply.find(node.getGrid(),id,this)!=null){
                refreshNetwork();linkedVis=id;visOutput=visInterface();refreshNetwork();setChanged();
                message=appeng.api.implementations.items.MemoryCardMessages.SETTINGS_LOADED;
            }
        }
        if(card.getData(stack).isEmpty()){
            refreshNetwork();linkedVis=null;visOutput=false;refreshNetwork();setChanged();
            message=appeng.api.implementations.items.MemoryCardMessages.SETTINGS_LOADED;
        }
        card.notifyUser(player,message);return true;
    }
    @Override public void onLoad() { super.onLoad(); if (level != null && !level.isClientSide) GridHelper.onFirstTick(this, machine -> machine.node.create(machine.level, machine.worldPosition)); }
    @Override public void setRemoved() { node.destroy(); super.setRemoved(); }
    @Override public void onChunkUnloaded() { node.destroy(); super.onChunkUnloaded(); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); node.saveToNBT(tag); tag.put("fuel", fuel.save()); tag.put("vis", vis.save());
        if(kind().equals("arcane_assembler")){tag.remove("vis");if(!legacyAssemblerVis.isEmpty())tag.put("legacyAssemblerVis",legacyAssemblerVis.copy());}
        tag.putBoolean("visOutput",visOutput); tag.put("pending", pending.save()); tag.putDouble("energy", energy); tag.putDouble("burnRate", burnRate); tag.putInt("burnSpeed",burnSpeed);
        tag.putString("demand", demand);
        tag.putUUID("visIdentity",visIdentity);if(linkedVis!=null)tag.putUUID("linkedVis",linkedVis);
        workstation.save(tag); var settings=new CompoundTag();terminalConfig.writeToNBT(settings);tag.put("terminalSettings",settings);
        getUpgrades().writeToNBT(tag,"upgrades");
        if (job != null) { var jobTag = new CompoundTag(); job.writeToNBT(jobTag); tag.put("job", jobTag); }
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); node.loadFromNBT(tag); fuel.load(tag.getCompound("fuel")); vis.load(tag.getCompound("vis"));
        if(kind().equals("arcane_assembler")){
            legacyAssemblerVis=tag.getCompound(tag.contains("legacyAssemblerVis")?"legacyAssemblerVis":"vis").copy();
            vis.load(new CompoundTag());
        }
        visOutput=tag.getBoolean("visOutput"); pending.load(tag.getCompound("pending")); energy = Math.max(0, tag.getDouble("energy"));
        burnRate=tag.getDouble("burnRate")==8?8:5; burnSpeed=tag.contains("burnSpeed")?Math.max(20,Math.min(200,tag.getInt("burnSpeed"))):100;
        demand = tag.getString("demand");
        if(tag.hasUUID("visIdentity"))visIdentity=tag.getUUID("visIdentity");linkedVis=tag.hasUUID("linkedVis")?tag.getUUID("linkedVis"):null;
        workstation.load(tag);terminalConfig.readFromNBT(tag.getCompound("terminalSettings"));
        getUpgrades().readFromNBT(tag,"upgrades");
        if(kind().equals("arcane_assembler")){
            var legacy=workstation.inventory.getItem(22);
            if(legacy.is(appeng.core.definitions.AEItems.SPEED_CARD.asItem())){
                var rest=getUpgrades().addItems(legacy.copy());workstation.inventory.setItem(22,rest);
            }
        }
        job = tag.contains("job") ? StorageHelper.loadCraftingLink(tag.getCompound("job"), this) : null;
    }
    public void tick() {
        if (level == null || level.isClientSide || !node.isReady()) return;
        if (visInterface()) {
            tickNetwork();
            if (node.isActive() && !visOutput) for (var primal : tc4revival.aspect.TCAspects.catalog().primals()) {
                int wanted = Math.min(100, 10000 - vis.contents().amount(primal));
                if (wanted > 0) {
                    if(kind().equals("creative_vis_interface"))vis.insert(primal,wanted,false);
                    else if(!(networkRoot() instanceof VisSupply supply && supply.node().getGrid()==node.getGrid())) {
                        var power=node.getGrid().getEnergyService();
                        if(power.extractAEPower(4,Actionable.SIMULATE,PowerMultiplier.CONFIG)>=4){
                            int received=consumeVis(primal,wanted);
                            if(received>0){power.extractAEPower(4,Actionable.MODULATE,PowerMultiplier.CONFIG);vis.insert(primal,received,false);}
                        }
                    }
                }
            }
        }
        if((kind().equals("arcane_terminal")||kind().equals("arcane_assembler"))&&node.isActive()&&linkedVis!=null&&level.getGameTime()%5==0)
            workstation.chargeWand(VisSupply.find(node.getGrid(),linkedVis,this));
        if (generator()) tickGenerator();
        if (craftingProvider()) tickRequests();
        if(kind().equals("essentia_provider") && level.getGameTime()%5==0) tickTransportInput();
        workstation.tick();
        int activity=generator()?(energy>0?(burnRate==8?2:1):0):(node.isActive()?1:0);
        if(getBlockState().getValue(MachineBlock.STATE)!=activity)level.setBlock(worldPosition,getBlockState().setValue(MachineBlock.STATE,activity),3);
    }
    private void tickGenerator() {
        var grid = node.getGrid();
        if (grid == null) return;
        if (energy <= 0 && grid.getEnergyService().injectPower(5, Actionable.SIMULATE) < 5) {
            for (var aspect : fuel.contents().aspects()) if (fuel.extract(aspect, 1, false) == 1) {
                burnRate=aspect.tag().equals("ignis") ? 5 : 8;
                energy = aspect.tag().equals("ignis") ? 4000 : 8000; setChanged(); break;
            }
        }
        if (energy > 0) {
            double offer = Math.min(burnRate*burnSpeed/100.0, energy);
            double remainder = grid.getEnergyService().injectPower(offer, Actionable.MODULATE);
            if (remainder < offer) { energy -= offer - remainder; setChanged(); }
            burnSpeed=Math.max(20,Math.min(200,burnSpeed+(remainder>0?-1:1)));
        }
        // Native tubes push into this transport. Pulling supports storage-only adjacent containers too.
        if (level.getGameTime() % 10 == 0 && fuel.contents().total() < 64) {
            for (Direction side : Direction.values()) {
                if(!canInput(side)||!level.hasChunkAt(worldPosition.relative(side)))continue;
                var adjacent = thaumcraft.api.aspects.EssentiaStorageApi.at((net.minecraft.server.level.ServerLevel) level, worldPosition.relative(side), side.getOpposite());
                for (var aspect : adjacent.contents().aspects()) {
                    int fit = insert(aspect, 1, true);
                    if (fit > 0) insert(aspect, adjacent.extract(aspect, fit, false), false);
                }
            }
        }
    }
    public int availableVis(Aspect aspect) { return node.isActive() && visInterface() && !visOutput ? vis.contents().amount(aspect) : 0; }
    public int storedVis(Aspect aspect) {
        if(!kind().equals("arcane_assembler"))return vis.contents().amount(aspect);
        var stack=workstation.inventory.getItem(9);
        return stack.getItem() instanceof tc4revival.content.CastingWandItem wand?wand.vis(stack,aspect):0;
    }
    public int takeVis(Aspect aspect, int amount) { return node.isActive() && visInterface() && !visOutput ? vis.extract(aspect, amount, false) : 0; }
    public boolean consumeNetworkVis(Map<Aspect,Integer> costs, boolean simulate) {
        if (!node.isActive() || node.getGrid() == null) return false;
        if(!kind().equals("arcane_assembler"))return false;
        var stack=workstation.inventory.getItem(9);
        if(!(stack.getItem() instanceof tc4revival.content.CastingWandItem wand)||wand.isStaff(stack))return false;
        boolean result=costs.isEmpty()||wand.consume(stack,costs,a->1,false,!simulate,level);
        if(result&&!simulate)setChanged();return result;
    }
    @Override public boolean contains(Aspect aspect, int amount) { return provider() && amount >= 0 && extract(aspect, amount, true) == amount; }
    @Override public Object availabilityIdentity() { return node.getGrid()==null?this:node.getGrid(); }
    @Override public java.util.List<? extends tc4revival.content.EssentiaSource> availabilitySources() { return provider()&&node.isActive()?java.util.List.of(this):java.util.List.of(); }
    @Override public boolean takeFromContainer(Aspect aspect, int amount) {
        if (!contains(aspect, amount)) {
            if (craftingProvider() && demand.isEmpty()) { demand = aspect.tag(); setChanged(); }
            return false;
        }
        int taken = extract(aspect, amount, false);
        if (taken == amount) return true;
        if (taken > 0) pending.insert(aspect, taken, false);
        return false;
    }
    private void tickRequests() {
        if (job != null) { if (job.isDone() || job.isCanceled()) { job = null; setChanged(); } else return; }
        if (!node.isActive() || node.getGrid() == null) return;
        if (calculation != null) {
            if (!calculation.isDone()) return;
            try {
                var result = node.getGrid().getCraftingService().submitJob(calculation.get(), this, null, false, action());
                if (result.successful()) { job = result.link(); demand = ""; setChanged(); }
            } catch (java.util.concurrent.ExecutionException ignored) {
                // Missing ingredients are retried with a delay; the simulation never removed resources.
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { calculation = null; nextRequest = level.getGameTime() + 100; }
            return;
        }
        var key = EssentiaKey.parse(demand);
        if (key != null && level.getGameTime() >= nextRequest) {
            calculation = node.getGrid().getCraftingService().beginCraftingCalculation(level, this, key, 8, CalculationStrategy.CRAFT_LESS);
            nextRequest = level.getGameTime() + 100;
        }
    }
    @Override public ImmutableSet<ICraftingLink> getRequestedJobs() { return job == null ? ImmutableSet.of() : ImmutableSet.of(job); }
    @Override public long insertCraftedItems(ICraftingLink link, appeng.api.stacks.AEKey what, long amount, Actionable mode) {
        if (link != job || !(what instanceof EssentiaKey key) || key.aspect() == null || amount <= 0) return 0;
        return pending.insert(key.aspect(), (int) Math.min(Integer.MAX_VALUE, amount), mode == Actionable.SIMULATE);
    }
    @Override public void jobStateChange(ICraftingLink link) { if (job == link && (link.isDone() || link.isCanceled())) { job = null; setChanged(); } }
    @Override public AspectAmounts contents() {
        if (generator()) return fuel.contents();
        var result = pending.contents();
        if (provider() && node.isActive() && node.getGrid() != null) {
            for (var entry : node.getGrid().getStorageService().getInventory().getAvailableStacks()) {
                if (entry.getKey() instanceof EssentiaKey key && key.aspect() != null)
                    result.merge(key.aspect(), (int) Math.min(Integer.MAX_VALUE, entry.getLongValue()));
            }
        }
        return result;
    }
    @Override public int capacity(Aspect aspect) { return generator() ? fuel.capacity(aspect) : kind().equals("essentia_provider") && node.isActive() ? Integer.MAX_VALUE : 0; }
    @Override public int insert(Aspect aspect, int amount, boolean simulate) {
        if(generator()){
            if(fuel.contents().total()>0&&fuel.contents().amount(aspect)==0)return 0;
            return fuel.insert(aspect,amount,simulate);
        }
        if(amount<0)throw new IllegalArgumentException("Negative essentia");
        if(!kind().equals("essentia_provider")||!node.isActive()||node.getGrid()==null)return 0;
        var grid=node.getGrid();return (int)StorageHelper.poweredInsert(grid.getEnergyService(),grid.getStorageService().getInventory(),EssentiaKey.of(aspect),amount,action(),simulate?Actionable.SIMULATE:Actionable.MODULATE);
    }
    private void tickTransportInput() {
        if(!node.isActive())return;
        for(Direction side:Direction.values()) {
            if(!level.hasChunkAt(worldPosition.relative(side)) || !(level.getBlockEntity(worldPosition.relative(side)) instanceof EssentiaTransport transport))continue;
            Direction face=side.getOpposite();
            if(!transport.canOutput(face)||transport.suction(face)>=8||transport.minimumSuction()>8)continue;
            var aspect=transport.essentiaType(face);if(aspect==null||insert(aspect,1,true)==0)continue;
            int taken=transport.takeEssentia(aspect,1,face);if(taken<=0)continue;
            int accepted=insert(aspect,taken,false);if(accepted<taken)pending.insert(aspect,taken-accepted,false);
        }
    }
    @Override public int extract(Aspect aspect, int amount, boolean simulate) {
        if (amount < 0) throw new IllegalArgumentException("Negative essentia");
        if (generator()) return fuel.extract(aspect, amount, simulate);
        if (!provider() || !node.isActive() || node.getGrid() == null) return 0;
        // Providers are not advertised as generic external stores: that would recursively expose their own ME grid.
        int recovered = pending.extract(aspect, amount, simulate);
        if (recovered == amount) return recovered;
        var grid = node.getGrid();
        long taken = StorageHelper.poweredExtraction(grid.getEnergyService(), grid.getStorageService().getInventory(),
                EssentiaKey.of(aspect), amount - recovered, action(), simulate ? Actionable.SIMULATE : Actionable.MODULATE);
        return recovered + (int) taken;
    }
    public int fuelAmount(){return (int)fuel.contents().total();}
    public int fuelKind(){return fuel.contents().amount(tc4revival.aspect.TCAspects.catalog().find("potentia").orElseThrow())>0?2:fuelAmount()>0?1:0;}
    public int energyRemaining(){return (int)Math.ceil(energy);}
    public int fuelEnergy(){return burnRate==8?8000:4000;}
    public int productionRate(){return (int)Math.round(burnRate*burnSpeed);}
    public int operatingSpeed(){return burnSpeed;}
    public Component status() { return Component.translatable("gui.thaumicenergistics.machine_status", kind(), node.isActive(), fuel.contents().total(), vis.contents().total(), (long) energy); }
    public boolean crank() {
        if (level == null || level.isClientSide || !kind().endsWith("gearbox")) return false;
        var targets=new appeng.api.implementations.blockentities.ICrankable[6];var accepting=new boolean[6];boolean any=false;
        for(Direction side:Direction.values()){
            int i=side.ordinal();targets[i]=appeng.api.implementations.blockentities.ICrankable.get(level,worldPosition.relative(side),side.getOpposite());
            accepting[i]=targets[i]!=null&&targets[i].canTurn();any|=accepting[i];
        }
        var turns=shaftDrive.crank(accepting);if(!any)return false;
        for(int i=0;i<6;i++)if(turns[i])targets[i].applyTurn();
        level.blockEvent(worldPosition,getBlockState().getBlock(),1,0);
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.WOODEN_BUTTON_CLICK_ON, net.minecraft.sounds.SoundSource.BLOCKS, .3f, .7f);
        return true;
    }
    @Override public boolean triggerEvent(int id,int value){if(id==1){if(level!=null)clientCrankUntil=level.getGameTime()+10;return true;}return super.triggerEvent(id,value);}
    public void dropContents() {
        workstation.dropContents();
        for(var stack:getUpgrades())if(!stack.isEmpty())Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,stack.copy());
        getUpgrades().clear();
        if (job != null) job.cancel();
        if (calculation != null) calculation.cancel(true);
        for (var buffer : new EssentiaBuffer[]{fuel, pending}) buffer.contents().view().forEach((aspect,amount) ->
                Containers.dropItemStack(level, worldPosition.getX()+.5, worldPosition.getY()+.5, worldPosition.getZ()+.5, EssentiaAmpoule.filled(EssentiaKey.of(aspect), amount)));
    }
    @Override public boolean connectable(Direction face) { return generator() && face!=getBlockState().getValue(MachineBlock.FACING) || kind().equals("essentia_provider"); }
    @Override public boolean canInput(Direction face) { return generator() && face!=getBlockState().getValue(MachineBlock.FACING) || kind().equals("essentia_provider"); }
    @Override public boolean canOutput(Direction face) { return kind().equals("essentia_provider"); }
    @Override public Aspect suctionType(Direction face) { return generator()?fuel.contents().aspects().stream().findFirst().orElse(tc4revival.aspect.TCAspects.catalog().require(level!=null&&level.getGameTime()%200>100?"potentia":"ignis")):null; }
    @Override public int suction(Direction face) { return generator() && fuel.contents().total() < 64 ? (int)(128*(1-fuel.contents().total()*.75/64)) : kind().equals("essentia_provider") && node.isActive() ? 8 : 0; }
    @Override public int minimumSuction() { return 0; }
    @Override public Aspect essentiaType(Direction face) {
        // Null is an unsided contents query, used by tubes when no aspect is selected.
        if(face!=null && kind().equals("essentia_provider") && level!=null && level.hasChunkAt(worldPosition.relative(face))
                && level.getBlockEntity(worldPosition.relative(face)) instanceof EssentiaTransport neighbor) {
            var wanted=neighbor.suctionType(face.getOpposite());
            if(wanted!=null){if(!contains(wanted,1)&&demand.isEmpty()){demand=wanted.tag();setChanged();}return wanted;}
        }
        return contents().aspects().stream().findFirst().orElse(null);
    }
    @Override public int essentiaAmount(Direction face) { var type=essentiaType(face); return type==null?0:contents().amount(type); }
    @Override public int takeEssentia(Aspect aspect, int amount, Direction face) { return aspect!=null && amount>0 && canOutput(face) ? extract(aspect, amount, false) : 0; }
    @Override public int addEssentia(Aspect aspect, int amount, Direction face) { return aspect!=null && amount>0 && canInput(face) ? insert(aspect, amount, false) : 0; }
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, Direction side) {
        if(capability==appeng.capabilities.Capabilities.CRAFTING_MACHINE&&kind().equals("arcane_assembler"))return craftingCapability.cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();craftingCapability.invalidate();}
}
