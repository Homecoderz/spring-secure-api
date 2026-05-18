package ci.homecoderz.springsecureapi.entities.dto;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Integer id;
    private String username;
    private String email;
    private String firstname;
    private String lastname;
}
