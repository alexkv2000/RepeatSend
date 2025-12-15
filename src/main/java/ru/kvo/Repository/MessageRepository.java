package ru.kvo.Repository;

import ru.kvo.Entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query(value = "SELECT * FROM `sql-kafka`.messages m " +
            "WHERE m.message LIKE %:email%",
            nativeQuery = true)
    List<Message> findByEmailInXml(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.status = null, m.dateEnd = null, m.server = '' " +
            "WHERE m.id IN :ids")
    int resetMessages(@Param("ids") List<Long> ids);

    @Query("SELECT m FROM Message m WHERE m.id IN :ids")
    List<Message> findByIds(@Param("ids") List<Long> ids);
}
