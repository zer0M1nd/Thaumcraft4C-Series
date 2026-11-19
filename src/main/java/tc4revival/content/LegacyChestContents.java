package tc4revival.content;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.function.IntUnaryOperator;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Data driven weighted loot inserted without replacing existing container contents. */
public  class LegacyChestContents {
    public record Entry(ItemStack stack,int minimum,int maximum,int weight,boolean randomBook,boolean prechargedAmulet) {
        public Entry(ItemStack stack,int minimum,int maximum,int weight,boolean randomBook) { this(stack,minimum,maximum,weight,randomBook,false); }
        public Entry(ItemStack stack,int minimum,int maximum,int weight) { this(stack,minimum,maximum,weight,false); }
        public Entry {
            if (stack.isEmpty() || minimum<1 || maximum<minimum || weight<=0)
                throw new IllegalArgumentException("Invalid legacy chest entry");
            stack=stack.copy(); stack.setCount(1);
        }
        @Override public ItemStack stack() { return stack.copy(); }
        public ItemStack generate(net.minecraft.util.RandomSource random) {
            var result=prechargedAmulet?LegacyLootTemplates.prechargedAmulet(random::nextInt):stack();
            if(randomBook) {
                var book=new ItemStack(net.minecraft.world.item.Items.BOOK);
                if(result.hasTag())book.setTag(result.getTag().copy());
                result=LegacyRandomEnchantments.apply(book,30,random);
            }
            return result;
        }
    }
    private final List<Entry> entries;
    private final int totalWeight;
    public LegacyChestContents(List<Entry> entries) {
        this.entries=List.copyOf(entries);
        if (entries.isEmpty()) throw new IllegalArgumentException("An empty chest pool cannot be selected");
        long total=entries.stream().mapToLong(Entry::weight).sum();
        if (total>Integer.MAX_VALUE) throw new IllegalArgumentException("Legacy chest weights exceed the random bound");
        totalWeight=(int)total;
    }
    public List<Entry> entries() { return entries; }
    public int totalWeight() { return totalWeight; }
    public Entry select(IntUnaryOperator random) {
        int roll=next(random,totalWeight);
        for (var entry:entries) {
            if (roll<entry.weight()) return entry;
            roll-=entry.weight();
        }
        throw new IllegalStateException("Invalid weighted selection");
    }
    public void fill(Container inventory,int rounds,IntUnaryOperator random) {
        if (entries.stream().anyMatch(Entry::randomBook)) throw new IllegalArgumentException("Chest book initialization requires a full RandomSource");
        if (rounds<0 || inventory.getContainerSize()<=0) throw new IllegalArgumentException("Invalid chest destination or round count");
        fill(inventory,rounds,random,null);
    }
    public void fill(Container inventory,int rounds,net.minecraft.util.RandomSource random) {
        if(rounds<0 || inventory.getContainerSize()<=0)throw new IllegalArgumentException("Invalid chest destination or round count");
        fill(inventory,rounds,random::nextInt,random);
    }
    private void fill(Container inventory,int rounds,IntUnaryOperator random,net.minecraft.util.RandomSource enchantRandom) {
        var handler=new net.minecraftforge.items.wrapper.InvWrapper(inventory);
        for(int round=0;round<rounds;round++) {
            var entry=select(random);
            var stack=entry.prechargedAmulet()?LegacyLootTemplates.prechargedAmulet(random):entry.stack();
            stack.setCount(entry.minimum()+next(random,entry.maximum()-entry.minimum()+1));
            if(entry.randomBook()) {
                var book=new ItemStack(net.minecraft.world.item.Items.BOOK,stack.getCount());
                if(stack.hasTag())book.setTag(stack.getTag().copy());
                stack=LegacyRandomEnchantments.apply(book,30,enchantRandom);
            }
            // As with native generated container loot, excess selections stop when storage is full.
            if(!net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(handler,stack,false).isEmpty())break;
        }
        inventory.setChanged();
    }
    private static int next(IntUnaryOperator random,int bound) {
        int value=random.applyAsInt(bound);
        if (value<0 || value>=bound) throw new IllegalArgumentException("Random source returned an out-of-range value");
        return value;
    }
    /** Data remains declarative; unusual NBT (such as precharged amulets) is retained exactly. */
    public static LegacyChestContents decode(JsonObject json) {
        var entries=new java.util.ArrayList<Entry>();
        for (var element:GsonHelper.getAsJsonArray(json,"entries")) {
            var entry=element.getAsJsonObject(); var result=GsonHelper.getAsJsonObject(entry,"stack");
            var stack=LegacyLootTemplates.decode(result);
            entries.add(new Entry(stack,GsonHelper.getAsInt(entry,"min",1),GsonHelper.getAsInt(entry,"max",1),GsonHelper.getAsInt(entry,"weight"),GsonHelper.getAsBoolean(entry,"random_book",false),GsonHelper.getAsBoolean(result,"precharged_amulet",false)));
        }
        return new LegacyChestContents(entries);
    }
}
