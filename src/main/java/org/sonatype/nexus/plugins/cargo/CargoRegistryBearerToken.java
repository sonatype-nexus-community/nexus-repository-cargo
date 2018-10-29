/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License Version 1.0, which accompanies this
 * distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
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
