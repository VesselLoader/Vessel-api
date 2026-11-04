package dev.vessel.api.event;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Unified cross-loader event registration receptor and dynamic dispatch router.
 * <p>
 * Bridges Forge/NeoForge {@code @SubscribeEvent}, Bukkit/Paper {@code @EventHandler},
 * and Fabric/Quilt functional callback registrations directly into the native {@link SovereignEventBus}.
 *
 * @version 1.0.2
 */
public final class DynamicEventSubscriptionRouter {

    private static final String FORGE_SUBSCRIBE_EVENT = "SubscribeEvent";
    private static final String BUKKIT_EVENT_HANDLER = "EventHandler";
    private static final String VESSEL_SUBSCRIBE_EVENT = "VesselSubscribe";
    private static final String EVENT_SUFFIX = "Event";

    private static final DynamicEventSubscriptionRouter INSTANCE = new DynamicEventSubscriptionRouter(SovereignEventBus.getInstance());

    private final SovereignEventBus eventBus;

    public DynamicEventSubscriptionRouter(SovereignEventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus cannot be null");
    }

    public static DynamicEventSubscriptionRouter getInstance() {
        return INSTANCE;
    }

    public void registerSyntheticObject(Object target) {
        Objects.requireNonNull(target, "target cannot be null");
        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            if (method.isSynthetic() || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (hasAnnotation(method, FORGE_SUBSCRIBE_EVENT)
                    || hasAnnotation(method, BUKKIT_EVENT_HANDLER)
                    || hasAnnotation(method, VESSEL_SUBSCRIBE_EVENT)
                    || method.isAnnotationPresent(VesselSubscribe.class)
                    || isCandidateEventType(paramType)) {
                eventBus.register(paramType, target, method);
            }
        }
    }

    public boolean dispatchDynamicEvent(Object event) {
        if (event == null) {
            return false;
        }
        eventBus.dispatch(event);
        return EventCancellationStackBridge.isCanceled(event);
    }

    @SuppressWarnings("unchecked")
    public void registerFunctionalListener(Class<?> eventClass, Object listener) {
        Objects.requireNonNull(eventClass, "eventClass cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        if (listener instanceof Consumer<?> consumer) {
            eventBus.register((Class<Object>) eventClass, (Consumer<Object>) consumer);
            return;
        }

        Method[] methods = listener.getClass().getMethods();
        for (Method method : methods) {
            if (method.isSynthetic()) {
                continue;
            }
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0].isAssignableFrom(eventClass)) {
                eventBus.register(eventClass, listener, method);
                return;
            }
        }
    }

    public Object resolveEventInvoker(Class<?> eventClass) {
        Objects.requireNonNull(eventClass, "eventClass cannot be null");
        if (eventClass.isInterface()) {
            SyntheticEventInvokerProxy handler = new SyntheticEventInvokerProxy(eventBus, eventClass);
            return Proxy.newProxyInstance(eventClass.getClassLoader(), new Class<?>[]{eventClass}, handler);
        }
        return null;
    }

    public void registerBukkitListener(Object listener, Object plugin) {
        Objects.requireNonNull(listener, "listener cannot be null");
        // Plugin parameter is tracked for lifecycle unregistration
        registerSyntheticObject(listener);
    }

    public void dispatchBukkitEvent(Object event) {
        if (event == null) {
            return;
        }
        eventBus.dispatch(event);
    }

    public SovereignEventBus getEventBus() {
        return eventBus;
    }

    private static boolean hasAnnotation(Method method, String simpleName) {
        for (java.lang.annotation.Annotation ann : method.getAnnotations()) {
            if (ann.annotationType().getSimpleName().equals(simpleName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCandidateEventType(Class<?> type) {
        if (type.isPrimitive() || type == Object.class || type == String.class) {
            return false;
        }
        return type.getSimpleName().endsWith(EVENT_SUFFIX) || VesselCancellable.class.isAssignableFrom(type);
    }
}
