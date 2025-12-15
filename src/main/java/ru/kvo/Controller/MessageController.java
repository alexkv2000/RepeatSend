package ru.kvo.Controller;

import org.springframework.http.ResponseEntity;
import ru.kvo.Entity.Message;
import ru.kvo.Service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
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

    @PostMapping("/api/resend")
    @ResponseBody
    public Map<String, Object> resendMessages(@RequestBody Map<String, List<Long>> request) {
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
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
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
            } else {
                response.put("success", false);
                response.put("error", "Сообщение не найдено");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}