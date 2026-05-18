package ci.homecoderz.springsecureapi.entities.mapper;

import ci.homecoderz.springsecureapi.entities.dto.UserDTO;
import ci.homecoderz.springsecureapi.entities.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(UserDTO userDTO);

    UserDTO toDTO(User user);
}
