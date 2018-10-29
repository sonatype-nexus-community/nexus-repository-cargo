/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.token.BearerToken;

@Named(CargoRegistryBearerToken.NAME)
@Singleton
public final class CargoRegistryBearerToken
        extends BearerToken
{
    public static final String NAME = "CargoRegistryBearerToken";

    public CargoRegistryBearerToken() {
        super(NAME);
    }
}
