// src/main/java/corelinks/henry/gui/ConfirmHolder.java
package corelinks.henry.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public record ConfirmHolder(int homeId) implements InventoryHolder {
    @Override
    public Inventory getInventory() {
        return null;
    }
}