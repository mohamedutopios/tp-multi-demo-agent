# Projet tp3-validation

Module de facturation hérité — Java 21, JDBC, base H2 en mémoire, Maven.
Code : `src/main/java/fr/formation/facturation/legacy/`.

## Commandes
- Compiler : `mvn -B -ntp compile`
- Tests : `mvn -B -ntp test`
- Analyse statique : `mvn -B -ntp -P qualite compile spotbugs:check pmd:check`

## Conventions
- Le texte produit par `genererRapportClient` ne doit pas changer (test golden master).
- Signatures publiques de `ServiceFacturation` inchangées.
- Pas de dépendance supplémentaire.
