package automagy.logic;

public final class PearlRules {
    public static boolean fractures(int stacks,int attempts,int roll){
        if(stacks<=0)return false;
        return roll<=stacks*(7.5+Math.max(0,attempts)*10.0);
    }
    public static boolean goldBonus(long items,int roll){return roll>Math.max(0,items)*(2.0/3);}
    public static boolean canCollect(boolean miser,int collected){return !miser||collected<10;}
    private PearlRules(){}
}
