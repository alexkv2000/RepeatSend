package ru.kvo.Service;

import ru.kvo.Entity.Message;
import ru.kvo.Repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    public List<Message> searchByEmail(String email) {
        // Формируем паттерны для поиска email в XML тегах
        String pattern1 = "<To>" + email + "</To>";
        String pattern2 = "<CC>" + email + "</CC>";
        String pattern3 = "<BCC>" + email + "</BCC>";

        return messageRepository.findByEmailInXml(pattern1, pattern2, pattern3);
    }

    public int resendMessages(List<Long> messageIds) {
        // Обновляем поля для повторной отправки
        return messageRepository.resetMessages(messageIds);
    }

    public List<Message> getMessagesByIds(List<Long> ids) {
        return messageRepository.findByIds(ids);
    }
}