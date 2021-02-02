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

import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectStream;
import org.sonatype.nexus.plugins.cargo.git.assets.AssetKindObjectAttributes;
import org.sonatype.nexus.repository.storage.Asset;

public class ObjectLoader
        extends org.eclipse.jgit.lib.ObjectLoader
{
    private final AssetKindObjectAttributes asset_attributes;

    private final AnyObjectId obj_id;

    private final Asset obj_asset;

    ObjectLoader(Repository db, AnyObjectId obj_id, Asset obj_asset) {
        super();
        this.asset_attributes = db.getAssetAttributesObject();
        this.obj_id = obj_id;
        this.obj_asset = obj_asset;
    }

    @Override
    public int getType() {
        return asset_attributes.getObjectType(obj_asset);
    }

    @Override
    public long getSize() {
        return obj_asset.size();
    }

    @Override
    public boolean isLarge() {
        return true;
    }

    @Override
    public byte[] getCachedBytes() throws LargeObjectException {
        throw new LargeObjectException(this.obj_id);
    }

    @Override
    public ObjectStream openStream() throws MissingObjectException, IOException {
        InputStream contents = asset_attributes.getContents(this.obj_asset);
        if (contents == null) {
            throw new MissingObjectException(this.obj_id.toObjectId(), this.getType());
        }

        return new ObjectStream.Filter(this.getType(), this.getSize(), contents);
    }
}
