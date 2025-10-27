package ar.edu.uade.analytics.ai;

import com.google.gson.Gson;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiSqlChatController {

    private final AiGeminiService aiService;

    public AiSqlChatController(AiGeminiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/query")
    public Map<String, Object> handleQuery(@RequestBody Map<String, String> body) {
        String query = body.get("query");
        String response = aiService.answer(query);

        // 🔍 Si el service devuelve JSON (String), parsealo a Map antes de devolver
        try {
            return new Gson().fromJson(response, Map.class);
        } catch (Exception e) {
            // Si por alguna razón no es JSON válido, lo devolvemos igual en un Map
            return Map.of("response", response);
        }
    }
}
