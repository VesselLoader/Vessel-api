package dev.vessel.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking an event listener method within the VESSEL Super-API ecosystem.
 *
 * <p>Methods annotated with {@code @VesselSubscribe} must accept exactly one parameter
 * indicating the event type to receive.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VesselSubscribe {

    /**
     * Priority at which this subscriber executes relative to other subscribers.
     *
     * @return priority level
     */
    VesselEventPriority priority() default VesselEventPriority.NORMAL;

    /**
     * Whether this subscriber should still be called even if the event was previously cancelled.
     *
     * @return true to receive cancelled events
     */
    boolean receiveCancelled() default false;
}
