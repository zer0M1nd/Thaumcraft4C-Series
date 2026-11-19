package tc4revival.aspect.sync;

/** Login and reload send recipes/tags in different orders. Both must belong to this generation. */
public class AspectPreparationGate {
    private boolean recipes,tags,resources,started;
    public void reset(){recipes=false;tags=false;resources=false;started=false;}
    public void recipes(){recipes=true;}
    public void tags(){tags=true;}
    public void resources(){resources=true;}
    public boolean inputsReady(){return recipes&&tags&&resources;}
    public boolean ready(){return inputsReady()&&!started;}
    public void start(){if(!ready())throw new IllegalStateException("Aspect inputs are not ready");started=true;}
    public void retry(){started=false;}
}
