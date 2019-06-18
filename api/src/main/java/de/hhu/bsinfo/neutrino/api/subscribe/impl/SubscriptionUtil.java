package de.hhu.bsinfo.neutrino.api.subscribe.impl;

import de.hhu.bsinfo.neutrino.api.subscribe.Subscriber;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class SubscriptionUtil {

    private static final String SUBSCRIBER_METHOD_NAME = SubscriberMethod.class.getMethods()[0].getName();

    private SubscriptionUtil() {}

    /**
     * Returns all subscriptions present within the target object.
     *
     * @param target The target.
     * @return All subscriptions within the target.
     */
    public static List<Subscription> findSubscriptions(final Subscriber target) {
        return Arrays.stream(target.getClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Subscribe.class))
                .map(method -> convert(target, method))
                .collect(Collectors.toList());
    }

    private static Subscription convert(final Subscriber target, final Method method) {
        final Class<?> declaringClass = method.getDeclaringClass();
        if (!target.getClass().equals(declaringClass)) {
            throw new IllegalSubscriberException("Method {} does not belong to target's class {}",
                    method.getName(), target.getClass().getSimpleName());
        }

        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new IllegalSubscriberException("The subscriber method must have exactly one argument");
        }

        final Class<?> eventClass = parameterTypes[0];

        Subscribe annotation = method.getAnnotation(Subscribe.class);

        method.setAccessible(true);

        try {
            final Lookup lookup = target.lookup();
            final MethodHandle actualMethod = lookup.unreflect(method);
            final MethodType actualType = MethodType.methodType(void.class, eventClass);
            final MethodType interfaceType = MethodType.methodType(void.class, Object.class);
            final MethodType callSiteType = MethodType.methodType(SubscriberMethod.class, declaringClass);
            final CallSite site = LambdaMetafactory.metafactory(
                    lookup,
                    SUBSCRIBER_METHOD_NAME,
                    callSiteType,
                    interfaceType,
                    actualMethod,
                    actualType);

            final MethodHandle factory = site.getTarget();
            final SubscriberMethod subscriberMethod = (SubscriberMethod) factory.bindTo(target).invokeExact();
            return new Subscription(eventClass, subscriberMethod);
        } catch (Throwable e) {
            throw new IllegalSubscriberException("Could not create subscriber method", e);
        }
    }
}
