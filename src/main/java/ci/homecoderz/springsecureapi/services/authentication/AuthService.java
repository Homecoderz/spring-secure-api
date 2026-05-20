package ci.homecoderz.springsecureapi.services.authentication;

import ci.homecoderz.springsecureapi.entities.dto.UserLoginDTO;
import ci.homecoderz.springsecureapi.entities.user.User;
import ci.homecoderz.springsecureapi.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;


    public String authenticate(UserLoginDTO userLoginDTO) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userLoginDTO.getUsername().trim(), userLoginDTO.getPassword().trim()));
        if(authentication.isAuthenticated()) {
            User authenticated_user = userRepository.findByUsername(userLoginDTO.getUsername().trim()).orElseThrow(() -> new UsernameNotFoundException("User not found."));
            return jwtService.generateToken(authenticated_user);
        }
        return "Aucun utilisateur trouvé";
    }


}
