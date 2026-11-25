package fr.formation.boutique;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

@RestController
public class BoutiqueController {

    private static final Logger LOG = LoggerFactory.getLogger(BoutiqueController.class);

    private static final Map<Long, Produit> PRODUITS = Map.of(
            1L, new Produit(1, "Clavier mécanique", new BigDecimal("89.90"), 12),
            2L, new Produit(2, "Souris ergonomique", new BigDecimal("49.00"), 0),
            3L, new Produit(3, "Écran 27 pouces", new BigDecimal("279.00"), 5));

    private static final Map<String, Integer> REMISES = Map.of("BIENVENUE10", 10, "NOEL20", 20);

    private final AtomicLong ids = new AtomicLong();

    @GetMapping("/sante")
    public Map<String, Object> sante() {
        return Map.of("status", "ok", "commandes", ids.get());
    }

    @GetMapping("/produits")
    public List<Produit> listerProduits(@RequestParam(defaultValue = "false") boolean enStock) {
        return PRODUITS.values().stream()
                .filter(p -> !enStock || p.stock() > 0)
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }

    @GetMapping("/produits/{id}")
    public Produit detailProduit(@PathVariable long id) {
        Produit p = PRODUITS.get(id);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + id + " introuvable");
        }
        return p;
    }

    @PostMapping("/commandes")
    @ResponseStatus(HttpStatus.CREATED)
    public CommandeCreee creerCommande(@Valid @RequestBody Commande commande) {
        BigDecimal total = BigDecimal.ZERO;
        for (LigneCommande ligne : commande.lignes()) {
            Produit produit = PRODUITS.get(ligne.produitId());
            if (produit == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + ligne.produitId() + " introuvable");
            }
            if (produit.stock() < ligne.quantite()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuffisant pour " + produit.nom());
            }
            total = total.add(produit.prix().multiply(BigDecimal.valueOf(ligne.quantite())));
        }
        if (commande.codePromo() != null) {
            Integer remise = REMISES.get(commande.codePromo());
            if (remise == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Code promo inconnu : " + commande.codePromo());
            }
            total = total.multiply(BigDecimal.valueOf(100 - remise)).divide(BigDecimal.valueOf(100));
        }
        CommandeCreee creee = new CommandeCreee(ids.incrementAndGet(), commande.clientEmail(),
                total.setScale(2, RoundingMode.HALF_UP));
        LOG.info("Commande {} créée : {} EUR", creee.id(), creee.total());
        return creee;
    }
}
