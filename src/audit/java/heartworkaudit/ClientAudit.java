package heartworkaudit;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="heartwork_audit",value=Dist.CLIENT)
public class ClientAudit {
    private static Screen lastBook;
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        var screen=Minecraft.getInstance().screen;
        if(screen instanceof tc4revival.client.ResearchBrowserScreen || screen instanceof tc4revival.client.ResearchReaderScreen)lastBook=screen;
    }
    @SubscribeEvent public static void commands(net.minecraftforge.client.event.RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hwclient")
            .then(Commands.literal("gallery").then(Commands.argument("page",IntegerArgumentType.integer(0)).executes(c -> {
                Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new Gallery(IntegerArgumentType.getInteger(c,"page"))));return 1;
            })))
            .then(Commands.literal("audit").executes(c -> {audit();return 1;}))
            .then(Commands.literal("book").then(Commands.argument("research",StringArgumentType.greedyString()).executes(c -> {
                String key=StringArgumentType.getString(c,"research");
                Minecraft.getInstance().tell(() -> {
                    var screen=lastBook;
                    if(screen!=null)Minecraft.getInstance().setScreen(screen);
                    if(screen instanceof tc4revival.client.ResearchBrowserScreen tree)tree.locateRecipe(key);
                    else if(screen instanceof tc4revival.client.ResearchReaderScreen reader)reader.follow(key);
                    else throw new IllegalStateException("Open the real book first");
                });return 1;
            })))
            .then(Commands.literal("quit").executes(c -> {Minecraft.getInstance().tell(() -> Minecraft.getInstance().stop());return 1;})));
    }
    private static List<ItemStack> gallery() {
        var mc=Minecraft.getInstance();var items=new ArrayList<ItemStack>();
        for(var id:net.minecraftforge.registries.ForgeRegistries.ITEMS.getKeys().stream().filter(id -> id.getNamespace().equals("heartwork")).sorted().toList())
            items.add(new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id)));
        var registry=mc.level.registryAccess().registryOrThrow(mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition.REGISTRY_KEY);
        for(var id:registry.keySet().stream().filter(id -> id.getNamespace().equals("heartwork")).sorted().toList()) {
            var blade=registry.get(id).getBlade();if(!blade.isEmpty())items.add(blade);
        }
        return items;
    }
    private static void audit() {
        var mc=Minecraft.getInstance();var failures=new ArrayList<String>();int models=0,resources=0,names=0;
        for(var stack:gallery()) {
            models++;var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
            if(model==mc.getModelManager().getMissingModel())failures.add("model "+stack);
            if(stack.getHoverName().getString().matches("(?:item|block)\\.heartwork\\..*"))failures.add("name "+stack.getHoverName().getString());else names++;
            for(var line:stack.getTooltipLines(mc.player,net.minecraft.world.item.TooltipFlag.Default.NORMAL))
                if(line.getString().matches(".*(?:slash_art|item|block)\\.heartwork\\..*"))failures.add("tooltip "+line.getString());
            if(!model.isCustomRenderer()) {
                var faces=new ArrayList<net.minecraft.core.Direction>(List.of(net.minecraft.core.Direction.values()));faces.add(null);
                for(var face:faces)for(var quad:model.getQuads(null,face,net.minecraft.util.RandomSource.create(1)))
                    if(quad.getSprite().contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()))failures.add("quad "+stack);
            }
        }
        try(var reader=new java.io.InputStreamReader(heartwork.Heartwork.class.getResourceAsStream("/data/heartwork/thaumcraft/original_resources.json"),java.nio.charset.StandardCharsets.UTF_8)) {
            var root=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var sources=new ArrayList<com.google.gson.JsonObject>();sources.add(root);
            for(var source:root.getAsJsonArray("additional_sources"))sources.add(source.getAsJsonObject());
            for(var source:sources)for(String path:source.getAsJsonObject("mapping").keySet()) {
                String[] split=path.split("/",3);var id=new ResourceLocation(split[1],split[2]);resources++;
                if(mc.getResourceManager().getResource(id).isEmpty())failures.add("resource "+id);
            }
        }catch(Exception ex){throw new IllegalStateException(ex);}
        var result=new LinkedHashMap<String,Object>();result.put("models",models);result.put("resources",resources);result.put("translated_names",names);result.put("failures",failures);
        try{java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("heartwork-asset-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(Exception ex){throw new IllegalStateException(ex);}
        if(!failures.isEmpty())throw new IllegalStateException("Asset audit failed: "+failures);
    }
    private static class Gallery extends Screen {
        private final int page;private List<ItemStack> items;
        Gallery(int page){super(Component.literal("Heartwork audit gallery"));this.page=page;}
        @Override protected void init(){items=gallery();}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int mx,int my,float tick) {
            g.fill(0,0,width,height,0xFF24242C);g.drawString(font,"Heartwork actual item renderer — page "+page+" / "+((items.size()-1)/40),12,8,0xFFFFFF);
            for(int i=page*40;i<Math.min(items.size(),(page+1)*40);i++) {
                int local=i%40,x=12+(local%8)*(width-24)/8,y=28+(local/8)*(height-42)/5;
                var stack=items.get(i);g.renderItem(stack,x+8,y);g.renderItemDecorations(font,stack,x+8,y);
                String label=font.plainSubstrByWidth(stack.getHoverName().getString(),(width-24)/8-4);g.drawString(font,label,x,y+20,0xFFFFFF);
                if(mx>=x&&mx<x+(width-24)/8&&my>=y&&my<y+30)g.renderTooltip(font,stack,mx,my);
            }
        }
    }
}
