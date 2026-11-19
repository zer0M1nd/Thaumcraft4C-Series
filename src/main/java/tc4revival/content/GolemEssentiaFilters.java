package tc4revival.content;

import tc4revival.aspect.Aspect;

/** Essentia samples describe contents, never the material aspects of the sample item. */
public final class GolemEssentiaFilters {
    private GolemEssentiaFilters() {}
    public static boolean matches(GolemWorkHost host,Aspect aspect,int markerColor) {
        if (aspect==null) return false;
        if (host.filters().isEmpty()) return true;
        for (int i=0;i<host.filters().size();i++) {
            var sample=host.filters().sample(i);
            if (sample.isEmpty()) continue;
            boolean matches=sample.getItem() instanceof StoredAspectItem item && item.storedAspects(sample).aspects().contains(aspect);
            if (!matches && sample.hasTag()) matches=aspect.tag().equals(sample.getTag().getString("AspectFilter"));
            if (matches && (markerColor==-2 || host.configuration().upgrades(4)==0 || host.filters().color(i)<0 || host.filters().color(i)==markerColor)) return true;
        }
        return false;
    }
}
