package ru.kvo.Service;

import ru.kvo.Entity.Message;
import ru.kvo.Repository.MessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * Извлекает содержимое поля Body из JSON сообщения
     */
    public String extractBodyFromJsonMessage(String jsonMessage) {
        if (jsonMessage == null || jsonMessage.isEmpty()) {
            return "";
        }

        try {
            log.debug("Обработка JSON сообщения, длина: {}", jsonMessage.length());

            // Проверяем, является ли строка JSON
            String trimmedMessage = jsonMessage.trim();

            // Если сообщение начинается с {, пробуем парсить как JSON
            if (trimmedMessage.startsWith("{")) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(trimmedMessage);

                    // Ищем поле Body (регистронезависимо)
                    JsonNode bodyNode = findBodyField(jsonNode);

                    if (bodyNode != null && !bodyNode.isNull()) {
                        String bodyContent = bodyNode.asText();
                        log.debug("Найдено содержимое Body, длина: {}", bodyContent.length());
                        return bodyContent;
                    } else {
                        log.debug("Поле Body не найдено в JSON");
                    }
                } catch (Exception jsonException) {
                    log.debug("Не удалось распарсить как JSON, пробуем другие методы");
                }
            }

            // Если не JSON или Body не найден, пробуем найти паттерн "Body":"..."
            return extractBodyWithPatterns(jsonMessage);

        } catch (Exception e) {
            log.error("Ошибка при извлечении Body из сообщения: {}", e.getMessage(), e);
            return "<div class='alert alert-danger'>Ошибка при обработке сообщения: " +
                    e.getMessage() + "</div>";
        }
    }

    /**
     * Ищет поле Body в JSON (регистронезависимо)
     */
    private JsonNode findBodyField(JsonNode node) {
        // Проверяем основные варианты написания
        String[] possibleNames = {"Body", "body", "BODY", "HtmlBody", "htmlBody", "HTML"};

        for (String fieldName : possibleNames) {
            if (node.has(fieldName)) {
                return node.get(fieldName);
            }
        }

        // Если не нашли, ищем среди всех полей
        return node.findValue("Body");
    }
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
     * Извлекает Body с помощью регулярных выражений для разных форматов
     */
    private String extractBodyWithPatterns(String message) {
        // Паттерны для поиска Body в разных форматах
        String[] patterns = {
                // JSON формат: "Body":"content"
                "\"Body\"\\s*:\\s*\"(.*?)\"(?=[,}\\s])",
                "\"body\"\\s*:\\s*\"(.*?)\"(?=[,}\\s])",
                "'Body'\\s*:\\s*'(.*?)'(?=[,}\\s])",

                // XML-like формат: <Body>content</Body>
                "<Body[^>]*>(.*?)</Body>",
                "<body[^>]*>(.*?)</body>",

                // Без кавычек: Body:content
                "\"Body\"\\s*:\\s*(.*?)(?=[,}\\s])"
        };

        for (String pattern : patterns) {
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern,
                        java.util.regex.Pattern.DOTALL);
                java.util.regex.Matcher m = p.matcher(message);

                if (m.find()) {
                    String content = m.group(1);

                    // Убираем экранирование
                    content = unescapeJsonString(content);

                    log.debug("Найдено Body с паттерном {}, длина: {}", pattern, content.length());
                    return content;
                }
            } catch (Exception e) {
                log.debug("Ошибка при применении паттерна {}: {}", pattern, e.getMessage());
            }
        }

        log.warn("Body не найден ни одним из паттернов");
        return "<div class='alert alert-warning'>Тело сообщения не найдено в формате JSON/XML</div>";
    }

    /**
     * Убирает экранирование из JSON строки
     */
    private String unescapeJsonString(String str) {
        if (str == null) return "";

        return str.replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\/", "/");
    }

    /**
     * Получает полную информацию о сообщении
     */
    public Map<String, Object> getMessageWithBody(Long id) {
        Map<String, Object> result = new HashMap<>();

        Message message = getMessageById(id);
        if (message != null) {
            result.put("message", message);
            result.put("bodyContent", extractBodyFromJsonMessage(message.getMessage()));
            result.put("isJson", message.getMessage().trim().startsWith("{"));
        }

        return result;
    }
}