package ci.homecoderz.springsecureapi.entities.mapper;

import ci.homecoderz.springsecureapi.entities.dto.UserRegistrationDTO;
import ci.homecoderz.springsecureapi.entities.dto.UserResponseDTO;
import ci.homecoderz.springsecureapi.entities.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(UserRegistrationDTO userRegistrationDTO);
    UserResponseDTO toDTO(User user);
}
