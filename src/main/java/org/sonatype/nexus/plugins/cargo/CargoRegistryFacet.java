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

import java.io.IOException;
import java.io.InputStream;

import com.google.gson.JsonElement;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Response;

@Exposed
public interface CargoRegistryFacet
        extends Facet
{
    public void writeConfigJson() throws Exception;

    public void rebuildIndexForCrate(CrateCoordinates crateId) throws IOException;

    public Response publishCrate(CrateCoordinates crateId,
                                 JsonElement metadata,
                                 InputStream tarball) throws IOException;

    public Content downloadMetadata(CrateCoordinates crateId) throws IOException;

    public Content downloadTarball(CrateCoordinates crateId) throws IOException;
}
