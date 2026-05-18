package ci.homecoderz.springsecureapi.services.student;

import ci.homecoderz.springsecureapi.entities.dto.StudentDTO;
import ci.homecoderz.springsecureapi.entities.mapper.StudentMapper;
import ci.homecoderz.springsecureapi.entities.student.Student;
import ci.homecoderz.springsecureapi.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    public List<Student> retrieveAll() {
        return studentRepository.findAll();
    }

    public ResponseEntity <StudentDTO> retrieveById(int id) {
        return studentRepository.findById(id).map(studentMapper::toDTO).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    public Student store(Student student) {
        return studentRepository.save(student);
    }
}
