package tc4revival.resources;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CurseForgeDownloadTest {
    @TempDir Path folder;
    @Test void autoResearchShareUsesVerifiedAuthorRelease() throws Exception {
        var direct=URI.create("https://github.com/cdj68765/TC4AutoResearch/releases/download/0.0.3/TCAutoResearch.jar");
        assertEquals(List.of(direct),CurseForgeDownload.attempts(List.of("https://wwamr.lanzoum.com/ivivH4909gfe")));
        assertTrue(CurseForgeDownload.allowed(direct));
        assertTrue(CurseForgeDownload.allowed(URI.create("https://release-assets.githubusercontent.com/asset.jar?signature=value")));
        assertThrows(IOException.class,()->CurseForgeDownload.endpoint("https://wwamr.lanzoum.com/another-file"));
        assertFalse(CurseForgeDownload.allowed(URI.create("https://github.com/unrelated/repository/a.jar")));
    }
    @Test void fixedFilePagesAndRedirectAllowlistRejectUntrustedTargets() throws Exception {
        assertEquals("https://mediafilez.forgecdn.net/files/2227/552/Thaumcraft-1.7.10-4.2.3.5.jar",
                CurseForgeDownload.endpoint("https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/2227552/").toString());
        assertEquals("https://www.curseforge.com/minecraft/mc-mods/thaumcraft/download/999/file",
                CurseForgeDownload.endpoint("https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/999").toString());
        assertTrue(CurseForgeDownload.endpoint("https://www.curseforge.com/minecraft/mc-mods/forbidden-magic/files/2303822").toASCIIString().contains("Forbidden%20Magic"));
        for(var page:List.of("http://www.curseforge.com/minecraft/mc-mods/a/files/1","https://evil.test/minecraft/mc-mods/a/files/1",
                "https://www.curseforge.com/minecraft/mc-mods/a/files/latest","https://user@www.curseforge.com/minecraft/mc-mods/a/files/1",
                "https://www.curseforge.com:443/minecraft/mc-mods/a/files/1","https://www.curseforge.com/minecraft/mc-mods/a/files/1?url=evil"))
            assertThrows(IOException.class,()->CurseForgeDownload.endpoint(page));
        assertTrue(CurseForgeDownload.allowed(URI.create("https://edge.forgecdn.net/files/1/2/a.jar")));
        for(var uri:List.of("http://edge.forgecdn.net/a.jar","https://www.curseforge.com.evil.test/a.jar","https://localhost/a.jar","file:///tmp/file.jar"))
            assertFalse(CurseForgeDownload.allowed(URI.create(uri)));
    }
    @Test void transportFollowsAllowedRedirectAndRejectsErrorsHtmlSizeAndLoops() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        byte[] bytes={0x50,0x4b,3,4,1,2};
        server.createContext("/file",exchange->{exchange.getResponseHeaders().add("Content-Type","application/java-archive");exchange.sendResponseHeaders(200,bytes.length);try(var out=exchange.getResponseBody()){out.write(bytes);}});
        server.createContext("/redirect",exchange->{exchange.getResponseHeaders().add("Location","/file");exchange.sendResponseHeaders(302,-1);exchange.close();});
        server.createContext("/forbidden",exchange->{exchange.sendResponseHeaders(403,-1);exchange.close();});
        server.createContext("/html",exchange->{exchange.getResponseHeaders().add("Content-Type","text/html");exchange.sendResponseHeaders(200,-1);exchange.close();});
        server.createContext("/loop",exchange->{exchange.getResponseHeaders().add("Location","/loop");exchange.sendResponseHeaders(302,-1);exchange.close();});
        server.createContext("/outside",exchange->{exchange.getResponseHeaders().add("Location","https://evil.test/file");exchange.sendResponseHeaders(302,-1);exchange.close();});
        server.createContext("/chunked",exchange->{exchange.sendResponseHeaders(200,0);try(var out=exchange.getResponseBody()){out.write(bytes);}});
        server.createContext("/slow",exchange->{try{Thread.sleep(200);exchange.sendResponseHeaders(200,-1);}catch(Exception ignored){}finally{exchange.close();}});
        server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort();
            java.util.function.Predicate<URI> policy=uri->uri.toString().startsWith(base+"/");
            Path output=folder.resolve("download.part");
            CurseForgeDownload.transfer(URI.create(base+"/redirect"),output,policy,100,1000,3000);assertArrayEquals(bytes,Files.readAllBytes(output));
            for(var path:List.of("/forbidden","/html","/loop","/outside"))assertThrows(IOException.class,()->CurseForgeDownload.transfer(URI.create(base+path),output,policy,100,1000,3000));
            assertThrows(IOException.class,()->CurseForgeDownload.transfer(URI.create(base+"/file"),output,policy,2,1000,3000));
            assertThrows(IOException.class,()->CurseForgeDownload.transfer(URI.create(base+"/chunked"),output,policy,2,1000,3000));
            assertThrows(java.net.SocketTimeoutException.class,()->CurseForgeDownload.transfer(URI.create(base+"/file"),output,policy,100,1000,0));
            assertThrows(java.net.SocketTimeoutException.class,()->CurseForgeDownload.transfer(URI.create(base+"/slow"),output,policy,30,30,1000));
        } finally { server.stop(0); }
    }
}
