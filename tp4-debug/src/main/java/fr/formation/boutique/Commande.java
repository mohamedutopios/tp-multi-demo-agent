package fr.formation.boutique;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record Commande(@NotBlank String clientEmail, @NotEmpty List<@Valid LigneCommande> lignes, String codePromo) {
}
