package dev.vessel.api.event;

import java.util.Objects;

/**
 * Lightweight, zero-overhead subscription token representing an active event listener registration.
 * <p>
 * Provides O(1) listener cancellation and lifecycle tracking without generating persistent heap garbage.
 *
 * @version 1.0.0
 */
public final class EventSubscriptionToken {

    private final SovereignEventBus eventBus;
    private final int eventId;
    private final int subscriptionId;
    private final long registrationEpoch;
    private volatile boolean active;

    public EventSubscriptionToken(SovereignEventBus eventBus, int eventId, int subscriptionId, long registrationEpoch) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus cannot be null");
        this.eventId = eventId;
        this.subscriptionId = subscriptionId;
        this.registrationEpoch = registrationEpoch;
        this.active = true;
    }

    public int eventId() {
        return eventId;
    }

    public int subscriptionId() {
        return subscriptionId;
    }

    public long registrationEpoch() {
        return registrationEpoch;
    }

    public boolean isActive() {
        return active;
    }

    public boolean unregister() {
        if (!active) {
            return false;
        }
        synchronized (this) {
            if (!active) {
                return false;
            }
            active = false;
            eventBus.unregister(this);
            return true;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EventSubscriptionToken other)) {
            return false;
        }
        return this.eventId == other.eventId && this.subscriptionId == other.subscriptionId;
    }

    @Override
    public int hashCode() {
        return (eventId * 31) ^ subscriptionId;
    }

    @Override
    public String toString() {
        return "EventSubscriptionToken[eventId=" + eventId + ", subscriptionId=" + subscriptionId + ", active=" + active + "]";
    }
}
