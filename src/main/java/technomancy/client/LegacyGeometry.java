package technomancy.client;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import technomancy.Technomancy;

/** Runtime adapter for the numerically verified Blockbench exports of the author's models. */
final class LegacyGeometry {
    private static final Map<String,ModelPart> MODELS=new HashMap<>();
    static void clear(){MODELS.clear();}
    static ModelPart get(String name){return MODELS.computeIfAbsent(name,LegacyGeometry::load);}
    private static float number(JsonObject o,String key,int i){return o.getAsJsonArray(key).get(i).getAsFloat();}
    private static ModelPart load(String name){
        var mesh=new MeshDefinition();
        try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(Technomancy.id("geometry/"+name+".json"))){
            var json=JsonParser.parseReader(reader).getAsJsonObject();
            for(var entry:json.getAsJsonArray("parts")){
                var part=entry.getAsJsonObject();var cubes=CubeListBuilder.create();
                for(var element:part.getAsJsonArray("boxes")){
                    var box=element.getAsJsonObject();
                    cubes.texOffs((int)number(box,"uv",0),(int)number(box,"uv",1)).mirror(box.get("mirror").getAsBoolean())
                        .addBox(number(box,"position",0),number(box,"position",1),number(box,"position",2),
                            number(box,"size",0),number(box,"size",1),number(box,"size",2),new CubeDeformation(box.get("inflate").getAsFloat()));
                }
                mesh.getRoot().addOrReplaceChild(part.get("name").getAsString(),cubes,PartPose.offsetAndRotation(
                    number(part,"pivot",0),number(part,"pivot",1),number(part,"pivot",2),
                    number(part,"rotation",0),number(part,"rotation",1),number(part,"rotation",2)));
            }
            return LayerDefinition.create(mesh,json.get("width").getAsInt(),json.get("height").getAsInt()).bakeRoot();
        }catch(IOException|RuntimeException e){throw new IllegalStateException("Invalid Technomancy geometry: "+name,e);}
    }
    private LegacyGeometry(){}
}
