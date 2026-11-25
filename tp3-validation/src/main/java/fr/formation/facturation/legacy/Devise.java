package fr.formation.facturation.legacy;

import java.util.Locale;

/** Devises supportées ; le taux est exprimé en dix-millièmes d'euro (10000 = parité). */
enum Devise {
    EUR(10000), USD(10800), CHF(9400);

    private static final double DIVISEUR = 1_000_000.0;
    private final long taux;

    Devise(long taux) {
        this.taux = taux;
    }

    static Devise parse(String code) {
        if (code != null) {
            for (Devise d : values()) {
                if (d.name().equals(code)) {
                    return d;
                }
            }
        }
        throw new IllegalArgumentException("Devise non supportée : " + code);
    }

    /** Formate un montant en centimes d'euro, converti dans cette devise, avec son code. */
    String formater(long centimes) {
        return String.format(Locale.ROOT, "%.2f %s", centimes * taux / DIVISEUR, name());
    }
}
