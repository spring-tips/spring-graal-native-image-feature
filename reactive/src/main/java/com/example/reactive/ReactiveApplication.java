package com.example.reactive;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Log4j2
@SpringBootApplication(proxyBeanMethods = false)
public class ReactiveApplication {

	@Bean
	RouterFunction<ServerResponse> routes(ReservationRepository rr) {
		return route()
			.GET("/reservations", r -> ok().body(rr.findAll(), Reservation.class))
			.build();
	}

	@Bean
	ApplicationRunner runner(DatabaseClient databaseClient, ReservationRepository reservationRepository) {
		return args -> {

			Flux<Reservation> names = Flux
				.just("Andy", "Sebastien")
				.map(name -> new Reservation(null, name))
				.flatMap(reservationRepository::save);

			databaseClient
				.execute("create table reservation ( id   serial primary key, name varchar(255) not null )")
				.fetch()
				.rowsUpdated()
				.thenMany(names)
				.thenMany(reservationRepository.findAll())
				.subscribe(log::info);
		};
	}


	public static void main(String[] args) {
		SpringApplication.run(ReactiveApplication.class, args);
	}
}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, Integer> {
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class Reservation {

	@Id
	private Integer id;
	private String name;
}


/**
	* Bonus round! This is a simple websocket application.
	* Point <a href="http://localhost:8080/ws.htm"></a>your browser
	* here</a> to see it in action.
	*
 */
@Configuration
class WebSocketConfiguration {

	@Bean
	WebSocketHandler webSocketHandler() {
		return session -> {
			Flux<WebSocketMessage> map = Flux
				.fromStream(Stream.generate(() -> "Hello, world @ " + Instant.now()))
				.delayElements(Duration.ofSeconds(1))
				.map(session::textMessage);
			return session.send(map);
		};
	}

	@Bean
	SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler wsh) {
		return new SimpleUrlHandlerMapping(Collections.singletonMap("/ws/greetings", wsh), 10);
	}

	@Bean
	WebSocketHandlerAdapter webSocketHandlerAdapter() {
		return new WebSocketHandlerAdapter();
	}
}