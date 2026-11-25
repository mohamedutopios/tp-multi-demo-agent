package fr.formation.facturation.legacy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Accès JDBC aux factures et lignes. */
final class DepotFacturation {

    private DepotFacturation() {
    }

    static List<FactureResume> facturesParNomClient(Connection conn, String nomClient) throws SQLException {
        String requete = "SELECT f.id, f.numero, f.date_emission, f.echeance, f.statut, c.id AS client_id "
                + "FROM factures f JOIN clients c ON c.id = f.client_id "
                + "WHERE c.nom = ? ORDER BY f.date_emission";
        List<FactureResume> factures = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(requete)) {
            ps.setString(1, nomClient);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    factures.add(new FactureResume(rs.getLong("id"), rs.getString("numero"),
                            rs.getDate("date_emission").toLocalDate(), rs.getDate("echeance").toLocalDate(),
                            rs.getString("statut"), rs.getLong("client_id")));
                }
            }
        }
        return factures;
    }

    /** Numéros de facture d'un client, par id de facture, triés par numéro. */
    static Map<Long, String> numerosParFacture(Connection conn, long clientId) throws SQLException {
        Map<Long, String> numeros = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, numero FROM factures WHERE client_id = ? ORDER BY numero")) {
            ps.setLong(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    numeros.put(rs.getLong("id"), rs.getString("numero"));
                }
            }
        }
        return numeros;
    }

    /** Toutes les lignes des factures d'un client en une requête, groupées par facture. */
    static Map<Long, List<LigneFacture>> lignesParFacture(Connection conn, long clientId) throws SQLException {
        Map<Long, List<LigneFacture>> lignes = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT l.facture_id, l.designation, l.prix_centimes, l.quantite, l.remise_pct "
                        + "FROM lignes l JOIN factures f ON f.id = l.facture_id "
                        + "WHERE f.client_id = ? ORDER BY l.facture_id, l.id")) {
            ps.setLong(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long factureId = rs.getLong("facture_id");
                    lignes.computeIfAbsent(factureId, k -> new ArrayList<>()).add(new LigneFacture(factureId,
                            rs.getString("designation"), rs.getLong("prix_centimes"),
                            rs.getInt("quantite"), rs.getInt("remise_pct")));
                }
            }
        }
        return lignes;
    }
}
