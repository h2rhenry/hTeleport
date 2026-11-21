// src/main/java/corelinks/henry/models/TpaRequest.java
package corelinks.henry.models;

import java.util.UUID;

public class TpaRequest {

    public enum Type {
        TPA,
        TPHERE
    }

    private final UUID sender;
    private final UUID target;
    private final Type type;
    private final long createdAt;

    public TpaRequest(UUID sender, UUID target, Type type) {
        this.sender = sender;
        this.target = target;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getTarget() {
        return target;
    }

    public Type getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}