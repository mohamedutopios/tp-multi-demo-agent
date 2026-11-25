package fr.formation.facturation.legacy;

/** Niveau de relance, du moins grave au plus grave (l'ordre des constantes compte). */
enum NiveauRelance {
    AUCUN("AUCUN"), RELANCE_1("RELANCE 1"), RELANCE_2("RELANCE 2"), CONTENTIEUX("CONTENTIEUX");

    static final long SEUIL_RELANCE_2_JOURS = 30;
    static final long SEUIL_CONTENTIEUX_JOURS = 60;

    private final String libelle;

    NiveauRelance(String libelle) {
        this.libelle = libelle;
    }

    String libelle() {
        return libelle;
    }

    static NiveauRelance pourRetard(long jours) {
        if (jours > SEUIL_CONTENTIEUX_JOURS) {
            return CONTENTIEUX;
        }
        return jours > SEUIL_RELANCE_2_JOURS ? RELANCE_2 : RELANCE_1;
    }

    NiveauRelance plusGrave(NiveauRelance autre) {
        return autre.compareTo(this) > 0 ? autre : this;
    }
}
