package ru.kvo.Service;

import ru.kvo.Entity.Message;
import ru.kvo.Repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;

    public List<Message> searchByEmail(String email) {
        log.info("Поиск сообщений по email: {}", email);

        if (email == null || email.trim().isEmpty()) {
            log.warn("Email для поиска не указан");
            return List.of();
        }

        String cleanEmail = email.trim();
        List<Message> results = messageRepository.findByEmailInXml(cleanEmail);
        log.info("Найдено сообщений: {}", results.size());

        return results;
    }

    public Message getMessageById(Long id) {
        if (id == null) {
            return null;
        }

        Optional<Message> message = messageRepository.findById(id);
        return message.orElse(null);
    }

    public int resendMessages(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return 0;
        }

        log.info("Сброс статуса для {} сообщений", messageIds.size());
        return messageRepository.resetMessages(messageIds);
    }

    public List<Message> getMessagesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return messageRepository.findByIds(ids);
    }
}