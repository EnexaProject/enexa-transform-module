package org.dice_research.enexa.transform;

/**
 * Supported compression formats.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public enum Compression {

    /**
     * The file won't be compressed.
     */
    NONE,
    /**
     * The file will be compressed using the gzip compression.
     */
    GZIP,
    /**
     * The file will be compressed using the bzip2 compression.
     */
    BZIP2;
}
