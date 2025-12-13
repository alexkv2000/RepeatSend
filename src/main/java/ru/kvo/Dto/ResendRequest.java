package ru.kvo.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ResendRequest {
    private List<Long> messageIds;
}
