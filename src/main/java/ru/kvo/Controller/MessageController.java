package ru.kvo.Controller;

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

    @GetMapping("/api/message/{id}/body")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMessageBody(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Message message = messageService.getMessageById(id);
            if (message != null) {
                String bodyContent = messageService.extractBodyFromMessage(message.getMessage());
                response.put("success", true);
                response.put("bodyContent", bodyContent);
                response.put("messageId", id);
            } else {
                response.put("success", false);
                response.put("error", "Сообщение не найдено");
            }
        } catch (Exception e) {
            log.error("Ошибка получения тела сообщения", e);
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