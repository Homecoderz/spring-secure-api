package ci.homecoderz.springsecureapi.repositories;

import ci.homecoderz.springsecureapi.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    public User findByUsername(String username);
}
