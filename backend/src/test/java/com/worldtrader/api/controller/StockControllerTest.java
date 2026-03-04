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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StockApiApplication.class)
@AutoConfigureMockMvc
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getStocksContainsAapl() throws Exception {
        mockMvc.perform(get("/api/v1/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ticker == 'AAPL')]").exists());
    }

    @Test
    void getPriceByTickerReturnsNumericPrice() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/price/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker", is("AAPL")))
                .andExpect(jsonPath("$.price", greaterThan(0.0)));
    }

    @Test
    void unknownTickerReturns404Not500() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/price/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void dottedTickerIsSupported() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/price/BRK.B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker", is("BRK.B")));
    }
}
