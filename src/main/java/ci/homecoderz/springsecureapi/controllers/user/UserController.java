package ci.homecoderz.springsecureapi.controllers.user;

import ci.homecoderz.springsecureapi.entities.dto.CredentialDTO;
import ci.homecoderz.springsecureapi.entities.dto.UserDTO;
import ci.homecoderz.springsecureapi.entities.mapper.UserMapper;
import ci.homecoderz.springsecureapi.entities.user.User;
import ci.homecoderz.springsecureapi.services.authentication.AuthService;
import ci.homecoderz.springsecureapi.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final UserMapper userMapper;


    @GetMapping
    public List<UserDTO> findAllUsers() {
        return userService.findAll().stream().map(userMapper::toDTO).toList();
    }

    @GetMapping("/retrieve/{username}")
    public ResponseEntity<UserDTO> retrieveUserByUsername(@PathVariable String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(userMapper.toDTO(user));
    }

    @GetMapping("/{user_id}")
    public ResponseEntity<UserDTO> retrieveUserById(@PathVariable int user_id) {
        return userService.findById(user_id)
                .map(user -> ResponseEntity.ok(userMapper.toDTO(user))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody UserDTO userDTO) {
        if (!isValidRegistrationRequest(userDTO)) {
            return ResponseEntity.badRequest().build();
        }

        UserDTO createdUser = userMapper.toDTO(userService.store(userDTO));
        URI location = URI.create("/user/" + createdUser.getId());
        return ResponseEntity.created(location).body(createdUser);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody CredentialDTO credentialDTO) {
        if (!isValidLoginRequest(credentialDTO)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(authService.authenticate(credentialDTO));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(401).build();
        }
    }

    private boolean isValidRegistrationRequest(UserDTO userDTO) {
        return userDTO != null
                && StringUtils.hasText(userDTO.getUsername())
                && StringUtils.hasText(userDTO.getPassword())
                && StringUtils.hasText(userDTO.getEmail());
    }

    private boolean isValidLoginRequest(CredentialDTO credentialDTO) {
        return credentialDTO != null
                && StringUtils.hasText(credentialDTO.getPrincipal())
                && StringUtils.hasText(credentialDTO.getPassword());
    }
}
