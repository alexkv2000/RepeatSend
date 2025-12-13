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

    @Query(value = "SELECT * FROM `sql-kafka`.messages m WHERE m.message LIKE %:emailPattern1% OR m.message LIKE %:emailPattern2% OR m.message LIKE %:emailPattern3%",
            nativeQuery = true)
    List<Message> findByEmailInXml(
            @Param("emailPattern1") String emailPattern1,
            @Param("emailPattern2") String emailPattern2,
            @Param("emailPattern3") String emailPattern3
    );

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.status = null, m.dateEnd = null, m.server = '' " +
            "WHERE m.id IN :ids")
    int resetMessages(@Param("ids") List<Long> ids);

    @Query("SELECT m FROM Message m WHERE m.id IN :ids")
    List<Message> findByIds(@Param("ids") List<Long> ids);
}
