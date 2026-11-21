package tc4revival.network;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import tc4revival.research.BrowserRecipe;
public record BrowserRecipesPacket(int request,boolean catalog,boolean last,List<BrowserRecipe> recipes) {
    public BrowserRecipesPacket {recipes=List.copyOf(recipes);if(recipes.size()>32)throw new IllegalArgumentException("Too many recipes");}
    public void encode(FriendlyByteBuf b){
        b.writeVarInt(request);b.writeBoolean(catalog);b.writeBoolean(last);b.writeVarInt(recipes.size());
        for(var r:recipes){b.writeUtf(r.kind(),32);b.writeUtf(r.id(),512);b.writeUtf(r.research(),256);
            b.writeVarInt(r.inputs().size());for(var group:r.inputs()){b.writeVarInt(group.size());for(var stack:group)b.writeItem(stack);}
            b.writeItem(r.output());write(b,r.aspectInputs());write(b,r.aspectOutputs());b.writeVarInt(r.instability());b.writeVarInt(r.xp());b.writeBoolean(r.dynamic());}
    }
    private static void write(FriendlyByteBuf b,Map<String,Integer> values){b.writeVarInt(values.size());values.forEach((k,v)->{b.writeUtf(k,64);b.writeVarInt(v);});}
    private static int count(FriendlyByteBuf b,int max){int n=b.readVarInt();if(n<0||n>max)throw new IllegalArgumentException("Invalid browser collection size");return n;}
    private static Map<String,Integer> read(FriendlyByteBuf b){var map=new LinkedHashMap<String,Integer>();int n=count(b,256);for(int i=0;i<n;i++){String key=b.readUtf(64);int v=b.readVarInt();if(v<0)throw new IllegalArgumentException("Negative aspect quantity");map.put(key,v);}return map;}
    public static BrowserRecipesPacket decode(FriendlyByteBuf b){int id=b.readVarInt();boolean catalog=b.readBoolean(),last=b.readBoolean();int count=count(b,32);var rows=new ArrayList<BrowserRecipe>();
        for(int i=0;i<count;i++){String kind=b.readUtf(32),key=b.readUtf(512),research=b.readUtf(256);var inputs=new ArrayList<List<ItemStack>>();int groups=count(b,4096);
            for(int j=0;j<groups;j++){int n=count(b,16384);var stacks=new ArrayList<ItemStack>();for(int k=0;k<n;k++)stacks.add(b.readItem());inputs.add(stacks);}
            rows.add(new BrowserRecipe(kind,key,research,inputs,b.readItem(),read(b),read(b),b.readVarInt(),b.readVarInt(),b.readBoolean()));}
        return new BrowserRecipesPacket(id,catalog,last,rows);
    }
}
