package io.pivotal.portfolio.repository;


import java.util.List;

import io.pivotal.portfolio.domain.Order;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * 
 * @author David Ferreira Pinto
 *
 */
public interface OrderRepository extends CrudRepository<Order,Integer> {

	@Query("from Order where userid = ?#{principal.claims['user_id']} order by completionDate asc")
	//@Query("from Order order by completionDate asc")
	List<Order> getOrders();

}
