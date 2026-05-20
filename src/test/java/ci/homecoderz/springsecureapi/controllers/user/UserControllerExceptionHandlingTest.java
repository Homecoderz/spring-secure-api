package ci.homecoderz.springsecureapi.controllers.user;

import ci.homecoderz.springsecureapi.entities.dto.UserLoginDTO;
import ci.homecoderz.springsecureapi.entities.dto.UserRegistrationDTO;
import ci.homecoderz.springsecureapi.entities.dto.UserResponseDTO;
import ci.homecoderz.springsecureapi.entities.mapper.UserMapper;
import ci.homecoderz.springsecureapi.exceptionHandler.GlobalExceptionHandler;
import ci.homecoderz.springsecureapi.services.authentication.AuthService;
import ci.homecoderz.springsecureapi.services.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerExceptionHandlingTest {

    private MockMvc mockMvc;
    private UserService userService;
    private AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        authService = Mockito.mock(AuthService.class);
        UserMapper userMapper = Mockito.mock(UserMapper.class);

        UserController controller = new UserController(userService, authService, userMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void retrieveUserByUsernameShouldUseGlobalNotFoundHandler() throws Exception {
        when(userService.findByUsernameOrThrow("missing"))
                .thenThrow(new EntityNotFoundException("User not found: missing"));

        mockMvc.perform(get("/user/retrieve/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: missing"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void loginShouldUseGlobalAuthenticationHandler() throws Exception {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("john");
        dto.setPassword("wrongpass");

        when(authService.authenticate(any(UserLoginDTO.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentification échouée."))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void registerShouldUseBeanValidationHandler() throws Exception {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("");
        dto.setPassword("short");
        dto.setEmail("invalid");

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("username: must not be blank"));
    }
}
