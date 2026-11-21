package tc4revival.aspect.entity;

/** Only facts obtained from the server's actual entity may reach the gameplay resolver. */
public record EntityFacts(String type, String playerName, boolean baby, boolean zombieVillager,
                          boolean swellingCreeper, boolean powered, boolean oakBoat, int pechType,String golemMaterial) {
    public EntityFacts(String type,String playerName,boolean baby,boolean zombieVillager,boolean swellingCreeper,boolean powered,boolean oakBoat,int pechType) { this(type,playerName,baby,zombieVillager,swellingCreeper,powered,oakBoat,pechType,""); }
    public EntityFacts(String type,String playerName,boolean baby,boolean zombieVillager,boolean swellingCreeper,boolean powered,boolean oakBoat) { this(type,playerName,baby,zombieVillager,swellingCreeper,powered,oakBoat,-1); }
}
