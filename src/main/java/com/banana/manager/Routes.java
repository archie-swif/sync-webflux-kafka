package com.banana.manager;

import com.banana.data.Event;
import com.banana.manager.event.ManagerEventHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
@Profile("manager")
public class Routes {


    @Bean
    public RouterFunction<ServerResponse> router(ManagerEventHandler eventHandler) {

        return RouterFunctions

                .route(GET("/blocking"), serverRequest -> {

                    String id = UUID.randomUUID().toString();
                    Mono<Boolean> bridge = Mono.create(sink -> eventHandler.register(sink, id));

                    Mono<Boolean> sendAndWait = Mono.just(id)
                            .map(Event::new)
                            .map(eventHandler::sendTodo)
                            .then(bridge);

                    return ServerResponse.ok().body(sendAndWait, Boolean.class);
                })
                .andRoute(GET("/non-blocking"), serverRequest -> {

                    String id = UUID.randomUUID().toString();

                    Mono.just(id)
                            .map(Event::new)
                            .map(eventHandler::sendTodo)
                            .subscribe();

                    return ServerResponse.ok().build();
                });
    }

}
