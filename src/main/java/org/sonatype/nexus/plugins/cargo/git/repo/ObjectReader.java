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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.sonatype.nexus.plugins.cargo.git.assets.AssetKindObjectAttributes;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.transaction.UnitOfWork;

public class ObjectReader
        extends org.eclipse.jgit.lib.ObjectReader
{
    private final Repository db;

    private final AssetKindObjectAttributes asset_attributes;

    private final PackConfig pack_config;

    private final Bucket bucket;

    private final Component component;

    private final StorageFacet storage_facet;

    ObjectReader(Repository db) {
        super();
        this.db = db;
        this.asset_attributes = db.getAssetAttributesObject();
        this.pack_config = new PackConfig(db);
        this.bucket = db.getBucket();
        this.component = db.getComponent();
        this.storage_facet = db.getStorageFacet();
    }

    @Override
    public boolean has(AnyObjectId object_id, int type_hint) throws IOException {
        boolean newUnitOfWorkStarted = startNewUnitOfWorkIfNotAlreadyStarted();
        try {
            Asset obj_asset = asset_attributes.findObjectAssetWithObjectId(bucket, component, object_id);
            if (obj_asset == null)
                return false;

            if (type_hint != OBJ_ANY && type_hint != asset_attributes.getObjectType(obj_asset)) {
                throw new IncorrectObjectTypeException(object_id.toObjectId(), type_hint);
            }

            return true;
        } finally {
            endUnitOfWorkIfNeeded(newUnitOfWorkStarted);
        }
    }

    @Override
    public ObjectLoader open(AnyObjectId object_id, int type_hint)
            throws MissingObjectException, IncorrectObjectTypeException, IOException {
        boolean newUnitOfWorkStarted = startNewUnitOfWorkIfNotAlreadyStarted();
        try {
            Asset obj_asset = asset_attributes.findObjectAssetWithObjectId(bucket, component, object_id);
            if (obj_asset == null) {
                if (type_hint == OBJ_ANY)
                    throw new MissingObjectException(object_id.copy(), JGitText.get().unknownObjectType2);

                throw new MissingObjectException(object_id.toObjectId(), type_hint);
            }
            if (type_hint != OBJ_ANY && type_hint != asset_attributes.getObjectType(obj_asset)) {
                throw new IncorrectObjectTypeException(object_id.toObjectId(), type_hint);
            }

            if (obj_asset.size() < this.pack_config.getBigFileThreshold()) {
                ByteArrayOutputStream obj_buf = new ByteArrayOutputStream(obj_asset.size().intValue());
                IOUtils.copy(this.asset_attributes.getContents(obj_asset), obj_buf);
                return new ObjectLoader.SmallObject(this.asset_attributes.getObjectType(obj_asset), obj_buf.toByteArray());
            }
            else {
                return new org.sonatype.nexus.plugins.cargo.git.repo.ObjectLoader(this.db, object_id, obj_asset);
            }
        } finally {
            endUnitOfWorkIfNeeded(newUnitOfWorkStarted);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public Collection<ObjectId> resolve(AbbreviatedObjectId id) throws IOException {
        boolean newUnitOfWorkStarted = startNewUnitOfWorkIfNotAlreadyStarted();
        try {
            List<ObjectId> matching_ids = new ArrayList<ObjectId>();
            for (Asset obj_asset : asset_attributes.browseObjectAssetByAbbreviatedObjectId(bucket, component, id)) {
                matching_ids.add(asset_attributes.getObjectId(obj_asset));
            }

            return matching_ids;
        } finally {
            endUnitOfWorkIfNeeded(newUnitOfWorkStarted);
        }
    }

    @Override
    public org.eclipse.jgit.lib.ObjectReader newReader() {
        return new ObjectReader(db);
    }

    @Override
    public Set<ObjectId> getShallowCommits() throws IOException {
        return Collections.emptySet();
    }

    private boolean startNewUnitOfWorkIfNotAlreadyStarted() {
        UnitOfWork old = UnitOfWork.pause();
        if (old == null) {
            UnitOfWork.begin(storage_facet.txSupplier());
        } else {
            UnitOfWork.resume(old);
        }
        return old == null;
    }

    private void endUnitOfWorkIfNeeded(boolean newUnitOfWorkStarted) {
        if (newUnitOfWorkStarted) {
            UnitOfWork.end();
        }
    }
}
