package org.legendstack.basebot.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class ApiVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username="testuser", roles={"USER"})
    public void testGraphDataApi() throws Exception {
        String response = mockMvc.perform(get("/api/graph/data?contextId=global"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        System.out.println("GRAPH API RESPONSE START ====");
        System.out.println(response);
        System.out.println("==== GRAPH API RESPONSE END");
    }

    @Test
    @WithMockUser(username="testuser", roles={"USER"})
    public void testDocumentChunksApi() throws Exception {
        // Since we don't have the exact URI in the test context, we'll just check if it returns 200
        // with the known mock file URI
        String uri = "upload://global/bc45c672ed41a5ff629150d725ab7ece2421a905f8cbdc795ba30cfa109e8b32/Enterprise_AI_Standards_v1.md";
        String response = mockMvc.perform(get("/api/documents/chunks").param("uri", uri))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
                
        System.out.println("CHUNKS API RESPONSE START ====");
        System.out.println(response);
        System.out.println("==== CHUNKS API RESPONSE END");
    }
}
