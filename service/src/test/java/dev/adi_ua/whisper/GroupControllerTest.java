package dev.adi_ua.whisper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "whisper.db.url=jdbc:sqlite::memory:")
@AutoConfigureMockMvc
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void createAndGetGroup() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "test-couple", "schedule": "daily", "timezone": "America/New_York"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-couple"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        String groupId = body.get("id").asText();

        mockMvc.perform(get("/api/groups/" + groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-couple"));
    }

    @Test
    void addMemberToGroup() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "family"}
                    """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        String groupId = body.get("id").asText();

        mockMvc.perform(post("/api/groups/" + groupId + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Alice", "channel": "ntfy:alice-secret-topic"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.channel").value("ntfy:alice-secret-topic"));

        mockMvc.perform(get("/api/groups/" + groupId + "/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    void pinProtectedGroupRejectsBadPin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "secured", "pin": "1234"}
                    """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        String groupId = body.get("id").asText();

        // Wrong PIN
        mockMvc.perform(post("/api/groups/" + groupId + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Eve", "channel": "ntfy:eve", "pin": "0000"}
                    """))
                .andExpect(status().isForbidden());

        // Correct PIN
        mockMvc.perform(post("/api/groups/" + groupId + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Bob", "channel": "ntfy:bob", "pin": "1234"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bob"));
    }

    @Test
    void getNonExistentGroupReturns404() throws Exception {
        mockMvc.perform(get("/api/groups/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
