package com.example.reactive;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

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

