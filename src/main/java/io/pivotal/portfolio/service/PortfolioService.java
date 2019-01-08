package io.pivotal.portfolio.service;

import com.newrelic.api.agent.Trace;
import io.pivotal.portfolio.domain.*;
import io.pivotal.portfolio.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

/**
 * Manages a portfolio of holdings of stock/shares.
 * 
 * @author David Ferreira Pinto
 *
 */
@Service
@RefreshScope
public class PortfolioService {
	private static final Logger logger = LoggerFactory
			.getLogger(PortfolioService.class);

	/**
	 * The service than handles the calls to get quotes.
	 */
	@Autowired
	QuoteRemoteCallService quoteService;

	@Autowired
	private WebClient webClient;

	@Autowired
	private OrderRepository orderRepository;

	@Value("${pivotal.accountsService.name}")
	protected String accountsService;

	/**
	 * Retrieves the portfolio for the given accountId.
	 *
	 *            The account id to retrieve for.
	 * @return The portfolio.
	 */
	@Trace(async = true)
	public Portfolio getPortfolio() {
		/*
		 * Retrieve all orders for accounts id and build portfolio. - for each
		 * order create holding. - for each holding find current price.
		 */
		logger.debug("Getting portfolio for accountId: " );
		List<Order> orders = orderRepository.getOrders();
		Portfolio folio = new Portfolio();
		return createPortfolio(folio, orders);
	}

	/**
	 * Builds a portfolio object with the list of orders.
	 * 
	 * @param portfolio
	 *            the portfolio object to build.
	 * @param orders
	 *            the list of orders.
	 * @return the portfolio object
	 */
	@Trace(async = true)
	private Portfolio createPortfolio(Portfolio portfolio, List<Order> orders) {
		// TODO: change to forEach() and maybe in parallel?

		Set<String> symbols = new HashSet<>();
		Holding holding = null;
		for (Order order : orders) {
			holding = portfolio.getHolding(order.getSymbol());
			if (holding == null) {
				holding = new Holding();
				holding.setSymbol(order.getSymbol());
				holding.setCurrency(order.getCurrency());
				portfolio.addHolding(holding);
				symbols.add(order.getSymbol());
			}
			holding.addOrder(order);
		}
		List<Quote> quotes = new ArrayList<>();
		
		if (symbols.size() > 0) {
			quotes = quoteService.getMultipleQuotes(symbols);
		}

		for (Quote quote : quotes) {
			portfolio.getHolding(quote.getSymbol()).setCurrentValue(quote.getLastPrice());
		}
		portfolio.refreshTotalValue();
		logger.debug("Portfolio: " + portfolio);
		return portfolio;
	}

	/**
	 * Calculates the current value of the holding.
	 * 
	 * @param holding
	 *            the holding to refresh.
	 */

	/*private void refreshHolding(Holding holding) {
		Quote quote = quoteService.getQuote(holding.getSymbol());
		if (quote.getStatus().equalsIgnoreCase(Quote.STATUS_SUCCESS)) {
			holding.setCurrentValue(new BigDecimal(quote.getLastPrice()));
		}

	}
	*/

	/**
	 * Add an order to the repository and modify account balance.
	 * 
	 * @param order
	 *            the order to add.
	 * @return the saved order.
	 */
	@Transactional
	@Trace(async = true)
	public Order addOrder(Order order, String bearerToken) {
		logger.debug("Adding order: " + order);
		if (order.getOrderFee() == null) {
			order.setOrderFee(Order.DEFAULT_ORDER_FEE);
			logger.debug("Adding Fee to order: " + order);
		}
		Transaction transaction = new Transaction();
		
		if (order.getOrderType().equals(OrderType.BUY)) {
			double amount = order.getQuantity()
					* order.getPrice().doubleValue()
					+ order.getOrderFee().doubleValue();
			
			transaction.setAccountId(order.getAccountId());
			transaction.setAmount(BigDecimal.valueOf(amount));
			transaction.setCurrency(order.getCurrency());
			transaction.setDate(order.getCompletionDate());
			transaction.setDescription(order.toString());
			transaction.setType(TransactionType.DEBIT);
			
		} else if (order.getOrderType().equals(OrderType.SELL)){
			double amount = order.getQuantity()
					* order.getPrice().doubleValue()
					- order.getOrderFee().doubleValue();
			
			transaction.setAccountId(order.getAccountId());
			transaction.setAmount(BigDecimal.valueOf(amount));
			transaction.setCurrency(order.getCurrency());
			transaction.setDate(order.getCompletionDate());
			transaction.setDescription(order.toString());
			transaction.setType(TransactionType.CREDIT);
			
		}

		ClientResponse result = webClient
				.post()
				.uri("//"
								+ accountsService
								+ "/accounts/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody(transaction)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
				.exchange()
				.block();
		if (result.statusCode() == HttpStatus.OK) {
			logger.info(String
					.format("Account funds updated successfully for account: %s and new funds are: %s",
							order.getAccountId(), result.bodyToMono(String.class).block()));
			return orderRepository.save(order);
			
		} else {
			// TODO: throw exception - not enough funds!
			// SK - Whats the expected behaviour?
			logger.warn("PortfolioService:addOrder - decresing balance HTTP not ok: ");
			return null;
		}

	}
}
