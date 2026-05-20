package ci.homecoderz.springsecureapi.controllers.user;

import ci.homecoderz.springsecureapi.entities.dto.UserLoginDTO;
import ci.homecoderz.springsecureapi.entities.dto.UserRegistrationDTO;
import ci.homecoderz.springsecureapi.entities.dto.UserResponseDTO;
import ci.homecoderz.springsecureapi.entities.mapper.UserMapper;
import ci.homecoderz.springsecureapi.entities.user.User;
import jakarta.validation.Valid;
import ci.homecoderz.springsecureapi.services.authentication.AuthService;
import ci.homecoderz.springsecureapi.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
        User user = userService.findByUsernameOrThrow(username);
        return ResponseEntity.ok(userMapper.toDTO(user));
    }

    @GetMapping("/{user_id}")
    public ResponseEntity<UserResponseDTO> retrieveUserById(@PathVariable int user_id) {
        User user = userService.findByIdOrThrow(user_id);
        return ResponseEntity.ok(userMapper.toDTO(user));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRegistrationDTO userRegistrationDTO) {
        UserResponseDTO createdUser = userMapper.toDTO(userService.store(userRegistrationDTO));
        URI location = URI.create("/user/" + createdUser.getId());
        return ResponseEntity.created(location).body(createdUser);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        return ResponseEntity.ok(authService.authenticate(userLoginDTO));
    }
}
