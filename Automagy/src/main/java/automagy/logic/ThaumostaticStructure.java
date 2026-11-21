package automagy.logic;

/** Vertical structure scan; unloaded positions suspend validation rather than break a tower. */
public final class ThaumostaticStructure {
    public interface World {
        int minimumY(); int maximumY(); boolean loaded(int y);
        boolean pylon(int y); boolean base(int y); boolean thaumium(int y);
        boolean spire(int y); boolean spireTop(int y); boolean air(int y); boolean sky(int y);
    }
    public record Result(String problem,int bottom,int spire,int pylons) {
        public boolean valid(){return problem.isEmpty();}
        public boolean unloaded(){return problem.equals("unloaded");}
    }
    private ThaumostaticStructure() {}
    public static Result scan(World world,int controller,boolean active) {
        int count=0,bottom=controller-1,top=controller+1;
        while(bottom>=world.minimumY()){
            if(!world.loaded(bottom))return fail("unloaded");
            if(!world.pylon(bottom))break;
            count++;bottom--;
        }
        if(bottom<world.minimumY()||!world.base(bottom))return fail("base");
        while(top<world.maximumY()){
            if(!world.loaded(top))return fail("unloaded");
            if(!world.pylon(top))break;
            count++;top++;
        }
        if(count<2)return fail("pylons");
        if(top+1>=world.maximumY())return fail("height");
        if(!world.loaded(top)||!world.loaded(top+1))return fail("unloaded");
        if(active){if(!world.spire(top)||!world.spireTop(top+1))return fail("spire");}
        else if(!world.thaumium(top)||!world.air(top+1)||!world.sky(top+1))return fail("cap");
        return new Result("",bottom,top,Math.min(6,count));
    }
    private static Result fail(String problem){return new Result(problem,0,0,0);}
}
