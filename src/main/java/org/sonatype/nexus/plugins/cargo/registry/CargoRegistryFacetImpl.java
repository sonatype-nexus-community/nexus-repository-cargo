/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo.registry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.plugins.cargo.CargoRegistryFacet;
import org.sonatype.nexus.plugins.cargo.CrateCoordinates;
import org.sonatype.nexus.plugins.cargo.GitRepositoryFacet;
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

        Repository indexRepo = this.getRepository().facet(GitRepositoryFacet.class).getGitRepository("index");

        // Generate config.json with the current repository URL
        JsonObject config = new JsonObject();
        config.addProperty("dl", this.getRepository().getUrl() + "/api/v1/crates");
        config.addProperty("api", this.getRepository().getUrl());
        JsonArray allowedRegistries = new JsonArray();
        allowedRegistries.add(new JsonPrimitive(this.config.allowedRegistries.toString()));
        config.add("allowed-registries", allowedRegistries);

        // Calculate the ObjectId for the generated config.json and insert it into the object index.
        ObjectInserter ins = indexRepo.newObjectInserter();
        byte[] configBytes = config.toString().getBytes(StandardCharsets.UTF_8);
        AnyObjectId configObjId = ins.idFor(Constants.OBJ_BLOB, configBytes);
        ins.insert(Constants.OBJ_BLOB, configBytes);
        ins.flush();

        // Start with a blank tree.
        DirCache dirCache = DirCache.newInCore();
        RevCommit parent = null;

        // If there is a current HEAD, read that commit's tree. It will become
        // our parent.
        AnyObjectId headId = indexRepo.resolve(this.indexBranch + "^{commit}"); //$NON-NLS-1$
        if (headId != null) {
            try (RevWalk revWalk = new RevWalk(indexRepo)) {
                parent = revWalk.parseCommit(headId);
                DirCacheBuilder builder = dirCache.builder();
                builder.addTree(null, DirCacheEntry.STAGE_0, indexRepo.newObjectReader(), parent.getTree());
                builder.finish();
            }
        }

        // Update config.json in the tree to point to the correct ObjectId.
        DirCacheEditor editor = dirCache.editor();
        editor.add(new DirCacheEditor.PathEdit("config.json")
        {
            @Override
            public void apply(DirCacheEntry ent) {
                ent.setFileMode(FileMode.REGULAR_FILE);
                ent.setObjectId(configObjId);
            }
        });
        editor.finish();
        AnyObjectId indexTreeId = dirCache.writeTree(ins);

        // Check for empty commits
        if (parent != null && indexTreeId.equals(parent.getTree())) {
            return;
        }

        // Create a Commit object, populate it and write it
        PersonIdent adminIdent = new PersonIdent("Nexus System", this.emailManager.getConfiguration().getFromAddress());
        CommitBuilder commit = new CommitBuilder();
        commit.setCommitter(adminIdent);
        commit.setAuthor(adminIdent);
        commit.setMessage("Update registry URL to " + this.getRepository().getUrl());
        if (parent != null) {
            commit.setParentIds(parent);
        }
        commit.setTreeId(indexTreeId);
        AnyObjectId commitId = ins.insert(commit);
        ins.flush();

        try (RevWalk revWalk = new RevWalk(indexRepo)) {
            RevCommit revCommit = revWalk.parseCommit(commitId);
            RefUpdate headUpdate = indexRepo.updateRef(this.indexBranch);
            headUpdate.setNewObjectId(commitId);
            String reflogMsgPrefix = parent == null ? "commit (initial): " : "commit: ";
            headUpdate.setRefLogMessage(reflogMsgPrefix + revCommit.getShortMessage(), false);

            if (headId != null)
                headUpdate.setExpectedOldObjectId(headId);
            else
                headUpdate.setExpectedOldObjectId(ObjectId.zeroId());
            headUpdate.forceUpdate();
        }
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
