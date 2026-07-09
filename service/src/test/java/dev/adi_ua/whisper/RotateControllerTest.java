package dev.adi_ua.whisper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "whisper.db.url=jdbc:sqlite::memory:",
    "whisper.ntfy.base-url=http://localhost:9999",
    "whisper.rate-limit.rotate-rpm=5"
})
@AutoConfigureMockMvc
@DirtiesContext
class RotateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void rotateWithNoGroupsReturnsZero() throws Exception {
        mockMvc.perform(post("/api/rotate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rotated").value(0));
    }

    @Test
    void rotateGeneratesPhraseAndStoresHistory() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "rotate-test"}
                    """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        String groupId = body.get("id").asText();

        mockMvc.perform(post("/api/rotate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rotated").value(1));

        mockMvc.perform(get("/api/groups/" + groupId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phrase").isNotEmpty());
    }

    @Test
    void rotateReturns429WhenRateLimitExceeded() throws Exception {
        // Consume the entire bucket (limit is 5 in test config, 2 used above)
        mockMvc.perform(post("/api/rotate")).andExpect(status().isOk());
        mockMvc.perform(post("/api/rotate")).andExpect(status().isOk());
        mockMvc.perform(post("/api/rotate")).andExpect(status().isOk());

        // Next request exceeds the budget
        mockMvc.perform(post("/api/rotate"))
                .andExpect(status().isTooManyRequests());
    }
}
