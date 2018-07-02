package org.sonatype.nexus.plugins.cargo;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;

/**
 * Cargo repository format.
 */
@Named(CargoFormat.NAME)
@Singleton
public class CargoFormat
        extends Format {
    public static final String NAME = "cargo";

    public CargoFormat() {
        super(NAME);
    }
}
