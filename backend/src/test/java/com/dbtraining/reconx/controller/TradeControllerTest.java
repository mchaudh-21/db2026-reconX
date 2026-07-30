package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.service.TradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADV063–ADV067 controller tests.
 *
 * Uses standalone MockMvc so this controller can be tested independently from
 * unfinished JWT, exception-handler, Kafka, and JPA configuration tickets.
 */
@ExtendWith(MockitoExtension.class)
class TradeControllerTest {

    @Mock
    private TradeService service;

    @Mock
    private TradeMapper mapper;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        TradeController controller =
                new TradeController(service, mapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver()
                )
                .setValidator(validator)
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();
    }

    @Test
    void listReturnsStablePaginationEnvelope() throws Exception {
        Trade trade = tradeWithId(42L);
        TradeResponse response = response(42L, "PENDING");

        var pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "tradeDate")
        );

        when(service.list(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(trade), pageable, 1));

        when(mapper.toResponse(trade)).thenReturn(response);

        mockMvc.perform(get("/v1/trades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(42))
                .andExpect(jsonPath("$.items[0].tradeRef")
                        .value("EQU-20260730-0001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void createReturns201LocationAndMappedBody() throws Exception {
        TradeRequest request = validRequest();
        Trade trade = tradeWithId(42L);
        TradeResponse response = response(42L, "PENDING");

        when(service.create(eq(request), eq("anonymous")))
                .thenReturn(trade);
        when(mapper.toResponse(trade)).thenReturn(response);

        mockMvc.perform(post("/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/trades/42"
                ))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void invalidCreateIsRejectedBeforeServiceRuns() throws Exception {
        TradeRequest invalid = new TradeRequest(
                "",
                1L,
                1L,
                "EQUITY",
                "BUY",
                new BigDecimal("-1"),
                BigDecimal.ZERO,
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any(), any());
    }

    @Test
    void updateReturnsMappedTrade() throws Exception {
        TradeRequest request = validRequest();
        Trade trade = tradeWithId(42L);
        TradeResponse response = response(42L, "PENDING");

        when(service.update(42L, request, "anonymous"))
                .thenReturn(trade);
        when(mapper.toResponse(trade)).thenReturn(response);

        mockMvc.perform(put("/v1/trades/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.quantity").value(100));
    }

    @Test
    void patchStatusValidatesAndUpdatesOnlyStatus() throws Exception {
        Trade trade = tradeWithId(42L);
        TradeResponse response = response(42L, "MATCHED");

        when(service.updateStatus(42L, "MATCHED", "anonymous"))
                .thenReturn(trade);
        when(mapper.toResponse(trade)).thenReturn(response);

        mockMvc.perform(patch("/v1/trades/42/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"MATCHED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MATCHED"));

        mockMvc.perform(patch("/v1/trades/42/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"FOOBAR"}
                                """))
                .andExpect(status().isBadRequest());

        verify(service, never())
                .updateStatus(anyLong(), eq("FOOBAR"), any());
    }

    @Test
    void deleteReturns204WithNoBody() throws Exception {
        mockMvc.perform(delete("/v1/trades/42"))
                .andExpect(status().isNoContent());

        verify(service).softDelete(42L, "anonymous");
    }

    private static TradeRequest validRequest() {
        return new TradeRequest(
                "EQU-20260730-0001",
                1L,
                1L,
                "EQUITY",
                "BUY",
                new BigDecimal("100.0000"),
                new BigDecimal("245.5000"),
                LocalDate.now()
        );
    }

    private static Trade tradeWithId(Long id) {
        Trade trade = new Trade();
        ReflectionTestUtils.setField(trade, "id", id);
        return trade;
    }

    private static TradeResponse response(Long id, String status) {
        return new TradeResponse(
                id,
                "EQU-20260730-0001",
                1L,
                "AAPL",
                1L,
                "Apex",
                "EQUITY",
                "BUY",
                new BigDecimal("100.0000"),
                new BigDecimal("245.5000"),
                LocalDate.now(),
                status,
                Instant.now(),
                Instant.now()
        );
    }
}