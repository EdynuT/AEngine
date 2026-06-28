package com.aengine.ecs;

import com.aengine.utils.Logger;
import java.lang.reflect.Array;
import java.util.Arrays;

public final class ComponentPool<T> {

    private final Class<?> componentType;
    private T[] denseComponents;
    private int[] denseToEntity;
    private int[] entityToDense;
    private int size = 0;

    @SuppressWarnings("unchecked")
    public ComponentPool(Class<?> type) {
        this.componentType = type;
        this.denseComponents = (T[]) Array.newInstance(type, 1024);
        this.denseToEntity = new int[1024];
        this.entityToDense = new int[1024];
        Arrays.fill(entityToDense, -1);
    }

    /**
     * Maps an entity ID to a packed dense array index slot in O(1).
     */
    public void put(int entity, T component) {
        ensureCapacity(entity);

        if (entityToDense[entity] != -1) {
            Logger.trace(Logger.System.CORE, "Overwriting component data at dense index: %d for Entity: %d", entityToDense[entity], entity);
            denseComponents[entityToDense[entity]] = component;
            return;
        }

        int index = size;
        denseComponents[index] = component;
        denseToEntity[index] = entity;
        entityToDense[entity] = index;
        size++;
        
        Logger.trace(Logger.System.CORE, "Packed component type [%s] at dense storage slot: %d for Entity: %d", componentType.getSimpleName(), index, entity);
    }

    /**
     * Unmaps an entity and swaps the last element of the dense array to the deleted slot
     * to preserve absolute contiguous sequence lines (Unordered Fast-Delete Pattern).
     */
    public void remove(int entity) {
        if (entity >= entityToDense.length || entityToDense[entity] == -1) {
            Logger.warn(Logger.System.CORE, "Bypassing component data removal. Entity ID %d has no binding in pool: %s", entity, componentType.getSimpleName());
            return;
        }

        int indexToRemove = entityToDense[entity];
        int lastIndex = size - 1;

        // Move last element into the deleted element slot position
        T lastComponent = denseComponents[lastIndex];
        int lastEntity = denseToEntity[lastIndex];

        denseComponents[indexToRemove] = lastComponent;
        denseToEntity[indexToRemove] = lastEntity;
        entityToDense[lastEntity] = indexToRemove;

        // Nullify trailing dangling pointers
        denseComponents[lastIndex] = null;
        entityToDense[entity] = -1;
        size--;
        
        Logger.trace(Logger.System.CORE, "Swapped trailing dense index %d to index %d to evict component for Entity: %d", lastIndex, indexToRemove, entity);
    }

    public T get(int entity) {
        if (entity >= entityToDense.length || entityToDense[entity] == -1) {
            return null;
        }
        return denseComponents[entityToDense[entity]];
    }

    private void ensureCapacity(int entity) {
        if (entity >= entityToDense.length) {
            int oldCapacity = entityToDense.length;
            int newLength = Math.max(entity + 1, oldCapacity * 2);
            
            Logger.debug(Logger.System.CORE, "Resizing component sparse lookup hardware mapping table. Capacity: %d -> %d", oldCapacity, newLength);
            
            entityToDense = Arrays.copyOf(entityToDense, newLength);
            denseToEntity = Arrays.copyOf(denseToEntity, newLength);
            denseComponents = Arrays.copyOf(denseComponents, newLength);
            Arrays.fill(entityToDense, oldCapacity, newLength, -1);
        }
    }

    public int size() { return size; }
    public T[] getRawComponents() { return denseComponents; }
    public int[] getRawDenseToEntity() { return denseToEntity; }
}
