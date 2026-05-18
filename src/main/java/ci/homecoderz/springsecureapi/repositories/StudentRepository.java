package ci.homecoderz.springsecureapi.repositories;

import ci.homecoderz.springsecureapi.entities.student.Student;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {
    public Optional<Student> findByMatricule(String matricule);
    public List<Student> findByLastname(String lastname);
    public @NonNull List<Student> findAll();
}
