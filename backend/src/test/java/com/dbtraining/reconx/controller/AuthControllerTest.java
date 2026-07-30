package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.AppUserRepository;
import com.dbtraining.reconx.repository.entity.AppUser;
import com.dbtraining.reconx.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AuthControllerTest {

    private static final String SEEDED_TRADER_HASH =
            "$2y$10$LIpkayi0QDWyVguPtEQbkOATM7PTnMKFDNq47K92TUx2LHTBFQf1i";

    private AppUserRepository users;
    private JwtTokenProvider jwt;
    private PasswordEncoder encoder;
    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        jwt = mock(JwtTokenProvider.class);
        encoder = new BCryptPasswordEncoder();
        AuthController controller = new AuthController(users, encoder, jwt);

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void seededTraderLoginReturnsBearerEnvelope() throws Exception {
        AppUser trader = user("trader@db.com", SEEDED_TRADER_HASH, "TRADER");
        when(users.findByEmail("trader@db.com")).thenReturn(Optional.of(trader));
        when(jwt.generate("trader@db.com", "TRADER")).thenReturn("signed-token");
        when(jwt.expirationSeconds()).thenReturn(3600L);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"trader@db.com","password":"trader123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.role").value("TRADER"));

        verify(jwt).generate("trader@db.com", "TRADER");
    }

    @Test
    void wrongPasswordAndUnknownUserReturnIdenticalUnauthorizedResponses() throws Exception {
        AppUser trader = user("trader@db.com", SEEDED_TRADER_HASH, "TRADER");
        when(users.findByEmail("trader@db.com")).thenReturn(Optional.of(trader));
        when(users.findByEmail("missing@db.com")).thenReturn(Optional.empty());

        MvcResult wrongPassword = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"trader@db.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MvcResult unknownUser = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@db.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(wrongPassword.getResponse().getContentAsString())
                .isEqualTo(unknownUser.getResponse().getContentAsString());
        verifyNoInteractions(jwt);
    }

    @Test
    void invalidLoginRequestReturnsBadRequestBeforeRepositoryLookup() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").isNotEmpty());

        verifyNoInteractions(users);
    }

    private AppUser user(String email, String passwordHash, String role) {
        AppUser user = mock(AppUser.class);
        when(user.getEmail()).thenReturn(email);
        when(user.getPasswordHash()).thenReturn(passwordHash);
        when(user.getRole()).thenReturn(role);
        when(user.getEnabled()).thenReturn(true);
        return user;
    }
}
