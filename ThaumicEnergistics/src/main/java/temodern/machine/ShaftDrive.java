package temodern.machine;

/** Six units of hand power per click; each accepting shaft needs eighteen units per turn. */
public final class ShaftDrive {
    private final int[] stored=new int[6];
    public boolean[] crank(boolean[] accepting){
        if(accepting.length!=6)throw new IllegalArgumentException("Six shaft faces required");
        int count=0;for(boolean active:accepting)if(active)count++;
        var turn=new boolean[6];int power=count==0?0:6/count;
        for(int i=0;i<6;i++){
            if(!accepting[i])stored[i]=0;
            else if((stored[i]+=power)>=18){stored[i]=0;turn[i]=true;}
        }
        return turn;
    }
}
