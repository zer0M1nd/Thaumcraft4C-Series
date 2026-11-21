package tc4revival.aspect.editor;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Append-only save: no user-named path can overwrite an existing world datapack. */
public class EditorPackStore {
    public record Saved(Path directory,String packId,long order,String displayName) {}
    public static String safeName(String name){
        name=name.strip();
        if(name.isEmpty()||name.length()>64||!name.matches("[\\p{L}\\p{N}_ -]+"))throw new IllegalArgumentException("名称仅支持文字、数字、空格、下划线和连字符（最多 64 字）");
        return name;
    }
    public static String nextName(Path root,String player)throws IOException{
        String prefix=player.replaceAll("[^\\p{L}\\p{N}_-]","_");if(prefix.isEmpty())prefix="player";
        var names=new HashSet<String>();
        if(Files.isDirectory(root))try(var paths=Files.list(root)){paths.forEach(p->names.add(p.getFileName().toString()));}
        for(int n=1;n<Integer.MAX_VALUE;n++){
            String name=prefix+"_"+n;
            if(names.stream().noneMatch(s->s.endsWith("_"+name)))return name;
        }
        throw new IOException("No available datapack name");
    }
    public static synchronized Saved write(Path root,String name,String player,EditorDefinitions edits,long loadedOrder)throws IOException{
        Files.createDirectories(root);root=root.toAbsolutePath().normalize();
        String display=safeName(name.isBlank()?nextName(root,player):name);
        long order=Math.max(0,loadedOrder);
        try(var paths=Files.list(root)){
            for(var p:paths.toList()){
                var m=java.util.regex.Pattern.compile("tc4_aspects_([0-9]{19})_.*").matcher(p.getFileName().toString());
                if(m.matches())order=Math.max(order,Long.parseLong(m.group(1)));
            }
        }
        order=Math.addExact(order,1);
        String id="tc4_aspects_"+String.format(Locale.ROOT,"%019d",order)+"_"+display;
        Path destination=root.resolve(id),temporary=root.resolve(".tc4-editor-"+UUID.randomUUID()+".tmp");
        if(Files.exists(destination))throw new IOException("Datapack destination already exists");
        var definition=new EditorDefinitions(edits.entries(),order);
        var pack=new JsonObject();var metadata=new JsonObject();metadata.addProperty("pack_format",15);metadata.addProperty("description","Thaumcraft 要素编辑："+display);pack.add("pack",metadata);
        Path data=temporary.resolve("data/thaumcraft/aspect_editor");Files.createDirectories(data);
        Files.writeString(temporary.resolve("pack.mcmeta"),pack.toString(),StandardCharsets.UTF_8);
        Files.writeString(data.resolve(String.format(Locale.ROOT,"%019d",order)+".json"),definition.encode().toString(),StandardCharsets.UTF_8);
        try{Files.move(temporary,destination,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,destination);}
        return new Saved(destination,"file/"+id,order,display);
    }
}
