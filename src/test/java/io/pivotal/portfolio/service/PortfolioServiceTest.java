package io.pivotal.portfolio.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;

import com.sun.mail.iap.Argument;
import io.pivotal.portfolio.config.ServiceTestConfiguration;
import io.pivotal.portfolio.domain.Order;
import io.pivotal.portfolio.domain.Portfolio;
import io.pivotal.portfolio.domain.Quote;
import io.pivotal.portfolio.domain.Transaction;
import io.pivotal.portfolio.repository.OrderRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RunWith(MockitoJUnitRunner.class)
public class PortfolioServiceTest {

    private static final String ACCOUNTS_SERVICE = "accountsService";
    private static final String BEARER_TOKEN_VALUE = "bearerTokenValue";

    @InjectMocks
    private PortfolioService service;

    @Mock
    private OrderRepository repo;

    @Mock
    private QuoteRemoteCallService quoteService;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private ClientResponse clientResponse;


    @Before
    public void setup() {
        ReflectionTestUtils.setField(service, "accountsService", ACCOUNTS_SERVICE);
    }

    @Test
    public void doGetPortfolio() {
        when(repo.getOrders()).thenReturn(ServiceTestConfiguration.orders());
        ArgumentCaptor<Set<String>> symbolsCaptor = ArgumentCaptor.forClass(Set.class);
        when(quoteService.getMultipleQuotes(symbolsCaptor.capture())).thenReturn(Arrays.asList(ServiceTestConfiguration.quote()));
        Portfolio folio = service.getPortfolio();
        assertEquals(ServiceTestConfiguration.order().getSymbol(), symbolsCaptor.getValue().iterator().next());
        assertNotNull(folio);
    }

    @Test
    public void doSaveOrder() {
        Order expectedOrder = ServiceTestConfiguration.order();
        expectedOrder.setOrderId(1);

        double amount = ServiceTestConfiguration.order().getQuantity() * ServiceTestConfiguration.order().getPrice().doubleValue() + ServiceTestConfiguration.order().getOrderFee().doubleValue();
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("//"
                + ACCOUNTS_SERVICE
                + "/accounts/transaction")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(requestBodySpec.syncBody(transactionArgumentCaptor.capture())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq(HttpHeaders.AUTHORIZATION), eq("Bearer " + BEARER_TOKEN_VALUE))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchange()).thenReturn(Mono.just(clientResponse));
        when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
        when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just("SUCCESS"));

        when(repo.save(expectedOrder)).thenReturn(expectedOrder);

        Order order = service.addOrder(expectedOrder, BEARER_TOKEN_VALUE);
        assertEquals(expectedOrder, order);
    }

    @Test
    public void doSaveOrderNullOrderFee() {
        Order returnOrder = ServiceTestConfiguration.order();
        returnOrder.setOrderId(1);
        double amount = returnOrder.getQuantity() * returnOrder.getPrice().doubleValue() + returnOrder.getOrderFee().doubleValue();
        ResponseEntity<String> response = new ResponseEntity<String>("SUCCESS", HttpStatus.OK);


        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("//"
                + ACCOUNTS_SERVICE
                + "/accounts/transaction")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(requestBodySpec.syncBody(transactionArgumentCaptor.capture())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq(HttpHeaders.AUTHORIZATION), eq("Bearer " + BEARER_TOKEN_VALUE))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchange()).thenReturn(Mono.just(clientResponse));
        when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
        when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just("SUCCESS"));
        when(repo.save(isA(Order.class))).thenReturn(returnOrder);
        Order requestOrder = ServiceTestConfiguration.order();
        requestOrder.setOrderFee(null);
        Order order = service.addOrder(requestOrder, BEARER_TOKEN_VALUE);
        assertEquals(order.getOrderFee(), ServiceTestConfiguration.order().getOrderFee());
    }

    @Test
    public void doSaveOrderSellOrder() {
        Order returnOrder = ServiceTestConfiguration.sellOrder();
        returnOrder.setOrderId(1);
        double amount = ServiceTestConfiguration.sellOrder().getQuantity() * ServiceTestConfiguration.sellOrder().getPrice().doubleValue() - ServiceTestConfiguration.sellOrder().getOrderFee().doubleValue();
        ResponseEntity<String> response = new ResponseEntity<String>("SUCCESS", HttpStatus.OK);


        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("//"
                + ACCOUNTS_SERVICE
                + "/accounts/transaction")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        when(requestBodySpec.syncBody(transactionArgumentCaptor.capture())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq(HttpHeaders.AUTHORIZATION), eq("Bearer " + BEARER_TOKEN_VALUE))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchange()).thenReturn(Mono.just(clientResponse));
        when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
        when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just("SUCCESS"));
        when(repo.save(ServiceTestConfiguration.sellOrder())).thenReturn(returnOrder);
        Order order = service.addOrder(ServiceTestConfiguration.sellOrder(), BEARER_TOKEN_VALUE);
        assertEquals(order, returnOrder);
    }

}
