package fr.formation.facturation.legacy;

import java.util.List;

/** Règles de calcul des montants (centimes). */
final class CalculFacturation {

    static final long TAUX_TVA_PCT = 20;

    private CalculFacturation() {
    }

    /** Montant HT d'une ligne, avec division entière par ligne. */
    static long montantLigneHt(long prixCentimes, int quantite, int remisePct) {
        return prixCentimes * quantite * (100 - remisePct) / 100;
    }

    static long totalHt(List<LigneFacture> lignes) {
        long total = 0;
        for (LigneFacture l : lignes) {
            total += montantLigneHt(l.prixCentimes(), l.quantite(), l.remisePct());
        }
        return total;
    }

    static long totalTtc(List<LigneFacture> lignes) {
        return totalHt(lignes) * (100 + TAUX_TVA_PCT) / 100;
    }
}
