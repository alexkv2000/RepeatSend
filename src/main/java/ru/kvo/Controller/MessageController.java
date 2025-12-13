package ru.kvo.Controller;

import ru.kvo.Dto.ResendRequest;
import ru.kvo.Dto.SearchRequest;
import ru.kvo.Entity.Message;
import ru.kvo.Service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public String index(Model model) {
        model.addAttribute("searchRequest", new SearchRequest());
        return "index";
    }

    @PostMapping("/search")
    public String search(@ModelAttribute SearchRequest request, Model model) {
        List<Message> messages = messageService.searchByEmail(request.getEmail());
        model.addAttribute("messages", messages);
        model.addAttribute("searchRequest", request);
        return "index";
    }

    @PostMapping("/api/resend")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resendMessages(@RequestBody ResendRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Имитация задержки для демонстрации анимации
            Thread.sleep(1500);

            int updated = messageService.resendMessages(request.getMessageIds());
            response.put("success", true);
            response.put("updatedCount", updated);

            // Получаем обновленные сообщения
            List<Message> updatedMessages = messageService.getMessagesByIds(request.getMessageIds());
            response.put("messages", updatedMessages);

        } catch (InterruptedException e) {
            response.put("success", false);
            response.put("error", "Ошибка при обновлении сообщений");
        }

        return ResponseEntity.ok(response);
    }
}
