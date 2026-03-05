package com.worldtrader.api.market.controller;

import com.worldtrader.api.StockApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StockApiApplication.class)
@AutoConfigureMockMvc
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void candlesEndpointReturnsValidShape() throws Exception {
        mockMvc.perform(get("/api/v1/candles")
                        .param("ticker", "AAPL")
                        .param("tf", "1s")
                        .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].t").exists())
                .andExpect(jsonPath("$[0].o").exists())
                .andExpect(jsonPath("$[0].c").exists());
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
