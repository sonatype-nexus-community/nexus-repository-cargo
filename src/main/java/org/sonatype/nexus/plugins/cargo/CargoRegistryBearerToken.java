
package org.sonatype.nexus.plugins.cargo;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.token.BearerToken;

@Named(CargoRegistryBearerToken.NAME)
@Singleton
public final class CargoRegistryBearerToken extends BearerToken {
    public static final String NAME = "CargoRegistryBearerToken";

    public CargoRegistryBearerToken() {
        super(NAME);
    }
}
