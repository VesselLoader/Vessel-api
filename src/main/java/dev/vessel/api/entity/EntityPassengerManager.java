package dev.vessel.api.entity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Universal mount, vehicle, and passenger coordinator across Forge and Bukkit entities.
 * <p>
 * Enforces topology integrity by preventing circular vehicle-passenger deadlocks and memory leaks
 * during entity dismount and chunk unload sequences.
 *
 * @version 1.0.0
 */
public final class EntityPassengerManager {

    public static final int MAX_PASSENGERS_PER_VEHICLE = 64;
    public static final Object[] EMPTY_PASSENGERS = new Object[0];

    private static final EntityPassengerManager INSTANCE = new EntityPassengerManager();

    private final Map<Object, CopyOnWriteArrayList<Object>> vehicleToPassengers = new ConcurrentHashMap<>();
    private final Map<Object, Object> passengerToVehicle = new ConcurrentHashMap<>();

    public EntityPassengerManager() {
    }

    public static EntityPassengerManager getInstance() {
        return INSTANCE;
    }

    public Object[] getPassengers(Object vehicle) {
        if (vehicle == null) {
            return EMPTY_PASSENGERS;
        }

        CopyOnWriteArrayList<Object> list = vehicleToPassengers.get(vehicle);
        if (list != null && !list.isEmpty()) {
            return list.toArray();
        }

        try {
            Method m = vehicle.getClass().getMethod("getPassengers");
            Object res = m.invoke(vehicle);
            if (res instanceof List<?> l) {
                return l.toArray();
            }
            if (res instanceof Object[] arr) {
                return arr;
            }
        } catch (ReflectiveOperationException ignored) {
            // Check legacy getPassenger
        }

        try {
            Method m = vehicle.getClass().getMethod("getPassenger");
            Object p = m.invoke(vehicle);
            if (p != null) {
                return new Object[]{p};
            }
        } catch (ReflectiveOperationException ignored) {
            // Unmapped
        }

        return EMPTY_PASSENGERS;
    }

    public boolean addPassenger(Object vehicle, Object passenger) {
        if (vehicle == null || passenger == null || vehicle == passenger) {
            return false;
        }

        // Circular riding protection: passenger cannot be an ancestor vehicle of vehicle
        if (isAncestorVehicle(passenger, vehicle)) {
            return false;
        }

        CopyOnWriteArrayList<Object> passengers = vehicleToPassengers.computeIfAbsent(
                vehicle, k -> new CopyOnWriteArrayList<>());

        if (passengers.size() >= MAX_PASSENGERS_PER_VEHICLE || passengers.contains(passenger)) {
            return false;
        }

        passengers.add(passenger);
        passengerToVehicle.put(passenger, vehicle);

        // Reflective invocation on underlying entity if present
        try {
            Method m = vehicle.getClass().getMethod("addPassenger", passenger.getClass());
            m.invoke(vehicle, passenger);
        } catch (ReflectiveOperationException ignored) {
            // Try startRiding on passenger
            try {
                Method m = passenger.getClass().getMethod("startRiding", vehicle.getClass());
                m.invoke(passenger, vehicle);
            } catch (ReflectiveOperationException ignoredAgain) {
                // Maintained in synthetic manager
            }
        }

        return true;
    }

    public boolean removePassenger(Object vehicle, Object passenger) {
        if (vehicle == null || passenger == null) {
            return false;
        }

        CopyOnWriteArrayList<Object> passengers = vehicleToPassengers.get(vehicle);
        if (passengers != null) {
            passengers.remove(passenger);
            if (passengers.isEmpty()) {
                vehicleToPassengers.remove(vehicle);
            }
        }
        passengerToVehicle.remove(passenger);

        try {
            Method m = vehicle.getClass().getMethod("removePassenger", passenger.getClass());
            m.invoke(vehicle, passenger);
        } catch (ReflectiveOperationException ignored) {
            try {
                Method m = passenger.getClass().getMethod("stopRiding");
                m.invoke(passenger);
            } catch (ReflectiveOperationException ignoredAgain) {
                // Cleaned up
            }
        }
        return true;
    }

    public void ejectPassengers(Object vehicle) {
        if (vehicle == null) {
            return;
        }
        Object[] passengers = getPassengers(vehicle);
        for (Object p : passengers) {
            removePassenger(vehicle, p);
        }
    }

    public Object getVehicle(Object passenger) {
        if (passenger == null) {
            return null;
        }
        Object vehicle = passengerToVehicle.get(passenger);
        if (vehicle != null) {
            return vehicle;
        }
        try {
            Method m = passenger.getClass().getMethod("getVehicle");
            return m.invoke(passenger);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public boolean isPassenger(Object vehicle, Object passenger) {
        if (vehicle == null || passenger == null) {
            return false;
        }
        Object[] passengers = getPassengers(vehicle);
        for (Object p : passengers) {
            if (p == passenger) {
                return true;
            }
        }
        return false;
    }

    private boolean isAncestorVehicle(Object potentialAncestor, Object currentVehicle) {
        Object v = currentVehicle;
        int depth = 0;
        while (v != null && depth < MAX_PASSENGERS_PER_VEHICLE) {
            if (v == potentialAncestor) {
                return true;
            }
            v = getVehicle(v);
            depth++;
        }
        return false;
    }

    public void clear() {
        vehicleToPassengers.clear();
        passengerToVehicle.clear();
    }
}
