package ttmodern;

import java.nio.file.*;

/** Development-only export/fixture paths must be explicit, independent of the server working directory. */
final class DevelopmentPaths {
    static Path property(String name){String value=System.getProperty(name);if(value==null||value.isBlank())throw new IllegalStateException("Development test requires -D"+name+"=<path>");return Path.of(value).toAbsolutePath().normalize();}
    static Path output(String relative){var root=property("ttmodern.workspace");var path=root.resolve(relative).normalize();if(!path.startsWith(root))throw new IllegalArgumentException("Output leaves addon workspace");return path;}
    private DevelopmentPaths(){}
}
