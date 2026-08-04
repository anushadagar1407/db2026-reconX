package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.service.TradeStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TradeStreamControllerTest {

    @Test
    void streamStartsAnSseResponse() throws Exception {
        TradeStreamService service = new TradeStreamService();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TradeStreamController(service))
                .build();

        mockMvc.perform(get("/v1/trades/stream")
                        .header("Origin", "http://localhost:5500")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5500"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
}
