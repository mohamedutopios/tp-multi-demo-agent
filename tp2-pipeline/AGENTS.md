# Projet tp2-pipeline

API REST de réservation de salles de réunion — Java 21, Spring Boot 4.2, Maven.

## Structure imposée
- Package racine : `fr.formation.reservation`
- Classe principale : `fr.formation.reservation.ReservationApplication`
- Code : `src/main/java/fr/formation/reservation/`
- Tests : `src/test/java/fr/formation/reservation/`
- Conception : `docs/design.md`
- Documentation utilisateur : `README.md`

## Commandes
- Compiler : `mvn -B -ntp compile`
- Tests : `mvn -B -ntp test`
- Lancer : `mvn -q spring-boot:run` (port 8080)

## Conventions
- Stockage en mémoire (pas de base de données dans cette itération).
- Pas de dépendance supplémentaire sans justification dans `docs/design.md`.
