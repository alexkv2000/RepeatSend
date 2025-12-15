package ru.kvo.Service;

import ru.kvo.Entity.Message;
import ru.kvo.Repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        log.info("Сброс статуса для {} сообщений: {}", messageIds.size(), messageIds);

        try {
            int updated = messageRepository.resetMessages(messageIds);
            log.info("Обновлено {} записей", updated);
            return updated;
        } catch (Exception e) {
            log.error("Ошибка при сбросе статуса: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обновить сообщения", e);
        }
    }

    public List<Message> getMessagesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return messageRepository.findByIds(ids);
    }

    /**
     * Извлекает содержимое тега Body из XML сообщения
     */
    public String extractBodyFromMessage(String xmlMessage) {
        if (xmlMessage == null || xmlMessage.isEmpty()) {
            return "";
        }

        try {
            // Паттерн для поиска тега Body (учитываем возможные атрибуты)
            Pattern pattern = Pattern.compile("<Body[^>]*>(.*?)</Body>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(xmlMessage);

            if (matcher.find()) {
                String bodyContent = matcher.group(1);
                log.debug("Найдено содержимое Body, длина: {}", bodyContent.length());

                // Декодируем HTML entities если нужно
                bodyContent = decodeHtmlEntities(bodyContent);

                return bodyContent;
            } else {
                log.debug("Тег Body не найден в сообщении");
                return "<div class='alert alert-warning'>Тег Body не найден в сообщении</div>";
            }
        } catch (Exception e) {
            log.error("Ошибка при извлечении Body из XML: {}", e.getMessage(), e);
            return "<div class='alert alert-danger'>Ошибка при обработке сообщения: " +
                    e.getMessage() + "</div>";
        }
    }

    /**
     * Декодирует HTML entities
     */
    private String decodeHtmlEntities(String text) {
        if (text == null) return "";

        // Простая замена основных entities
        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#xA;", "\n")
                .replace("&#xD;", "\r");
    }

    /**
     * Получает сообщение с извлеченным Body
     */
    public Map<String, Object> getMessageWithBody(Long id) {
        Map<String, Object> result = new HashMap<>();

        Message message = getMessageById(id);
        if (message != null) {
            result.put("message", message);
            result.put("bodyContent", extractBodyFromMessage(message.getMessage()));
        }

        return result;
    }
}