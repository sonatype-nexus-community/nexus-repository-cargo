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

package org.sonatype.nexus.plugins.cargo.git.assets;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;

import com.google.inject.Inject;

import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.sonatype.goodies.common.Loggers;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.plugins.cargo.git.Constants;
import org.sonatype.nexus.plugins.cargo.git.repo.ObjectIdHashFunction;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.transaction.UnitOfWork;

@Named
public class AssetKindObjectAttributes
{
    private final static String P_OBJECT_TYPE = "git-object-type";

    private final static String CONTENT_TYPE_LOOSE_OBJECT = "application/x-git-loose-object";

    protected BucketEntityAdapter bucketEntityAdapter;

    protected ComponentEntityAdapter componentEntityAdapter;

    private static String getAttributePropertyName(String format_attribute) {
        return MetadataNodeEntityAdapter.P_ATTRIBUTES + "." + format_attribute;
    }

    @Inject
    protected AssetKindObjectAttributes(BucketEntityAdapter bucketEntityAdapter,
                                        ComponentEntityAdapter componentEntityAdapter)
    {
        this.bucketEntityAdapter = bucketEntityAdapter;
        this.componentEntityAdapter = componentEntityAdapter;
    }

    @TransactionalTouchMetadata
    @TransactionalTouchBlob
    public Asset createObjectAsset(Bucket bucket,
                                   Component component,
                                   int object_type,
                                   long length,
                                   InputStream contents) throws IOException
    {
        StorageTx tx = UnitOfWork.currentTx();

        // Calculate the Git object id while the blob is being written.
        HashAlgorithm object_id_hash =
                new HashAlgorithm("git-object-id", new ObjectIdHashFunction(object_type, length));
        List<HashAlgorithm> hashes = new ArrayList<HashAlgorithm>(Constants.OBJECT_HASHES);
        hashes.add(object_id_hash);

        // Create a blob from the contents of the InputStream. This will be a
        // unique blob but the object ID may already exist. The blob will be
        // thrown away if it isn't attached to an Asset.
        AssetBlob assetBlob = tx.createBlob("git-object", () -> contents, hashes, null,
                CONTENT_TYPE_LOOSE_OBJECT, true);

        // If the calculated ObjectId is already saved in the object database,
        // we're done.
        ObjectId object_id = ObjectId.fromRaw(assetBlob.getHashes().get(object_id_hash).asBytes());
        Asset existing_asset = this.findObjectAssetWithObjectId(bucket, component, object_id);
        if (existing_asset != null)
            return existing_asset;

        // Create an Asset to hold the new Git object. Attach the objectType as metadata.
        Asset asset = tx.createAsset(bucket, component);
        asset.name(object_id.name());
        asset.attributes().set(AssetEntityAdapter.P_ASSET_KIND, AssetKind.OBJECT.name());
        setObjectType(asset, object_type);
        tx.attachBlob(asset, assetBlob);
        tx.saveAsset(asset);

        Loggers.getLogger(this).debug("Inserting object asset (type={}, length={}): {}", object_type, length, asset);
        Loggers.getLogger(this).debug("Inserted blob (id={}, headers={}, metrics={})", assetBlob.getBlob().getId(),
                assetBlob.getBlob().getHeaders(), assetBlob.getBlob().getMetrics().toString());
        return asset;
    }

    @TransactionalTouchMetadata
    public Iterable<Asset> browseObjectAssetByAbbreviatedObjectId(Bucket bucket,
                                                                  Component component,
                                                                  AbbreviatedObjectId object_id)
    {
        StorageTx tx = UnitOfWork.currentTx();
        Query ids_like_abbrev = Query.builder().where(AssetEntityAdapter.P_BUCKET)
                .eq(bucketEntityAdapter.recordIdentity(bucket)).and(AssetEntityAdapter.P_COMPONENT)
                .eq(componentEntityAdapter.recordIdentity(component))
                .and(getAttributePropertyName(AssetEntityAdapter.P_ASSET_KIND))
                .and(AssetEntityAdapter.P_NAME + " like ").param(object_id.name() + "%").and("LIMIT 256").build();
        Loggers.getLogger(this).debug("Looking for objects matching \"{}\"\nQuery: {}", object_id.name(),
                ids_like_abbrev.toString());
        return tx.findAssets(ids_like_abbrev, null);
    }

    @TransactionalTouchMetadata
    public Asset findObjectAssetWithObjectId(Bucket bucket, Component component, AnyObjectId object_id) {
        Loggers.getLogger(this).debug("Looking for objects matching \"{}\"", object_id.name());

        StorageTx tx = UnitOfWork.currentTx();
        Iterator<Asset> objAssets = tx.findAssets(
                Query.builder().where(AssetEntityAdapter.P_BUCKET).eq(bucketEntityAdapter.recordIdentity(bucket))
                        .and(AssetEntityAdapter.P_COMPONENT).eq(componentEntityAdapter.recordIdentity(component))
                        .and(getAttributePropertyName(AssetEntityAdapter.P_ASSET_KIND)).eq(AssetKind.OBJECT.name())
                        .and(AssetEntityAdapter.P_NAME).eq(object_id.name()).build(),
                null).iterator();
        return objAssets.hasNext() ? objAssets.next() : null;
    }

    public ObjectId getObjectId(Asset obj_asset) {
        return ObjectId.fromString(obj_asset.name());
    }

    public int getObjectType(Asset obj_asset) {
        return obj_asset.formatAttributes().require(P_OBJECT_TYPE, Integer.class);
    }

    public void setObjectType(Asset obj_asset, int object_type) {
        obj_asset.formatAttributes().set(P_OBJECT_TYPE, new Integer(object_type));
    }

    @TransactionalTouchBlob
    public InputStream getContents(Asset obj_asset) throws IOException {
        StorageTx tx = UnitOfWork.currentTx();
        Blob blob = tx.getBlob(obj_asset.requireBlobRef());
        if (blob == null) {
            return null;
        }

        return blob.getInputStream();
    }
}
