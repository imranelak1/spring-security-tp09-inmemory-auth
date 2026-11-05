package ma.fstg.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void laPageAccueilEstPublique() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("application publique")));
    }

    @Test
    void laPageLoginEstAccessibleSansAuthentification() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Veuillez vous identifier")));
    }

    @Test
    void accesEspaceUtilisateurSansAuthRedirigeVersLogin() throws Exception {
        mvc.perform(get("/user/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void utilisateurAccedeASonEspace() throws Exception {
        mvc.perform(get("/user/dashboard").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tableau de bord Utilisateur")));
    }

    @Test
    void utilisateurNePeutPasAccederEspaceAdmin() throws Exception {
        mvc.perform(get("/admin/dashboard").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administrateurAccedeAuxDeuxEspaces() throws Exception {
        mvc.perform(get("/user/dashboard").with(user("admin").roles("ADMIN", "USER")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN", "USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tableau de bord Administrateur")));
    }

    @Test
    void connexionAvecBonsIdentifiantsReussit() throws Exception {
        mvc.perform(formLogin("/login").user("user").password("1111"))
                .andExpect(authenticated().withUsername("user").withRoles("USER"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void connexionAdminReussit() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("1234"))
                .andExpect(authenticated().withUsername("admin").withRoles("ADMIN", "USER"));
    }

    @Test
    void connexionAvecMauvaisMotDePasseEchoue() throws Exception {
        mvc.perform(formLogin("/login").user("user").password("mauvais"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void deconnexionRedirigeVersAccueil() throws Exception {
        mvc.perform(logout())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void lEncodeurDeMotDePasseEstBCrypt() {
        assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);
        String hash = passwordEncoder.encode("1111");
        assertTrue(hash.startsWith("$2"), "Le hash BCrypt doit commencer par $2");
        assertTrue(passwordEncoder.matches("1111", hash), "BCrypt doit vérifier le mot de passe encodé");
    }
}
