package cookie04.digitalitems.common.peripherals;

import cookie04.digitalitems.Config;
import cookie04.digitalitems.DigitalItems;
import cookie04.digitalitems.common.luameta.LuaItem;
import cookie04.digitalitems.common.tileentities.ItemDigitizerTileEntity;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;

public class ItemDigitizerPeripheral implements IPeripheral {
    private final ItemDigitizerTileEntity tileEntity;
    public ItemDigitizerPeripheral(ItemDigitizerTileEntity te) {
        this.tileEntity = te;
    }

    IEnergyStorage energy;
    IItemHandler handler;
    ItemStack itemStack;
    boolean powerEnabled;

    private void updateInfo() {
        assert (tileEntity.energy.resolve().isPresent() && tileEntity.handler.resolve().isPresent());
        energy = tileEntity.energy.resolve().get();
        handler = tileEntity.handler.resolve().get();
        itemStack = handler.getStackInSlot(0);
        powerEnabled = Config.ITEM_DIGITIZER_POWER_ENABLED.get();
    }
    @Nonnull
    @Override
    public String getType() {
        return "item_digitizer";
    }

    @Override
    public boolean equals(IPeripheral iPeripheral) {
        return iPeripheral == this;
    }

    @LuaFunction
    public final String digitize() throws LuaException {
        updateInfo();
        if (itemStack.isEmpty())
            throw new LuaException("No item to digitize");

        if (powerEnabled) {
            int digitizeCost = Config.ITEM_DIGITIZER_DIGITIZE_COST.get();
            if (energy.extractEnergy(digitizeCost, true) != digitizeCost) {
                throw new LuaException("Not enough energy to digitize, requires at least: " + digitizeCost);
            } else {
                energy.extractEnergy(digitizeCost, false);
            }
        }

        String key;
        do {
            key = DigitalItems.randomString(10);
        } while (DigitalItems.digital_items.containsKey(key));
        DigitalItems.digital_items.put(key, itemStack.serializeNBT());
        handler.extractItem(0, itemStack.getCount(), false);
        return key;
    }

    @LuaFunction
    public final void materialize(String itemKey) throws LuaException {
        updateInfo();
        if (!DigitalItems.digital_items.containsKey(itemKey))
            throw new LuaException("Invalid item key");

        if (powerEnabled) {
            int rematerializeCost = Config.ITEM_DIGITIZER_REMATERIALIZE_COST.get();
            if (energy.extractEnergy(rematerializeCost, true) != rematerializeCost) {
                throw new LuaException("Not enough energy to rematerialize, requires at least: " + rematerializeCost);
            } else {
                energy.extractEnergy(rematerializeCost, false);
            }
        }
        CompoundNBT nbt = DigitalItems.digital_items.get(itemKey);
        ItemStack insert = handler.insertItem(0, ItemStack.read(nbt), true);
        if (insert.getCount() != 0)
            throw new LuaException("Digitizer not empty and can't merge items");

        handler.insertItem(0, ItemStack.read(nbt), false);
        DigitalItems.digital_items.remove(itemKey);
    }

    @LuaFunction
    public final Map<String, Object> getInfo(Optional<String> itemKey) throws LuaException {
        if (itemKey.isPresent()) {
            if (!DigitalItems.digital_items.containsKey(itemKey))
                throw new LuaException("Invalid item key");

            if (powerEnabled) {
                int checkCost = Config.ITEM_DIGITIZER_CHECK_COST.get();
                if (energy.extractEnergy(checkCost, true) != checkCost) {
                    throw new LuaException("Not enough energy to check, requires at least: " + checkCost);
                } else {
                    energy.extractEnergy(checkCost, false);
                }
            }
            return LuaItem.get(ItemStack.read(DigitalItems.digital_items.get(itemKey)));
        } else {
            updateInfo();
            ItemStack stack = handler.getStackInSlot(0);
            if (stack.isEmpty())
                return null;

            if (powerEnabled) {
                int checkCost = Config.ITEM_DIGITIZER_REMATERIALIZE_COST.get();
                if(energy.extractEnergy(checkCost, true) != checkCost) {
                    throw new LuaException("Not enough energy to check, requires at least: " + checkCost);
                } else {
                    energy.extractEnergy(checkCost, false);
                }
            }
            return LuaItem.get(stack);
        }
    }
}
