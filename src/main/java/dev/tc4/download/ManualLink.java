package dev.tc4.download;

import java.net.URI;
import java.util.*;

/** Browser destinations only. The transport never reads this list. */
public record ManualLink(URI url, String type) {
    public static List<ManualLink> ordered(Collection<URI> pages, boolean chinese) {
        return pages.stream().distinct().filter(url -> url.getHost() != null && url.getUserInfo() == null
                        && ("http".equalsIgnoreCase(url.getScheme()) || "https".equalsIgnoreCase(url.getScheme())))
                .map(url -> new ManualLink(url, typeOf(url)))
                .sorted(Comparator.comparingInt(link -> switch (link.type) {
                    case "MC 百科" -> chinese ? 0 : 6;
                    case "CurseForge" -> 1;
                    case "Modrinth" -> 2;
                    case "GitHub Release" -> 3;
                    default -> 4;
                })).toList();
    }

    private static String typeOf(URI uri) {
        return switch (uri.getHost().toLowerCase(Locale.ROOT)) {
            case "www.mcmod.cn", "mcmod.cn" -> "MC 百科";
            case "curseforge.com", "www.curseforge.com" -> "CurseForge";
            case "modrinth.com", "www.modrinth.com" -> "Modrinth";
            case "github.com" -> uri.getPath().contains("/releases") ? "GitHub Release" : "GitHub";
            case "tc4c.zer0m1nd.me", "tc4c1.zer0m1nd.me", "sn.zer0m1nd.me" -> "官网";
            default -> uri.getHost();
        };
    }
}
