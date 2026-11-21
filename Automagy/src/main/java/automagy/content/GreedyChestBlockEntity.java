package automagy.content;

import automagy.api.GreedyExtractionPermission;
import automagy.logic.PilferRules;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.RangedWrapper;

public final class GreedyChestBlockEntity extends InventoryBlockEntity implements MenuProvider, GreedyExtractionPermission {
    public int mode, amount = 64;
    public double fraction = .5;
    public final boolean[] directions = {true,true,true,true,true,true};
    private final Map<Direction,List<ItemStack>> snapshots = new EnumMap<>(Direction.class);
    private final Map<Direction,net.minecraft.world.level.block.entity.BlockEntity> sourceTiles = new EnumMap<>(Direction.class);
    private final List<Credit> credits = new ArrayList<>();
    private final Set<UUID> viewers = new HashSet<>();
    private boolean open;
    private List<ItemStack> lastContents=List.of();
    private int recheckSides;
    public float lid, previousLid;
    public int litTicks;
    private LazyOptional<IItemHandler> exposed = LazyOptional.of(() -> new RangedWrapper(inventory,0,27));
    private static final class Credit { final ItemStack key; double fraction; Credit(ItemStack key) { this.key=key.copyWithCount(1); } }
    public GreedyChestBlockEntity(BlockPos pos, BlockState state) { super(Content.GREEDY_TILE.get(),pos,state,28); }
    public IItemHandler storage() { return new RangedWrapper(inventory,0,27); }
    public IItemHandler menuInventory() { return inventory; }
    @Override protected int slotLimit(int slot) { return slot == 27 ? 1 : 64; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return slot != 27 || stack.getItem() instanceof FilterItem && ItemFilter.load(stack).populated(); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) { return cap == ForgeCapabilities.ITEM_HANDLER && !isRemoved() ? exposed.cast() : super.getCapability(cap,side); }
    @Override public void invalidateCaps() { super.invalidateCaps(); exposed.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); exposed=LazyOptional.of(() -> new RangedWrapper(inventory,0,27)); }
    @Override public boolean allowGreedyExtraction(BlockPos collector, Direction side) { return !directions[side.ordinal()]; }
    private boolean allowed(BlockPos source, Direction direction) {
        if (!level.hasChunkAt(source)) return false;
        var tile=level.getBlockEntity(source);
        return !(direction==Direction.DOWN && tile instanceof HopperBlockEntity)
                && (!(tile instanceof GreedyExtractionPermission guard) || guard.allowGreedyExtraction(worldPosition,direction.getOpposite()));
    }
    private ItemFilter filter() { return stored(27).getItem() instanceof FilterItem ? ItemFilter.load(stored(27)) : null; }
    private boolean matches(ItemStack a, ItemStack b) { ItemFilter filter=filter(); return filter==null ? ItemStack.isSameItemSameTags(a,b) : filter.matches(a,b); }
    private PilferRules.Limit limit(ItemStack key, long source, long here) {
        ItemFilter filter=filter(); return filter==null ? PilferRules.Limit.unlimited() : filter.transferLimit(key, ((FilterItem)stored(27).getItem()).blacklist,source,here);
    }
    private Credit credit(ItemStack key) {
        for (Credit c:credits) if (matches(c.key,key)) return c;
        Credit c=new Credit(key); credits.add(c); if (credits.size()>4096) credits.remove(0); return c;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, GreedyChestBlockEntity tile) {
        if(tile.litTicks>0)tile.litTicks--;
        if (level.isClientSide) { tile.previousLid=tile.lid; tile.lid=Math.max(0,Math.min(1,tile.lid+(tile.open?.1f:-.1f))); return; }
        if (level.getGameTime()%20==0) { tile.viewers.removeIf(id -> { Player p=level.getPlayerByUUID(id); return p==null || !(p.containerMenu instanceof GreedyChestMenu menu) || menu.tile()!=tile; }); tile.updateOpen(); }
        for (Direction side:Direction.values()) if (tile.directions[side.ordinal()] && Math.floorMod(level.getGameTime()+side.ordinal(),10)==0) tile.poll(side);
    }
    private void poll(Direction side) {
        BlockPos sourcePos=worldPosition.relative(side);
        if (!allowed(sourcePos,side)) { snapshots.remove(side); sourceTiles.remove(side); return; }
        var source=InventoryTransfer.at(level,sourcePos,side.getOpposite());
        if (source.isEmpty()) { snapshots.remove(side); sourceTiles.remove(side); return; }
        var sourceTile=level.getBlockEntity(sourcePos);
        if(sourceTiles.put(side,sourceTile)!=sourceTile)snapshots.remove(side);
        List<ItemStack> current=InventoryTransfer.snapshot(source.get(),true), previous=snapshots.get(side);
        if (previous==null) { snapshots.put(side,current); return; }
        if (mode==0) {
            var collectors=collectors(sourcePos);
            if (!collectors.isEmpty() && collectors.get(0)==this) distribute(sourcePos,current,previous,collectors);
        } else {
            List<ItemStack> seen=new ArrayList<>();
            for (ItemStack key:current) {
                if (seen.stream().anyMatch(s -> matches(s,key))) continue; seen.add(key);
                long total=InventoryTransfer.count(current,key,this::matches), here=InventoryTransfer.count(InventoryTransfer.snapshot(storage(),false),key,this::matches);
                long before=InventoryTransfer.count(previous,key,this::matches);
                if (total<=before && (recheckSides&(1<<side.ordinal()))==0) continue;
                int wanted=PilferRules.amount(mode,fraction,amount,total,here,InventoryTransfer.room(storage(),key,Integer.MAX_VALUE),limit(key,total,here));
                transfer(source.get(),key,wanted);
            }
        }
        snapshots.put(side,InventoryTransfer.snapshot(source.get(),true));
        recheckSides &= ~(1<<side.ordinal());
    }
    private List<GreedyChestBlockEntity> collectors(BlockPos source) {
        List<GreedyChestBlockEntity> result=new ArrayList<>();
        for (Direction face:Direction.values()) {
            BlockPos target=source.relative(face);
            if (level.hasChunkAt(target) && level.getBlockEntity(target) instanceof GreedyChestBlockEntity chest
                    && chest.mode==0 && chest.directions[face.getOpposite().ordinal()] && chest.allowed(source,face.getOpposite())) result.add(chest);
        }
        return result;
    }
    private void distribute(BlockPos sourcePos, List<ItemStack> now, List<ItemStack> previous, List<GreedyChestBlockEntity> collectors) {
        Collections.shuffle(collectors,new Random(level.random.nextLong()));
        List<ItemStack> seen=new ArrayList<>();
        for (ItemStack key:now) {
            if (seen.stream().anyMatch(s -> ItemStack.isSameItemSameTags(s,key))) continue; seen.add(key);
            long total=InventoryTransfer.count(now,key,ItemStack::isSameItemSameTags), old=InventoryTransfer.count(previous,key,ItemStack::isSameItemSameTags);
            int added=(int)Math.min(Integer.MAX_VALUE,Math.max(0,total-old)); if (added==0) continue;
            int n=collectors.size(); double[] fractions=new double[n], carried=new double[n]; int[] room=new int[n];
            for (int i=0;i<n;i++) {
                var chest=collectors.get(i); long here=InventoryTransfer.count(InventoryTransfer.snapshot(chest.storage(),false),key,chest::matches);
                long sourceCount=InventoryTransfer.count(now,key,chest::matches);
                room[i]=(int)Math.min(InventoryTransfer.room(chest.storage(),key,Integer.MAX_VALUE),chest.limit(key,sourceCount,here).maximum());
                fractions[i]=chest.fraction; carried[i]=chest.credit(key).fraction;
            }
            var share=PilferRules.share(added,(int)Math.min(Integer.MAX_VALUE,total),fractions,room,carried);
            int[] amounts=share.amounts(); double[] remainders=share.remainders();
            for (int i=0;i<n;i++) {
                var chest=collectors.get(i); chest.credit(key).fraction=remainders[i];
                chest.setChanged();
                Direction sourceDirection=directionTo(chest.worldPosition,sourcePos);
                InventoryTransfer.at(level,sourcePos,sourceDirection.getOpposite()).ifPresent(handler -> chest.transfer(handler,key,amounts[collectors.indexOf(chest)]));
            }
        }
        for (var chest:collectors) {
            Direction sourceDirection=directionTo(chest.worldPosition,sourcePos);
            InventoryTransfer.at(level,sourcePos,sourceDirection.getOpposite()).ifPresent(handler -> chest.snapshots.put(sourceDirection,InventoryTransfer.snapshot(handler,true)));
        }
    }
    private static Direction directionTo(BlockPos from, BlockPos to) { return Direction.fromDelta(to.getX()-from.getX(),to.getY()-from.getY(),to.getZ()-from.getZ()); }
    private void transfer(IItemHandler source, ItemStack key, int amount) {
        int moved=InventoryTransfer.move(source,storage(),key,amount,this::matches,stack -> Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,stack));
        if(moved>0){litTicks=5;sync();}
    }
    public void setQuantity(double value){if(mode<2){fraction=Math.max(0,Math.min(1,value/100d));credits.clear();}else amount=Math.max(0,Math.min(999,(int)value));setChanged();}
    public void configure(int button) {
        if (button<6) { directions[button]=!directions[button]; snapshots.remove(Direction.values()[button]); }
        else if (button==6||button==14) { mode=Math.floorMod(mode+(button==6?1:-1),4); snapshots.clear(); }
        else if (button==7 || button==8) { fraction=Math.max(0,Math.min(1,fraction+(button==7?-.05:.05))); credits.clear(); }
        else if (button==9 || button==10) amount=Math.max(0,Math.min(999,amount+(button==9?-1:1)));
        else if(button>=11&&button<=13){fraction=button==11?1.0/3:button==12?1.0/6:.5;credits.clear();}
        sync();
    }
    public void viewer(Player player, boolean added) { if (level.isClientSide) return; if (added) viewers.add(player.getUUID()); else viewers.remove(player.getUUID()); updateOpen(); }
    private void updateOpen() {
        boolean next=!viewers.isEmpty(); if (next==open) return; open=next; sync();
        level.playSound(null,worldPosition,open?net.minecraft.sounds.SoundEvents.CHEST_OPEN:net.minecraft.sounds.SoundEvents.CHEST_CLOSE,net.minecraft.sounds.SoundSource.BLOCKS,.5f,.9f+level.random.nextFloat()*.1f);
    }
    @Override protected void inventoryChanged() {
        super.inventoryChanged();
        List<ItemStack> next=InventoryTransfer.snapshot(storage(),false);
        for(ItemStack key:lastContents) if(InventoryTransfer.count(next,key,ItemStack::isSameItemSameTags)<InventoryTransfer.count(lastContents,key,ItemStack::isSameItemSameTags)){recheckSides=63;break;}
        lastContents=next;
        if (level!=null&&!level.isClientSide) level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putInt("Mode",mode); tag.putInt("Amount",amount); tag.putDouble("Fraction",fraction);
        int mask=0; for(int i=0;i<6;i++) if(directions[i])mask|=1<<i; tag.putInt("Directions",mask);
        var saved=new net.minecraft.nbt.ListTag();
        for(Credit credit:credits) if(credit.fraction>0){var entry=new CompoundTag();entry.put("Item",credit.key.save(new CompoundTag()));entry.putDouble("Value",credit.fraction);saved.add(entry);}
        tag.put("Credits",saved);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); mode=Math.max(0,Math.min(3,tag.getInt("Mode"))); amount=tag.contains("Amount")?Math.max(0,Math.min(999,tag.getInt("Amount"))):64;
        fraction=tag.contains("Fraction")?PilferRules.boundedFraction(tag.getDouble("Fraction")):.5;
        int mask=tag.contains("Directions")?tag.getInt("Directions"):63; for(int i=0;i<6;i++) directions[i]=(mask&(1<<i))!=0;
        snapshots.clear(); sourceTiles.clear(); credits.clear(); open=tag.getBoolean("Open");
        litTicks=Math.max(0,Math.min(5,tag.getInt("Lit")));
        var saved=tag.getList("Credits",10);
        for(int i=0;i<Math.min(4096,saved.size());i++){var entry=saved.getCompound(i);var key=ItemStack.of(entry.getCompound("Item"));double value=entry.getDouble("Value");
            if(!key.isEmpty()&&Double.isFinite(value)&&value>0&&value<1){Credit credit=new Credit(key);credit.fraction=value;credits.add(credit);}}
        lastContents=InventoryTransfer.snapshot(storage(),false);recheckSides=0;
    }
    @Override public CompoundTag getUpdateTag() { var tag=super.getUpdateTag(); tag.putBoolean("Open",open); tag.putInt("Lit",litTicks); return tag; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new GreedyChestMenu(id,inventory,this); }
}
