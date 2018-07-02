
package org.sonatype.nexus.plugins.cargo.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.cargo.CargoFormat;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.security.RepositoryFormatSecurityContributor;

/**
 * Cargo format security resource.
 */
@Named
@Singleton
public class CargoFormatSecurityContributor
        extends RepositoryFormatSecurityContributor {
    @Inject
    public CargoFormatSecurityContributor(@Named(CargoFormat.NAME) final Format format) {
        super(format);
    }
}
