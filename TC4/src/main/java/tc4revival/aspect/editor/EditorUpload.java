package tc4revival.aspect.editor;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class EditorUpload {
    public static final int CHUNK=24000,LIMIT=4*1024*1024;
    public static List<byte[]> frames(String json){
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);if(bytes.length==0||bytes.length>LIMIT)throw new IllegalArgumentException("编辑内容超过 4 MiB");
        var result=new ArrayList<byte[]>();for(int i=0;i<bytes.length;i+=CHUNK)result.add(Arrays.copyOfRange(bytes,i,Math.min(i+CHUNK,bytes.length)));return result;
    }
    public static class Receiver {
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();private int next,total;
        public Optional<String> accept(int index,int count,byte[] chunk){
            if(index!=next||count<1||count>(LIMIT+CHUNK-1)/CHUNK||index>=count||chunk.length>CHUNK||(index>0&&total!=count)||bytes.size()+chunk.length>LIMIT)
                throw new IllegalArgumentException("Invalid editor frame sequence");
            total=count;next++;bytes.writeBytes(chunk);
            return next==total?Optional.of(bytes.toString(StandardCharsets.UTF_8)):Optional.empty();
        }
    }
}
