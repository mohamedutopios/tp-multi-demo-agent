package fr.formation.facturation.legacy;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Service de facturation : délègue l'accès aux données et le calcul, assemble le rapport. */
public class ServiceFacturation {

    private static final int LARGEUR_RAPPORT = 60;
    private static final int LONGUEUR_MAX_NOM = 100;
    private static final int SEUIL_REMISE_ANORMALE_PCT = 50;
    private static final String STATUT_ANNULEE = "annulee";
    private static final String STATUT_PAYEE = "payee";

    /** Cumuls du rapport. */
    private static final class Cumuls {
        long general;
        long paye;
        long du;
        long retard;
        int nbRetard;
        int nbAnnulees;
        NiveauRelance pireNiveau = NiveauRelance.AUCUN;
    }

    /** Renvoie les factures d'un client, triées par date d'émission. */
    public List<FactureResume> rechercherFacturesClient(Connection conn, String nomClient) throws SQLException {
        validerNomClient(nomClient);
        return DepotFacturation.facturesParNomClient(conn, nomClient);
    }

    /** Renvoie le total TTC (en centimes) de chaque facture d'un client. */
    public Map<String, Long> totauxFactures(Connection conn, long clientId) throws SQLException {
        Map<Long, String> numeros = DepotFacturation.numerosParFacture(conn, clientId);
        Map<Long, List<LigneFacture>> lignes = DepotFacturation.lignesParFacture(conn, clientId);
        Map<String, Long> totaux = new LinkedHashMap<>();
        for (Map.Entry<Long, String> e : numeros.entrySet()) {
            totaux.put(e.getValue(), CalculFacturation.totalTtc(lignes.getOrDefault(e.getKey(), List.of())));
        }
        return totaux;
    }

    /** Génère le rapport texte de situation d'un client. */
    public String genererRapportClient(Connection conn, String nomClient, LocalDate dateReference,
                                       boolean inclureDetail, String devise) throws SQLException {
        List<FactureResume> factures = rechercherFacturesClient(conn, nomClient);
        if (factures.isEmpty()) {
            return "Aucune facture pour le client " + nomClient;
        }
        long clientId = factures.get(0).clientId();
        Map<Long, List<LigneFacture>> lignes = DepotFacturation.lignesParFacture(conn, clientId);
        List<String> rapport = new ArrayList<>();
        ajouterEnTete(rapport, nomClient, dateReference);
        Cumuls cumuls = new Cumuls();
        for (FactureResume f : factures) {
            List<LigneFacture> lignesFacture = f.clientId() == clientId
                    ? lignes.getOrDefault(f.id(), List.of()) : List.of();
            ajouterFacture(rapport, f, lignesFacture, cumuls, dateReference, inclureDetail, devise);
        }
        ajouterPied(rapport, cumuls, devise);
        return String.join("\n", rapport);
    }

    private static void validerNomClient(String nomClient) {
        if (nomClient == null || nomClient.length() > LONGUEUR_MAX_NOM) {
            throw new IllegalArgumentException("Nom de client invalide");
        }
        if (nomClient.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Nom de client invalide : caractères de contrôle interdits");
        }
    }

    private static String separateur(char c) {
        return String.valueOf(c).repeat(LARGEUR_RAPPORT);
    }

    private static void ajouterEnTete(List<String> rapport, String nomClient, LocalDate dateReference) {
        rapport.add(separateur('='));
        rapport.add("RAPPORT CLIENT : " + nomClient);
        rapport.add("Date de référence : " + dateReference);
        rapport.add(separateur('='));
    }

    private static void ajouterFacture(List<String> rapport, FactureResume f, List<LigneFacture> lignes,
                                       Cumuls cumuls, LocalDate dateReference, boolean inclureDetail,
                                       String devise) {
        if (STATUT_ANNULEE.equals(f.statut())) {
            cumuls.nbAnnulees++;
            rapport.add(f.numero() + " | ANNULÉE");
            return;
        }
        long montant = CalculFacturation.totalTtc(lignes);
        Devise cible = Devise.parse(devise);
        String etat = etatFacture(f, montant, cumuls, dateReference);
        rapport.add(f.numero() + " | " + f.dateEmission() + " | " + cible.formater(montant) + " | " + etat);
        if (inclureDetail) {
            lignes.forEach(l -> rapport.add(detailLigne(l)));
        }
    }

    /** Classe la facture (non annulée), met à jour les cumuls et renvoie son libellé d'état. */
    private static String etatFacture(FactureResume f, long montant, Cumuls cumuls, LocalDate dateReference) {
        cumuls.general += montant;
        if (STATUT_PAYEE.equals(f.statut())) {
            cumuls.paye += montant;
            return "PAYÉE";
        }
        cumuls.du += montant;
        if (!f.echeance().isBefore(dateReference)) {
            return "À ÉCHÉANCE " + f.echeance();
        }
        long jours = ChronoUnit.DAYS.between(f.echeance(), dateReference);
        NiveauRelance niveau = NiveauRelance.pourRetard(jours);
        cumuls.retard += montant;
        cumuls.nbRetard++;
        cumuls.pireNiveau = cumuls.pireNiveau.plusGrave(niveau);
        return "EN RETARD " + jours + " j - " + niveau.libelle();
    }

    private static String detailLigne(LigneFacture l) {
        if (l.quantite() <= 0) {
            return "    ? " + l.designation() + " : quantité invalide";
        }
        if (l.remisePct() >= SEUIL_REMISE_ANORMALE_PCT) {
            return "    ! " + l.designation() + " : remise anormale " + l.remisePct() + " %";
        }
        if (l.remisePct() > 0) {
            long ht = CalculFacturation.montantLigneHt(l.prixCentimes(), l.quantite(), l.remisePct());
            return "    - " + l.designation() + " x" + l.quantite() + " (-" + l.remisePct() + " %) : "
                    + montantHt(ht) + " HT";
        }
        long ht = CalculFacturation.montantLigneHt(l.prixCentimes(), l.quantite(), 0);
        return "    - " + l.designation() + " x" + l.quantite() + " : " + montantHt(ht) + " HT";
    }

    private static String montantHt(long centimes) {
        return String.format(Locale.ROOT, "%.2f", centimes / 100.0);
    }

    private static void ajouterPied(List<String> rapport, Cumuls c, String devise) {
        Devise cible = deviseDuPied(devise);
        rapport.add(separateur('-'));
        rapport.add("Total facturé : " + cible.formater(c.general));
        rapport.add("Total payé    : " + cible.formater(c.paye));
        rapport.add("Reste dû      : " + cible.formater(c.du));
        ajouterRetard(rapport, c);
        if (c.nbAnnulees > 0) {
            rapport.add("Factures annulées : " + c.nbAnnulees);
        }
        rapport.add(separateur('='));
    }

    /** Comportement historique : une devise inconnue n'échoue que si une facture est formatée, sinon CHF. */
    private static Devise deviseDuPied(String devise) {
        try {
            return Devise.parse(devise);
        } catch (IllegalArgumentException e) {
            return Devise.CHF;
        }
    }

    /** Le montant en retard est toujours affiché en EUR (comportement historique). */
    private static void ajouterRetard(List<String> rapport, Cumuls c) {
        if (c.nbRetard == 0) {
            rapport.add("Aucune facture en retard");
            return;
        }
        String montant = Devise.EUR.formater(c.retard);
        rapport.add(c.nbRetard == 1
                ? "1 facture en retard (" + montant + ")"
                : c.nbRetard + " factures en retard (" + montant + ")");
        rapport.add("Niveau de relance : " + c.pireNiveau.libelle());
    }
}
