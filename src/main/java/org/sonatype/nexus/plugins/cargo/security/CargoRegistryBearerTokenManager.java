
package org.sonatype.nexus.plugins.cargo.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.cargo.CargoRegistryBearerToken;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;
import org.sonatype.nexus.security.token.BearerTokenManager;

@Named
@Singleton
public class CargoRegistryBearerTokenManager extends BearerTokenManager {
    @Inject
    public CargoRegistryBearerTokenManager(final ApiKeyStore apiKeyStore,
            final SecurityHelper securityHelper) {
        super(apiKeyStore, securityHelper, CargoRegistryBearerToken.NAME);
    };

    public String getTokenForCurrentUser() {
        log.debug("Creating cargo token for subject: {}", this.securityHelper.subject().toString());
        return super.createToken(this.securityHelper.subject().getPrincipals());
    }
}
