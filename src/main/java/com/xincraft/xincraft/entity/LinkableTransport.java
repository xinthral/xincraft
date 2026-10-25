package com.xincraft.xincraft.entity;

// A minecart or boat a linking bar can hitch into a train (see LinkedTransport). Each one holds a
// LinkedTransport and hands it its ticks, interactions, pushes, removal and saving. The ender minecart
// deliberately isn't one: there's no good way to teleport a whole linked train through an ender rail.
public interface LinkableTransport {
    LinkedTransport transport();

    // How many vehicles this one can tow behind it when it leads a train, counting every one down
    // the line: only locomotives and motorboats can lead
    default int maxTowed() {
        return 0;
    }
}
