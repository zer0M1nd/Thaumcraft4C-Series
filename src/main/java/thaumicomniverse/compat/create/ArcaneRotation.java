package thaumicomniverse.compat.create;

import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import tc4revival.content.*;

/** Only documented directional fields are transformed; inventories and arbitrary numeric NBT remain untouched. */
public class ArcaneRotation {
    public static void tile(BlockEntity tile,StructureTransform transform){
        var tag=tile.saveWithoutMetadata();boolean changed=false;
        var type=net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(tile.getType());
        if(tile instanceof EssentiaJarBlockEntity||tile instanceof AlembicBlockEntity||tile instanceof EldritchCrabSpawnerBlockEntity)
            changed|=face(tag,"facing",transform);
        if(tile instanceof EssentiaReservoirBlockEntity)changed|=face(tag,"face",transform);
        if(tile instanceof EssentiaTubeBlockEntity){
            changed|=face(tag,"side",transform);changed|=sides(tag,"open",transform);changed|=sides(tag,"choke",transform);
        }
        if(tag.contains("GadSupport",Tag.TAG_ANY_NUMERIC))changed|=face(tag,"GadSupport",transform);
        if(type!=null)switch(type.toString()){
            case "automagy:greedy_chest"->changed|=mask(tag,"Directions",transform);
            case "automagy:vis_reader"->{
                for(var entry:tag.getList("Channels",Tag.TAG_COMPOUND))changed|=face((CompoundTag)entry,"Direction",transform);
                tag.remove("Signals");
            }
            case "thaumicomniverse:machine_aspect_port"->{changed|=mask(tag,"EnabledSides",transform);changed|=face(tag,"AbsorptionSide",transform);}
            case "thaumic_tinkerer:mobilizer"->changed|=namedFace(tag,"Direction",transform);
            case "witchinggadgets:wall_mirror"->changed|=face(tag,"facing",transform);
            default->{}
        }
        // Bore/lamp/bellows orientations are derived from the already-transformed block state on save.
        if(tile instanceof InfusionPillarBlockEntity pillar&&transform.rotationAxis==Direction.Axis.Y){
            int turns=Math.floorMod(Math.round(transform.angle/90f),4);
            tag.putByte("orientation",(byte)Math.floorMod(pillar.orientation()-turns,4));changed=true;
        }
        if(changed){tile.load(tag);tile.setChanged();}
    }
    private static boolean namedFace(CompoundTag tag,String key,StructureTransform transform){
        if(!tag.contains(key,Tag.TAG_STRING))return false;var old=Direction.byName(tag.getString(key));if(old==null)return false;
        tag.putString(key,ArcaneContraptions.direction(transform,old).getName());return true;
    }
    private static boolean mask(CompoundTag tag,String key,StructureTransform transform){
        if(!tag.contains(key,Tag.TAG_ANY_NUMERIC))return false;int old=tag.getInt(key),next=0;
        for(var side:Direction.values())if((old&(1<<side.get3DDataValue()))!=0)next|=1<<ArcaneContraptions.direction(transform,side).get3DDataValue();
        tag.putInt(key,next);return true;
    }
    private static boolean face(CompoundTag tag,String key,StructureTransform transform){
        if(!tag.contains(key,Tag.TAG_ANY_NUMERIC))return false;
        int old=tag.getInt(key);if(old<0||old>5)return false;
        int next=ArcaneContraptions.direction(transform,Direction.from3DDataValue(old)).get3DDataValue();
        if(tag.getTagType(key)==Tag.TAG_BYTE)tag.putByte(key,(byte)next);else tag.putInt(key,next);return true;
    }
    private static boolean sides(CompoundTag tag,String key,StructureTransform transform){
        var old=tag.getByteArray(key);if(old.length!=6)return false;var next=new byte[6];
        for(var face:Direction.values())next[ArcaneContraptions.direction(transform,face).get3DDataValue()]=old[face.get3DDataValue()];
        tag.putByteArray(key,next);return true;
    }
}
