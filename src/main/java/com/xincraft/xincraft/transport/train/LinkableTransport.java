package com.xincraft.xincraft.transport.train;

/**
 * @brief A minecart or boat a linking bar can hitch into a train.
 *
 * Each one holds a LinkedTransport and hands it its ticks, interactions, pushes, removal and
 * saving.
 *
 * @see LinkedTransport
 */
public interface LinkableTransport {
    LinkedTransport transport();

    /**
     * @brief How many vehicles this one can tow behind it when it leads a train, counting every one
     *        down the line.
     *
     * Only locomotives and motorboats can lead.
     *
     * @return The most vehicles it can tow. Only locomotives and motorboats can lead, so this
     *     defaults to 0.
     */
    default int maxTowed() {
        return 0;
    }
}
