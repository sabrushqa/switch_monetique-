package com.lanacash.switchmonetique.iso8583;

import com.lanacash.switchmonetique.entities.Tpe;
import com.lanacash.switchmonetique.entities.TransactionMonetique;
import com.lanacash.switchmonetique.repositories.SiteEcommerceRepository;
import com.lanacash.switchmonetique.repositories.TpeRepository;
import com.lanacash.switchmonetique.repositories.TransactionRepository;
import com.lanacash.switchmonetique.services.AuthorizationService;
import com.lanacash.switchmonetique.services.DemoTransactionNotifier;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOServer;
import org.jpos.iso.ServerChannel;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.ThreadPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test d'integration "bout en bout" du canal TPE : un vrai client ISO 8583
 * (memes classes jPOS que le module tpe-simulator : ASCIIChannel +
 * GenericPackager charge depuis iso8583-packager.xml) parle sur un vrai
 * socket TCP a un vrai ISOServer, lui-meme branche sur le TpeRequestListener
 * et l'AuthorizationService reels de ce service.
 *
 * Seuls les repositories JPA (donc Oracle) et le notifieur webhook vers
 * "demo" sont mockes avec Mockito - exactement comme AuthorizationServiceTest
 * et TpeRequestListenerTest le font deja - afin de ne pas dependre d'une base
 * de donnees ni d'un service externe pour valider le protocole. Ce test
 * couvre donc ce que ni TpeRequestListenerTest (appel direct de
 * listener.process(), sans socket ni encodage ISO 8583 reel) ni
 * ScenarioRunnerTest / ScenarioTest (cote tpe-simulator, sans switch en
 * face) ne verifient : que les deux modules se comprennent reellement sur le
 * fil, avec les memes scenarios que docs/scenarios.md.
 */
class TpeSwitchProtocolIntegrationTest {

    private static final String PACKAGER_RESOURCE = "iso8583-packager.xml";
    private static final AtomicInteger STAN_SEQUENCE = new AtomicInteger(1);

    private static ExecutorService serverExecutor;
    private static ISOServer isoServer;
    private static int port;

    private static TpeRepository tpeRepository;
    private static TransactionRepository transactionRepository;
    private static SiteEcommerceRepository siteEcommerceRepository;
    private static DemoTransactionNotifier demoTransactionNotifier;

    @BeforeAll
    static void demarrerServeurIso8583Reel() throws Exception {
        tpeRepository = mock(TpeRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        siteEcommerceRepository = mock(SiteEcommerceRepository.class);
        demoTransactionNotifier = mock(DemoTransactionNotifier.class);

        AuthorizationService authorizationService = new AuthorizationService(
                transactionRepository, tpeRepository, demoTransactionNotifier, siteEcommerceRepository);
        TpeRequestListener listener = new TpeRequestListener(authorizationService);

        port = portLibre();
        GenericPackager packager = new GenericPackager(packagerDepuisClasspath());
        ServerChannel channel = new ASCIIChannel(packager);
        isoServer = new ISOServer(port, channel, new ThreadPool(1, ThreadPool.DEFAULT_MAX_THREADS));
        isoServer.addISORequestListener(listener);

        serverExecutor = Executors.newSingleThreadExecutor();
        serverExecutor.submit(isoServer);
        attendreQueLePortEcoute();
    }

    @AfterAll
    static void arreterServeur() {
        if (isoServer != null) {
            isoServer.shutdown();
        }
        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
        }
    }

    @BeforeEach
    void reinitialiserLesMocks() {
        org.mockito.Mockito.reset(tpeRepository, transactionRepository, siteEcommerceRepository, demoTransactionNotifier);
        lenient().when(transactionRepository.save(any(TransactionMonetique.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void verifierAucuneInteractionOubliee() {
        // rien a fermer cote client : chaque test ouvre/ferme son propre channel.
    }

    @Test
    void scenarioApprouveStandardRepondApprouveAvecCodeAutorisation() throws Exception {
        tpeActifAffecte("TPE00001", "COM-1", "PDV-1", "50000");
        aucunCumulNiVelociteSurLeTpe("TPE00001");

        ISOMsg response = envoyerScenario("TPE00001", "4111111111111111", "000000015000");

        assertEquals("0210", response.getMTI());
        assertEquals("00", response.getString(39));
        assertTrue(response.hasField(38), "une transaction approuvee doit porter un code d'autorisation (DE38)");
    }

    @Test
    void scenarioCarteExpireeEstRefuseParLeSwitch() throws Exception {
        tpeActifAffecte("TPE00001", "COM-1", "PDV-1", "50000");
        aucunCumulNiVelociteSurLeTpe("TPE00001");

        ISOMsg response = envoyerScenario("TPE00001", "4111111111110000", "000000008000");

        assertEquals("54", response.getString(39));
        assertFalse(response.hasField(38));
    }

    @Test
    void scenarioCarteInvalideEstRefuseParLeSwitch() throws Exception {
        tpeActifAffecte("TPE00001", "COM-1", "PDV-1", "50000");
        aucunCumulNiVelociteSurLeTpe("TPE00001");

        ISOMsg response = envoyerScenario("TPE00001", "123", "000000005000");

        assertEquals("14", response.getString(39));
        assertFalse(response.hasField(38));
    }

    @Test
    void scenarioPlafondJournalierDepasseEstRefuseParLeSwitch() throws Exception {
        tpeActifAffecte("TPE00001", "COM-1", "PDV-1", "50000");
        lenient().when(transactionRepository.sumMontantApprouveDepuis(anyString(), any()))
                .thenReturn(new BigDecimal("49999"));
        lenient().when(transactionRepository.countByIdTpeAndDateTransactionAfter(anyString(), any()))
                .thenReturn(0L);

        ISOMsg response = envoyerScenario("TPE00001", "4111111111111111", "000099999900");

        assertEquals("61", response.getString(39));
        assertFalse(response.hasField(38));
    }

    @Test
    void scenarioTpeInconnuOuNonAffilieEstRefuseParLeSwitch() throws Exception {
        when(tpeRepository.findById("TPE-FANTOME")).thenReturn(Optional.empty());

        ISOMsg response = envoyerScenario("TPE-FANTOME", "4111111111111111", "000000015000");

        assertEquals("05", response.getString(39));
        assertFalse(response.hasField(38));
    }

    // ---- Aides de test : cote "base de donnees" simulee ----

    private void tpeActifAffecte(String idTpe, String idCommercant, String idPdv, String plafond) {
        Tpe tpe = new Tpe();
        tpe.setIdTpe(idTpe);
        tpe.setIdCommercant(idCommercant);
        tpe.setIdPdv(idPdv);
        tpe.setActif(true);
        tpe.setPlafondJournalier(new BigDecimal(plafond));
        when(tpeRepository.findById(idTpe)).thenReturn(Optional.of(tpe));
    }

    private void aucunCumulNiVelociteSurLeTpe(String idTpe) {
        lenient().when(transactionRepository.sumMontantApprouveDepuis(anyString(), any())).thenReturn(BigDecimal.ZERO);
        lenient().when(transactionRepository.countByIdTpeAndDateTransactionAfter(anyString(), any())).thenReturn(0L);
    }

    // ---- Aides de test : cote "client TPE" reel (memes classes jPOS que tpe-simulator) ----

    /**
     * Envoie une demande d'autorisation (MTI 0200) au vrai ISOServer du switch,
     * comme le ferait ScenarioRunner.executer() dans le module tpe-simulator,
     * et retourne la reponse 0210 recue en clair sur le socket.
     */
    private ISOMsg envoyerScenario(String idTpe, String pan, String montantCentimes) throws Exception {
        GenericPackager packager = new GenericPackager(packagerDepuisClasspath());
        ASCIIChannel client = new ASCIIChannel("localhost", port, packager);
        client.setTimeout(5000);
        client.connect();
        try {
            ISOMsg request = new ISOMsg();
            request.setMTI("0200");
            request.set(2, pan);
            request.set(3, "000000");
            request.set(4, montantCentimes);
            request.set(11, String.format("%06d", STAN_SEQUENCE.getAndIncrement()));
            request.set(37, String.format("%012d", System.nanoTime() % 1_000_000_000_000L));
            request.set(41, idTpe);

            client.send(request);
            return client.receive();
        } finally {
            client.disconnect();
        }
    }

    private static InputStream packagerDepuisClasspath() {
        InputStream stream = TpeSwitchProtocolIntegrationTest.class.getClassLoader()
                .getResourceAsStream(PACKAGER_RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Ressource classpath " + PACKAGER_RESOURCE + " introuvable.");
        }
        return stream;
    }

    private static int portLibre() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void attendreQueLePortEcoute() throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite) {
            try (java.net.Socket probe = new java.net.Socket("localhost", port)) {
                return;
            } catch (ConnectException notReadyYet) {
                Thread.sleep(50);
            } catch (IOException other) {
                Thread.sleep(50);
            }
        }
        throw new IllegalStateException("Le serveur ISO 8583 de test n'a pas demarre sur le port " + port);
    }
}
