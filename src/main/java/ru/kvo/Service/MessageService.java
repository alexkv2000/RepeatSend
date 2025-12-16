package ru.kvo.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import ru.kvo.Dto.MessageUpdateRequest;
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

    public int resendMessages(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return 0;

        log.info("Сброс статуса для {} сообщений", messageIds.size());
        return messageRepository.resetMessages(messageIds);
    }

    @Transactional
    public Map<String, Object> updateMessageRecipients(Long id, MessageUpdateRequest updateRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Message> messageOpt = messageRepository.findById(id);

            if (messageOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Сообщение не найдено");
                return response;
            }

            Message message = messageOpt.get();
            String messageJson = message.getMessage();

            // Парсим JSON
            JsonNode rootNode = objectMapper.readTree(messageJson);

            if (rootNode.isObject()) {
                ObjectNode objectNode = (ObjectNode) rootNode;

                // Обновляем поля
                if (updateRequest.getTo() != null) {
                    objectNode.put("To", updateRequest.getTo());
                }

                if (updateRequest.getToCC() != null) {
                    objectNode.put("ToCC", updateRequest.getToCC());
                }

                if (updateRequest.getBcc() != null) {
                    objectNode.put("BCC", updateRequest.getBcc());
                }

                // Сохраняем обновленный JSON
                String updatedJson = objectMapper.writeValueAsString(objectNode);
                message.setMessage(updatedJson);

                messageRepository.save(message);

                response.put("success", true);
                response.put("message", "Данные успешно обновлены");
                response.put("updatedFields", Map.of(
                        "To", updateRequest.getTo(),
                        "ToCC", updateRequest.getToCC(),
                        "BCC", updateRequest.getBcc()
                ));
            } else {
                response.put("success", false);
                response.put("error", "Неверный формат JSON в поле message");
            }

        } catch (Exception e) {
            log.error("Ошибка при обновлении сообщения", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    // Метод для извлечения полей из JSON
    public Map<String, String> extractFieldsFromMessage(String messageJson) {
        Map<String, String> fields = new HashMap<>();

        try {
            JsonNode rootNode = objectMapper.readTree(messageJson);

            if (rootNode.has("To")) {
                fields.put("To", rootNode.get("To").asText());
            }

            if (rootNode.has("ToCC")) {
                fields.put("ToCC", rootNode.get("ToCC").asText());
            }

            if (rootNode.has("BCC")) {
                fields.put("BCC", rootNode.get("BCC").asText());
            }

            if (rootNode.has("Caption")) {
                fields.put("Caption", rootNode.get("Caption").asText());
            }

        } catch (Exception e) {
            log.warn("Не удалось распарсить JSON: {}", e.getMessage());
        }

        return fields;
    }
    /**
     * Получение полной информации о сообщении для API
     */
    public Map<String, Object> getMessageFullInfo(Long id) {
        Map<String, Object> response = new HashMap<>();

        Optional<Message> messageOpt = messageRepository.findById(id);

        if (messageOpt.isPresent()) {
            Message message = messageOpt.get();

            // Извлекаем поля из JSON
            Map<String, String> fields = extractFieldsFromMessage(message.getMessage());

            response.put("message", message);
            response.put("fields", fields);

            // Также возвращаем тело контента, если есть
            try {
                JsonNode rootNode = objectMapper.readTree(message.getMessage());
                if (rootNode.has("bodyContent")) {
                    response.put("bodyContent", rootNode.get("bodyContent").asText());
                } else {
                    response.put("bodyContent", message.getMessage());
                }
            } catch (Exception e) {
                response.put("bodyContent", message.getMessage());
            }
        }

        return response;
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