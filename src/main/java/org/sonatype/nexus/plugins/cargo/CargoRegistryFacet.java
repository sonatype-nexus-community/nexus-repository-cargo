
package org.sonatype.nexus.plugins.cargo;

import java.io.IOException;
import java.io.InputStream;

import com.google.gson.JsonElement;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Response;

@Exposed
public interface CargoRegistryFacet extends Facet {
    public void writeConfigJson() throws Exception;
    
    public void rebuildIndexForCrate(CrateCoordinates crateId) throws IOException;

    public Response publishCrate(CrateCoordinates crateId, JsonElement metadata,
            InputStream tarball)
            throws IOException;

    public Content downloadMetadata(CrateCoordinates crateId) throws IOException;

    public Content downloadTarball(CrateCoordinates crateId) throws IOException;
}
