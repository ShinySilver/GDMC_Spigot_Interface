package com.gdmc;

import com.gdmc.endpoints.BuildAreaHandler;
import com.gdmc.endpoints.ChunkHandler;
import com.gdmc.endpoints.CommandHandler;
import com.gdmc.endpoints.BlocksHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class GdmcHttpServer {
    private static HttpServer httpServer;

    public static void startServer() throws IOException {

        httpServer = HttpServer.create(new InetSocketAddress(9000), 0);
        httpServer.setExecutor(null);
        createContexts();
        httpServer.start();
    }

    public static void stopServer() {
        if(httpServer != null) {
            httpServer.stop(5);
        }
    }

    private static void createContexts() {
        httpServer.createContext("/command", new CommandHandler());
        httpServer.createContext("/chunks", new ChunkHandler());
        httpServer.createContext("/blocks", new BlocksHandler());
        httpServer.createContext("/buildarea", new BuildAreaHandler());
    }
}

