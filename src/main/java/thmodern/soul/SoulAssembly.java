package thmodern.soul;

/** Exact integer accounting: one cognition essentia buys 1000 bits; 9600 bits assemble a soul. */
public final class SoulAssembly {
    public static final int BITS_PER_SOUL=9600,MAX_SOULS=16;
    private int bits,souls,energy;
    public int bits(){return bits;}public int souls(){return souls;}public int energy(){return energy;}
    public int room(){return Math.max(0,Math.min(energy,(MAX_SOULS-souls)*BITS_PER_SOUL-bits));}
    public int addEssentia(int amount){if(amount<=0||energy>=3000)return 0;energy+=1000;return 1;}
    public int accept(int amount){int accepted=Math.min(Math.max(0,amount),room());energy-=accepted;bits+=accepted;souls+=bits/BITS_PER_SOUL;bits%=BITS_PER_SOUL;return accepted;}
    public boolean takeSoul(){if(souls==0)return false;souls--;return true;}
    public void load(int bits,int souls,int energy){this.souls=Math.max(0,Math.min(MAX_SOULS,souls));this.bits=this.souls==MAX_SOULS?0:Math.max(0,Math.min(BITS_PER_SOUL-1,bits));this.energy=Math.max(0,Math.min(3999,energy));}
}
