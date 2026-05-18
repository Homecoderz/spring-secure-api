package ci.homecoderz.springsecureapi.entities.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDTO {
    private Integer id;
    private String username;
    private String password;
    private String email;
    private String firstname;
    private String lastname;
    private String role;
    private boolean enabled;
}
