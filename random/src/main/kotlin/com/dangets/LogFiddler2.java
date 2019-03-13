package com.dangets;

import akka.event.Logging;
import akka.event.LoggingAdapter;

public class LogFiddler2 extends MyAbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(msg -> log.info("received '{}'", msg))
                .build();
    }
}
