package com.aengine.entity;

public abstract class Component {

    protected Entity owner;

    public void setOwner(Entity owner) { this.owner = owner; }
    public Entity getOwner()           { return owner; }

    public void update(float deltaTime) {}
}
