package ci.homecoderz.springsecureapi.entities.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CredentialDTO {
    private String principal;
    private String password;
}
