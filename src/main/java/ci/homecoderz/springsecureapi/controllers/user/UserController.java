package ci.homecoderz.springsecureapi.controllers.user;

import ci.homecoderz.springsecureapi.entities.dto.UserLoginDTO;
import ci.homecoderz.springsecureapi.entities.dto.UserRegistrationDTO;
import ci.homecoderz.springsecureapi.entities.dto.UserResponseDTO;
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
    public List<UserResponseDTO> findAllUsers() {
        return userService.findAll().stream().map(userMapper::toDTO).toList();
    }

    @GetMapping("/retrieve/{username}")
    public ResponseEntity<UserResponseDTO> retrieveUserByUsername(@PathVariable String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(userMapper.toDTO(user));
    }

    @GetMapping("/{user_id}")
    public ResponseEntity<UserResponseDTO> retrieveUserById(@PathVariable int user_id) {
        return userService.findById(user_id).map(userMapper::toDTO).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody UserRegistrationDTO userRegistrationDTO) {
        if (!isValidRegistrationRequest(userRegistrationDTO)) {
            return ResponseEntity.badRequest().build();
        }

        UserResponseDTO createdUser = userMapper.toDTO(userService.store(userRegistrationDTO));
        URI location = URI.create("/user/" + createdUser.getId());
        return ResponseEntity.created(location).body(createdUser);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserLoginDTO userLoginDTO) {
        if (!isValidLoginRequest(userLoginDTO)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(authService.authenticate(userLoginDTO));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(401).build();
        }
    }

    private boolean isValidRegistrationRequest(UserRegistrationDTO userRegistrationDTO) {
        return userRegistrationDTO != null
                && StringUtils.hasText(userRegistrationDTO.getUsername())
                && StringUtils.hasText(userRegistrationDTO.getPassword())
                && StringUtils.hasText(userRegistrationDTO.getEmail());
    }

    private boolean isValidLoginRequest(UserLoginDTO userLoginDTO) {
        return userLoginDTO != null
                && StringUtils.hasText(userLoginDTO.getUsername())
                && StringUtils.hasText(userLoginDTO.getPassword());
    }
}
