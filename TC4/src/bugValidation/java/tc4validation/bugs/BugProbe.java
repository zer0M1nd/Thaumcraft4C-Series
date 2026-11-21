package tc4validation.bugs;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.research.*;

/** Explicitly opted-in isolated-runtime setup and authoritative observation. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public final class BugProbe {
    static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    static String previous="";
    static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(!Boolean.getBoolean("tc4.bugValidation")||event.phase!=TickEvent.Phase.END||++ticks%5!=0)return;
        Path path=Path.of("bug-server-request.json");
        if(!Files.isRegularFile(path))return;
        JsonObject out=new JsonObject();
        try {
            String raw=Files.readString(path);if(raw.equals(previous))return;previous=raw;
            JsonObject request=JsonParser.parseString(raw).getAsJsonObject();out.add("request",request);
            MinecraftServer server=event.getServer();ServerLevel level=server.overworld();
            if(request.has("commands")) {
                JsonArray returns=new JsonArray();
                for(JsonElement command:request.getAsJsonArray("commands"))
                    returns.add(server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command.getAsString()));
                out.add("commandResults",returns);
            }
            if(request.has("researchSetup"))for(ServerPlayer p:server.getPlayerList().getPlayers()) {
                KnowledgeSavedData.get(server).update(p.getUUID(),p.getRandom()::nextInt,k->{
                    for(var entry:ResearchDefinitions.server().catalog().all())k.rememberResearch(entry.key());
                    for(var aspect:tc4revival.aspect.TCAspects.catalog().all())k.setPool(aspect,(short)100);
                    return true;
                });KnowledgeSync.flushBeforeScreen(p);
            }
            if(request.has("notes"))for(ServerPlayer p:server.getPlayerList().getPlayers())for(var specRaw:request.getAsJsonArray("notes")) {
                var spec=specRaw.getAsJsonObject();String key=spec.get("key").getAsString();
                var definition=ResearchDefinitions.server().catalog().require(key);
                var random=new Random(spec.has("seed")?spec.get("seed").getAsLong():42);
                var note=new ResearchNotes(tc4revival.aspect.TCAspects.catalog()).generate(definition,random::nextInt,tc4revival.config.TCConfig.SERVER.randomizedResearchNotes.get()).orElseThrow();
                p.getInventory().add(tc4revival.content.ResearchNotesItem.create(note));
                KnowledgeSavedData.get(server).update(p.getUUID(),p.getRandom()::nextInt,k->k.forgetResearch(key));KnowledgeSync.flushBeforeScreen(p);
            }
            if(request.has("curios"))for(ServerPlayer p:server.getPlayerList().getPlayers()) {
                var spec=request.getAsJsonObject("curios");var inventory=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
                var handler=inventory.getStacksHandler(spec.get("slot").getAsString()).orElseThrow();
                var stacks=spec.has("cosmetic")&&spec.get("cosmetic").getAsBoolean()?handler.getCosmeticStacks():handler.getStacks();
                stacks.setStackInSlot(0,new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(spec.get("item").getAsString()))));
            }
            if(request.has("handlers")) {
                JsonArray operations=new JsonArray();
                for(var rawOp:request.getAsJsonArray("handlers")) {
                    var op=rawOp.getAsJsonObject();var xyz=op.getAsJsonArray("pos");
                    var pos=new BlockPos(xyz.get(0).getAsInt(),xyz.get(1).getAsInt(),xyz.get(2).getAsInt());
                    String side=op.get("side").getAsString();
                    var handler=level.getBlockEntity(pos).getCapability(ForgeCapabilities.ITEM_HANDLER,side.equals("null")?null:Direction.byName(side)).resolve().orElseThrow();
                    int slot=op.get("slot").getAsInt();boolean simulate=op.has("simulate")&&op.get("simulate").getAsBoolean();
                    JsonObject row=new JsonObject();row.add("operation",op);row.add("before",stack(handler.getStackInSlot(slot)));
                    ItemStack result;
                    if(op.get("action").getAsString().equals("extract"))result=handler.extractItem(slot,op.get("count").getAsInt(),simulate);
                    else {
                        var item=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(op.get("item").getAsString())),op.get("count").getAsInt());
                        if(op.has("tag"))item.setTag(TagParser.parseTag(op.get("tag").getAsString()));
                        result=handler.insertItem(slot,item,simulate);
                    }
                    row.add("returned",stack(result));row.add("after",stack(handler.getStackInSlot(slot)));operations.add(row);
                }out.add("handlerOperations",operations);
            }
            out.addProperty("time",java.time.Instant.now().toString());out.addProperty("gameTime",level.getGameTime());
            out.addProperty("profile",tc4revival.config.TCConfig.SERVER.revival.profile().name());
            out.add("blocks",blocks(level,request));
            JsonArray players=new JsonArray();for(ServerPlayer p:server.getPlayerList().getPlayers()) {
                JsonObject row=new JsonObject();row.addProperty("name",p.getScoreboardName());row.addProperty("position",p.position().toString());
                row.addProperty("health",p.getHealth());row.addProperty("inventory",p.getInventory().save(new ListTag()).toString());
                CompoundTag full=new CompoundTag();p.saveWithoutId(full);row.addProperty("forgeCaps",full.getCompound("ForgeCaps").toString());
                row.addProperty("access",ResearchTableAccess.server(p).toString());
                row.addProperty("revealing",tc4revival.content.RevealingGear.visible(p,false));
                KnowledgeSavedData.get(server).snapshot(p.getUUID()).ifPresent(k->row.addProperty("knowledge",KnowledgeNbt.write(k).toString()));
                players.add(row);
            }out.add("players",players);
            JsonArray entities=new JsonArray();for(var entity:level.getAllEntities())if(entity instanceof LivingEntity living) {
                JsonObject row=new JsonObject();row.addProperty("id",entity.getId());row.addProperty("type",net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString());
                row.addProperty("position",entity.position().toString());row.addProperty("health",living.getHealth());row.addProperty("maxHealth",living.getMaxHealth());
                row.addProperty("armor",living.getArmorValue());row.addProperty("speed",living.getAttributeValue(Attributes.MOVEMENT_SPEED));
                if(living.getAttribute(Attributes.ATTACK_DAMAGE)!=null)row.addProperty("attack",living.getAttributeValue(Attributes.ATTACK_DAMAGE));
                if(living instanceof tc4revival.content.GolemEntity golem){row.addProperty("navigationSpeed",golem.navigationSpeed());row.addProperty("configuration",golem.configuration().toString());}
                CompoundTag nbt=new CompoundTag();entity.saveWithoutId(nbt);row.addProperty("nbt",nbt.toString());entities.add(row);
            }out.add("entities",entities);
            out.addProperty("success",true);
        } catch(Throwable error) {
            out.addProperty("success",false);out.addProperty("error",error.toString());
            org.slf4j.LoggerFactory.getLogger("BUG_PROBE").error("Isolated probe failure",error);
        }
        try{Files.writeString(Path.of("bug-server-response.json"),GSON.toJson(out));}catch(Exception error){throw new IllegalStateException(error);}
    }
    public static JsonArray blocks(Level level,JsonObject request) {
        JsonArray result=new JsonArray();if(!request.has("blocks")||level==null)return result;
        for(JsonElement raw:request.getAsJsonArray("blocks")) {
            JsonArray xyz=raw.getAsJsonArray();BlockPos pos=new BlockPos(xyz.get(0).getAsInt(),xyz.get(1).getAsInt(),xyz.get(2).getAsInt());
            JsonObject row=new JsonObject();row.add("pos",xyz);row.addProperty("state",level.getBlockState(pos).toString());
            if(request.has("collision")) {
                var state=level.getBlockState(pos);
                row.addProperty("collisionEmpty",state.getCollisionShape(level,pos,net.minecraft.world.phys.shapes.CollisionContext.empty()).isEmpty());
                row.addProperty("cachedCollisionEmpty",state.getCollisionShape(level,pos).isEmpty());
                try {
                    var cls=Class.forName("timodern.InfusionData");
                    var data=cls.getMethod("at",Level.class,BlockPos.class).invoke(null,level,pos);
                    row.addProperty("infusion",data==null?"none":String.valueOf(data.getClass().getMethod("save").invoke(data)));
                }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            }
            var be=level.getBlockEntity(pos);
            if(be!=null) {
                row.addProperty("class",be.getClass().getName());row.addProperty("nbt",be.saveWithoutMetadata().toString());
                if(be instanceof tc4revival.content.InfusionMatrixBlockEntity matrix && level instanceof ServerLevel server){
                    row.addProperty("validAltar",matrix.validLocation());row.addProperty("layout",tc4revival.content.InfusionLayout.scan(level,pos).toString());
                    JsonObject previews=new JsonObject();for(var p:server.players())previews.addProperty(p.getScoreboardName(),matrix.previewFor(p).toString());row.add("previews",previews);
                }
                JsonObject capabilities=new JsonObject();
                for(Direction side:Direction.values()) {
                    JsonObject cap=new JsonObject();var handler=be.getCapability(ForgeCapabilities.ITEM_HANDLER,side).resolve();
                    cap.addProperty("present",handler.isPresent());
                    if(handler.isPresent()){JsonArray items=new JsonArray();for(int i=0;i<handler.get().getSlots();i++)items.add(stack(handler.get().getStackInSlot(i)));cap.add("items",items);}
                    capabilities.add(side.getName(),cap);
                }row.add("itemHandlers",capabilities);
                if(be instanceof Container container){JsonArray items=new JsonArray();for(int i=0;i<container.getContainerSize();i++)items.add(stack(container.getItem(i)));row.add("inventory",items);}
            }result.add(row);
        }return result;
    }
    static JsonObject stack(ItemStack stack) {
        JsonObject out=new JsonObject();out.addProperty("item",net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
        out.addProperty("count",stack.getCount());out.addProperty("nbt",stack.save(new CompoundTag()).toString());return out;
    }
}
