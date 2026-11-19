package thaumicfutures;

/** Implemented by optional recipes without linking their classes when that integration is absent. */
public interface FeatureRecipeMutation {
    void omniverse$applyFeatures();
}
