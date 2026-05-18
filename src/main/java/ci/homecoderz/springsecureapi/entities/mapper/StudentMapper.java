package ci.homecoderz.springsecureapi.entities.mapper;

import ci.homecoderz.springsecureapi.entities.dto.StudentDTO;
import ci.homecoderz.springsecureapi.entities.student.Student;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    Student toEntity(StudentDTO studentDTO);

    StudentDTO toDTO(Student student);
}
