package automagy.logic;

/** Four independent channels use these same rules in the block and in headless tests. */
public record ReaderChannel(int mode, int capacity, int direction, boolean inverted) {
    public static final int[] CAPACITIES={0,1,8,16,25,32,50,64,100,125};
    public ReaderChannel {
        if(mode<0||mode>4||capacity<0||capacity>Integer.MAX_VALUE/2||direction<0||direction>6||direction==1)
            throw new IllegalArgumentException("Invalid vis reader channel");
    }
    public int evaluate(AspectReading reading,String filter) {
        if(mode==0||!reading.valid())return 0;
        int signal;
        if(mode==4)signal=Math.min(inverted?15:99,reading.contents().size());
        else {
            long totalCapacity=capacity>0?capacity:reading.totalCapacity();
            long individual=capacity>0?capacity:reading.individualCapacity();
            if(individual<1)individual=totalCapacity;
            if(filter!=null&&!filter.isEmpty()) {
                long maximum=capacity==0&&reading.perAspectCapacity()?reading.capacities().getOrDefault(filter,0):mode==1?totalCapacity:individual;
                signal=proportion(reading.contents().getOrDefault(filter,0),maximum);
            } else if(mode==1) {
                long total=0;for(int value:reading.contents().values())total+=Math.max(0,value);
                signal=proportion(total,totalCapacity);
            } else {
                signal=mode==2?0:15;boolean found=false;
                for(var entry:reading.contents().entrySet()) {
                    long maximum=capacity==0&&reading.perAspectCapacity()?reading.capacities().getOrDefault(entry.getKey(),0):individual;
                    if(reading.perAspectCapacity()&&capacity==0&&maximum<=0)continue;
                    int value=proportion(entry.getValue(),maximum);
                    signal=mode==2?Math.max(signal,value):Math.min(signal,value);found=true;
                }
                if(!found)signal=0;
            }
        }
        return inverted?15-signal:signal;
    }
    public static int proportion(long amount,long capacity) {
        if(amount<=0||capacity<=0)return 0;
        if(amount>=capacity)return 15;
        return 1+(int)(14.0*amount/capacity);
    }
}
