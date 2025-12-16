package ru.kvo.Controller;

import org.springframework.http.ResponseEntity;
import ru.kvo.Dto.MessageUpdateRequest;
import ru.kvo.Dto.SearchRequest;
import ru.kvo.Entity.Message;
import ru.kvo.Service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
        model.addAttribute("searchRequest", new SearchRequest());

        // Получаем список уникальных дат для автозаполнения
        List<LocalDate> uniqueDates = messageService.getUniqueDates();
        model.addAttribute("uniqueDates", uniqueDates);

        return "index";
    }

    @PostMapping("/search")
    public String searchMessages(@ModelAttribute SearchRequest searchRequest,
                                 Model model) {

        log.info("Поиск: email={}, date={}",
                searchRequest.getEmail(), searchRequest.getDateCreate());

        // Валидация
        boolean emailEmpty = searchRequest.getEmail() == null ||
                searchRequest.getEmail().trim().isEmpty();
        boolean dateEmpty = searchRequest.getDateCreate() == null;

        if (emailEmpty && dateEmpty) {
            model.addAttribute("error", "Заполните хотя бы одно поле: Email или Дата");
            model.addAttribute("searchRequest", searchRequest);

            // Добавляем список дат
            List<LocalDate> uniqueDates = messageService.getUniqueDates();
            model.addAttribute("uniqueDates", uniqueDates);

            return "index";
        }

        try {
            List<Message> messages = messageService.searchMessages(searchRequest);
            model.addAttribute("messages", messages);
            model.addAttribute("searchRequest", searchRequest); // Сохраняем запрос
            model.addAttribute("searchResultCount", messages.size());

            // Добавляем список дат
            List<LocalDate> uniqueDates = messageService.getUniqueDates();
            model.addAttribute("uniqueDates", uniqueDates);

        } catch (Exception e) {
            model.addAttribute("error", "Ошибка поиска: " + e.getMessage());
            model.addAttribute("searchRequest", searchRequest);

            // Добавляем список дат
            List<LocalDate> uniqueDates = messageService.getUniqueDates();
            model.addAttribute("uniqueDates", uniqueDates);
        }

        return "index";
    }

    @GetMapping("/api/message/{id}/full")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMessageFull(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> messageInfo = messageService.getMessageFullInfo(id);

            if (messageInfo.containsKey("message")) {
                response.put("success", true);
                response.putAll(messageInfo);
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
    @PostMapping("/api/message/{id}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateMessageRecipients(
            @PathVariable Long id,
            @RequestBody MessageUpdateRequest updateRequest) {

        Map<String, Object> response = messageService.updateMessageRecipients(id, updateRequest);
        return ResponseEntity.ok(response);
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
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }
}