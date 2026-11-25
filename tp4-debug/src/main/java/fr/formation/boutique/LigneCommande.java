package fr.formation.boutique;

import jakarta.validation.constraints.Positive;

public record LigneCommande(long produitId, @Positive int quantite) {
}
