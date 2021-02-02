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

package org.sonatype.nexus.plugins.cargo.git.repo;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.plugins.cargo.git.Constants;
import org.sonatype.nexus.plugins.cargo.git.assets.AssetKindObjectAttributes;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import org.eclipse.jgit.lib.ObjectId;

public class ObjectInserter
        extends org.eclipse.jgit.lib.ObjectInserter
{
    private final Repository db;

    private final AssetKindObjectAttributes asset_attributes;

    private final StorageFacet storage_facet;

    private final Bucket bucket;

    private final Component component;

    ObjectInserter(Repository db) {
        this.db = db;
        this.asset_attributes = db.getAssetAttributesObject();
        this.storage_facet = db.getStorageFacet();
        this.bucket = db.getBucket();
        this.component = db.getComponent();
    }

    @Override
    public ObjectId insert(int object_type, long length, InputStream in) throws IOException {
        Asset asset = asset_attributes.createObjectAsset(this.bucket, this.component, object_type, length, in);
        return asset_attributes.getObjectId(asset);
    }

    @Override
    public void close() {
        // Nothing to do as insert() queues all the work in the current transaction.
    }

    @Override
    public void flush() throws IOException {
        // Nothing to do as insert() queues all the work in the current transaction.
    }

    @Override
    public PackParser newPackParser(InputStream in) throws IOException {
        TempBlob packBlob = storage_facet.createTempBlob(in, Constants.OBJECT_HASHES);
        return new PackParser(this.db, packBlob);
    }

    @Override
    public ObjectReader newReader() {
        return new ObjectReader(this.db);
    }

}
