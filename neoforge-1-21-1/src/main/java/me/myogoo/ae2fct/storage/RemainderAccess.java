package me.myogoo.ae2fct.storage;

public interface RemainderAccess {
    interface Host {
        FluidRemainders ae2fct$remainders();
    }

    interface Node {
        FluidRemainders.Slot ae2fct$slot();
        void ae2fct$slot(FluidRemainders.Slot slot);
    }

    interface Network {
        void ae2fct$service(FluidRemainderService service);
    }
}
