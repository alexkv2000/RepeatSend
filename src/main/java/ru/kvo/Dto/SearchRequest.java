package ru.kvo.Dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SearchRequest {
    private String email;
    private LocalDate dateCreate; // Для поиска по дате
}
