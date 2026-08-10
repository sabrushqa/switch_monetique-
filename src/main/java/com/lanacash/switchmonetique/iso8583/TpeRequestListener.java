package com.lanacash.switchmonetique.iso8583;

import com.lanacash.switchmonetique.entities.TransactionMonetique;
import com.lanacash.switchmonetique.entities.enums.StatutTransaction;
import com.lanacash.switchmonetique.entities.enums.TypeTransaction;
import com.lanacash.switchmonetique.services.AuthorizationService;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Recoit les messages ISO 8583 envoyes par les TPE simules (MTI 0200 = demande d'autorisation)
 * et renvoie une reponse 0210 avec le code reponse (DE39) decide par l'AuthorizationService.
 *
 * DE utilises : DE2 = PAN, DE4 = montant, DE11 = STAN, DE37 = RRN, DE39 = code reponse,
 * DE41 = id TPE.
 */
@Component
public class TpeRequestListener implements ISORequestListener {

    private final AuthorizationService authorizationService;

    public TpeRequestListener(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        try {
            if (!"0200".equals(request.getMTI())) {
                return false;
            }

            String pan = request.getString(2);
            BigDecimal montant = new BigDecimal(request.getString(4)).movePointLeft(2);
            String stan = request.getString(11);
            String idTpe = request.getString(41);
            String rrn = request.hasField(37) ? request.getString(37) : stan;

            boolean carteValide = pan != null && pan.length() >= 12;
            boolean carteExpiree = pan != null && pan.endsWith("0000");

            TransactionMonetique transaction = authorizationService.autoriserTransactionTpe(
                    idTpe, montant, TypeTransaction.ACHAT, stan, rrn, carteValide, carteExpiree);

            ISOMsg response = (ISOMsg) request.clone();
            response.setResponseMTI();
            response.set(39, transaction.getCodeReponse());
            if (transaction.getStatut() == StatutTransaction.APPROVED) {
                response.set(38, transaction.getCodeAutorisation());
            }
            source.send(response);
        } catch (ISOException | IOException | RuntimeException e) {
            // En cas d'erreur de parsing, on ne repond pas : le TPE simulera un TIMEOUT (scenario 5).
            return false;
        }
        return true;
    }
}
