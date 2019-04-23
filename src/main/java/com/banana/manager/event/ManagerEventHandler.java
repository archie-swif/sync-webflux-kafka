package com.banana.manager.event;

import com.banana.data.Event;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.MonoSink;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;


@Profile("manager")
@Component
@EnableBinding(ManagerChannels.class)
@Log4j2
public class ManagerEventHandler {

    @Autowired
    private ManagerChannels managerChannels;

    private Map<String, MonoSink<Boolean>> sinks = new ConcurrentHashMap<>();

    private Histogram loopTime;
    private Histogram kafkaTime;
    private Meter tps;

    @PostConstruct
    public void initMetrics() {
        MetricRegistry metrics = new MetricRegistry();
        loopTime = metrics.histogram(name(ManagerEventHandler.class, "loop-time"));
        kafkaTime = metrics.histogram(name(ManagerEventHandler.class, "kafka-time"));
        tps = metrics.meter("requests");

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(10, TimeUnit.SECONDS);
    }

    public void register(MonoSink<Boolean> sink, String id) {
        sinks.put(id, sink);
    }


    public boolean sendTodo(Event event) {
        Message<Event> msg = MessageBuilder.withPayload(event).build();
        return managerChannels.output().send(msg);
    }


    @StreamListener
    public void inputHandler(@Input(ManagerChannels.WORK_COMPLETE) Flux<Message<Event>> incomingEvent) {

        incomingEvent
                .doOnNext(msg -> {
                    Long time = (Long) msg.getHeaders().get("kafka_receivedTimestamp");
                    kafkaTime.update(Duration.between(Instant.ofEpochMilli(time), Instant.now()).toMillis());
                })
                .map(msg -> msg.getPayload())
                .doOnNext(e -> loopTime.update(Duration.between(e.getCreatedTime(), Instant.now()).toMillis()))
                .doOnNext(e -> tps.mark())
                .map(Event::getId)
                .doOnNext(id -> Optional.ofNullable(sinks.remove(id)).ifPresent(snk -> snk.success(true)))
                .subscribe();
    }

}
