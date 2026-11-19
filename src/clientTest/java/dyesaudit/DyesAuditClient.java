package dyesaudit;

import com.mojang.brigadier.arguments.StringArgumentType;
import dyesmodern.*;
import java.security.MessageDigest;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.client.*;
import tc4revival.research.ResearchBookNavigation;

/** All UI/model work runs on the real client thread; outputs do not certify screenshot parity. */
public final class DyesAuditClient {
    private record Request(String id,String arguments){}
    private record ResearchRequest(String id,String key,int page,long deadline){}
    private static final ArrayDeque<Request> QUEUE=new ArrayDeque<>();
    private static ResearchRequest research;
    private static long ticks,shutdownAt=-1;
    private static String previousFileRequest="";
    private static java.util.concurrent.CompletableFuture<Void> reload;
    public static void register(){MinecraftForge.EVENT_BUS.addListener(DyesAuditClient::commands);MinecraftForge.EVENT_BUS.addListener(DyesAuditClient::tick);}
    private static void commands(RegisterClientCommandsEvent event){
        event.getDispatcher().register(Commands.literal("dyesaudit").executes(ctx->queue("help"))
                .then(Commands.argument("arguments",StringArgumentType.greedyString()).executes(ctx->queue(StringArgumentType.getString(ctx,"arguments")))));
    }
    private static int queue(String arguments){
        String id=DyesAuditHarness.requestId();QUEUE.add(new Request(id,arguments));
        DyesAuditHarness.record("client",id,arguments,true,Map.of("status","queued_for_client_tick"));return 1;
    }
    private static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;ticks++;var mc=Minecraft.getInstance();
        if(shutdownAt>=0&&ticks>=shutdownAt){shutdownAt=-1;mc.stop();return;}
        // Test transport preserves open menus and serializes operations on the real client tick.
        var directory=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        if(directory.toString().replace('\\','/').contains("/Data/Verification/20261031-01a19066-concilium/"))try{
            var path=directory.resolve("dyes-client-request.json");
            if(java.nio.file.Files.exists(path)){
                String text=java.nio.file.Files.readString(path);
                if(!text.equals(previousFileRequest)){
                    previousFileRequest=text;var json=com.google.gson.JsonParser.parseString(text).getAsJsonObject();
                    String id=json.get("id").getAsString();if(!id.matches("[A-Za-z0-9_-]{1,80}"))throw new IllegalArgumentException("Unsafe request ID");
                    QUEUE.add(new Request(id,json.get("arguments").getAsString()));
                }
            }
        }catch(Exception error){DyesAuditHarness.LOG.error("DYES_AUDIT file request failed",error);}
        if(!QUEUE.isEmpty()){var request=QUEUE.remove();execute(request);}
        if(research!=null){var pending=research;
            try{
                if(mc.screen instanceof ResearchBrowserScreen tree){tree.locateRecipe(pending.key());}
                if(mc.screen instanceof ResearchReaderScreen reader){
                    var navigation=(ResearchBookNavigation)field(reader,"navigation");
                    if(navigation.reading().orElse("").equals(pending.key())&&ClientBookSession.active()){
                        navigation.goPage(pending.page());var data=new LinkedHashMap<String,Object>(probe());data.put("research",pending.key());data.put("requestedPage",pending.page());data.put("spread",navigation.spread());data.put("visiblePages",navigation.visiblePages().size());
                        DyesAuditHarness.record("client",pending.id(),"research_ready",true,data);research=null;
                    }
                }
                if(research!=null&&ticks>pending.deadline())throw new IllegalStateException("Timed out waiting for authenticated real research reader: "+pending.key());
            }catch(Exception error){research=null;failure(pending.id(),"research_ready",error);}
        }
    }
    private static void execute(Request request){
        var mc=Minecraft.getInstance();String[] args=request.arguments().trim().split("\\s+");
        try{
            if(!Boolean.getBoolean("tc4.validationHarness"))throw new IllegalStateException("Test helper disabled");
            Map<String,Object> result;
            switch(args[0]){
                case "help"->result=Map.of("commands",List.of("resources","catalog [zero-based-page]","tooltip <legacyKey|held>","research <thaumicdyes:key> [zero-based-page]","probe","close","quit"),"serverCommands","/dyesaudit_scene help");
                case "resources"->{requireWorld();result=resources();}
                case "catalog"->{requireWorld();int page=args.length>1?Integer.parseInt(args[1]):0;mc.setScreen(new CatalogScreen(request.id(),page,null));result=Map.of("status","opened_real_item_renderer_catalog","page",page);}
                case "tooltip"->{requireWorld();if(args.length!=2)throw new IllegalArgumentException("tooltip requires legacyKey or held");ItemStack stack=args[1].equals("held")?mc.player.getMainHandItem().copy():new ItemStack(DyesItems.get(args[1]));if(stack.isEmpty())throw new IllegalArgumentException("Tooltip stack is empty");mc.setScreen(new CatalogScreen(request.id(),0,stack));result=Map.of("stack",DyesAuditHarness.stack(stack),"tooltip",tooltip(stack));}
                case "research"->{
                    requireWorld();if(args.length<2||args.length>3||!args[1].startsWith("thaumicdyes:"))throw new IllegalArgumentException("research requires a Dyes key and optional page");if(research!=null)throw new IllegalStateException("A research navigation request is already pending");
                    int page=args.length==3?Integer.parseInt(args[2]):0;if(page<0)throw new IllegalArgumentException("Page must be nonnegative");
                    research=new ResearchRequest(request.id(),args[1],page,ticks+200);
                    mc.getConnection().sendCommand("dyesaudit_scene research "+args[1]+" request="+request.id());result=Map.of("status","awaiting_server_book_open","research",args[1],"page",page,"knowledge","Server command explicitly grants this fixture research before using the real book item.");
                }
                case "probe"->{requireWorld();result=probe();}
                case "reload"->{reload=mc.reloadResourcePacks();result=Map.of("reloadRequested",true);}
                case "language"->{
                    if(args.length!=2||mc.getLanguageManager().getLanguage(args[1])==null)throw new IllegalArgumentException("Unknown language");
                    mc.getLanguageManager().setSelected(args[1]);mc.options.languageCode=args[1];reload=mc.reloadResourcePacks();result=Map.of("language",args[1],"reloadRequested",true);
                }
                case "close"->{research=null;mc.setScreen(null);result=Map.of("screenClosed",true);}
                case "quit"->{research=null;mc.setScreen(null);shutdownAt=ticks+2;result=Map.of("status","normal_client_stop_scheduled","remainingClientTicks",2);}
                default->throw new IllegalArgumentException("Unknown audit action "+args[0]);
            }
            boolean success=!result.containsKey("failures")||((Collection<?>)result.get("failures")).isEmpty();DyesAuditHarness.record("client",request.id(),request.arguments(),success,result);
            if(mc.player!=null)mc.player.displayClientMessage(Component.literal("DYES_AUDIT "+(success?"recorded ":"reported failures ")+request.id()),false);
        }catch(Exception error){failure(request.id(),request.arguments(),error);}
    }
    private static void failure(String id,String operation,Throwable error){DyesAuditHarness.LOG.error("DYES_AUDIT client failure {}",operation,error);DyesAuditHarness.record("client",id,operation,false,DyesAuditHarness.error(error));var player=Minecraft.getInstance().player;if(player!=null)player.displayClientMessage(Component.literal("DYES_AUDIT failed: "+error),false);}
    private static void requireWorld(){var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getConnection()==null)throw new IllegalStateException("A connected real client world is required");}
    private static Object field(Object owner,String name)throws ReflectiveOperationException{var field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner);}
    private static List<String> tooltip(ItemStack stack){return stack.getTooltipLines(Minecraft.getInstance().player,TooltipFlag.Default.ADVANCED).stream().map(Component::getString).toList();}
    private static Map<String,Object> resource(ResourceLocation id)throws Exception{
        var optional=Minecraft.getInstance().getResourceManager().getResource(id);if(optional.isEmpty())return Map.of("resource",id.toString(),"missing",true);
        var selected=optional.get();byte[] bytes;try(var stream=selected.open()){bytes=stream.readAllBytes();}
        return Map.of("resource",id.toString(),"missing",false,"sourcePack",selected.sourcePackId(),"bytes",bytes.length,"sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    }
    private static Map<String,Object> resources(){
        var mc=Minecraft.getInstance();var failures=new ArrayList<String>();var rows=new ArrayList<Object>();var textures=new TreeMap<String,Object>();int quads=0;
        for(var entry:DyesItems.ALL.entrySet()){
            var row=new LinkedHashMap<String,Object>();var item=entry.getValue().get();var stack=new ItemStack(item);var id=ForgeRegistries.ITEMS.getKey(item);row.put("legacyKey",entry.getKey());row.put("item",id.toString());
            try{
                var registered=mc.getModelManager().getModel(new ModelResourceLocation(id,"inventory"));var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
                boolean missing=registered==mc.getModelManager().getMissingModel()||model==mc.getModelManager().getMissingModel();row.put("missingModel",missing);row.put("customRenderer",model.isCustomRenderer());if(missing)failures.add("Missing item model "+id);
                row.put("modelResource",resource(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"models/item/"+id.getPath()+".json")));
                var sprites=new TreeSet<String>();var faces=new ArrayList<Direction>(List.of(Direction.values()));faces.add(null);int count=0;
                for(var face:faces)for(var quad:model.getQuads(null,face,RandomSource.create(42))){count++;var sprite=quad.getSprite().contents().name();sprites.add(sprite.toString());if(sprite.equals(MissingTextureAtlasSprite.getLocation()))failures.add("Missing quad texture "+id);}
                row.put("quads",count);quads+=count;row.put("sprites",sprites);row.put("tint0",mc.getItemColors().getColor(stack,0));row.put("tint1",mc.getItemColors().getColor(stack,1));row.put("tooltip",tooltip(stack));
                if(item instanceof DyesArmorItem armor){
                    row.put("family",armor.family);row.put("slot",armor.getEquipmentSlot().getName());
                    var original=new HumanoidModel<LivingEntity>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER));var actual=IClientItemExtensions.of(stack).getGenericArmorModel(mc.player,stack,armor.getEquipmentSlot(),original);row.put("armorModelClass",actual.getClass().getName());
                    if(actual instanceof dyesmodern.client.DyesArmorModel)row.put("modelKind",field(actual,"kind"));
                    for(String layer:new String[]{"base","overlay"}){String path=armor.getArmorTexture(stack,mc.player,armor.getEquipmentSlot(),layer.equals("base")?null:"overlay");var info=resource(ResourceLocation.parse(path));textures.put(path,info);if(Boolean.TRUE.equals(info.get("missing")))failures.add("Missing armor texture "+path);}
                }
            }catch(Exception error){row.putAll(DyesAuditHarness.error(error));failures.add(id+": "+error);DyesAuditHarness.LOG.error("DYES_AUDIT resource inspection {}",id,error);}
            rows.add(row);
        }
        var allTextures=mc.getResourceManager().listResources("textures",id->id.getNamespace().equals("thaumicdyes")&&id.getPath().endsWith(".png"));
        for(var id:allTextures.keySet())try{textures.put(id.toString(),resource(id));}catch(Exception error){failures.add("Texture read "+id+": "+error);DyesAuditHarness.LOG.error("DYES_AUDIT texture inspection {}",id,error);}
        if(rows.size()!=57)failures.add("Expected 57 Dyes registry items, found "+rows.size());
        var out=new LinkedHashMap<String,Object>();out.put("items",rows);out.put("itemCount",rows.size());out.put("quadCount",quads);out.put("textures",textures);out.put("textureCount",allTextures.size());out.put("failures",failures);out.put("requiresViewedScreenshots",true);out.put("environment",environment());return out;
    }
    private static Map<String,Object> environment(){
        var mc=Minecraft.getInstance();var window=mc.getWindow();var out=new LinkedHashMap<String,Object>();out.put("gui",List.of(window.getGuiScaledWidth(),window.getGuiScaledHeight()));out.put("framebuffer",List.of(window.getWidth(),window.getHeight()));out.put("guiScale",window.getGuiScale());
        out.put("language",mc.options.languageCode);out.put("fancyGraphics",Minecraft.useFancyGraphics());out.put("graphicsMode",mc.options.graphicsMode().get().toString());out.put("shaderRelatedMods",List.of("oculus","iris","embeddium","rubidium","optifine").stream().filter(id->ModList.get().isLoaded(id)).toList());
        out.put("resourcePacks",mc.getResourceManager().listPacks().map(pack->pack.packId()).toList());out.put("screen",mc.screen==null?"none":mc.screen.getClass().getName());
        if(reload!=null)out.put("reload",Map.of("done",reload.isDone(),"failed",reload.isCompletedExceptionally()));return out;
    }
    private static Map<String,Object> probe(){
        var mc=Minecraft.getInstance();var out=new LinkedHashMap<String,Object>(environment());out.put("player",DyesAuditHarness.entity(mc.player));out.put("worldTick",mc.level.getGameTime());out.put("dimension",mc.level.dimension().location().toString());
        out.put("runicCharge",ClientRunicShield.charge());out.put("runicMaximum",ClientRunicShield.maximum());out.put("bookActive",ClientBookSession.active());out.put("selectedSlot",mc.player.getInventory().selected);
        out.put("research",ClientKnowledge.snapshot().map(k->k.researchMarkers().stream().sorted().toList()).orElse(List.of()));
        var items=new ArrayList<Object>();for(int i=0;i<mc.player.getInventory().getContainerSize();i++)items.add(Map.of("slot",i,"stack",DyesAuditHarness.stack(mc.player.getInventory().getItem(i))));out.put("inventory",items);
        var slots=new ArrayList<Object>();for(var slot:mc.player.containerMenu.slots)slots.add(Map.of("index",slot.index,"x",slot.x,"y",slot.y,"stack",DyesAuditHarness.stack(slot.getItem())));out.put("menuSlots",slots);out.put("menu",mc.player.containerMenu.getClass().getName());
        var entities=new ArrayList<Object>();for(var entity:mc.level.entitiesForRendering())if(entity instanceof LivingEntity living&&entity.distanceToSqr(mc.player)<4096)entities.add(DyesAuditHarness.entity(living));out.put("nearbyLivingEntities",entities);
        out.put("heldTooltip",tooltip(mc.player.getMainHandItem()));return out;
    }
    /** Uses product item rendering and tooltips. Does not replace any product crafting or book screen. */
    private static final class CatalogScreen extends Screen {
        private final String id;private final int page;private final ItemStack focus;private boolean observed;
        CatalogScreen(String id,int page,ItemStack focus){super(Component.literal("Thaumic Dyes visual audit"));if(page<0)throw new IllegalArgumentException("Page must be nonnegative");this.id=id;this.page=page;this.focus=focus;}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
            try{
                graphics.fill(0,0,width,height,0xff20242c);graphics.drawString(font,"Thaumic Dyes: actual item renderer / tooltip",12,10,0xffffff,false);
                var shown=new ArrayList<Object>();
                if(focus!=null){graphics.renderItem(focus,24,50);graphics.renderTooltip(font,focus,52,48);shown.add(Map.of("stack",DyesAuditHarness.stack(focus),"tooltip",tooltip(focus)));}
                else{
                    int columns=Math.max(2,Math.min(6,(width-24)/70)),rows=Math.max(1,Math.min(5,(height-54)/44)),size=columns*rows,start=page*size;
                    var items=new ArrayList<>(DyesItems.ALL.entrySet());if(start>=items.size())throw new IllegalArgumentException("Catalog page outside current viewport's "+((items.size()+size-1)/size)+" pages");
                    graphics.drawString(font,"Page "+page+" / "+((items.size()-1)/size)+"   ["+start+".."+(Math.min(start+size,items.size())-1)+"]",12,24,0xc3c8d0,false);
                    ItemStack hover=ItemStack.EMPTY;
                    for(int i=start;i<Math.min(start+size,items.size());i++){var entry=items.get(i);var stack=new ItemStack(entry.getValue().get());int col=(i-start)%columns,row=(i-start)/columns,x=12+col*(width-24)/columns,y=42+row*44;
                        graphics.renderItem(stack,x+4,y);String label=ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();graphics.drawString(font,font.plainSubstrByWidth(i+" "+label,(width-24)/columns-4),x,y+20,0xffffff,false);
                        shown.add(Map.of("index",i,"legacyKey",entry.getKey(),"stack",DyesAuditHarness.stack(stack),"guiBounds",List.of(x,y,(width-24)/columns,40)));
                        if(mouseX>=x&&mouseX<x+(width-24)/columns&&mouseY>=y&&mouseY<y+40)hover=stack;
                    }
                    if(!hover.isEmpty())graphics.renderTooltip(font,hover,mouseX,mouseY);
                }
                if(!observed){observed=true;DyesAuditHarness.record("client",id,"catalog_frame_submitted",true,Map.of("page",page,"entries",shown,"environment",environment(),"instruction","Capture and inspect the actual framebuffer with MC Pilot. A submitted frame is not visual acceptance."));}
            }catch(Exception error){if(!observed){observed=true;failure(id,"catalog_render",error);}throw new IllegalStateException("Dyes audit catalog rendering failed",error);}
            super.render(graphics,mouseX,mouseY,partial);
        }
    }
    private DyesAuditClient(){}
}
