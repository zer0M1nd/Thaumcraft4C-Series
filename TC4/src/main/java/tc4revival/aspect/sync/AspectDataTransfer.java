package tc4revival.aspect.sync;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Bounded, ordered compression frames, shared by login/reload synchronization and tests. */
public class AspectDataTransfer {
    public static final int CHUNK=65536,MAX_COMPRESSED=16*1024*1024;
    private static final int MAX_ENVELOPE=AspectDataSet.MAX_BYTES*2+AspectDataSet.MAX_FILES*1024;
    public static List<byte[]> encode(Map<String,String> values){
        AspectDataSet.transferBytes(values);
        byte[] raw=new GsonBuilder().disableHtmlEscaping().create().toJson(values).getBytes(StandardCharsets.UTF_8);
        if(raw.length>MAX_ENVELOPE)throw new IllegalArgumentException("Aspect sync envelope too large");
        try{
            var output=new ByteArrayOutputStream();try(var zip=new GZIPOutputStream(output)){zip.write(raw);}
            byte[] bytes=output.toByteArray();if(bytes.length>MAX_COMPRESSED)throw new IllegalArgumentException("Compressed aspect sync too large");
            var chunks=new ArrayList<byte[]>();for(int i=0;i<bytes.length;i+=CHUNK)chunks.add(Arrays.copyOfRange(bytes,i,Math.min(bytes.length,i+CHUNK)));return chunks;
        }catch(IOException error){throw new IllegalStateException(error);}
    }
    public static Map<String,String> decode(byte[] bytes){
        if(bytes.length>MAX_COMPRESSED)throw new IllegalArgumentException("Compressed aspect sync too large");
        try(var zip=new GZIPInputStream(new ByteArrayInputStream(bytes))){
            byte[] raw=zip.readNBytes(MAX_ENVELOPE+1);if(raw.length>MAX_ENVELOPE)throw new IllegalArgumentException("Expanded aspect sync too large");
            var json=JsonParser.parseString(new String(raw,StandardCharsets.UTF_8)).getAsJsonObject();
            if(json.size()>AspectDataSet.MAX_FILES)throw new IllegalArgumentException("Too many aspect resources");
            var result=new TreeMap<String,String>();json.entrySet().forEach(e->{if(!e.getKey().equals(AspectInputDelta.SETTINGS))AspectDataSet.validateKey(e.getKey());result.put(e.getKey(),e.getValue().getAsString());});AspectDataSet.transferBytes(result);return result;
        }catch(IOException error){throw new IllegalArgumentException("Invalid compressed aspect data",error);}
    }
    public static class Receiver {
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();private int next,total;
        public Optional<Map<String,String>> accept(int index,int count,byte[] chunk){
            if(index!=next||count<1||count>MAX_COMPRESSED/CHUNK||index>=count||chunk.length>CHUNK||(index>0&&count!=total))throw new IllegalArgumentException("Invalid aspect frame sequence");
            if(bytes.size()+chunk.length>MAX_COMPRESSED)throw new IllegalArgumentException("Aspect sync too large");
            total=count;next++;bytes.writeBytes(chunk);
            return next==total?Optional.of(decode(bytes.toByteArray())):Optional.empty();
        }
    }
}
