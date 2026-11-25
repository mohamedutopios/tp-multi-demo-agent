package fr.formation.boutique;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Ticket 4127 : un code promo inconnu (PROMO50) ne doit pas provoquer une erreur 500. */
@SpringBootTest
@AutoConfigureMockMvc
class Ticket4127Test {

    @Autowired
    MockMvc mvc;

    @Test
    void code_promo_inconnu_renvoie_400_avec_message() throws Exception {
        String body = "{\"clientEmail\":\"a@example.com\",\"lignes\":[{\"produitId\":1,\"quantite\":1}],"
                + "\"codePromo\":\"PROMO50\"}";
        var result = mvc.perform(post("/commandes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andReturn();
        String reason = result.getResponse().getErrorMessage();
        String content = result.getResponse().getContentAsString();
        String all = (reason == null ? "" : reason) + content;
        assertEquals(true, all.contains("PROMO50") && all.toLowerCase().contains("promo"),
                "Le message doit mentionner le code promo PROMO50 invalide, reçu : " + all);
    }
}
