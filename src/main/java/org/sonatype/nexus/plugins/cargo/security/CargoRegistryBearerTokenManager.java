/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

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
public class CargoRegistryBearerTokenManager
        extends BearerTokenManager
{
    @Inject
    public CargoRegistryBearerTokenManager(final ApiKeyStore apiKeyStore, final SecurityHelper securityHelper) {
        super(apiKeyStore, securityHelper, CargoRegistryBearerToken.NAME);
    };

    public String getTokenForCurrentUser() {
        log.debug("Creating cargo token for subject: {}", this.securityHelper.subject().toString());
        return super.createToken(this.securityHelper.subject().getPrincipals());
    }
}
