package ru.kvo.Dto;

import lombok.Data;

@Data
public class MessageResponse {
    private Long id;
    private String kafkaTopic;
    private String message;
    private String dateCreate;
    private String status;
    private String dateEnd;
    private String server;
    private Integer numAttempt;
    private String typeMes;
}
