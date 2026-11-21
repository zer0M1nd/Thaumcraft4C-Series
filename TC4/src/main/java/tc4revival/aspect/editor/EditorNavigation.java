package tc4revival.aspect.editor;

import java.util.Locale;

/** Shared search and scrollbar geometry, independent of Minecraft rendering. */
public class EditorNavigation {
    public static boolean matchesNonItem(String query,String key,String name){
        String id=key.toLowerCase(Locale.ROOT),label=name.toLowerCase(Locale.ROOT);
        for(String token:query.strip().toLowerCase(Locale.ROOT).split("\\s+")){
            if(token.startsWith("@")){
                String namespace=id.substring(id.indexOf(':')+1).split(":",2)[0];
                if(!namespace.contains(token.substring(1)))return false;
            }else if(!id.contains(token)&&!label.contains(token))return false;
        }
        return true;
    }
    public static int thumbHeight(int visibleRows,int maxRow,int trackHeight){
        return Math.min(trackHeight,Math.max(10,(int)((long)trackHeight*visibleRows/Math.max(visibleRows,maxRow+visibleRows))));
    }
    public static int dragRow(double pointerY,double offset,int top,int trackHeight,int thumbHeight,int maxRow){
        if(maxRow<=0||trackHeight<=thumbHeight)return 0;
        return (int)Math.round(Math.max(0,Math.min(1,(pointerY-offset-top)/(trackHeight-thumbHeight)))*maxRow);
    }
}
