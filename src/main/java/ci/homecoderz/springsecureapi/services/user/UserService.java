package ci.homecoderz.springsecureapi.services.user;
import ci.homecoderz.springsecureapi.entities.dto.UserRegistrationDTO;
import ci.homecoderz.springsecureapi.entities.mapper.UserMapper;
import ci.homecoderz.springsecureapi.entities.user.RoleAuthority;
import ci.homecoderz.springsecureapi.entities.user.User;
import jakarta.persistence.EntityNotFoundException;
import ci.homecoderz.springsecureapi.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public Optional<User> findById(Integer teacher_id) {
        return userRepository.findById(teacher_id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User findByIdOrThrow(Integer userId) {
        return findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    public User findByUsernameOrThrow(String username) {
        return findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User store(UserRegistrationDTO userRegistrationDTO) {

        User user = userMapper.toEntity(userRegistrationDTO);
        user.setPassword(passwordEncoder.encode(userRegistrationDTO.getPassword()));
        user.setRole(RoleAuthority.USER);
        user.setEnabled(true);

        return userRepository.save(user);
    }


}
