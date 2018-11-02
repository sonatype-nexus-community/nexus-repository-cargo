/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo.git.repo;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.jgit.lib.BaseRepositoryBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ReflogReader;
import org.sonatype.nexus.plugins.cargo.git.assets.AssetKindConfigAttributes;
import org.sonatype.nexus.plugins.cargo.git.assets.AssetKindObjectAttributes;
import org.sonatype.nexus.plugins.cargo.git.assets.AssetKindRefAttributes;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;

/*
 * Provides a Git Repository implemented atop StorageFace. Git repositories primarily consist of two
 * datastores: the object database and the ref database. Traditional Git implements boths of these
 * using filesystem constructs. Since Nexus uses an asset database and blob store, the traditional
 * repository representation does not map well. GitStorageFacetRepository instances
 * GitStorageFacetObjectDatabase and GitStorageFacetRefDatabase to implmement these databases on
 * Nexus Assets.
 */

public class Repository
        extends org.eclipse.jgit.lib.Repository
{
    @Named
    public static class Builder
            extends BaseRepositoryBuilder<Builder, Repository>
    {
        private final AssetKindConfigAttributes asset_attributes_config;

        private final AssetKindObjectAttributes asset_attributes_object;

        private final AssetKindRefAttributes asset_attributes_ref;

        private StorageFacet storage_facet;

        private Bucket bucket;

        private Component component;

        @Inject
        public Builder(AssetKindConfigAttributes asset_attributes_config,
                       AssetKindObjectAttributes asset_attributes_object,
                       AssetKindRefAttributes asset_attributes_ref)
        {
            setBare();
            this.asset_attributes_config = asset_attributes_config;
            this.asset_attributes_object = asset_attributes_object;
            this.asset_attributes_ref = asset_attributes_ref;
        }

        public Builder setStorageFacet(StorageFacet storage_facet) {
            this.storage_facet = storage_facet;
            return self();
        }

        public Builder setComponent(Bucket bucket, Component component) {
            this.bucket = bucket;
            this.component = component;
            return self();
        }

        @Override
        public Repository build() throws IOException {

            return new Repository(this);
        }
    }

    private final AssetKindConfigAttributes asset_attributes_config;

    private final AssetKindObjectAttributes asset_attributes_object;

    private final AssetKindRefAttributes asset_attributes_ref;

    private final StorageFacet storage_facet;

    private final StoredConfig config;

    private final ObjectDatabase obj_db;

    private final RefDatabase ref_db;

    Bucket bucket;

    Component component;

    public Repository(Builder builder) {
        super(builder);
        this.asset_attributes_config = builder.asset_attributes_config;
        this.asset_attributes_object = builder.asset_attributes_object;
        this.asset_attributes_ref = builder.asset_attributes_ref;
        this.storage_facet = builder.storage_facet;
        this.bucket = builder.bucket;
        this.component = builder.component;

        this.config = new StoredConfig(this);
        this.obj_db = new ObjectDatabase(this);
        this.ref_db = new RefDatabase(this);
    }

    AssetKindConfigAttributes getAssetAttributesConfig() {
        return this.asset_attributes_config;
    }

    AssetKindObjectAttributes getAssetAttributesObject() {
        return this.asset_attributes_object;
    }

    AssetKindRefAttributes getAssetAttributesRef() {
        return this.asset_attributes_ref;
    }

    StorageFacet getStorageFacet() {
        return this.storage_facet;
    }

    Bucket getBucket() {
        return this.bucket;
    }

    Component getComponent() {
        return this.component;
    }

    @Override
    public void create(boolean bare) throws IOException {
        String master = Constants.R_HEADS + Constants.MASTER;
        RefUpdate.Result result = updateRef(Constants.HEAD, true).link(master);
        if (result != RefUpdate.Result.NEW)
            throw new IOException(result.name());
    }

    @Override
    public StoredConfig getConfig() {
        return this.config;
    }

    @Override
    public RefDatabase getRefDatabase() {
        return this.ref_db;
    }

    @Override
    public ObjectDatabase getObjectDatabase() {
        return this.obj_db;
    }

    @Override
    public ReflogReader getReflogReader(String refName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void notifyIndexChanged(boolean internal) {
        // Do not send notifications.
        // There is no index, as there is no working tree.
    }

    @Override
    public void scanForRepoChanges() throws IOException {
        this.ref_db.refresh();
    }

    @Override
    public AttributesNodeProvider createAttributesNodeProvider() {
        return new AttributesNodeProvider();
    }

}
