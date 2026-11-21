package temodern;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.crafting.conditions.*;

public final class RecipeConfig {
    private static final java.util.Map<String,ForgeConfigSpec.BooleanValue> OPTIONS=new java.util.LinkedHashMap<>();
    public static final ForgeConfigSpec SPEC;
    public static final ResourceLocation CONDITION=ResourceLocation.fromNamespaceAndPath(TE.ID,"feature_enabled");
    static {
        var builder=new ForgeConfigSpec.Builder();builder.push("crafting");
        for(var name:new String[]{"arcane_assembler","arcane_terminal","essentia_cells","essentia_provider","vibration_chamber","golem_backpack","infusion_provider","vis_interface","gearbox","quartz_duplication","wrench_focus","thaumcraft_facades"})
            OPTIONS.put(name,builder.comment("原有合成选项："+description(name)+"。布尔值，默认 true；不随新增功能模式变化。修改后重启世界。",
                    "Legacy crafting option: "+name+". Boolean, default true; unaffected by revival profiles. Restart the world after editing.").worldRestart().define(name,true));
        builder.pop();SPEC=builder.build();
    }
    private static String description(String id) { return switch(id) {
        case "arcane_assembler" -> "奥术装配室与知识刻印设备"; case "arcane_terminal" -> "奥术终端";
        case "essentia_cells" -> "源质元件与组件"; case "essentia_provider" -> "源质供应器";
        case "vibration_chamber" -> "源质共振室"; case "golem_backpack" -> "傀儡无线背包";
        case "infusion_provider" -> "注魔供应器"; case "vis_interface" -> "魔力中继接口";
        case "gearbox" -> "铁与神秘齿轮箱"; case "quartz_duplication" -> "石英增殖";
        case "wrench_focus" -> "扳手核心"; default -> "神秘方块立面";
    }; }
    public static boolean enabled(String id) {
        return switch(id) {
            case "extended_cells" -> RevivalFeatures.extended(); case "cells_256k" -> RevivalFeatures.CELLS_256K.get();
            case "wireless_arcane_terminal" -> RevivalFeatures.wireless(); case "vis_charging_card" -> RevivalFeatures.charging();
            default -> { var option=OPTIONS.get(id); yield option!=null && (SPEC.isLoaded()?option.get():option.getDefault()); }
        };
    }
    public static boolean itemEnabled(String id){
        if(id.equals("essentia_ampoule"))return false;
        if(id.equals("wireless_arcane_terminal"))return RevivalFeatures.wireless();
        if(id.equals("vis_charging_card"))return RevivalFeatures.charging();
        if(id.startsWith("essentia_cell_")||id.startsWith("storage_component_"))
            return RevivalFeatures.tierEnabled(id.substring(id.lastIndexOf('_')+1));
        return true;
    }
    public record Enabled(String feature) implements ICondition {
        @Override public ResourceLocation getID(){return CONDITION;}
        @Override public boolean test(IContext context){return enabled(feature);}
    }
    public static final IConditionSerializer<Enabled> SERIALIZER=new IConditionSerializer<>(){
        public ResourceLocation getID(){return CONDITION;}
        public void write(JsonObject json,Enabled value){json.addProperty("feature",value.feature());}
        public Enabled read(JsonObject json){return new Enabled(net.minecraft.util.GsonHelper.getAsString(json,"feature"));}
    };
}
