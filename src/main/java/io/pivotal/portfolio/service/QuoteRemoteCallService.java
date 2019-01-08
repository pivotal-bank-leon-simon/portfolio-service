package io.pivotal.portfolio.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.newrelic.api.agent.Trace;
import io.pivotal.portfolio.domain.Quote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

/**
 * Retrieves quotes from the quote service. Uses hystrix to manage failure.
 * 
 * @author David Ferreira Pinto
 *
 */
@Service
@RefreshScope
public class QuoteRemoteCallService {

	private static final Logger logger = LoggerFactory.getLogger(QuoteRemoteCallService.class);

	@Value("${pivotal.quotesService.name}")
	private String quotesService;

	@Autowired
	private WebClient webClient;

	/**
	 * Retrieve up to date quotes.
	 * 
	 * @param symbol
	 *            the symbol of the quote to fetch.
	 * @return The quote
	 */
	@Trace(async = true)
	@HystrixCommand(fallbackMethod = "getQuoteFallback")
	public Quote getQuote(String symbol) {
		logger.debug("Fetching quote: " + symbol);
		Quote quote = webClient
				.get()
				.uri("//" + quotesService + "/quote/" + symbol)
				.retrieve()
				.bodyToMono(Quote.class)
				.block();
		return quote;
	}

	/**
	 * Fallback for the quote service.
	 * 
	 * @param symbol
	 *            the symbol of the quote to fetch.
	 * @return Empty quote in FAILED state.
	 */
	@SuppressWarnings("unused")
	private Quote getQuoteFallback(String symbol) {
		logger.debug("Fetching fallback quote for: " + symbol);
		// Quote quote =
		// restTemplate.getForObject("http://quotes/quote/{symbol}",
		// Quote.class, symbol);
		Quote quote = new Quote();
		quote.setSymbol(symbol);
		quote.setStatus("FAILED");
		return quote;
	}
	/**
	 * Retrieve multiple quotes.
	 * 
	 * @param symbols comma separated list of symbols.
	 * @return
	 */
	@Trace(async = true)
	public List<Quote> getMultipleQuotes(String symbols) {
		logger.debug("retrieving multiple quotes: " + symbols);
		ParameterizedTypeReference<List<Quote>> typeRef = new ParameterizedTypeReference<List<Quote>>() {};
		List<Quote> quotes = webClient
				.get()
				.uri("//" + quotesService + "/v1/quotes?q=" + symbols)
				.retrieve()
				.bodyToMono(typeRef)
				.block();
		logger.debug("Received quotes: {}",quotes);
		return quotes;
		
	}
	/**
	 * Retrieve multiple quotes.
	 * 
	 * @param symbols
	 * @return
	 */
	public List<Quote> getMultipleQuotes(Collection<String> symbols) {
		logger.debug("Fetching multiple quotes array: {} ",symbols);
		StringBuilder builder = new StringBuilder();
		for (Iterator<String> i = symbols.iterator(); i.hasNext();) {
			builder.append(i.next());
			if (i.hasNext()) {
				builder.append(",");
			}
		}
		return getMultipleQuotes(builder.toString());
	}
}
