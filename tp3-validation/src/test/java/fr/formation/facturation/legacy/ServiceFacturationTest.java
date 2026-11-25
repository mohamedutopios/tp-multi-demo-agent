package fr.formation.facturation.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests fonctionnels du service de facturation : ils sont tous verts.
 * Le rapport attendu est figé (test « golden master ») : toute refactorisation
 * doit produire exactement le même texte.
 */
class ServiceFacturationTest {

    private static final LocalDate REF = LocalDate.of(2026, 11, 23);

    private static final String RAPPORT_DUPONT = """
            ============================================================
            RAPPORT CLIENT : Dupont SARL
            Date de référence : 2026-11-23
            ============================================================
            F-2026-001 | 2026-09-01 | 1800.00 EUR | PAYÉE
                - Audit sécurité x1 : 1200.00 HT
                - Frais de déplacement x2 : 300.00 HT
            F-2026-002 | 2026-09-15 | 3078.00 EUR | EN RETARD 39 j - RELANCE 2
                - Formation Kubernetes x3 (-10 %) : 2565.00 HT
            F-2026-003 | 2026-10-20 | 912.00 EUR | EN RETARD 3 j - RELANCE 1
                - Support mensuel x1 : 400.00 HT
                - Heures supplémentaires x4 : 360.00 HT
            ------------------------------------------------------------
            Total facturé : 5790.00 EUR
            Total payé    : 1800.00 EUR
            Reste dû      : 3990.00 EUR
            2 factures en retard (3990.00 EUR)
            Niveau de relance : RELANCE 2
            ============================================================""";

    private static final String RAPPORT_MARTIN_USD = """
            ============================================================
            RAPPORT CLIENT : Martin & Fils
            Date de référence : 2026-11-23
            ============================================================
            F-2026-004 | 2026-08-01 | 3693.60 USD | EN RETARD 84 j - CONTENTIEUX
            F-2026-005 | ANNULÉE
            ------------------------------------------------------------
            Total facturé : 3693.60 USD
            Total payé    : 0.00 USD
            Reste dû      : 3693.60 USD
            1 facture en retard (3420.00 EUR)
            Niveau de relance : CONTENTIEUX
            Factures annulées : 1
            ============================================================""";

    private Connection conn;
    private final ServiceFacturation service = new ServiceFacturation();

    @BeforeEach
    void ouvrir() throws Exception {
        conn = Base.creer();
    }

    @AfterEach
    void fermer() throws Exception {
        conn.close();
    }

    @Test
    void recherche_factures_triees_par_date() throws Exception {
        List<String> numeros = service.rechercherFacturesClient(conn, "Dupont SARL").stream()
                .map(FactureResume::numero).toList();
        assertEquals(List.of("F-2026-001", "F-2026-002", "F-2026-003"), numeros);
    }

    @Test
    void recherche_client_inconnu() throws Exception {
        assertEquals(List.of(), service.rechercherFacturesClient(conn, "Inconnu SA"));
    }

    @Test
    void totaux_ttc_en_centimes() throws Exception {
        assertEquals(Map.of("F-2026-001", 180000L, "F-2026-002", 307800L, "F-2026-003", 91200L),
                service.totauxFactures(conn, 1));
    }

    @Test
    void rapport_complet_dupont() throws Exception {
        assertEquals(RAPPORT_DUPONT, service.genererRapportClient(conn, "Dupont SARL", REF, true, "EUR"));
    }

    @Test
    void rapport_martin_usd_sans_detail() throws Exception {
        assertEquals(RAPPORT_MARTIN_USD, service.genererRapportClient(conn, "Martin & Fils", REF, false, "USD"));
    }

    @Test
    void rapport_client_sans_facture() throws Exception {
        assertEquals("Aucune facture pour le client Inconnu SA",
                service.genererRapportClient(conn, "Inconnu SA", REF, true, "EUR"));
    }

    @Test
    void rapport_chf() throws Exception {
        String rapport = service.genererRapportClient(conn, "Atelier Nord", REF, true, "CHF");
        assertTrue(rapport.contains("Total facturé : 2481.60 CHF"));
        assertTrue(rapport.contains("Aucune facture en retard"));
    }

    @Test
    void devise_inconnue_refusee() {
        assertThrows(IllegalArgumentException.class,
                () -> service.genererRapportClient(conn, "Dupont SARL", REF, true, "GBP"));
    }
}
