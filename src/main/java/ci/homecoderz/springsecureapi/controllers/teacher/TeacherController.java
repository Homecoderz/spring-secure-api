package ci.homecoderz.springsecureapi.controllers.teacher;

import ci.homecoderz.springsecureapi.entities.teacher.Teacher;
import ci.homecoderz.springsecureapi.services.teacher.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping
    public List<Teacher> getTeachers() {
        return teacherService.retrieveAll();
    }

    @GetMapping("/{teacher_id}")
    public Optional<Teacher> getTeacher(@PathVariable int teacher_id) {
        return teacherService.retrieve(teacher_id);
    }

    @PostMapping
    public Teacher createTeacher(@RequestBody Teacher teacher) {
        return teacherService.store(teacher);
    }
}
