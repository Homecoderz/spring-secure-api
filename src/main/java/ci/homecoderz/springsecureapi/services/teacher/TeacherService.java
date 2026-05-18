package ci.homecoderz.springsecureapi.services.teacher;

import ci.homecoderz.springsecureapi.entities.teacher.Teacher;
import ci.homecoderz.springsecureapi.repositories.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;


    public List<Teacher> retrieveAll() {
        return teacherRepository.findAll();
    }

    public Optional<Teacher> retrieve(int teacherId) {
        return teacherRepository.findById(teacherId);
    }

    public Teacher store(Teacher teacher) {
        return teacherRepository.save(teacher);
    }
}
