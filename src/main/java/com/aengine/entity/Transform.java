package com.aengine.entity;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Transform extends Component {

    public final Vector3f position = new Vector3f(0.0f);
    public final Vector3f rotation = new Vector3f(0.0f); // degrees: pitch, yaw, roll
    public final Vector3f scale    = new Vector3f(1.0f);

    public Matrix4f getMatrix() {
        return new Matrix4f()
            .translate(position)
            .rotateX((float) Math.toRadians(rotation.x))
            .rotateY((float) Math.toRadians(rotation.y))
            .rotateZ((float) Math.toRadians(rotation.z))
            .scale(scale);
    }

    public void setPosition(float x, float y, float z) { position.set(x, y, z); }
    public void setRotation(float x, float y, float z) { rotation.set(x, y, z); }
    public void setScale(float x, float y, float z)    { scale.set(x, y, z); }
    public void setScale(float uniform)                { scale.set(uniform, uniform, uniform); }
}
