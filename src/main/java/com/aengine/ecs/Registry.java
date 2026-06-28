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
    private final List<Integer> activeEntities = new ArrayList<>(); // Track baseline allocated set tokens

    public int createEntity() {
        int id;
        if (!freeEntities.isEmpty()) {
            id = freeEntities.remove(freeEntities.size() - 1);
            Logger.debug(Logger.System.CORE, "Recycled Entity ID allocation token: %d", id);
        } else {
            id = entityCounter++;
            Logger.debug(Logger.System.CORE, "Allocated absolute Entity ID sequence index: %d", id);
        }
        activeEntities.add(id);
        return id;
    }

    public void destroyEntity(int entity) {
        Logger.debug(Logger.System.CORE, "Initiating global teardown sequence for Entity ID: %d", entity);
        freeEntities.add(entity);
        activeEntities.remove(Integer.valueOf(entity));
        
        for (Map.Entry<Class<?>, ComponentPool<?>> entry : componentPools.entrySet()) {
            entry.getValue().remove(entity);
        }
    }

    /**
     * Fetches a fast iterable collection list filtering matching dynamic component flags.
     * Temporary view strategy before moving to strict automated Bitset grouping layout structures.
     */
    public List<Integer> getEntitiesWith(Class<?>... componentTypes) {
        List<Integer> viewList = new ArrayList<>();
        
        for (int i = 0; i < activeEntities.size(); i++) {
            int entity = activeEntities.get(i);
            boolean match = true;
            
            for (Class<?> type : componentTypes) {
                ComponentPool<?> pool = componentPools.get(type);
                if (pool == null || !pool.has(entity)) {
                    match = false;
                    break;
                }
            }
            
            if (match) {
                viewList.add(entity);
            }
        }
        return viewList;
    }

    @SuppressWarnings("unchecked")
    public <T> void addComponent(int entity, T component) {
        Class<?> type = component.getClass();
        ComponentPool<T> pool = (ComponentPool<T>) componentPools.computeIfAbsent(type, k -> {
            Logger.info(Logger.System.CORE, "Allocating cold infrastructure ComponentPool for type: %s", type.getSimpleName());
            return new ComponentPool<>(type);
        });
        pool.put(entity, component);
    }

    @SuppressWarnings("unchecked")
    public <T> void removeComponent(int entity, Class<T> componentType) {
        ComponentPool<T> pool = (ComponentPool<T>) componentPools.get(componentType);
        if (pool != null) {
            pool.remove(entity);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(int entity, Class<T> componentType) {
        ComponentPool<T> pool = (ComponentPool<T>) componentPools.get(componentType);
        if (pool == null) return null;
        return pool.get(entity);
    }

    @SuppressWarnings("unchecked")
    public <T> ComponentPool<T> getPool(Class<T> componentType) {
        return (ComponentPool<T>) componentPools.get(componentType);
    }
}
