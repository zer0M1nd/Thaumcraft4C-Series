package automagy.content;

import automagy.Automagy;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class FilterNetwork {
    private static final String VERSION = "7";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(Automagy.ID, "menus"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    private record GreedyQuantity(int menu,int mode,double value) {}
    public static void greedyQuantity(int menu,int mode,double value){CHANNEL.sendToServer(new GreedyQuantity(menu,mode,value));}
    private record CraftKey() {}
    private record Ghost(int menu,int page,int slot,net.minecraft.world.item.ItemStack item) {}
    private record RecipeTemplate(int menu,ResourceLocation recipe,int page) {}
    private record Rename(int menu, String name) {}
    private record Search(int menu,String query) {}
    private record Request(int menu,int slot,net.minecraft.world.item.ItemStack item,int count,boolean cancel,boolean alt) {}
    public static void recipeTemplate(int menu,ResourceLocation recipe,int page){CHANNEL.sendToServer(new RecipeTemplate(menu,recipe,page));}
    public static void ghost(int menu,int page,int slot,net.minecraft.world.item.ItemStack item){CHANNEL.sendToServer(new Ghost(menu,page,slot,item.copyWithCount(1)));}
    public static void craftKey(){CHANNEL.sendToServer(new CraftKey());}
    public static void register() {
        CHANNEL.messageBuilder(GreedyQuantity.class,6,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeVarInt(m.menu);b.writeVarInt(m.mode);b.writeDouble(m.value);}).decoder(b->new GreedyQuantity(b.readVarInt(),b.readVarInt(),b.readDouble())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null&&p.containerMenu.containerId==m.menu&&p.containerMenu instanceof GreedyChestMenu menu)menu.quantity(p,m.mode,m.value);}).add();
        CHANNEL.messageBuilder(CraftKey.class,5,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{}).decoder(b->new CraftKey())
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)CraftingFocusKey.press(p);}).add();
        CHANNEL.messageBuilder(Ghost.class,4,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeVarInt(m.menu);b.writeVarInt(m.page);b.writeVarInt(m.slot);b.writeItem(m.item);})
            .decoder(b->new Ghost(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readItem()))
            .consumerMainThread((m,c)->{
                var p=c.get().getSender();if(p==null||p.containerMenu.containerId!=m.menu||m.slot<0||m.slot>=9||m.item.isEmpty())return;
                if(m.item.hasTag()&&m.item.getTag().toString().length()>16384)return;
                if(p.containerMenu instanceof FilterMenu menu)menu.setGhost(p,m.slot,m.item);
                else if(p.containerMenu instanceof GolemWorkbenchMenu menu)menu.setGhost(p,m.page,m.slot,m.item);
            }).add();
        CHANNEL.messageBuilder(RecipeTemplate.class,3,NetworkDirection.PLAY_TO_SERVER)
            .encoder((m,b)->{b.writeVarInt(m.menu);b.writeResourceLocation(m.recipe);b.writeVarInt(m.page);})
            .decoder(b->new RecipeTemplate(b.readVarInt(),b.readResourceLocation(),b.readVarInt()))
            .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null&&p.containerMenu.containerId==m.menu&&p.containerMenu instanceof GolemWorkbenchMenu menu)menu.applyRecipe(p,m.recipe,m.page);}).add();
        CHANNEL.messageBuilder(Request.class,2,NetworkDirection.PLAY_TO_SERVER)
                .encoder((m,b)->{b.writeVarInt(m.menu);b.writeVarInt(m.slot);b.writeItem(m.item);b.writeVarInt(m.count);b.writeBoolean(m.cancel);b.writeBoolean(m.alt);})
                .decoder(b->new Request(b.readVarInt(),b.readVarInt(),b.readItem(),b.readVarInt(),b.readBoolean(),b.readBoolean()))
                .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null&&p.containerMenu.containerId==m.menu&&p.containerMenu instanceof InventariumMenu menu)menu.request(p,m.slot,m.item,m.count,m.cancel,m.alt);}).add();
        CHANNEL.messageBuilder(Search.class,1,NetworkDirection.PLAY_TO_SERVER)
                .encoder((message,buf)->{buf.writeVarInt(message.menu);buf.writeUtf(message.query,80);})
                .decoder(buf->new Search(buf.readVarInt(),buf.readUtf(80)))
                .consumerMainThread((message,context)->{var player=context.get().getSender();if(player!=null&&player.containerMenu.containerId==message.menu&&player.containerMenu instanceof InventariumMenu menu)menu.search(player,message.query);}).add();
        CHANNEL.messageBuilder(Rename.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buf) -> { buf.writeVarInt(message.menu); buf.writeUtf(message.name, ItemFilter.MAX_NAME); })
                .decoder(buf -> new Rename(buf.readVarInt(), buf.readUtf(ItemFilter.MAX_NAME)))
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null && player.containerMenu.containerId == message.menu && player.containerMenu instanceof FilterMenu menu)
                        menu.setName(player, message.name);
                }).add();
    }
    public static void rename(int menu, String name) { CHANNEL.sendToServer(new Rename(menu, name)); }
    public static void search(int menu,String query){CHANNEL.sendToServer(new Search(menu,query));}
    public static void request(int menu,int slot,net.minecraft.world.item.ItemStack item,int count,boolean cancel,boolean alt){CHANNEL.sendToServer(new Request(menu,slot,item.copyWithCount(1),count,cancel,alt));}
    private FilterNetwork() {}
}
