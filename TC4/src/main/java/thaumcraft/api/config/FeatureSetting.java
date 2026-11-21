package thaumcraft.api.config;

/** An explicit setting overrides the profile; feature dependencies are evaluated separately. */
public enum FeatureSetting {
    DEFAULT, TRUE, FALSE;

    public boolean resolve(boolean inherited) {
        return this == DEFAULT ? inherited : this == TRUE;
    }
}
