package automagy.logic;

/** Legacy GUIScribe click quantities; both item panels use the same signs. */
public class RequisitionControls {
    public static int delta(int button,boolean shift,boolean control,int maxStack){
        if(button!=0&&button!=1)return 0;
        int count=shift?Math.max(1,Math.min(64,maxStack)):control?10:1;
        return button==0?count:-count;
    }
    public static int limit(int delta,boolean demandSlot,boolean alt,long stock,long pending){
        if(delta<0)return -(int)Math.min(-(long)delta,Math.max(0,pending));
        if(demandSlot||alt)return delta;
        return (int)Math.min(delta,Math.max(0,stock-pending));
    }
}
