package technomancy.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import technomancy.*;

/** Isolated observation commands; excluded from the product jar. */
@Mod.EventBusSubscriber(modid=Technomancy.ID,value=Dist.CLIENT)
public class TechnomancyClientScenes {
    private static String book;
    private static Runnable queued;
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent e){
        e.getDispatcher().register(Commands.literal("technomancy_client")
            .then(Commands.literal("audit").executes(c->{queued=TechnomancyClientScenes::audit;return 1;}))
            .then(Commands.literal("book").then(Commands.argument("key",StringArgumentType.greedyString()).executes(c->{book=StringArgumentType.getString(c,"key");return 1;})))
            .then(Commands.literal("language").then(Commands.argument("code",StringArgumentType.word()).executes(c->{
                String code=StringArgumentType.getString(c,"code");if(!code.equals("zh_cn")&&!code.equals("en_us"))return 0;
                queued=()->{var mc=Minecraft.getInstance();mc.options.languageCode=code;mc.getLanguageManager().setSelected(code);mc.reloadResourcePacks();mc.options.save();};return 1;
            }))));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(queued!=null&&!(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)){var action=queued;queued=null;action.run();}
        if(book!=null&&mc.screen instanceof tc4revival.client.ResearchBrowserScreen tree){var target=book;book=null;tree.locateRecipe(target);}
    }
    private static void audit(){
        var mc=Minecraft.getInstance();var result=new com.google.gson.JsonObject();
        result.addProperty("language",mc.options.languageCode);
        result.addProperty("rawIronTags",new ItemStack(Items.RAW_IRON).getTags().map(t->t.location().toString()).toList().toString());
        result.addProperty("ironIngotTags",new ItemStack(Items.IRON_INGOT).getTags().map(t->t.location().toString()).toList().toString());
        result.addProperty("rawIronProcessing",OreProcessing.output(new ItemStack(Items.RAW_IRON),MachineKind.IGNIS_INCINERATOR,mc.level).save(new net.minecraft.nbt.CompoundTag()).toString());
        result.addProperty("purifiedSmelting",mc.level.getRecipeManager().getRecipes().stream().filter(r->r.getId().toString().startsWith("technomancy:purified/")).count());
        try{
            var field=TechnomancyJei.class.getDeclaredField("registered");field.setAccessible(true);
            var recipes=(java.util.Map<?,?>)field.get(null);var counts=new com.google.gson.JsonObject();recipes.forEach((k,v)->counts.addProperty(k.toString(),((java.util.List<?>)v).size()));result.add("jei",counts);
            var file=mc.gameDirectory.toPath().resolve("technomancy-audit-"+System.currentTimeMillis()+".json");java.nio.file.Files.writeString(file,result.toString());
        }catch(Exception failure){throw new IllegalStateException(failure);}
        org.slf4j.LoggerFactory.getLogger("TECHNOMANCY_CLIENT_AUDIT").info("{}",result);
        mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(result.toString()));
    }
}
