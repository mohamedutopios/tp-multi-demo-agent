package fr.formation.facturation.legacy;

import java.time.LocalDate;

/** Résumé d'une facture, tel que renvoyé par la recherche. */
public record FactureResume(long id, String numero, LocalDate dateEmission, LocalDate echeance,
                            String statut, long clientId) {
}
