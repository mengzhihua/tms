package com.tms.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class OpenTransportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void createsTransportOrderAndReplays() throws Exception {
        String body = "{\"orderNo\":\"OMS-SO-9\",\"receiverName\":\"张三\",\"address\":\"上海\","
                + "\"trackingNo\":\"SF1\",\"items\":[{\"sku\":\"SKU001\",\"qty\":2}]}";
        mockMvc.perform(post("/api/open/transport-order")
                        .header("X-Api-Key", "test-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sourceNo").value("OMS-SO-9"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.code").isNotEmpty());
        mockMvc.perform(post("/api/open/transport-order")
                        .header("X-Api-Key", "test-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceNo").value("OMS-SO-9"));
    }
}
