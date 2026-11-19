package tc4revival.client;

import java.util.ArrayList;
import java.util.List;

/** Splits a viewport into texture-sized strips, reflecting alternate tiles at their seams. */
public class MirroredBackground {
    public record Span(int offset,int length,int textureStart,int textureLength) {}
    public static List<Span> spans(int start,int length,int tileSize) {
        if(length<0 || tileSize<=0)throw new IllegalArgumentException();
        var spans=new ArrayList<Span>();
        for(int offset=0;offset<length;) {
            int coordinate=start+offset,tile=Math.floorDiv(coordinate,tileSize);
            int local=Math.floorMod(coordinate,tileSize),size=Math.min(length-offset,tileSize-local);
            boolean reverse=Math.floorMod(tile,2)!=0;
            spans.add(new Span(offset,size,reverse?tileSize-local:local,reverse?-size:size));
            offset+=size;
        }
        return List.copyOf(spans);
    }
}
