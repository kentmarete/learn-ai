package com.example.agent;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.agent.agent.AgentService;
import com.example.agent.api.AgentController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AgentService agent;

    @Test
    void health() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void ask() throws Exception {
        when(agent.run(anyString())).thenReturn("Nairobi is currently 28°C.");
        mvc.perform(post("/ask").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"What is the weather in Nairobi?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Nairobi is currently 28°C."));
    }

    @Test
    void rejectsEmptyQuery() throws Exception {
        mvc.perform(post("/ask").contentType(MediaType.APPLICATION_JSON).content("{\"query\": \"\"}"))
                .andExpect(status().isBadRequest());
    }
}
