package com.aengine.ecs;

import com.aengine.utils.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Registry {

    private int entityCounter = 0;
    private final List<Integer> freeEntities = new ArrayList<>();
    private final Map<Class<?>, ComponentPool<?>> componentPools = new HashMap<>();

    /**
     * Spawns a new entity ID, recycling dead IDs if available to prevent overflow.
     */
    public int createEntity() {
        if (!freeEntities.isEmpty()) {
            int recycled = freeEntities.remove(freeEntities.size() - 1);
            Logger.debug(Logger.System.CORE, "Recycled Entity ID allocation token: %d", recycled);
            return recycled;
        }
        int id = entityCounter++;
        Logger.debug(Logger.System.CORE, "Allocated absolute Entity ID sequence index: %d", id);
        return id;
    }

    /**
     * Kills an entity and releases its ID back to the pool for future recycling.
     */
    public void destroyEntity(int entity) {
        Logger.debug(Logger.System.CORE, "Initiating global teardown sequence for Entity ID: %d", entity);
        freeEntities.add(entity);
        
        for (Map.Entry<Class<?>, ComponentPool<?>> entry : componentPools.entrySet()) {
            entry.getValue().remove(entity);
        }
    }

    /**
     * Registers a component type pool if it does not exist, and assigns a component instance to an entity.
     */
    @SuppressWarnings("unchecked")
    public <T> void addComponent(int entity, T component) {
        Class<?> type = component.getClass();
        Logger.trace(Logger.System.CORE, "Binding component type mapping [%s] to Entity ID: %d", type.getSimpleName(), entity);
        
        ComponentPool<T> pool = (ComponentPool<T>) componentPools.computeIfAbsent(type, k -> {
            Logger.info(Logger.System.CORE, "Allocating cold infrastructure ComponentPool for type: %s", type.getSimpleName());
            return new ComponentPool<>(type);
        });
        pool.put(entity, component);
    }

    /**
     * Removes a component assignment from an entity.
     */
    @SuppressWarnings("unchecked")
    public <T> void removeComponent(int entity, Class<T> componentType) {
        Logger.trace(Logger.System.CORE, "Unbinding component type mapping [%s] from Entity ID: %d", componentType.getSimpleName(), entity);
        ComponentPool<T> pool = (ComponentPool<T>) componentPools.get(componentType);
        if (pool != null) {
            pool.remove(entity);
        }
    }

    /**
     * Direct O(1) retrieval of a component instance from its contiguously packed pool.
     */
    @SuppressWarnings("unchecked")
    public <T> T getComponent(int entity, Class<T> componentType) {
        ComponentPool<T> pool = (ComponentPool<T>) componentPools.get(componentType);
        if (pool == null) {
            Logger.trace(Logger.System.CORE, "Component query lookup missed. No pool register for type: %s", componentType.getSimpleName());
            return null;
        }
        return pool.get(entity);
    }

    /**
     * Returns the raw internal pool instance for direct System iteration.
     */
    @SuppressWarnings("unchecked")
    public <T> ComponentPool<T> getPool(Class<T> componentType) {
        return (ComponentPool<T>) componentPools.get(componentType);
    }
}
