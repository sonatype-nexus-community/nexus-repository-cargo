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
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.sonatype.nexus.plugins.cargo.git.assets.AssetKindConfigAttributes;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;

public class StoredConfig
        extends org.eclipse.jgit.lib.StoredConfig
{
    private final AssetKindConfigAttributes asset_attributes;

    private final Bucket repo_bucket;

    private final Component repo_component;

    StoredConfig(Repository db) {
        super();
        this.asset_attributes = db.getAssetAttributesConfig();
        this.repo_bucket = db.getBucket();
        this.repo_component = db.getComponent();
    }

    @Override
    public void save() throws IOException {
        Asset asset = asset_attributes.findConfigAsset(this.repo_bucket, this.repo_component);
        if (asset == null) {
            asset = asset_attributes.createConfigAsset(this.repo_bucket, this.repo_component);
        }
        asset_attributes.setContents(asset, IOUtils.toInputStream(this.toText(), StandardCharsets.UTF_8));
    }

    @Override
    public void load() throws IOException, ConfigInvalidException {
        // If the asset or blob doesn't exist, StoredConfig expects a blank
        // config as if an empty config had been stored.
        clear();

        Asset asset = asset_attributes.findConfigAsset(this.repo_bucket, this.repo_component);
        if (asset != null) {
            fromText(IOUtils.toString(asset_attributes.getContents(asset), StandardCharsets.UTF_8));
        }
    }
}
