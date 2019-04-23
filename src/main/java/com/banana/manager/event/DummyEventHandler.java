package com.banana.manager.event;

import com.banana.data.Event;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;


//@Profile("manager")
//@Component
//@Log4j2
public class DummyEventHandler {


    private Histogram loopTime;
    private Meter tps;
    private Random random = new Random();
    int mean = 71;
    int std = 65;

    @PostConstruct
    public void initMetrics() {
        MetricRegistry metrics = new MetricRegistry();
        loopTime = metrics.histogram(name(ManagerEventHandler.class, "loop-time"));
        tps = metrics.meter("requests");

        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(10, TimeUnit.SECONDS);
    }

    public boolean sendTodo(Event event) {
        return true;
    }

    public void register(MonoSink<Boolean> sink, String id) {
        Instant then = Instant.now();

        Mono.just(true)
                .delayElement(Duration.ofMillis(Double.valueOf(mean + std * random.nextGaussian()).longValue()))
                .doOnNext(e -> loopTime.update(Duration.between(then, Instant.now()).toMillis()))
                .doOnNext(e -> tps.mark())
                .doOnNext(sink::success)
                .subscribe();
    }

}
