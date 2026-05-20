package ci.homecoderz.springsecureapi.controllers.student;

import ci.homecoderz.springsecureapi.entities.dto.StudentDTO;
import ci.homecoderz.springsecureapi.entities.mapper.StudentMapper;
import ci.homecoderz.springsecureapi.entities.student.Student;
import ci.homecoderz.springsecureapi.services.student.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;
    private final StudentMapper studentMapper;

    @GetMapping
    public List<StudentDTO> getStudents() {
        return studentService.retrieveAll().stream().map(studentMapper::toDTO).toList();
    }

    @GetMapping("/{student_id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable int student_id) {
        return studentService.retrieveById(student_id);
    }

    @PostMapping("/add")
    public StudentDTO createStudent(@RequestBody StudentDTO studentDTO) {
        return studentMapper.toDTO(studentService.store(studentMapper.toEntity(studentDTO)));
    }

    @PostMapping("/populate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> populateStudentDB() {
        if (studentService.existsByMatricule("12345")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        List<StudentDTO> students = new ArrayList<>();
        students.add(studentMapper.toDTO(Student.builder().matricule("12345").firstname("John").lastname("Doe").age(23).is_present(true).build()));
        students.add(studentMapper.toDTO(Student.builder().matricule("786786").firstname("Alicia").lastname("Keys").age(34).is_present(true).build()));
        students.add(studentMapper.toDTO(Student.builder().matricule("0989084").firstname("Elvis").lastname("Presley").age(64).is_present(false).build()));
        students.add(studentMapper.toDTO(Student.builder().matricule("453098").firstname("Micheal").lastname("Jackson").age(42).is_present(true).build()));
        students.add(studentMapper.toDTO(Student.builder().matricule("923985").firstname("Erwing").lastname("Petr").age(67).is_present(false).build()));
        students.forEach(student -> studentService.store(studentMapper.toEntity(student)));

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
