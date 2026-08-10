package com.lanacash.switchmonetique.config;

import com.lanacash.switchmonetique.iso8583.TpeRequestListener;
import jakarta.annotation.PreDestroy;
import org.jpos.iso.ISOServer;
import org.jpos.iso.ServerChannel;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.ThreadPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.concurrent.Executors;

/**
 * Demarre un serveur ISO 8583 sur socket TCP : c'est le canal par lequel les TPE
 * physiques simules (module tpe-simulator) envoient leurs transactions, exactement
 * comme un vrai terminal de paiement le ferait vers un switch bancaire.
 */
@Configuration
public class Iso8583ServerConfig {

    @Value("${switch.iso8583.port:10000}")
    private int port;

    private ISOServer isoServer;

    @Bean
    public ISOServer isoServer(TpeRequestListener tpeRequestListener) throws Exception {
        // GenericPackager(String) resout le nom comme un chemin fichier relatif au
        // repertoire courant du process, pas comme une ressource classpath : on charge
        // donc explicitement le flux depuis le classpath (src/main/resources).
        InputStream packagerConfig = getClass().getClassLoader().getResourceAsStream("iso8583-packager.xml");
        if (packagerConfig == null) {
            throw new IllegalStateException("Ressource classpath iso8583-packager.xml introuvable.");
        }
        GenericPackager packager = new GenericPackager(packagerConfig);
        ServerChannel channel = new ASCIIChannel(packager);
        isoServer = new ISOServer(port, channel, new ThreadPool(1, ThreadPool.DEFAULT_MAX_THREADS));
        isoServer.addISORequestListener(tpeRequestListener);
        Executors.newSingleThreadExecutor().submit(isoServer);
        return isoServer;
    }

    @PreDestroy
    public void shutdown() {
        if (isoServer != null) {
            isoServer.shutdown();
        }
    }
}
