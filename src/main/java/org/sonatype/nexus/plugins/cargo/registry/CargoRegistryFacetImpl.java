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

package org.sonatype.nexus.plugins.cargo.registry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import com.google.common.eventbus.Subscribe;
import com.google.common.hash.HashCode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.eclipse.jgit.lib.Constants;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.plugins.cargo.CargoRegistryFacet;
import org.sonatype.nexus.plugins.cargo.CrateCoordinates;
import org.sonatype.nexus.plugins.cargo.GitRepositoryFacet;
import org.sonatype.nexus.plugins.cargo.git.repo.Repository;
import org.sonatype.nexus.plugins.cargo.registry.assets.AssetKindMetadataAttributes;
import org.sonatype.nexus.plugins.cargo.registry.assets.AssetKindTarballAttributes;
import org.sonatype.nexus.plugins.cargo.registry.assets.ComponentKindCrateAttributes;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.RepositoryStartedEvent;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.repository.view.Content.CONTENT_HASH_CODES_MAP;

/**
 * A {@link CargoRegistryFacet} that persists Cargo crates and metadata via {@link StorageFacet}.
 */

@Named
public class CargoRegistryFacetImpl
        extends FacetSupport
        implements CargoRegistryFacet
{
    private final EmailManager emailManager;

    private final ComponentKindCrateAttributes crateAttributes;

    private final AssetKindTarballAttributes tarballAttributes;

    private final AssetKindMetadataAttributes metadataAttributes;

    private final String indexBranch = Constants.R_HEADS + Constants.MASTER;

    static final String CONFIG_KEY = "cargo";

    static class Config
    {
        @NotNull(groups = {HostedType.ValidationGroup.class})
        // @Url(groups = { HostedType.ValidationGroup.class })
        public URI allowedRegistries;
    }

    private Config config;

    @Inject
    protected CargoRegistryFacetImpl(EmailManager emailManager,
                                     ComponentKindCrateAttributes crateAttributes,
                                     AssetKindTarballAttributes tarballAttributes,
                                     AssetKindMetadataAttributes metadataAttributes)
    {
        this.emailManager = emailManager;
        this.crateAttributes = crateAttributes;
        this.tarballAttributes = tarballAttributes;
        this.metadataAttributes = metadataAttributes;
    }

    @Override
    protected void doValidate(final Configuration configuration) throws Exception {
        facet(ConfigurationFacet.class).validateSection(configuration, CONFIG_KEY, Config.class, Default.class,
                getRepository().getType().getValidationGroup());
    }

    @Override
    protected void doConfigure(final Configuration configuration) throws Exception {
        config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);
    }

    @Override
    protected void doDestroy() throws Exception {
        config = null;
    }

    @Subscribe
    public void initializeGitRepo(RepositoryStartedEvent event) throws IOException {
        UnitOfWork.begin(this.getRepository().facet(StorageFacet.class).txSupplier());
        // Create a git repo if one doesn't already exist.
        GitRepositoryFacet gitFacet = this.getRepository().facet(GitRepositoryFacet.class);
        Repository indexRepo = gitFacet.getGitRepository("index");
        if (indexRepo == null) {
            indexRepo = gitFacet.createGitRepository("index");
        }
    }

    @Override
    @TransactionalStoreMetadata
    @TransactionalStoreBlob
    public void writeConfigJson() throws Exception {
        // Check that the repo URL is known. When a fixed URL is not set, the
        // URL is detected from the request URL and can be null until a request
        // with a Host header is seen.
        String requestUrl = this.getRepository().getUrl();
        if (requestUrl == null)
            return;

        GitRepositoryFacet gitFacet = this.getRepository().facet(GitRepositoryFacet.class);
        Repository indexRepo = gitFacet.getGitRepository("index");

        // Generate config.json with the current repository URL
        JsonObject config = new JsonObject();
        config.addProperty("dl", this.getRepository().getUrl() + "/api/v1/crates");
        config.addProperty("api", this.getRepository().getUrl());
        JsonArray allowedRegistries = new JsonArray();
        allowedRegistries.add(new JsonPrimitive(this.config.allowedRegistries.toString()));
        config.add("allowed-registries", allowedRegistries);

        gitFacet.replaceFile(indexRepo, this.indexBranch, "config.json",
                config.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @TransactionalTouchBlob
    @TransactionalStoreMetadata
    public void rebuildIndexForCrate(CrateCoordinates crateId) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();
        Bucket bucket = tx.findBucket(this.getRepository());

        ByteArrayOutputStream crate_index_entry = new ByteArrayOutputStream();

        for (Component component : this.crateAttributes.findAllVersions(this.getRepository(),
                crateId.getName())) {
            Content crate_metadata_json = this.metadataAttributes.getAssetContent(bucket,
                    component);

            final AttributesMap attributesMap = tarballAttributes.getAssetContent(bucket, component).getAttributes();
            //noinspection unchecked
            Map<HashAlgorithm, HashCode> hashMap = (Map<HashAlgorithm, HashCode>) attributesMap.get(CONTENT_HASH_CODES_MAP);
            HashCode sha256Hash = Objects.requireNonNull(hashMap).get(HashAlgorithm.SHA256);

            try (InputStream is = crate_metadata_json.openInputStream()) {
                try (InputStreamReader reader = new InputStreamReader(is)) {
                    JsonObject sourceManifest = new JsonParser().parse(reader).getAsJsonObject();

                    JsonObject result = new JsonObject();
                    result.addProperty("name", sourceManifest.get("name").getAsString());
                    result.addProperty("vers", sourceManifest.get("vers").getAsString());
                    result.add("features", sourceManifest.get("features").getAsJsonObject());
                    result.addProperty("cksum", sha256Hash.toString());

                    JsonArray deps = new JsonArray();
                    for (JsonElement element: sourceManifest.getAsJsonArray("deps")){
                        JsonObject sourceObj = element.getAsJsonObject();
                        JsonObject obj = new JsonObject();
                        obj.addProperty("name", sourceObj.get("name").getAsString());
                        obj.addProperty("req", sourceObj.get("version_req").getAsString());
                        obj.add("features", sourceObj.get("features").getAsJsonArray());
                        obj.addProperty("optional", sourceObj.get("optional").getAsBoolean());
                        obj.addProperty("default_features", sourceObj.get("default_features").getAsBoolean());
                        obj.addProperty("kind", "normal");
                        obj.addProperty("registry", sourceObj.get("registry").getAsString());
                        deps.add(obj);
                    }
                    result.add("deps", deps);

                    result.addProperty("yanked", false);
                    crate_index_entry.write(result.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            crate_index_entry.write('\n');
        }

        GitRepositoryFacet gitFacet = this.getRepository().facet(GitRepositoryFacet.class);
        Repository indexRepo = gitFacet.getGitRepository("index");

        gitFacet.replaceFile(indexRepo, this.indexBranch, crateId.getIndexEntryPath(),
                crate_index_entry.toByteArray());
    }

    @Override
    @TransactionalStoreMetadata
    @TransactionalStoreBlob
    public Response publishCrate(CrateCoordinates crateId,
                                 JsonElement metadata,
                                 InputStream tarball) throws IOException
    {
        StorageTx tx = UnitOfWork.currentTx();
        Bucket bucket = tx.findBucket(this.getRepository());

        // Abort if the request is for an existing crate.
        Component crateComponent = this.crateAttributes.find(this.getRepository(), crateId);
        if (crateComponent != null) {
            return HttpResponses.forbidden("Crate version already exists");

        }

        crateComponent = this.crateAttributes.create(this.getRepository(), crateId);

        this.metadataAttributes.createAsset(bucket, crateComponent,
                new ByteArrayInputStream(new Gson().toJson(metadata).getBytes(StandardCharsets.UTF_8)));
        this.tarballAttributes.createAsset(bucket, crateComponent, tarball);

        return HttpResponses.ok();
    }

    @Override
    @TransactionalTouchMetadata
    @TransactionalTouchBlob
    public Content downloadMetadata(CrateCoordinates crateId) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();

        Bucket bucket = tx.findBucket(this.getRepository());
        Component component = crateAttributes.find(this.getRepository(), crateId);
        if (component == null) {
            return null;
        }

        return metadataAttributes.getAssetContent(bucket, component);
    }

    @Override
    @TransactionalTouchMetadata
    @TransactionalTouchBlob
    public Content downloadTarball(CrateCoordinates crateId) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();

        Bucket bucket = tx.findBucket(this.getRepository());
        Component component = crateAttributes.find(this.getRepository(), crateId);
        if (component == null)
            return null;

        return tarballAttributes.getAssetContent(bucket, component);
    }
}
