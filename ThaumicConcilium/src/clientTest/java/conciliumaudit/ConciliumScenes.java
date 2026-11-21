package conciliumaudit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import conciliummodern.*;

/** Fixture preparation and observation only. Never executes the behavior being accepted. */
@Mod(ConciliumScenes.ID)
@Mod.EventBusSubscriber(modid=ConciliumScenes.ID)
public final class ConciliumScenes {
    public static final String ID="concilium_client_audit", TAG="concilium_audit_fixture";
    public static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final BlockPos BASE=new BlockPos(0,80,0);
    public ConciliumScenes(){guard();}
    public static void guard(){
        String dir=FMLPaths.GAMEDIR.get().toAbsolutePath().normalize().toString().replace('\\','/').toLowerCase(Locale.ROOT);
        if(!Boolean.getBoolean("tc4.validationHarness")||!dir.contains("/data/verification/20261031-01a19066-concilium/"))
            throw new IllegalStateException("Concilium audit requires the owned isolated run and tc4.validationHarness=true");
    }
    public static ResourceLocation id(String name){return ResourceLocation.parse(name.contains(":")?name:"thaumicconcilium:"+name);}
    public static ItemStack stack(String name){var key=id(name);return ForgeRegistries.ITEMS.containsKey(key)?new ItemStack(ForgeRegistries.ITEMS.getValue(key)):ItemStack.EMPTY;}
    public static List<ItemStack> items(){return ForgeRegistries.ITEMS.getKeys().stream().filter(k->k.getNamespace().equals("thaumicconcilium")).sorted(Comparator.comparing(Object::toString)).map(k->{var s=stack(k.toString());if(s.getItem() instanceof CastingBracelet b&&b.available())b.initializeParts(s);return s;}).toList();}
    public static String unavailable(ItemStack stack){
        String name=ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();var mods=net.minecraftforge.fml.ModList.get();
        if(stack.getItem() instanceof CastingBracelet b&&!b.available())return "bracelet host absent";
        if(stack.getItem() instanceof DrainageSyringe&&!DrainageSyringe.available())return "syringe hosts absent";
        if(stack.getItem() instanceof DimensionalGem&&!ConciliumGems.available())return "gadomancy absent";
        if((name.startsWith("pontifex_")||name.equals("crimson_annales"))&&!CrimsonContent.available())return "taintedmagic absent";
        if((name.equals("flesh_crucible")||name.equals("solid_void"))&&!mods.isLoaded("taintedmagic"))return "taintedmagic absent";
        if(name.equals("protobody")&&!(mods.isLoaded("taintedmagic")&&mods.isLoaded("thaumichorizons")))return "taintedmagic/horizons absent";
        if(name.startsWith("red_powered_mind")&&!(mods.isLoaded("automagy")&&mods.isLoaded("thaumichorizons")))return "automagy/horizons absent";
        return "";
    }
    public static void record(String name,Object result){
        guard();try{Path out=FMLPaths.GAMEDIR.get().resolve("concilium-audit");Files.createDirectories(out);String text=JSON.toJson(result)+"\n";
            Files.writeString(out.resolve(System.currentTimeMillis()+"-"+name+"-"+UUID.randomUUID()+".json"),text);
            Files.writeString(out.resolve("latest-"+name+".json"),text);
        }catch(java.io.IOException e){throw new RuntimeException(e);}
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("conciliumaudit_scene").requires(s->s.hasPermission(2))
            .then(Commands.argument("batch",StringArgumentType.word()).suggests((c,b)->{for(String name:List.of("platform","all","catalog","devices","foci","mobs","effects","probe","labels_off","labels_on","animate","freeze","reset_entities"))b.suggest(name);return b.buildFuture();})
            .executes(c->run(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"batch")))));
    }
    private static int run(ServerPlayer player,String batch){
        guard();var out=new LinkedHashMap<String,Object>();out.put("requestId",UUID.randomUUID().toString());out.put("batch",batch);out.put("player",player.getUUID().toString());out.put("dimension",player.level().dimension().location().toString());out.put("tick",player.level().getGameTime());out.put("fixtureOnly",true);
        try{
            switch(batch){
                case "platform"->{platform(player);out.put("platform",List.of(0,80,0,95,80,95));}
                case "all"->{catalog(player,out);devices(player,out);foci(player,out);mobs(player,out);effects(player,out);}
                case "catalog"->catalog(player,out);
                case "devices"->devices(player,out);
                case "foci"->foci(player,out);
                case "mobs"->mobs(player,out);
                case "effects"->effects(player,out);
                case "probe"->out.put("state",probe(player));
                case "labels_off","labels_on","animate","freeze","reset_entities"->{int count=0;for(var entity:player.serverLevel().getEntities((Entity)null,new AABB(BASE,BASE.offset(96,32,96)),e->e.getTags().contains(TAG))){
                    if(batch.equals("reset_entities"))entity.discard();else if(batch.startsWith("labels"))entity.setCustomNameVisible(batch.equals("labels_on"));else if(entity instanceof Mob mob)mob.setNoAi(batch.equals("freeze"));count++;
                }out.put("changedEntities",count);}
                default->throw new IllegalArgumentException("Unknown batch "+batch);
            }
            out.put("success",true);record("scene",out);player.sendSystemMessage(Component.literal("Concilium audit fixture "+batch+" prepared; observe product behavior separately."));return 1;
        }catch(Exception failure){out.put("success",false);out.put("error",failure.toString());record("scene",out);player.sendSystemMessage(Component.literal("Fixture failed: "+failure));return 0;}
    }
    private static void platform(ServerPlayer p){
        var level=p.serverLevel();for(var pos:BlockPos.betweenClosed(BASE,BASE.offset(95,0,95)))level.setBlock(pos,Blocks.SMOOTH_STONE.defaultBlockState(),3);
        level.setDayTime(6000);level.setWeatherParameters(12000,0,false,false);
        p.teleportTo(level,8.5,81,8.5,0,0);p.getPersistentData().putBoolean("ConciliumAuditPlatform",true);
    }
    private static void requirePlatform(ServerPlayer p){if(!p.getPersistentData().getBoolean("ConciliumAuditPlatform")||!p.serverLevel().getBlockState(BASE).is(Blocks.SMOOTH_STONE))throw new IllegalStateException("Run platform first in this dimension");}
    private static void chest(ServerPlayer p,BlockPos at,List<ItemStack> values,String label){
        p.serverLevel().setBlock(at,Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH),3);
        if(!(p.serverLevel().getBlockEntity(at) instanceof ChestBlockEntity chest))throw new IllegalStateException("No chest at "+at);
        chest.clearContent();chest.setCustomName(Component.literal(label));for(int i=0;i<Math.min(27,values.size());i++)chest.setItem(i,values.get(i).copy());chest.setChanged();
    }
    private static void catalog(ServerPlayer p,Map<String,Object> out){requirePlatform(p);var items=items();var rows=new ArrayList<Object>();for(int first=0;first<items.size();first+=27){var at=BASE.offset(2+(first/27)*4,1,2);var page=items.subList(first,Math.min(first+27,items.size()));chest(p,at,page,"Concilium catalog "+(first/27));rows.add(Map.of("pos",at.toShortString(),"items",page.stream().map(s->ForgeRegistries.ITEMS.getKey(s.getItem()).toString()).toList()));}out.put("catalog",rows);}
    private static void devices(ServerPlayer p,Map<String,Object> out){
        requirePlatform(p);var rows=new ArrayList<Object>();int i=0;
        for(String name:List.of("destabilized_crystal","vis_condenser","lithographer","quicksilver_crucible","flesh_crucible","solid_void","red_powered_mind")){
            var item=stack(name);if(!item.isEmpty()&&!unavailable(item).isEmpty()){rows.add(Map.of("id",name,"skipped",unavailable(item)));continue;}
            var key=id(name);if(!ForgeRegistries.BLOCKS.containsKey(key))continue;var block=ForgeRegistries.BLOCKS.getValue(key);var at=BASE.offset(4+i++*7,1,16);
            p.serverLevel().setBlock(at,block.defaultBlockState(),3);rows.add(Map.of("id",name,"pos",at.toShortString(),"preparedState","empty; not a successful craft"));
        }
        // Two unformed ritual structures, leaving the real wand interaction to the client.
        var center=BASE.offset(8,1,25);for(var pos:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))p.serverLevel().setBlock(pos,(pos.equals(center)?Blocks.BEDROCK:tc4revival.content.TCEldritch.STONE.get()).defaultBlockState(),3);
        rows.add(Map.of("id","hex_unformed","pos",center.toShortString()));
        chest(p,BASE.offset(4,1,10),List.of(stack("minecraft:glass_bottle"),stack("minecraft:redstone"),stack("thaumcraft:thaumonomicon"),stack("astral_monitor"),stack("coagulating_eye_drops"),stack("clear_water_bottle"),stack("thick_taint_bottle"),stack("thaumaturge_drum"),stack("warped_drum")),"Interaction inputs; no completed outcome");
        out.put("devices",rows);
    }
    private static ItemStack wand(ItemStack focus){
        var wand=tc4revival.content.TCItems.CASTING_WAND.get();ItemStack stack=wand.create(tc4revival.content.WandParts.Rod.SILVERWOOD,tc4revival.content.WandParts.Cap.THAUMIUM,false,true);tc4revival.content.WandFocusItem.attach(stack,focus.copy());return stack;
    }
    private static void foci(ServerPlayer p,Map<String,Object> out){
        requirePlatform(p);var focus=items().stream().filter(s->s.getItem() instanceof tc4revival.content.WandFocusItem).toList();var tools=new ArrayList<ItemStack>();for(var s:focus)tools.add(wand(s));
        tools.add(wand(ItemStack.EMPTY));chest(p,BASE.offset(8,1,10),tools,"Charged focus fixtures; cast normally");
        var shards=new ArrayList<ItemStack>();for(var type:PowderType.values()){var s=type.shard();if(!s.isEmpty()){s.setCount(16);shards.add(s);}}
        shards.add(stack("shard_mill"));shards.add(stack("silverwood_shard_mill"));chest(p,BASE.offset(12,1,10),shards,"15 shards and empty mills");
        p.serverLevel().setBlock(BASE.offset(12,1,25),Blocks.FIRE.defaultBlockState(),3);p.serverLevel().setBlock(BASE.offset(16,1,25),Blocks.WATER.defaultBlockState(),3);p.serverLevel().setBlock(BASE.offset(20,1,25),Blocks.STONE.defaultBlockState(),3);
        spawn(p,"minecraft:cow",BASE.offset(28,1,25),true);spawn(p,"minecraft:zombie",BASE.offset(34,1,25),true);out.put("foci",focus.stream().map(s->ForgeRegistries.ITEMS.getKey(s.getItem()).toString()).toList());
    }
    private static boolean availableMob(String id){
        if(List.of("chort","sloppy_alchemist","burned_witch","samurai","overanimated","thaum_gib","dope_squid","withered_botanist").contains(id))return RemainingMobs.available(id);
        if(id.startsWith("crimson_")||id.equals("lesser_portal"))return CrimsonContent.available()&&(!id.equals("crimson_archer")&&!id.equals("crimson_ranger")||CrimsonContent.dyes());
        if(id.equals("red_powered_mind"))return net.minecraftforge.fml.ModList.get().isLoaded("automagy")&&net.minecraftforge.fml.ModList.get().isLoaded("thaumichorizons");
        if(id.equals("golem_bydlo"))return net.minecraftforge.fml.ModList.get().isLoaded("thaumichorizons");
        if(id.equals("vengeful_golem"))return net.minecraftforge.fml.ModList.get().isLoaded("automagy");
        return true;
    }
    private static void mobs(ServerPlayer p,Map<String,Object> out){
        requirePlatform(p);var rows=new ArrayList<Object>();int i=0;
        for(String name:List.of("thaumaturge","mad_thaumaturge","nether_explorer","quicksilver_elemental","brightest_one","dissolved","samurai","overanimated","thaum_gib","dope_squid","withered_botanist","golem_bydlo","vengeful_golem","crimson_paladin","crimson_archer","crimson_ranger","crimson_pontifex","lesser_portal","chort","sloppy_alchemist","burned_witch","red_powered_mind")){
            if(!availableMob(name)){rows.add(Map.of("id",name,"skipped","host absent"));continue;}var at=BASE.offset(6+(i%7)*12,1,36+(i/7)*12);i++;
            if(name.equals("dope_squid"))for(var pos:BlockPos.betweenClosed(at.offset(-2,0,-2),at.offset(2,1,2)))p.serverLevel().setBlock(pos,Blocks.WATER.defaultBlockState(),3);
            var entity=spawn(p,name,at,true);rows.add(Map.of("id",name,"pos",at.toShortString(),"uuid",entity.getUUID().toString()));
        }out.put("mobs",rows);
    }
    private static Entity spawn(ServerPlayer p,String name,BlockPos at,boolean frozen){
        var key=id(name);if(!ForgeRegistries.ENTITY_TYPES.containsKey(key))throw new IllegalArgumentException("Unregistered entity "+key);
        var entity=ForgeRegistries.ENTITY_TYPES.getValue(key).create(p.serverLevel());if(entity==null)throw new IllegalStateException("Entity factory "+key);
        entity.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,180,0);entity.addTag(TAG);entity.setCustomName(Component.literal(name));entity.setCustomNameVisible(true);
        if(entity instanceof Mob mob){mob.finalizeSpawn(p.serverLevel(),p.serverLevel().getCurrentDifficultyAt(at),MobSpawnType.COMMAND,null,null);mob.setNoAi(frozen);mob.setPersistenceRequired();}
        if(!p.serverLevel().addFreshEntity(entity))throw new IllegalStateException("Entity spawn rejected "+key);return entity;
    }
    private static void effects(ServerPlayer p,Map<String,Object> out){
        requirePlatform(p);var rows=new ArrayList<Object>();
        for(int index=0;index<3;index++){
            var center=BASE.offset(12+index*20,1,82);for(var pos:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1))){p.serverLevel().setBlock(pos,ConciliumBlocks.HEX.get().defaultBlockState(),3);((PredictabilityBlockEntity)p.serverLevel().getBlockEntity(pos)).configure(center);}
            if(index==0){var rift=ConciliumEntities.RIFT.get().create(p.serverLevel());rift.initialize(20261031,35);rift.bind(center);rift.setPos(center.getX()+.5,center.getY()+2,center.getZ()+.5);rift.addTag(TAG);p.serverLevel().addFreshEntity(rift);}
            else {var peeler=ConciliumEntities.PEELER.get().create(p.serverLevel());peeler.bind(center);peeler.setPos(center.getX()+.5,center.getY()+2,center.getZ()+.5);peeler.addTag(TAG);p.serverLevel().addFreshEntity(peeler);}
            rows.add(Map.of("center",center.toShortString(),"fixture",index==0?"bound rift":"peeler","warning","visual fixture; not proof of formation or acquisition"));
        }
        out.put("shaderFixtures",rows);
    }
    private static Object probe(ServerPlayer p){
        var rows=new ArrayList<Object>();for(var e:p.serverLevel().getEntities((Entity)null,new AABB(BASE,BASE.offset(96,32,96)),e->e.getTags().contains(TAG)))rows.add(Map.of("id",e.getId(),"uuid",e.getUUID().toString(),"type",ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).toString(),"position",e.position().toString(),"nbt",e.serializeNBT().toString()));
        return Map.of("entities",rows,"inventory",p.getInventory().save(new net.minecraft.nbt.ListTag()).toString(),"menu",p.containerMenu.getClass().getName(),"playerData",p.getPersistentData().toString());
    }
}
