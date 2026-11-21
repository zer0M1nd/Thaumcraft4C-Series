package conciliumaudit;

import com.google.gson.*;
import com.mojang.brigadier.arguments.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.client.*;

/** File requests run on the client tick. No arbitrary eval and no business action shortcuts. */
@Mod.EventBusSubscriber(modid=ConciliumScenes.ID,value=Dist.CLIENT)
public final class ConciliumClientAudit {
    private static String previous="";
    private static JsonObject queued;
    private static CompletableFuture<Void> reload;
    private static int stopAfterTicks;
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event){
        var root=Commands.literal("conciliumaudit");
        for(String op:List.of("resources","probe","reload","close","quit"))root.then(Commands.literal(op).executes(c->queue(op,null)));
        root.then(Commands.literal("catalog").executes(c->queue("catalog",new JsonPrimitive(0))).then(Commands.argument("page",IntegerArgumentType.integer(0)).executes(c->queue("catalog",new JsonPrimitive(IntegerArgumentType.getInteger(c,"page"))))));
        for(String op:List.of("book","tooltip","language"))root.then(Commands.literal(op).then(Commands.argument("value",StringArgumentType.string()).executes(c->queue(op,new JsonPrimitive(StringArgumentType.getString(c,"value"))))));
        root.then(Commands.literal("shader").then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->queue("shader",new JsonPrimitive(BoolArgumentType.getBool(c,"enabled"))))));
        root.then(Commands.literal("turn").then(Commands.argument("direction",IntegerArgumentType.integer(-1,1)).executes(c->queue("turn",new JsonPrimitive(IntegerArgumentType.getInteger(c,"direction"))))));
        event.getDispatcher().register(root);
    }
    private static int queue(String op,JsonElement value){ConciliumScenes.guard();if(queued!=null)throw new IllegalStateException("Previous client request pending");queued=new JsonObject();queued.addProperty("id",UUID.randomUUID().toString());queued.addProperty("op",op);if(value!=null)queued.add("value",value);return 1;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        // A quit request is acknowledged and flushed below before shutdown on a later tick.
        if(stopAfterTicks>0){if(--stopAfterTicks==0)mc.stop();return;}
        if(mc.screen instanceof ChatScreen)return;
        try{
            Path file=mc.gameDirectory.toPath().resolve("concilium-client-request.json");
            if(queued==null&&Files.isRegularFile(file)){String raw=Files.readString(file);if(!previous.equals(raw)){previous=raw;queued=JsonParser.parseString(raw).getAsJsonObject();}}
            if(queued!=null){var request=queued;queued=null;execute(request);}
        }catch(Exception error){ConciliumScenes.record("client",Map.of("success",false,"error",error.toString()));}
    }
    private static void execute(JsonObject request){
        ConciliumScenes.guard();var mc=Minecraft.getInstance();var out=new LinkedHashMap<String,Object>();out.put("request",request);out.put("observationOnly",true);
        try{
            String op=request.get("op").getAsString();JsonElement value=request.get("value");
            switch(op){
                case "resources"->out.put("resources",resources());
                case "catalog"->mc.setScreen(new ConciliumCatalogScreen(value==null?0:value.getAsInt()));
                case "tooltip"->{var stack=ConciliumScenes.stack(value.getAsString());if(stack.isEmpty())throw new IllegalArgumentException("Unknown item");out.put("tooltip",stack.getTooltipLines(mc.player,TooltipFlag.Default.ADVANCED).stream().map(Component::getString).toList());}
                case "shader"->{tc4revival.config.TCConfig.CLIENT.shaders.set(value.getAsBoolean());out.put("shaderChangedInTestMemory",value.getAsBoolean());}
                case "binding"->{
                    var binding=value.getAsJsonObject();String name=binding.get("name").getAsString();
                    if(!name.contains("thaumicconcilium"))throw new IllegalArgumentException("Only this addon's isolated key mappings");
                    var key=Arrays.stream(mc.options.keyMappings).filter(k->k.getName().equals(name)).findFirst().orElseThrow();
                    key.setKeyModifierAndCode(net.minecraftforge.client.settings.KeyModifier.valueOf(binding.get("modifier").getAsString()),com.mojang.blaze3d.platform.InputConstants.getKey(binding.get("key").getAsString()));
                    net.minecraft.client.KeyMapping.resetMapping();out.put("testOnlyBindingChanged",name);
                }
                case "reload"->{reload=mc.reloadResourcePacks();out.put("reloadRequested",true);}
                case "language"->{String language=value.getAsString();if(mc.getLanguageManager().getLanguage(language)==null)throw new IllegalArgumentException("Unknown language "+language);mc.getLanguageManager().setSelected(language);mc.options.languageCode=language;reload=mc.reloadResourcePacks();}
                case "book"->{if(!(mc.screen instanceof ResearchBrowserScreen tree))throw new IllegalStateException("Open the real Thaumonomicon tree first; no synthesized book session");tree.locateRecipe(value.getAsString());}
                case "turn"->{if(!(mc.screen instanceof ResearchReaderScreen reader))throw new IllegalStateException("No research reader");navigation(reader).turn(value.getAsInt());}
                case "close"->{if(mc.screen!=null)mc.screen.onClose();}
                case "quit"->{stopAfterTicks=2;out.put("quitScheduledAfterTicks",2);}
                case "probe"->{}
                default->throw new IllegalArgumentException("Unknown op "+op);
            }
            out.put("state",probe());out.put("success",true);
        }catch(Exception error){out.put("success",false);out.put("error",error.toString());out.put("trace",Arrays.stream(error.getStackTrace()).limit(8).map(Object::toString).toList());}
        ConciliumScenes.record("client",out);
        try{Files.writeString(mc.gameDirectory.toPath().resolve("concilium-client-response.json"),ConciliumScenes.JSON.toJson(out)+"\n");}catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    private static tc4revival.research.ResearchBookNavigation navigation(Object screen)throws Exception{var f=screen.getClass().getDeclaredField("navigation");f.setAccessible(true);return (tc4revival.research.ResearchBookNavigation)f.get(screen);}
    private static Object probe(){
        var mc=Minecraft.getInstance();var out=new LinkedHashMap<String,Object>();out.put("screen",mc.screen==null?"none":mc.screen.getClass().getName());out.put("overlay",mc.getOverlay()==null?"none":mc.getOverlay().getClass().getName());out.put("framebuffer",List.of(mc.getWindow().getWidth(),mc.getWindow().getHeight()));out.put("gui",List.of(mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight()));out.put("language",mc.getLanguageManager().getSelected());out.put("shader",tc4revival.config.TCConfig.CLIENT.shaders.get());
        if(reload!=null)out.put("reload",Map.of("done",reload.isDone(),"failed",reload.isCompletedExceptionally()));
        if(mc.screen instanceof ConciliumCatalogScreen catalog)out.put("catalog",Map.of("page",catalog.page(),"items",catalog.visibleIds()));
        try{if(mc.screen instanceof ResearchReaderScreen reader){var nav=navigation(reader);out.put("book",Map.of("key",nav.reading().orElse(""),"spread",nav.spread(),"pages",nav.visiblePages().size()));}}catch(Exception e){out.put("bookProbeError",e.toString());}
        if(mc.player!=null){
            var p=mc.player;out.put("player",Map.of("name",p.getGameProfile().getName(),"uuid",p.getUUID().toString(),"position",p.position().toString(),"yaw",p.getYRot(),"pitch",p.getXRot(),"tick",p.tickCount,"using",p.isUsingItem(),"shift",p.isShiftKeyDown()));
            out.put("inventory",p.getInventory().save(new net.minecraft.nbt.ListTag()).toString());out.put("menu",Map.of("class",p.containerMenu.getClass().getName(),"id",p.containerMenu.containerId,"slots",p.containerMenu.slots.stream().map(s->Map.of("slot",s.index,"x",s.x,"y",s.y,"stack",s.getItem().save(new net.minecraft.nbt.CompoundTag()).toString())).toList()));
            out.put("runicShield",Map.of("charge",ClientRunicShield.charge(),"maximum",ClientRunicShield.maximum()));
            var entities=new ArrayList<Object>();for(var e:mc.level.entitiesForRendering())if(e.getTags().contains(ConciliumScenes.TAG)||ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).getNamespace().equals("thaumicconcilium"))entities.add(Map.of("id",e.getId(),"uuid",e.getUUID().toString(),"type",ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).toString(),"position",e.position().toString(),"invisible",e.isInvisible(),"invisibleToViewer",e.isInvisibleTo(p)));out.put("entities",entities);
        }
        out.put("keys",Arrays.stream(mc.options.keyMappings).filter(k->k.getName().contains("thaumicconcilium")).map(k->Map.of("name",k.getName(),"key",k.getKey().getName(),"modifier",k.getKeyModifier().name(),"translated",k.getTranslatedKeyMessage().getString(),"down",k.isDown())).toList());return out;
    }
    private static Object resources()throws Exception{
        var mc=Minecraft.getInstance();var rows=new ArrayList<Object>();int failures=0;
        for(var stack:ConciliumScenes.items()){
            var id=ForgeRegistries.ITEMS.getKey(stack.getItem());var row=new LinkedHashMap<String,Object>();row.put("id",id.toString());row.put("name",stack.getHoverName().getString());row.put("translationPresent",net.minecraft.client.resources.language.I18n.exists(stack.getDescriptionId()));
            String gate=ConciliumScenes.unavailable(stack);if(!gate.isEmpty()){row.put("gated",gate);rows.add(row);continue;}
            try{
                var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);boolean missing=model==mc.getModelManager().getMissingModel();row.put("missingModel",missing);row.put("customRenderer",model.isCustomRenderer());if(missing)failures++;
                var sprites=new TreeSet<String>();if(!model.isCustomRenderer())for(Direction face:new Direction[]{null,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST})for(var quad:model.getQuads(null,face,RandomSource.create(42))){String sprite=quad.getSprite().contents().name().toString();sprites.add(sprite);if(quad.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()))failures++;}row.put("sprites",sprites);
                var transforms=new LinkedHashMap<String,Object>();for(var ctx:ItemDisplayContext.values()){var t=model.getTransforms().getTransform(ctx);transforms.put(ctx.name(),Map.of("scale",t.scale.toString(),"rotation",t.rotation.toString(),"translation",t.translation.toString()));}row.put("transforms",transforms);
                var path=ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"models/item/"+id.getPath()+".json");row.put("modelPacks",mc.getResourceManager().getResourceStack(path).stream().map(r->r.sourcePackId()).toList());
            }catch(Exception e){row.put("error",e.toString());failures++;}rows.add(row);
        }
        var descriptorPath=net.minecraftforge.fml.ModList.get().getModFileById("thaumicconcilium").getFile().findResource("data","thaumicconcilium","thaumcraft","original_resources.json");
        var external=new ArrayList<Object>();try(var in=Files.newInputStream(descriptorPath)){
            if(in==null)throw new IllegalStateException("Missing product original_resources descriptor");var descriptor=JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();mapping(descriptor,external);if(descriptor.has("additional_sources"))for(var source:descriptor.getAsJsonArray("additional_sources"))mapping(source.getAsJsonObject(),external);
        }
        return Map.of("namespace","thaumicconcilium","models",rows,"modelFailures",failures,"externalResources",external,"scope","Actual runtime model/resource queries; screenshots, animations, custom renderers and gameplay remain separate acceptance requirements");
    }
    private static void mapping(JsonObject descriptor,List<Object> out){
        if(!descriptor.has("mapping"))return;var manager=Minecraft.getInstance().getResourceManager();
        for(var entry:descriptor.getAsJsonObject("mapping").entrySet()){
            String target=entry.getKey();if(!target.startsWith("assets/"))continue;String[] parts=target.split("/",3);var id=ResourceLocation.fromNamespaceAndPath(parts[1],parts[2]);var resource=manager.getResource(id);out.add(Map.of("target",id.toString(),"present",resource.isPresent(),"pack",resource.map(r->r.sourcePackId()).orElse(""),"declaredSource",entry.getValue().toString()));
        }
    }
}
