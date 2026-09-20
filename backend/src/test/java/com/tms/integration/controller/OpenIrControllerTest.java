package com.tms.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class OpenIrControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void snapshotsIncludeDelayedWaybillThenSyncTrack() throws Exception {
        String snapshots = mockMvc.perform(get("/api/open/ir/snapshots")
                        .header("X-Api-Key", "test-open-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.system").value("TMS"))
                .andReturn().getResponse().getContentAsString();
        JsonNode delayed = null;
        for (JsonNode row : objectMapper.readTree(snapshots).get("data").get("waybills")) {
            if ("WB-IR-DELAY".equals(row.path("waybillCode").asText())
                    || "WB-IR-DELAY".equals(row.path("code").asText())) {
                delayed = row;
                break;
            }
        }
        assertNotNull(delayed, "应包含 IR 延误运单");
        org.junit.jupiter.api.Assertions.assertEquals("IR-SO-STUCK", delayed.path("sourceNo").asText());

        mockMvc.perform(post("/api/open/ir/actions")
                        .header("X-Api-Key", "test-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TMS_SYNC_TRACK\",\"targetKey\":\"WB-IR-DELAY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.code").value("WB-IR-DELAY"));
    }

    @Test
    public void snapshotsIncludeExceptionAndDispatchCreatedWaybill() throws Exception {
        String snapshots = mockMvc.perform(get("/api/open/ir/snapshots")
                        .header("X-Api-Key", "test-open-key"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode exception = null;
        JsonNode created = null;
        for (JsonNode row : objectMapper.readTree(snapshots).get("data").get("waybills")) {
            if ("WB-IR-EXC".equals(row.path("waybillCode").asText())
                    || "WB-IR-EXC".equals(row.path("code").asText())) {
                exception = row;
            }
            if ("WB-IR-CREATED".equals(row.path("waybillCode").asText())
                    || "WB-IR-CREATED".equals(row.path("code").asText())) {
                created = row;
            }
        }
        assertNotNull(exception, "应包含 IR 异常运单");
        org.junit.jupiter.api.Assertions.assertTrue(
                exception.path("exceptionFlag").asBoolean(),
                "WB-IR-EXC 应为异常运单");
        assertNotNull(created, "应包含 IR 待调度运单");

        mockMvc.perform(post("/api/open/ir/actions")
                        .header("X-Api-Key", "test-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TMS_DISPATCH\",\"targetKey\":\"WB-IR-CREATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.code").value("WB-IR-CREATED"))
                .andExpect(jsonPath("$.data.status").value("DISPATCHED"));
    }

    @Test
    public void dispatchCreatedTransportOrderCreatesWaybill() throws Exception {
        mockMvc.perform(post("/api/open/ir/actions")
                        .header("X-Api-Key", "test-open-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TMS_DISPATCH\",\"targetKey\":\"TO-DEMO01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DISPATCHED"));
    }
}
