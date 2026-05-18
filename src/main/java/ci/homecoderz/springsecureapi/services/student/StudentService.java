package ci.homecoderz.springsecureapi.services.student;

import ci.homecoderz.springsecureapi.entities.student.Student;
import ci.homecoderz.springsecureapi.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public List<Student> retrieveAll() {
        return studentRepository.findAll();
    }

    public Student retrieveById(int id) {
        return studentRepository.findById(id).orElse(null);
    }

    public Student store(Student student) {
        return studentRepository.save(student);
    }
}
