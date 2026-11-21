package thmodern.soul;
/** Returns accepted bits, allowing producers to retain unpaid work rather than losing or duplicating it. */
public interface SoulReceiver { int soulRoom(); int acceptSoulBits(int amount); }
