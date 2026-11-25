package fr.formation.boutique;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Tests existants de l'API boutique (tous verts). */
@SpringBootTest
@AutoConfigureMockMvc
class BoutiqueControllerTest {

    @Autowired
    MockMvc mvc;

    private String commande(long produitId, int quantite, String codePromo) {
        String code = codePromo == null ? "" : ",\"codePromo\":\"" + codePromo + "\"";
        return "{\"clientEmail\":\"a@example.com\",\"lignes\":[{\"produitId\":" + produitId
                + ",\"quantite\":" + quantite + "}]" + code + "}";
    }

    @Test
    void sante() throws Exception {
        mvc.perform(get("/sante")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void liste_produits_en_stock() throws Exception {
        mvc.perform(get("/produits").param("enStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(3));
    }

    @Test
    void produit_inconnu_404() throws Exception {
        mvc.perform(get("/produits/99")).andExpect(status().isNotFound());
    }

    @Test
    void commande_sans_code_promo() throws Exception {
        mvc.perform(post("/commandes").contentType(MediaType.APPLICATION_JSON).content(commande(1, 2, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(179.80));
    }

    @Test
    void commande_avec_code_noel20() throws Exception {
        mvc.perform(post("/commandes").contentType(MediaType.APPLICATION_JSON).content(commande(3, 1, "NOEL20")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(223.20));
    }

    @Test
    void stock_insuffisant_409() throws Exception {
        mvc.perform(post("/commandes").contentType(MediaType.APPLICATION_JSON).content(commande(2, 1, null)))
                .andExpect(status().isConflict());
    }
}
