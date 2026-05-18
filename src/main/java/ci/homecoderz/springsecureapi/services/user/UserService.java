package ci.homecoderz.springsecureapi.services.user;
import ci.homecoderz.springsecureapi.entities.dto.UserRegistrationDTO;
import ci.homecoderz.springsecureapi.entities.dto.UserResponseDTO;
import ci.homecoderz.springsecureapi.entities.mapper.UserMapper;
import ci.homecoderz.springsecureapi.entities.user.User;
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

    public Optional <User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User store(UserRegistrationDTO userRegistrationDTO) {

        User user = userMapper.toEntity(userRegistrationDTO);
        user.setPassword(passwordEncoder.encode(userRegistrationDTO.getPassword()));
        user.setRole("USER");
        user.setEnabled(true);

        return userRepository.save(user);
    }


}
