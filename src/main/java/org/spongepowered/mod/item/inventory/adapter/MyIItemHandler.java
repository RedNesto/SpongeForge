package org.spongepowered.mod.item.inventory.adapter;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

public class MyIItemHandler implements IItemHandler {

    @Override public int getSlots() {
        return 7;
    }

    @Nonnull @Override public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Nonnull @Override public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Nonnull @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override public int getSlotLimit(int slot) {
        return 0;
    }
}
