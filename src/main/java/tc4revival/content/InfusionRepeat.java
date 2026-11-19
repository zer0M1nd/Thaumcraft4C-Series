package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import tc4revival.research.KnowledgeSavedData;

/** A successful altar's exact item placement. Inventory changes are planned before committing. */
public class InfusionRepeat {
    public record Entry(BlockPos pos,ItemStack stack) {}
    private final ResourceLocation recipe;
    private final ItemStack center;
    private final List<Entry> entries;
    public InfusionRepeat(ResourceLocation recipe,ItemStack center,List<Entry> entries) {
        this.recipe=recipe;this.center=center.copy();this.entries=entries.stream().map(e->new Entry(e.pos().immutable(),e.stack().copy())).toList();
    }
    public static InfusionRepeat capture(InfusionRecipe recipe,ItemStack center,List<ArcanePedestalBlockEntity> pedestals) {
        return new InfusionRepeat(recipe.getId(),center,pedestals.stream().filter(p->!p.isEmpty()).map(p->new Entry(p.getBlockPos(),p.getItem(0).copyWithCount(1))).toList());
    }
    public ResourceLocation recipe() { return recipe; }
    public ItemStack center() { return center.copy(); }
    public List<ItemStack> components() { return entries.stream().map(e->e.stack().copy()).toList(); }
    public boolean place(InfusionMatrixBlockEntity matrix,ServerPlayer player) { return prepare(matrix,player,true); }
    public boolean canPlace(InfusionMatrixBlockEntity matrix,ServerPlayer player) { return prepare(matrix,player,false); }
    private boolean prepare(InfusionMatrixBlockEntity matrix,ServerPlayer player,boolean commit) {
        var level=matrix.getLevel();var origin=matrix.getBlockPos();
        if(level==null||level!=player.level()||matrix.crafting()||!matrix.validLocation()||player.distanceToSqr(origin.getCenter())>64)return false;
        var surrounding=InfusionLayout.scan(level,origin).pedestals();
        for(var pos:surrounding)if(!(level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity p)||!p.isEmpty())return false;
        var targets=new ArrayList<ArcanePedestalBlockEntity>();
        for(var entry:entries) {
            if(!surrounding.contains(entry.pos())||!permitted(player,entry.pos())||!(level.getBlockEntity(entry.pos()) instanceof ArcanePedestalBlockEntity p))return false;
            targets.add(p);
        }
        if(!permitted(player,origin)||!permitted(player,origin.below(2))||!(level.getBlockEntity(origin.below(2)) instanceof ArcanePedestalBlockEntity middle))return false;
        var found=level.getRecipeManager().byKey(recipe).filter(InfusionRecipe.class::isInstance).map(InfusionRecipe.class::cast);
        if(found.isEmpty())return false;
        var knowledge=KnowledgeSavedData.get(player.server).snapshot(player.getUUID());
        var contents=entries.stream().map(e->e.stack().copy()).toList();
        java.util.function.Predicate<ItemStack> valid=stack->found.get().matches(new InfusionRecipe.Input(stack,contents,
                key->knowledge.map(k->k.researchMarkers().contains(key)).orElse(false)),level);
        var inventory=player.getInventory().items.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        var nextCenter=middle.getItem(0).copy();
        if(nextCenter.isEmpty()||!valid.test(nextCenter)) {
            if(!valid.test(center))return false;
            // Collect the completed output before restoring the input; never drop overflow.
            if(!deposit(inventory,nextCenter)||!take(inventory,center))return false;
            nextCenter=center.copy();
        }
        for(var entry:entries)if(!take(inventory,entry.stack()))return false;
        if(!commit)return true;
        // Single server-thread commit: no inventory, output or pedestal is changed on failure.
        for(int i=0;i<inventory.size();i++)player.getInventory().items.set(i,inventory.get(i));
        middle.setFromInfusion(nextCenter);
        for(int i=0;i<entries.size();i++)targets.get(i).setFromInfusion(entries.get(i).stack());
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();matrix.changed();
        return true;
    }
    private static boolean permitted(ServerPlayer player,BlockPos pos) {
        return player.level().mayInteract(player,pos)&&player.mayUseItemAt(pos,net.minecraft.core.Direction.UP,ItemStack.EMPTY);
    }
    public static boolean take(List<ItemStack> inventory,ItemStack desired) {
        int left=desired.getCount();
        for(var stack:inventory)if(ItemStack.isSameItemSameTags(stack,desired)) {
            int count=Math.min(left,stack.getCount());stack.shrink(count);left-=count;if(left==0)return true;
        }
        return left==0;
    }
    public static boolean deposit(List<ItemStack> inventory,ItemStack offered) {
        int left=offered.getCount();if(offered.isEmpty())return true;
        for(var stack:inventory)if(ItemStack.isSameItemSameTags(stack,offered)) {
            int count=Math.min(left,Math.max(0,stack.getMaxStackSize()-stack.getCount()));stack.grow(count);left-=count;if(left==0)return true;
        }
        for(int i=0;i<inventory.size();i++)if(inventory.get(i).isEmpty()) {
            int count=Math.min(left,offered.getMaxStackSize());inventory.set(i,offered.copyWithCount(count));left-=count;if(left==0)return true;
        }
        return false;
    }
    public CompoundTag save() {
        var tag=new CompoundTag();tag.putString("Recipe",recipe.toString());tag.put("Center",center.save(new CompoundTag()));
        var list=new ListTag();for(var entry:entries){var row=new CompoundTag();row.putLong("Pos",entry.pos().asLong());row.put("Item",entry.stack().save(new CompoundTag()));list.add(row);}tag.put("Entries",list);return tag;
    }
    public static InfusionRepeat load(CompoundTag tag,BlockPos matrix) {
        var id=ResourceLocation.tryParse(tag.getString("Recipe"));var center=ItemStack.of(tag.getCompound("Center"));var rows=tag.getList("Entries",Tag.TAG_COMPOUND);
        if(id==null||center.isEmpty()||rows.isEmpty()||rows.size()>128)return null;
        var entries=new ArrayList<Entry>();var unique=new java.util.HashSet<BlockPos>();
        for(var row:rows){var value=(CompoundTag)row;var pos=BlockPos.of(value.getLong("Pos"));var stack=ItemStack.of(value.getCompound("Item"));
            if(stack.isEmpty()||stack.getCount()!=1||!unique.add(pos)||pos.distSqr(matrix)>300)return null;
            entries.add(new Entry(pos,stack));}
        return new InfusionRepeat(id,center,entries);
    }
}
