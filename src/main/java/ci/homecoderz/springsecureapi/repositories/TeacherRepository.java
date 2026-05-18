package ci.homecoderz.springsecureapi.repositories;

import ci.homecoderz.springsecureapi.entities.teacher.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Integer> {

    public List<Teacher> findByDiscipline(String discipline);
    public List<Teacher> findByGrade(String grade);
    public List<Teacher> findByLastname(String lastname);
    public List<Teacher> findByFirstname(String firstname);
    public List<Teacher> findByFirstnameAndLastname(String firstname, String lastname);
    public Optional<Teacher> findById(int id);
    public Optional<Teacher> findByEmail(String email);
}
