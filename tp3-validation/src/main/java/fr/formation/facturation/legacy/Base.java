package fr.formation.facturation.legacy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/** Base H2 en mémoire de démonstration pour le module de facturation. */
public final class Base {

    private static final String SCHEMA = """
            CREATE TABLE clients (
                id   BIGINT PRIMARY KEY,
                nom  VARCHAR(100) NOT NULL UNIQUE
            );
            CREATE TABLE factures (
                id            BIGINT PRIMARY KEY,
                client_id     BIGINT NOT NULL REFERENCES clients(id),
                numero        VARCHAR(20) NOT NULL UNIQUE,
                date_emission DATE NOT NULL,
                echeance      DATE NOT NULL,
                statut        VARCHAR(10) NOT NULL CHECK (statut IN ('payee', 'emise', 'annulee'))
            );
            CREATE TABLE lignes (
                id            BIGINT PRIMARY KEY,
                facture_id    BIGINT NOT NULL REFERENCES factures(id),
                designation   VARCHAR(100) NOT NULL,
                prix_centimes BIGINT NOT NULL,
                quantite      INT NOT NULL,
                remise_pct    INT NOT NULL DEFAULT 0
            );
            CREATE INDEX idx_factures_client ON factures(client_id);
            CREATE INDEX idx_lignes_facture ON lignes(facture_id, id);
            """;

    private Base() {
    }

    private static Connection vide() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID());
        try (Statement s = c.createStatement()) {
            s.execute(SCHEMA);
        }
        return c;
    }

    /** Crée une base remplie avec les données de démonstration (3 clients, 6 factures). */
    public static Connection creer() throws SQLException {
        Connection c = vide();
        try (Statement s = c.createStatement()) {
            s.execute("""
                    INSERT INTO clients VALUES (1, 'Dupont SARL'), (2, 'Martin & Fils'), (3, 'Atelier Nord')""");
            s.execute("""
                    INSERT INTO factures VALUES
                    (1, 1, 'F-2026-001', DATE '2026-09-01', DATE '2026-10-01', 'payee'),
                    (2, 1, 'F-2026-002', DATE '2026-09-15', DATE '2026-10-15', 'emise'),
                    (3, 1, 'F-2026-003', DATE '2026-10-20', DATE '2026-11-20', 'emise'),
                    (4, 2, 'F-2026-004', DATE '2026-08-01', DATE '2026-08-31', 'emise'),
                    (5, 2, 'F-2026-005', DATE '2026-10-01', DATE '2026-10-31', 'annulee'),
                    (6, 3, 'F-2026-006', DATE '2026-10-05', DATE '2026-11-05', 'payee')""");
            s.execute("""
                    INSERT INTO lignes VALUES
                    (1, 1, 'Audit sécurité', 120000, 1, 0),
                    (2, 1, 'Frais de déplacement', 15000, 2, 0),
                    (3, 2, 'Formation Kubernetes', 95000, 3, 10),
                    (4, 3, 'Support mensuel', 40000, 1, 0),
                    (5, 3, 'Heures supplémentaires', 9000, 4, 0),
                    (6, 4, 'Développement API', 60000, 5, 5),
                    (7, 5, 'Maintenance', 30000, 1, 0),
                    (8, 6, 'Conseil architecture', 110000, 2, 0)""");
        }
        return c;
    }

    /** Base d'un seul client avec beaucoup de factures (mesures de performance). */
    public static Connection creerVolumineuse(int nbFactures, int lignesParFacture) throws SQLException {
        Connection c = vide();
        try (Statement s = c.createStatement()) {
            s.execute("INSERT INTO clients VALUES (1, 'Gros Client')");
        }
        try (PreparedStatement f = c.prepareStatement(
                "INSERT INTO factures VALUES (?, 1, ?, DATE '2026-10-01', DATE '2026-10-31', 'emise')");
             PreparedStatement l = c.prepareStatement("INSERT INTO lignes VALUES (?, ?, ?, ?, 1, 0)")) {
            long lid = 1;
            for (int i = 1; i <= nbFactures; i++) {
                f.setLong(1, i);
                f.setString(2, String.format("F-%06d", i));
                f.addBatch();
                for (int j = 0; j < lignesParFacture; j++) {
                    l.setLong(1, lid++);
                    l.setLong(2, i);
                    l.setString(3, "Prestation " + j);
                    l.setLong(4, 10000L + j);
                    l.addBatch();
                }
            }
            f.executeBatch();
            l.executeBatch();
        }
        return c;
    }
}
