package ru.kvo.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", schema = "sql-kafka")
@Data
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kafka_topic", nullable = false)
    private String kafkaTopic;

    @Column(name = "message", nullable = false, length = 10000)
    private String message;

    @Column(name = "date_create", nullable = false)
    private LocalDateTime dateCreate;

    @Column(name = "status")
    private String status;

    @Column(name = "date_end")
    private LocalDateTime dateEnd;

    @Column(name = "server")
    private String server;

    @Column(name = "num_attempt")
    private Integer numAttempt;

    @Column(name = "typemes")
    private String typeMes;
}
