package dev.vessel.api.event;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Dynamic proxy invocation handler routing Fabric/Quilt synthetic invoker calls to {@link SovereignEventBus}.
 * <p>
 * Dedicated standalone class adhering strictly to Rule 17 (zero inner/anonymous classes).
 *
 * @version 1.0.0
 */
public final class SyntheticEventInvokerProxy implements InvocationHandler {

    private final SovereignEventBus eventBus;
    private final Class<?> eventInterface;

    public SyntheticEventInvokerProxy(SovereignEventBus eventBus, Class<?> eventInterface) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus cannot be null");
        this.eventInterface = Objects.requireNonNull(eventInterface, "eventInterface cannot be null");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if ("equals".equals(methodName) && args != null && args.length == 1) {
            return proxy == args[0];
        }
        if ("hashCode".equals(methodName)) {
            return System.identityHashCode(proxy);
        }
        if ("toString".equals(methodName)) {
            return "SyntheticEventInvoker[" + eventInterface.getName() + "]";
        }

        if (args != null && args.length == 1 && args[0] != null) {
            eventBus.dispatch(args[0]);
        }
        return null;
    }
}
