package com.banana.worker.event;

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

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;


@Profile("worker")
@Component
@EnableBinding(WorkerChannels.class)
@Log4j2
public class WorkerEventHandler {

    @Autowired
    WorkerChannels workerChannels;

    private Histogram loopTime;
    private Histogram kafkaTime;
    private Meter tps;

    @PostConstruct
    public void initMetrics() {
        MetricRegistry metrics = new MetricRegistry();
        loopTime = metrics.histogram(name(WorkerEventHandler.class, "loop-time"));
        kafkaTime = metrics.histogram(name(WorkerEventHandler.class, "kafka-time"));
        tps = metrics.meter("requests");

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(10, TimeUnit.SECONDS);
    }

    @StreamListener
    public void inputHandler(@Input(WorkerChannels.TODO) Flux<Message<Event>> todo) {

        todo
                .doOnNext(msg -> {
                    Long time = (Long) msg.getHeaders().get("kafka_receivedTimestamp");
                    kafkaTime.update(Duration.between(Instant.ofEpochMilli(time), Instant.now()).toMillis());
                })
                .map(msg -> msg.getPayload())
                .doOnNext(u -> loopTime.update(Duration.between(u.getCreatedTime(), Instant.now()).toMillis()))
                .doOnNext(u -> tps.mark())
                .map(u -> MessageBuilder.withPayload(u).build())
                .map(msg -> workerChannels.output().send(msg))
                .subscribe();
    }

    //    Reactive sender
//    @StreamListener
//    public void inputHandler(@Input(WorkerChannels.TODO) Flux<Event> incomingEvent, @Output(WorkCompleteOut.name) FluxSender reactiveOut) {
//        reactiveOut.send(incomingEvent);
//    }

}
