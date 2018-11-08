/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo;

import com.vdurmont.semver4j.Semver;

/* CrateCoordinates are a unique identifier for a specific crate instance.
 * Since cargo registries have a flat namespace, only the crate name and version
 * are required.
 */
public class CrateCoordinates
{
    protected String name;

    protected Semver version;

    public CrateCoordinates(String name, Semver version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return this.name;
    }

    public Semver getVersion() {
        return this.version;
    }

    public String getFileBasename() {
        return String.format("%s-%s", this.name, this.version.getValue());
    }
}
