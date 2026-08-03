package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** TICKET-ADV104 — manages browser subscriptions to the live trade stream. */
@Service
public class TradeStreamService {

    private static final Logger log = LoggerFactory.getLogger(TradeStreamService.class);
    private static final long NO_SERVER_TIMEOUT = 0L;

    private final Set<SseEmitter> subscribers = ConcurrentHashMap.newKeySet();

    public SseEmitter subscribe() {
        SseEmitter emitter = createEmitter();
        subscribers.add(emitter);

        Runnable remove = () -> subscribers.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ignored -> remove.run());

        try {
            // Commit the response immediately so EventSource fires its open event even
            // when no trades have been created yet.
            emitter.send(SseEmitter.event().comment("connected").reconnectTime(3_000L));
        } catch (IOException exception) {
            remove.run();
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    SseEmitter createEmitter() {
        return new SseEmitter(NO_SERVER_TIMEOUT);
    }

    public void publish(TradeResponse trade) {
        subscribers.forEach(emitter -> send(emitter, trade));
    }

    private void send(SseEmitter emitter, TradeResponse trade) {
        try {
            // An unnamed event is intentional: the browser receives it through onmessage.
            emitter.send(SseEmitter.event().data(trade, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException exception) {
            subscribers.remove(emitter);
            emitter.complete();
            log.debug("Removed closed trade-stream subscriber", exception);
        }
    }

    int subscriberCount() {
        return subscribers.size();
    }
}
