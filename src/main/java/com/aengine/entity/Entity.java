package com.aengine.entity;

import java.util.ArrayList;
import java.util.List;

public class Entity {

    private final String          name;
    private final List<Component> components = new ArrayList<>();
    public  final Transform       transform  = new Transform();
    private       boolean         active     = true;

    public Entity(String name) {
        this.name = name;
        transform.setOwner(this);
        components.add(transform);
    }

    public Entity() { this("Entity"); }

    public <T extends Component> T addComponent(T component) {
        component.setOwner(this);
        components.add(component);
        return component;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> type) {
        for (Component c : components) {
            if (type.isInstance(c)) return (T) c;
        }
        return null;
    }

    public void update(float deltaTime) {
        if (!active) return;
        for (Component c : components) c.update(deltaTime);
    }

    public String  getName()   { return name; }
    public boolean isActive()  { return active; }
    public void    setActive(boolean active) { this.active = active; }
}
