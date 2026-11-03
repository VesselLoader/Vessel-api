package dev.vessel.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test fixture replicating Bukkit/Spigot/Paper @EventHandler annotation.
 * Strictly adheres to Rule 17 as an independent top-level class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
}
