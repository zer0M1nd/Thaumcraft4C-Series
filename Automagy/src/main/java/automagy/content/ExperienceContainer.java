package automagy.content;
/** Addon-owned XP storage. Returns the amount accepted/removed, never the remainder. */
public interface ExperienceContainer {
 int experience(); int capacity();
 int insertExperience(int amount,boolean simulate);
 int extractExperience(int amount,boolean simulate);
}
