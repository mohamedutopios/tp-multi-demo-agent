package fr.formation.boutique;

import java.math.BigDecimal;

public record Produit(long id, String nom, BigDecimal prix, int stock) {
}
