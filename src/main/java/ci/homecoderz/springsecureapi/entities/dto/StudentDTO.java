package ci.homecoderz.springsecureapi.entities.dto;

import lombok.Data;

@Data
public class StudentDTO {
    private Integer id;
    private String firstname;
    private String lastname;
    private String matricule;
    private int age;
    private boolean is_present;
}
