package fr.formation.facturation.legacy;

/** Ligne de facture (montants en centimes). */
record LigneFacture(long factureId, String designation, long prixCentimes, int quantite, int remisePct) {
}
