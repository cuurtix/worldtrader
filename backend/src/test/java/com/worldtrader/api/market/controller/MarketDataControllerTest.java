package com.worldtrader.api.market.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldtrader.api.StockApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StockApiApplication.class)
@AutoConfigureMockMvc
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void candlesEndpointReturnsValidShape() throws Exception {
        String body = mockMvc.perform(get("/api/v1/candles")
                        .param("ticker", "AAPL")
                        .param("tf", "1s")
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].t").exists())
                .andExpect(jsonPath("$[0].o").exists())
                .andExpect(jsonPath("$[0].h").exists())
                .andExpect(jsonPath("$[0].l").exists())
                .andExpect(jsonPath("$[0].c").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> candles = objectMapper.readValue(body, new TypeReference<>() {});
        Map<String, Object> first = candles.get(0);
        double o = ((Number) first.get("o")).doubleValue();
        double h = ((Number) first.get("h")).doubleValue();
        double l = ((Number) first.get("l")).doubleValue();
        double c = ((Number) first.get("c")).doubleValue();
        assertTrue(h >= Math.max(o, c), "h should be >= max(o,c)");
        assertTrue(l <= Math.min(o, c), "l should be <= min(o,c)");
    }

    @Test
    void tradesEndpointReturnsValidShape() throws Exception {
        mockMvc.perform(get("/api/v1/trades")
                        .param("ticker", "AAPL")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].t").exists())
                .andExpect(jsonPath("$[0].price").exists())
                .andExpect(jsonPath("$[0].qty").exists());
    }
}
