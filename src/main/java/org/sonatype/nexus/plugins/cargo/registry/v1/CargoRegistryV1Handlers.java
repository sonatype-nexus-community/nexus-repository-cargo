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

package org.sonatype.nexus.plugins.cargo.registry.v1;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.plugins.cargo.CargoRegistryFacet;
import org.sonatype.nexus.plugins.cargo.CrateCoordinates;
import org.sonatype.nexus.plugins.cargo.git.GitRepositoryHandlers;
import org.sonatype.nexus.plugins.cargo.registry.assets.AssetKindMetadataAttributes;
import org.sonatype.nexus.plugins.cargo.registry.assets.AssetKindTarballAttributes;
import org.sonatype.nexus.plugins.cargo.registry.assets.ComponentKindCrateAttributes;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.security.SecuritySystem;

import com.google.common.base.Preconditions;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vdurmont.semver4j.Semver;
import org.apache.commons.io.input.BoundedInputStream;

public final class CargoRegistryV1Handlers
{

    private static abstract class CrateHandlerSupport
            extends ComponentSupport
            implements Handler
    {
        protected final ComponentKindCrateAttributes crateAttributes;

        protected final AssetKindMetadataAttributes metadataAttributes;

        protected final AssetKindTarballAttributes tarballAttributes;

        @Inject
        protected CrateHandlerSupport(ComponentKindCrateAttributes crateAttributes,
                                      AssetKindMetadataAttributes metadataAttributes,
                                      AssetKindTarballAttributes tarballAttributes)
        {
            this.crateAttributes = crateAttributes;
            this.metadataAttributes = metadataAttributes;
            this.tarballAttributes = tarballAttributes;
        }
    }

    @Named
    public static class Publish
            extends CrateHandlerSupport
    {

        @Inject
        public Publish(ComponentKindCrateAttributes crateAttributes,
                       AssetKindMetadataAttributes metadataAttributes,
                       AssetKindTarballAttributes tarballAttributes)
        {
            super(crateAttributes, metadataAttributes, tarballAttributes);
        }

        @Override
        public Response handle(Context context) throws Exception {
            CargoRegistryFacet cargoImpl = context.getRepository().facet(CargoRegistryFacet.class);

            // Cargo sends a single-part body containing both metadata in JSON
            // and the actual crate in a tarball. Each part is prefixed with a
            // 32-bit little-endian length identifier. Split off the JSON and
            // turn the tarball into a Blob as the former needs to be parsed
            // before mapping onto an Asset while the latter is simply stored.
            LittleEndianDataInputStream requestBody =
                    new LittleEndianDataInputStream(context.getRequest().getPayload().openInputStream());
            try {
                int jsonLength = requestBody.readInt();
                byte[] jsonBytes = new byte[jsonLength];
                requestBody.readFully(jsonBytes);
                int tarballLength = requestBody.readInt();
                InputStream tarball = new BoundedInputStream(requestBody, tarballLength);

                try {
                    // Parse the metadata into a JSON object.
                    JsonParser jsonParser = new JsonParser();
                    JsonElement json = jsonParser.parse(new InputStreamReader(new ByteArrayInputStream(jsonBytes)));
                    if (!json.isJsonObject()) {
                        return HttpResponses.badRequest("Expected JSON portion to contain an object");
                    }
                    JsonObject publishRequest = json.getAsJsonObject();

                    // Use the provided name and verison to construct an identifier.
                    String crateName = publishRequest.get("name").getAsString();
                    Semver crateVersion = new Semver(publishRequest.get("vers").getAsString());
                    CrateCoordinates crateId = new CrateCoordinates(crateName, crateVersion);

                    Response response = cargoImpl.publishCrate(crateId, publishRequest, tarball);
                    if (response.getStatus().isSuccessful()) {
                        cargoImpl.rebuildIndexForCrate(crateId);
                    }
                    return response;
                }
                finally {
                    tarball.close();
                }
            }
            finally {
                requestBody.close();
            }
        }
    };

    /**
     * Fetches the crate for a download request. Requires a TokenMatcher that provides the following
     * tokens: - name: Name of the crate to download - version: Version of the crate to download
     * Returns: HttpResponses.ok if the crate is found, HttpResponses.notFound otherwise.
     */
    @Named
    public static class CrateDownload
            extends CrateHandlerSupport
    {
        @Inject
        public CrateDownload(ComponentKindCrateAttributes crateAttributes,
                             AssetKindMetadataAttributes metadataAttributes,
                             AssetKindTarballAttributes tarballAttributes)
        {
            super(crateAttributes, metadataAttributes, tarballAttributes);
        }

        @Override
        public Response handle(@Nonnull final Context context) throws Exception {
            TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
            String name = state.getTokens().get("name");
            String version = state.getTokens().get("version");

            Preconditions.checkNotNull(name, "Download handler called without a 'name' token");
            Preconditions.checkNotNull(version, "Download handler called without a 'version' token");

            CrateCoordinates coords = new CrateCoordinates(name, new Semver(version));
            CargoRegistryFacet crates = context.getRepository().facet(CargoRegistryFacet.class);
            Content content = crates.downloadTarball(coords);
            if (content != null) {
                return HttpResponses.ok(content);
            }
            else {
                return HttpResponses.notFound("Crate '" + name + "-" + version + "' not found");
            }
        }
    };

    /**
     * Fetches the metdata for a crate. Requires a TokenMatcher that provides the following tokens:
     * - name: Name of the crate to download - version: Version of the crate to download Returns:
     * HttpResponses.ok if the crate is found, HttpResponses.notFound otherwise.
     */
    @Named
    public static class MetadataDownload
            extends CrateHandlerSupport
    {
        @Inject
        public MetadataDownload(ComponentKindCrateAttributes crateAttributes,
                                AssetKindMetadataAttributes metadataAttributes,
                                AssetKindTarballAttributes tarballAttributes)
        {
            super(crateAttributes, metadataAttributes, tarballAttributes);
        }

        @Override
        public Response handle(@Nonnull final Context context) throws Exception {
            TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
            String name = state.getTokens().get("name");
            String version = state.getTokens().get("version");

            Preconditions.checkNotNull(name, "Download handler called without a 'name' token");
            Preconditions.checkNotNull(version, "Download handler called without a 'version' token");

            CrateCoordinates coords = new CrateCoordinates(name, new Semver(version));
            CargoRegistryFacet crates = context.getRepository().facet(CargoRegistryFacet.class);
            Content content = crates.downloadMetadata(coords);
            if (content != null) {
                return HttpResponses.ok(content);
            }
            else {
                return HttpResponses.notFound("Crate '" + name + "-" + version + "' not found");
            }
        }
    };

    @Named
    public static class IndexInfoRefs
            extends GitRepositoryHandlers.InfoRefs
    {
        @Override
        public Response handle(Context context) throws Exception {
            context.getRepository().facet(CargoRegistryFacet.class).writeConfigJson();
            return super.handle(context);
        }
    }

    @Named
    public static class IndexRecievePackService
            extends GitRepositoryHandlers.ReceivePackService
    {
        @Inject
        public IndexRecievePackService(SecuritySystem security) {
            super(security);
        }

        @Override
        public Response handle(Context context) throws Exception {
            context.getRepository().facet(CargoRegistryFacet.class).writeConfigJson();
            return super.handle(context);
        }
    }

    @Named
    public static class IndexUploadPackService
            extends GitRepositoryHandlers.UploadPackService
    {
        @Override
        public Response handle(Context context) throws Exception {
            context.getRepository().facet(CargoRegistryFacet.class).writeConfigJson();
            return super.handle(context);
        }
    }
}
