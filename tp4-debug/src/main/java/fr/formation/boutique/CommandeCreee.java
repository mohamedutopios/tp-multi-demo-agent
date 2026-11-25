package fr.formation.boutique;

import java.math.BigDecimal;

public record CommandeCreee(long id, String clientEmail, BigDecimal total) {
}
