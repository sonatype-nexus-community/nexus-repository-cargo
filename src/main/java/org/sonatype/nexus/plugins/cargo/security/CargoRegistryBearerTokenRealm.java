
package org.sonatype.nexus.plugins.cargo.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.sisu.Description;
import org.sonatype.nexus.plugins.cargo.CargoRegistryBearerToken;
import org.sonatype.nexus.security.UserPrincipalsHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;
import org.sonatype.nexus.security.token.BearerTokenRealm;

@Named(CargoRegistryBearerToken.NAME)
@Singleton
@Description("Cargo Registry Bearer Token Realm")
public final class CargoRegistryBearerTokenRealm extends BearerTokenRealm {
    @Inject
    public CargoRegistryBearerTokenRealm(final ApiKeyStore keyStore,
            final UserPrincipalsHelper principalsHelper) {
        super(keyStore, principalsHelper, CargoRegistryBearerToken.NAME);
    }
}
