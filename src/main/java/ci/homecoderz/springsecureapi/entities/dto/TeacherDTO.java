package ci.homecoderz.springsecureapi.entities.dto;

import lombok.Data;

@Data
public class TeacherDTO {

    private Integer id;
    private String firstname;
    private String lastname;
    private String email;
    private String discipline;
    private String grade;
}
