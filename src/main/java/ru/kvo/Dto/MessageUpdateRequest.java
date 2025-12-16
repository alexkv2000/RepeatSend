package ru.kvo.Dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class MessageUpdateRequest {
    private String to;
    private String toCC;
    private String bcc;

}