package ci.homecoderz.springsecureapi.services.authentication;

import ci.homecoderz.springsecureapi.entities.dto.UserLoginDTO;
import ci.homecoderz.springsecureapi.entities.user.User;
import ci.homecoderz.springsecureapi.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;


    public String authenticate(UserLoginDTO userLoginDTO) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userLoginDTO.getUsername().trim(), userLoginDTO.getPassword().trim()));
        if(authentication.isAuthenticated()) {
            Optional<User> authenticated_user = userRepository.findByUsername(userLoginDTO.getUsername().trim());
            return jwtService.generateToken(authenticated_user);
        }
        return "Aucun utilisateur trouvé";
    }


}
