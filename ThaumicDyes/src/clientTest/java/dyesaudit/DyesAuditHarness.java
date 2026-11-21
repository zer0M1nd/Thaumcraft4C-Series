package dyesaudit;

import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

/** Isolated, opt-in observation/setup helper. Never included in the product artifact. */
@Mod(DyesAuditHarness.ID)
public final class DyesAuditHarness {
    public static final String ID="dyes_client_audit";
    static final org.slf4j.Logger LOG=LogUtils.getLogger();
    static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    public DyesAuditHarness(){
        if(!Boolean.getBoolean("tc4.validationHarness")){LOG.info("DYES_AUDIT disabled; requires -Dtc4.validationHarness=true");return;}
        MinecraftForge.EVENT_BUS.addListener(DyesAuditScenes::commands);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->DyesAuditClient::register);
        LOG.info("DYES_AUDIT test-only commands enabled");
    }
    static String requestId(){return UUID.randomUUID().toString();}
    static Map<String,Object> stack(ItemStack stack){
        var out=new LinkedHashMap<String,Object>();
        out.put("item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());out.put("count",stack.getCount());
        out.put("damage",stack.getDamageValue());out.put("nbt",stack.hasTag()?stack.getTag().toString():"");
        if(stack.getItem() instanceof dyesmodern.DyesArmorItem item){out.put("legacyKey",item.legacyKey);out.put("family",item.family);out.put("slot",item.getEquipmentSlot().getName());}
        return out;
    }
    static Map<String,Object> entity(LivingEntity entity){
        var out=new LinkedHashMap<String,Object>();out.put("id",entity.getId());out.put("uuid",entity.getUUID().toString());
        out.put("type",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());out.put("position",List.of(entity.getX(),entity.getY(),entity.getZ()));
        out.put("pose",entity.getPose().name());out.put("young",entity.isBaby());out.put("health",entity.getHealth());
        var equipment=new LinkedHashMap<String,Object>();for(var slot:ARMOR)equipment.put(slot.getName(),stack(entity.getItemBySlot(slot)));out.put("equipment",equipment);
        out.put("mainHand",stack(entity.getMainHandItem()));out.put("offHand",stack(entity.getOffhandItem()));return out;
    }
    static Map<String,Object> error(Throwable error){var writer=new StringWriter();error.printStackTrace(new PrintWriter(writer));return Map.of("exception",error.toString(),"stackTrace",writer.toString());}
    static synchronized void record(String side,String id,String operation,boolean success,Map<String,?> data){
        if(!id.matches("[A-Za-z0-9_-]{1,80}"))throw new IllegalArgumentException("Unsafe audit request ID");
        var result=new LinkedHashMap<String,Object>();result.put("requestId",id);result.put("side",side);result.put("operation",operation);
        result.put("timestamp",Instant.now().toString());result.put("success",success);result.put("data",data);
        result.put("scope","Test helper operation/observation only. Actual gameplay assertions and viewed screenshots are recorded separately.");
        try{
            var directory=FMLPaths.GAMEDIR.get().resolve("dyes-audit");Files.createDirectories(directory);
            String json=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(result)+"\n";
            Files.writeString(directory.resolve(side+"-"+id+"-"+System.nanoTime()+".json"),json);
            Files.writeString(directory.resolve("last-"+side+".json"),json);
        }catch(java.io.IOException error){LOG.error("DYES_AUDIT cannot persist {} {}",side,id,error);throw new IllegalStateException("Unable to write audit evidence",error);}
        LOG.info("DYES_AUDIT side={} request={} operation={} success={}",side,id,operation,success);
    }
}
