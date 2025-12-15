package ru.kvo.Service;

import ru.kvo.Dto.SearchRequest;
import ru.kvo.Entity.Message;
import ru.kvo.Repository.MessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Message> searchMessages(SearchRequest request) {
        String email = request.getEmail();
        LocalDate dateCreate = request.getDateCreate();

        log.info("Поиск сообщений: email={}, date={}", email, dateCreate);

        // Валидация
        if ((email == null || email.trim().isEmpty()) && dateCreate == null) {
            log.warn("Ни одно поле поиска не заполнено");
            return Collections.emptyList();
        }

        // Очищаем email
        String cleanEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : null;

        // Выполняем поиск
        if (cleanEmail != null && dateCreate != null) {
            return messageRepository.findByEmailAndDate(cleanEmail, dateCreate);
        } else if (cleanEmail != null) {
            return messageRepository.findByEmail(cleanEmail);
        } else {
            return messageRepository.findByDate(dateCreate);
        }
    }

    public Message getMessageById(Long id) {
        if (id == null) return null;
        return messageRepository.findById(id).orElse(null);
    }

    public int resendMessages(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return 0;

        log.info("Сброс статуса для {} сообщений", messageIds.size());
        return messageRepository.resetMessages(messageIds);
    }

    public List<Message> getMessagesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return messageRepository.findByIds(ids);
    }

    /**
     * Универсальный метод извлечения Body из сообщения
     */
    public String extractBodyFromMessage(String messageContent) {
        if (messageContent == null || messageContent.isEmpty()) {
            return "";
        }

        log.debug("Извлечение Body из сообщения, длина: {}", messageContent.length());

        try {
            // 1. Пробуем извлечь из JSON (если начинается с {)
            String trimmed = messageContent.trim();
            if (trimmed.startsWith("{")) {
                String jsonBody = extractBodyFromJson(trimmed);
                if (!jsonBody.isEmpty() && !jsonBody.contains("alert-warning")) {
                    return jsonBody;
                }
            }

            // 2. Пробуем найти JSON поле "Body" даже если не начинается с {
            String jsonPatternBody = extractBodyWithJsonPattern(messageContent);
            if (!jsonPatternBody.isEmpty() && !jsonPatternBody.contains("alert-warning")) {
                return jsonPatternBody;
            }

            // 3. Пробуем найти XML тег <Body>
            String xmlBody = extractBodyFromXml(messageContent);
            if (!xmlBody.isEmpty() && !xmlBody.contains("alert-warning")) {
                return xmlBody;
            }

            // 4. Если ничего не нашли, показываем все сообщение
            log.warn("Body не найден ни в JSON, ни в XML");
            return "<div class='alert alert-warning'>Не удалось извлечь Body. Показано полное сообщение:</div>" +
                    "<pre>" + escapeHtml(messageContent.substring(0, Math.min(1000, messageContent.length()))) + "</pre>";

        } catch (Exception e) {
            log.error("Ошибка при извлечении Body: {}", e.getMessage(), e);
            return "<div class='alert alert-danger'>Ошибка обработки: " + e.getMessage() + "</div>";
        }
    }

    /**
     * Извлечение Body из JSON через парсинг
     */
    private String extractBodyFromJson(String jsonContent) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonContent);

            // Ищем поле Body в разных вариантах написания
            String[] possibleNames = {"Body", "body", "BODY", "HtmlBody", "htmlBody", "HTML"};

            for (String fieldName : possibleNames) {
                if (jsonNode.has(fieldName)) {
                    String body = jsonNode.get(fieldName).asText();
                    log.debug("Найдено поле '{}' в JSON, длина: {}", fieldName, body.length());
                    return unescapeJsonString(body);
                }
            }

            // Пробуем найти любое поле содержащее HTML
            for (JsonNode node : jsonNode) {
                if (node.isTextual()) {
                    String value = node.asText();
                    if (value.contains("<html") || value.contains("<HTML")) {
                        log.debug("Найдено HTML в поле JSON");
                        return unescapeJsonString(value);
                    }
                }
            }

            return "";

        } catch (Exception e) {
            log.debug("Не удалось распарсить как JSON: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Извлечение Body с помощью регулярных выражений для JSON
     */
    private String extractBodyWithJsonPattern(String text) {
        // Паттерны для поиска JSON поля Body
        java.util.regex.Pattern[] patterns = {
                // "Body":"content"
                java.util.regex.Pattern.compile("\"Body\"\\s*:\\s*\"(.*?)(?<!\\\\)\"", java.util.regex.Pattern.DOTALL),
                // "body":"content"
                java.util.regex.Pattern.compile("\"body\"\\s*:\\s*\"(.*?)(?<!\\\\)\"", java.util.regex.Pattern.DOTALL),
                // 'Body':'content'
                java.util.regex.Pattern.compile("'Body'\\s*:\\s*'(.*?)(?<!\\\\)'", java.util.regex.Pattern.DOTALL),
                // Без пробелов: "Body":"content"
                java.util.regex.Pattern.compile("\"Body\":\"(.*?)(?<!\\\\)\"", java.util.regex.Pattern.DOTALL)
        };

        for (java.util.regex.Pattern pattern : patterns) {
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    String content = matcher.group(1);
                    log.debug("Найдено Body паттерном {}, длина: {}", pattern.pattern(), content.length());
                    return unescapeJsonString(content);
                } catch (Exception e) {
                    log.debug("Ошибка обработки паттерна {}: {}", pattern.pattern(), e.getMessage());
                }
            }
        }

        return "";
    }

    /**
     * Извлечение Body из XML
     */
    private String extractBodyFromXml(String xmlContent) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<Body[^>]*>(.*?)</Body>", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher matcher = pattern.matcher(xmlContent);

            if (matcher.find()) {
                String bodyContent = matcher.group(1);
                log.debug("Найдено XML тег Body, длина: {}", bodyContent.length());
                return decodeHtmlEntities(bodyContent);
            }

            // Пробуем вариант в нижнем регистре
            pattern = java.util.regex.Pattern.compile("<body[^>]*>(.*?)</body>", java.util.regex.Pattern.DOTALL);
            matcher = pattern.matcher(xmlContent);

            if (matcher.find()) {
                String bodyContent = matcher.group(1);
                log.debug("Найдено XML тег body (нижний регистр), длина: {}", bodyContent.length());
                return decodeHtmlEntities(bodyContent);
            }

            return "";

        } catch (Exception e) {
            log.error("Ошибка при извлечении Body из XML: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Декодирование HTML entities
     */
    private String decodeHtmlEntities(String text) {
        if (text == null) return "";

        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#xA;", "\n")
                .replace("&#xD;", "\r")
                .replace("&#39;", "'")
                .replace("&#34;", "\"");
    }

    /**
     * Удаление экранирования из JSON строки
     */
    private String unescapeJsonString(String str) {
        if (str == null) return "";

        String result = str;

        // Заменяем экранированные последовательности
        result = result.replace("\\\\", "\\");
        result = result.replace("\\\"", "\"");
        result = result.replace("\\'", "'");
        result = result.replace("\\n", "\n");
        result = result.replace("\\r", "\r");
        result = result.replace("\\t", "\t");
        result = result.replace("\\b", "\b");
        result = result.replace("\\f", "\f");
        result = result.replace("\\/", "/");

        // Заменяем Unicode escape с помощью Pattern и Matcher
        Pattern unicodePattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = unicodePattern.matcher(result);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            char unicodeChar = (char) Integer.parseInt(hex, 16);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(unicodeChar)));
        }
        matcher.appendTail(sb);
        result = sb.toString();

        // Декодируем HTML entities после экранирования
        result = decodeHtmlEntities(result);

        return result;
    }

    /**
     * Экранирование HTML для безопасного отображения
     */
    private String escapeHtml(String text) {
        if (text == null) return "";

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Получение полной информации о сообщении для API
     */
    public Map<String, Object> getMessageFullInfo(Long id) {
        Map<String, Object> result = new HashMap<>();

        Message message = getMessageById(id);
        if (message != null) {
            result.put("message", message);

            // Извлекаем Body
            String bodyContent = extractBodyFromMessage(message.getMessage());
            result.put("bodyContent", bodyContent);

            // Определяем формат
            boolean isJson = message.getMessage() != null &&
                    message.getMessage().trim().startsWith("{");
            result.put("isJson", isJson);

            // Если JSON, извлекаем дополнительные поля
            if (isJson) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(message.getMessage().trim());

                    Map<String, String> fields = new HashMap<>();
                    fields.put("To", jsonNode.has("To") ? jsonNode.get("To").asText() : "");
                    fields.put("ToCC", jsonNode.has("ToCC") ? jsonNode.get("ToCC").asText() : "");
                    fields.put("Caption", jsonNode.has("Caption") ? jsonNode.get("Caption").asText() : "");
                    fields.put("typeMes", jsonNode.has("typeMes") ? jsonNode.get("typeMes").asText() : "");
                    fields.put("uuid", jsonNode.has("uuid") ? jsonNode.get("uuid").asText() : "");

                    result.put("fields", fields);
                } catch (Exception e) {
                    log.debug("Не удалось извлечь поля из JSON: {}", e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * Получает список уникальных дат для автозаполнения
     */
    public List<LocalDate> getUniqueDates() {
        List<Message> allMessages = messageRepository.findAll();
        Set<LocalDate> uniqueDates = new TreeSet<>();

        for (Message message : allMessages) {
            if (message.getDateCreate() != null) {
                uniqueDates.add(message.getDateCreate().toLocalDate());
            }
        }

        return new ArrayList<>(uniqueDates);
    }
}