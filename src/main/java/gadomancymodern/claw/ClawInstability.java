package gadomancymodern.claw;
public class ClawInstability {
    public static int calculate(int capacity,boolean staff,float capCost,boolean voidCap,boolean primalStaff){return 23+(capacity<100?3:-2)-(int)Math.floor(Math.min(capacity,300)/300f*10)-(staff?6:-1)+(int)Math.floor((capCost-.4)*3)-(voidCap?3:0)-(primalStaff?6:0);}
}
