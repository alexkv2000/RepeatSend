package ru.kvo.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kvo.Entity.Message;
import ru.kvo.Service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/")
    public String showSearchPage(Model model) {
        model.addAttribute("searchEmail", "");
        return "index";
    }

    @PostMapping("/search")
    public String searchMessages(@RequestParam("email") String email, Model model) {
        List<Message> messages = messageService.searchByEmail(email);
        model.addAttribute("messages", messages);
        model.addAttribute("searchEmail", email);
        return "index";
    }

    @GetMapping("/api/message/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMessage(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Message message = messageService.getMessageById(id);
            if (message != null) {
                response.put("success", true);
                response.put("message", message);

                // Извлекаем Body
                String bodyContent = messageService.extractBodyFromMessage(message.getMessage());
                response.put("bodyContent", bodyContent);
            } else {
                response.put("success", false);
                response.put("error", "Сообщение не найдено");
            }
        } catch (Exception e) {
            log.error("Ошибка получения сообщения", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/message/{id}/full")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFullMessage(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Message message = messageService.getMessageById(id);
            if (message != null) {
                response.put("success", true);
                response.put("message", message);

                // Извлекаем Body из JSON
                String bodyContent = messageService.extractBodyFromJsonMessage(message.getMessage());
                response.put("bodyContent", bodyContent);

                // Определяем формат сообщения
                boolean isJson = message.getMessage().trim().startsWith("{");
                response.put("isJson", isJson);

                // Получаем другие поля для отладки
                if (isJson) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode jsonNode = mapper.readTree(message.getMessage());

                        // Извлекаем основные поля
                        Map<String, String> fields = new HashMap<>();
                        fields.put("To", jsonNode.has("To") ? jsonNode.get("To").asText() : "");
                        fields.put("ToCC", jsonNode.has("ToCC") ? jsonNode.get("ToCC").asText() : "");
                        fields.put("Caption", jsonNode.has("Caption") ? jsonNode.get("Caption").asText() : "");
                        fields.put("typeMes", jsonNode.has("typeMes") ? jsonNode.get("typeMes").asText() : "");

                        response.put("fields", fields);
                    } catch (Exception e) {
                        log.debug("Не удалось распарсить JSON полностью: {}", e.getMessage());
                    }
                }
            } else {
                response.put("success", false);
                response.put("error", "Сообщение не найдено");
            }
        } catch (Exception e) {
            log.error("Ошибка получения сообщения", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/resend")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resendMessages(@RequestBody Map<String, List<Long>> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Long> messageIds = request.get("messageIds");
            int updated = messageService.resendMessages(messageIds);

            response.put("success", true);
            response.put("updatedCount", updated);

            // Получаем обновленные сообщения
            List<Message> updatedMessages = messageService.getMessagesByIds(messageIds);
            response.put("messages", updatedMessages);

        } catch (Exception e) {
            log.error("Ошибка при обновлении сообщений", e);
            response.put("success", false);
            response.put("error", "Ошибка при обновлении: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}