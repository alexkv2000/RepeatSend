package ru.kvo.Repository;

import ru.kvo.Entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Поиск только по email
    @Query(value = "SELECT * FROM `sql-kafka`.messages m " +
            "WHERE m.message LIKE %:email%",
            nativeQuery = true)
    List<Message> findByEmail(@Param("email") String email);

    // Поиск только по дате
    @Query(value = "SELECT * FROM `sql-kafka`.messages m " +
            "WHERE DATE(m.date_create) = :date",
            nativeQuery = true)
    List<Message> findByDate(@Param("date") LocalDate date);

    // Поиск по email и дате
    @Query(value = "SELECT * FROM `sql-kafka`.messages m " +
            "WHERE m.message LIKE %:email% " +
            "AND DATE(m.date_create) = :date",
            nativeQuery = true)
    List<Message> findByEmailAndDate(@Param("email") String email,
                                     @Param("date") LocalDate date);

    // Универсальный поиск с динамическими условиями
    @Query(value = "SELECT * FROM `sql-kafka`.messages m " +
            "WHERE (:email IS NULL OR m.message LIKE %:email%) " +
            "AND (:date IS NULL OR DATE(m.date_create) = :date)",
            nativeQuery = true)
    List<Message> search(@Param("email") String email,
                         @Param("date") LocalDate date);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.status = null, m.dateEnd = null, m.server = '', m.numAttempt = 4 " +
            "WHERE m.id IN :ids")
    int resetMessages(@Param("ids") List<Long> ids);

    @Query("SELECT m FROM Message m WHERE m.id IN :ids")
    List<Message> findByIds(@Param("ids") List<Long> ids);
}