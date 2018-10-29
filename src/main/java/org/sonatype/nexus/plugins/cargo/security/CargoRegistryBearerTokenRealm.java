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

import org.eclipse.sisu.Description;
import org.sonatype.nexus.plugins.cargo.CargoRegistryBearerToken;
import org.sonatype.nexus.security.UserPrincipalsHelper;
import org.sonatype.nexus.security.authc.apikey.ApiKeyStore;
import org.sonatype.nexus.security.token.BearerTokenRealm;

@Named(CargoRegistryBearerToken.NAME)
@Singleton
@Description("Cargo Registry Bearer Token Realm")
public final class CargoRegistryBearerTokenRealm
        extends BearerTokenRealm
{
    @Inject
    public CargoRegistryBearerTokenRealm(final ApiKeyStore keyStore, final UserPrincipalsHelper principalsHelper) {
        super(keyStore, principalsHelper, CargoRegistryBearerToken.NAME);
    }
}
