// src/main/java/corelinks/henry/models/Warp.java
package corelinks.henry.models;

import org.bukkit.Location;

public class Warp {
    private final String name;
    private final Location location;
    // volatile: được đọc/ghi từ nhiều luồng region của Folia
    private volatile String permission;

    public Warp(String name, Location location, String permission) {
        this.name = name;
        this.location = location;
        this.permission = permission;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location.clone();
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean hasPermission() {
        return permission != null && !permission.isBlank();
    }
}