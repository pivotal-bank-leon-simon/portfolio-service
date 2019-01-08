package io.pivotal.portfolio.service;

import io.pivotal.portfolio.PortfolioApplication;
import io.pivotal.portfolio.config.ServiceTestConfiguration;
import io.pivotal.portfolio.domain.Quote;

import io.pivotal.portfolio.domain.Transaction;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class QuoteRemoteCallServiceTest {

	private static final String QUOTES_URL = "quotes";

	@InjectMocks
	private QuoteRemoteCallService service;

	@Mock
	private WebClient webClient;


	@Mock
	private WebClient.RequestHeadersUriSpec requestHeadersSpec;

	@Mock
	private WebClient.ResponseSpec responseSpec;

	@Before
	public void setup() {
		ReflectionTestUtils.setField(service, "quotesService", QUOTES_URL);
	}
	
	/*
	 * resttemplate not being injected into service thus cannot test success of hystrix
	 */
	@Test
	public void doGetQuote() {
		when(webClient.get()).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.uri(eq("//" + QUOTES_URL + "/quote/" + ServiceTestConfiguration.SYMBOL))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.bodyToMono(Quote.class)).thenReturn(Mono.just(ServiceTestConfiguration.quote()));
		Quote quote = service.getQuote(ServiceTestConfiguration.SYMBOL);
		assertEquals(ServiceTestConfiguration.quote(),quote);
	}

}
