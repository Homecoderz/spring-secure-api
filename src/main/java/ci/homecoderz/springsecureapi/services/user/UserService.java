package ci.homecoderz.springsecureapi.services.user;
import ci.homecoderz.springsecureapi.entities.dto.UserDTO;
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

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User store(UserDTO userDTO) {
        userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        return userRepository.save(userMapper.toEntity(userDTO));
    }


}
