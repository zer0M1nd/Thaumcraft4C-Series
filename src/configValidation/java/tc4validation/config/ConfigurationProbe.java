package tc4validation.config;

import com.google.gson.*;
import com.mojang.brigadier.arguments.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.config.TCConfig;
import tc4revival.research.*;
import thaumcraft.api.config.*;

@Mod.EventBusSubscriber(modid="thaumcraft")
public class ConfigurationProbe {
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        if(!Boolean.getBoolean("tc4.configurationValidation"))return;
        event.getDispatcher().register(Commands.literal("configprobe").requires(s->s.hasPermission(2))
            .then(Commands.literal("snapshot").executes(c->snapshot(c.getSource())))
            .then(Commands.literal("stage").then(Commands.argument("stage",IntegerArgumentType.integer(0,3)).executes(c->{
                var p=c.getSource().getPlayerOrException();int stage=IntegerArgumentType.getInteger(c,"stage");
                KnowledgeSavedData.get(p.server).update(p.getUUID(),p.getRandom()::nextInt,k->{
                    // Explicit scenario setup; does not claim that these research entries were earned in play.
                    for(var entry:ResearchDefinitions.server().catalog().all())if(!entry.key().startsWith("REVIVAL_"))k.rememberResearch(entry.key());
                    String[] keys={ResearchTableAccess.FIRST,ResearchTableAccess.SECOND,ResearchTableAccess.THIRD};
                    for(int i=0;i<keys.length;i++){k.forgetResearch(keys[i]);if(i<stage)k.rememberResearch(keys[i]);}
                    for(var aspect:tc4revival.aspect.TCAspects.catalog().all())k.setPool(aspect,(short)50);
                    return true;
                });
                KnowledgeSync.flushBeforeScreen(p);return snapshot(c.getSource());
            })))
            .then(Commands.literal("profile").then(Commands.argument("profile",StringArgumentType.word()).executes(c->{
                var mode=RevivalProfile.valueOf(StringArgumentType.getString(c,"profile"));
                if(mode==RevivalProfile.FOLLOW_CORE)throw new IllegalArgumentException("Core cannot follow itself");
                TCConfig.SERVER.revival.global.set(mode);TCConfig.SERVER_SPEC.save();
                c.getSource().sendSuccess(()->Component.literal("Saved profile "+mode+"; restart this isolated world to test the real lifecycle."),false);
                return snapshot(c.getSource());
            }))));
    }
    static int snapshot(CommandSourceStack source) {
        var data=new JsonObject();data.addProperty("time",java.time.Instant.now().toString());
        var profiles=new JsonObject();for(var c:ConfigurationRuntimeTests.configs()){
            var v=new JsonObject();v.addProperty("loaded",c.loaded());v.addProperty("selected",c.global.get().name());v.addProperty("effective",c.profile().name());
            var features=new JsonObject();c.options().forEach((k,o)->features.addProperty(k,o.get()));v.add("features",features);
            profiles.add(c==TCConfig.SERVER.revival?"core":"addon-"+profiles.size(),v);
        }data.add("profiles",profiles);
        var legacy=new JsonObject();
        for(var entry:Map.of("thmodern.LegacyConfig",List.of("ALTERNATE_BELL","POCKET_PLANES"),
                "automagy.AutomagyConfig",List.of("LOONIUM_BLACKLIST"),"automagy.NetherConfig",List.of("IGNORE_CUSTOM")).entrySet()) {
            try {var type=Class.forName(entry.getKey());for(String name:entry.getValue()) {
                var value=(net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?>)type.getField(name).get(null);
                legacy.addProperty(entry.getKey()+"."+name,String.valueOf(value.get()));
            }} catch(ClassNotFoundException absent){} catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }data.add("legacyOptions",legacy);
        var activeResearch=new JsonArray();ResearchDefinitions.server().catalog().all().stream()
                .filter(ResearchCatalogs::configuredEnabled).map(ResearchDefinition::key)
                .filter(k->k.startsWith("thaumicomniverse:")||k.startsWith("thaumicenergistics:")||k.startsWith("thaumichorizons:")||k.equals("CONVENIENTTUBES")||k.equals("ALCHEMICALBRASS"))
                .sorted().forEach(activeResearch::add);data.add("enabledTargetResearch",activeResearch);
        var players=new JsonArray();for(var p:source.getServer().getPlayerList().getPlayers()){
            var row=new JsonObject();row.addProperty("name",p.getScoreboardName());row.addProperty("position",p.position().toString());
            row.addProperty("access",ResearchTableAccess.server(p).toString());
            KnowledgeSavedData.get(p.server).snapshot(p.getUUID()).ifPresent(k->row.addProperty("knowledge",KnowledgeNbt.write(k).toString()));
            var inventory=new JsonArray();for(int i=0;i<p.getInventory().getContainerSize();i++){
                var stack=p.getInventory().getItem(i);if(stack.isEmpty())continue;
                var slot=new JsonObject();slot.addProperty("slot",i);slot.addProperty("stack",stack.save(new net.minecraft.nbt.CompoundTag()).toString());inventory.add(slot);
            }row.add("inventory",inventory);players.add(row);
        }data.add("players",players);
        var recipes=source.getServer().getRecipeManager();var ids=new JsonArray();
        recipes.getRecipes().stream().map(r->r.getId().toString()).filter(id->id.startsWith("thaumicomniverse:")||id.startsWith("thaumicenergistics:")||id.startsWith("automagy:refining/")).sorted().forEach(ids::add);
        data.add("activeSampleRecipeIds",ids);data.addProperty("allRecipeCount",recipes.getRecipes().size());data.addProperty("aspectDefinitionRecipeCount",FeatureRecipes.definitions(recipes).size());
        var stages=new JsonObject();for(String key:List.of(ResearchTableAccess.FIRST,ResearchTableAccess.SECOND,ResearchTableAccess.THIRD)){
            var entry=ResearchDefinitions.server().catalog().require(key);var row=new JsonObject();row.addProperty("enabled",ResearchCatalogs.configuredEnabled(entry));row.addProperty("parents",entry.parents().toString());stages.add(key,row);
        }data.add("stages",stages);
        var chest=source.getLevel().getBlockEntity(new net.minecraft.core.BlockPos(0,-8,3));
        if(chest!=null)data.addProperty("chest",chest.saveWithoutMetadata().toString());
        try{Files.writeString(Path.of("configuration-probe.json"),new GsonBuilder().setPrettyPrinting().create().toJson(data));}
        catch(java.io.IOException e){throw new IllegalStateException(e);}
        source.sendSuccess(()->Component.literal("Configuration probe written; core="+TCConfig.SERVER.revival.profile()+", recipes="+recipes.getRecipes().size()),false);
        return 1;
    }
}
