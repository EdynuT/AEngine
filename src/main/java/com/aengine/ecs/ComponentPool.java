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
        this.denseComponents = (T[]) Array.newInstance(type, 128); // Smaller cold initial footprint
        this.denseToEntity = new int[128];
        this.entityToDense = new int[1024]; // Sparse lookup array starts reasonably sized
        Arrays.fill(entityToDense, -1);
    }

    /**
     * Maps an entity ID to a packed dense array index slot in O(1).
     */
    public void put(int entity, T component) {
        ensureSparseCapacity(entity);

        int existingIndex = entityToDense[entity];
        if (existingIndex != -1) {
            denseComponents[existingIndex] = component;
            return;
        }

        ensureDenseCapacity();

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
            return;
        }

        int indexToRemove = entityToDense[entity];
        int lastIndex = size - 1;

        T lastComponent = denseComponents[lastIndex];
        int lastEntity = denseToEntity[lastIndex];

        // Swap execution sequence
        denseComponents[indexToRemove] = lastComponent;
        denseToEntity[indexToRemove] = lastEntity;
        entityToDense[lastEntity] = indexToRemove;

        // Clean stale reference hooks for GC leverage
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

    public boolean has(int entity) {
        if (entity >= entityToDense.length) return false;
        return entityToDense[entity] != -1;
    }

    /**
     * Resizes the sparse lookup mapping array based strictly on maximum Entity ID bounds.
     */
    private void ensureSparseCapacity(int entity) {
        if (entity >= entityToDense.length) {
            int oldCapacity = entityToDense.length;
            int newLength = Math.max(entity + 1, oldCapacity * 2);
            
            entityToDense = Arrays.copyOf(entityToDense, newLength);
            Arrays.fill(entityToDense, oldCapacity, newLength, -1);
            
            Logger.debug(Logger.System.CORE, "Resized sparse layout tracking matrix. Capacity: %d -> %d", oldCapacity, newLength);
        }
    }

    /**
     * Resizes the dense arrays independently when memory block thresholds are exhausted.
     */
    private void ensureDenseCapacity() {
        if (size >= denseComponents.length) {
            int newLength = denseComponents.length * 2;
            denseComponents = Arrays.copyOf(denseComponents, newLength);
            denseToEntity = Arrays.copyOf(denseToEntity, newLength);
            
            Logger.debug(Logger.System.CORE, "Resized contiguous dense infrastructure for type [%s]: %d allocation blocks.", componentType.getSimpleName(), newLength);
        }
    }

    public int size() { return size; }
    public T[] getRawComponents() { return denseComponents; }
    public int[] getRawDenseToEntity() { return denseToEntity; }
}
