package com.worldtrader.api.controller;

import com.worldtrader.api.StockApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StockApiApplication.class)
@AutoConfigureMockMvc
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getIndividualStockDataBasicWorks() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/AAPL").param("view", "BASIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker", is("AAPL")));
    }

    @Test
    void getPriceByTickerReturnsNumericPrice() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/price/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$", greaterThan(0.0)));
    }

    @Test
    void unknownTickerReturns404Not500() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/price/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
