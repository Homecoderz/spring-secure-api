package ci.homecoderz.springsecureapi.entities.mapper;

import ci.homecoderz.springsecureapi.entities.dto.TeacherDTO;
import ci.homecoderz.springsecureapi.entities.teacher.Teacher;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TeacherMapper {

    Teacher toEntity(TeacherDTO teacherDTO);

    TeacherDTO toDTO(Teacher teacher);
}
