/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

package org.sonatype.nexus.plugins.cargo.git.assets;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.SymbolicRef;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.transaction.UnitOfWork;

@Named
public class AssetKindRefAttributes
{
    public final static String P_REF_TYPE = "git-ref-type";

    public final static String I_REF_TYPE_OBJECT_ID_UNPEELED = "object-id-unpeeled";

    public final static String I_REF_TYPE_OBJECT_ID_PEELED_TAG = "object-id-peeled-tag";

    public final static String I_REF_TYPE_OBJECT_ID_PEELED_NONTAG = "object-id-peeled-nontag";

    public final static String I_REF_TYPE_SYMBOLIC = "symbolic";

    public final static String P_REF_TARGET = "git-ref-target";

    public final static String P_REF_PEELED_OBJECT_ID = "git-ref-peeled-object-id";

    protected BucketEntityAdapter bucketEntityAdapter;

    protected ComponentEntityAdapter componentEntityAdapter;

    private static String getAttributePropertyName(String format_attribute) {
        return MetadataNodeEntityAdapter.P_ATTRIBUTES + "." + format_attribute;
    }

    @Inject
    protected AssetKindRefAttributes(BucketEntityAdapter bucketEntityAdapter,
                                     ComponentEntityAdapter componentEntityAdapter)
    {
        this.bucketEntityAdapter = bucketEntityAdapter;
        this.componentEntityAdapter = componentEntityAdapter;
    }

    @TransactionalStoreMetadata
    public Asset createRefAsset(Bucket bucket, Component component, String name) {
        StorageTx tx = UnitOfWork.currentTx();
        Asset asset = tx.createAsset(bucket, component);
        asset.name(name);
        asset.attributes().set(AssetEntityAdapter.P_ASSET_KIND, AssetKind.REF.name());

        return asset;
    }

    @TransactionalTouchMetadata
    public Iterable<Asset> browseRefAssetsByPrefix(Bucket bucket, Component component, String prefix) {
        StorageTx tx = UnitOfWork.currentTx();
        Query.Builder query_builder =
                Query.builder().where(AssetEntityAdapter.P_BUCKET).eq(bucketEntityAdapter.recordIdentity(bucket))
                        .and(AssetEntityAdapter.P_COMPONENT).eq(componentEntityAdapter.recordIdentity(component))
                        .and(getAttributePropertyName(AssetEntityAdapter.P_ASSET_KIND)).eq(AssetKind.REF.name());

        if (!prefix.isEmpty()) {
            query_builder = query_builder.and(AssetEntityAdapter.P_NAME).where(" LIKE ").param(prefix + "%");
        }

        return tx.findAssets(query_builder.build(), null);
    }

    @TransactionalTouchMetadata
    public Asset findRefAssetWithName(Bucket bucket, Component component, String name) {
        StorageTx tx = UnitOfWork.currentTx();
        Iterator<Asset> refAssets = tx.findAssets(
                Query.builder().where(AssetEntityAdapter.P_BUCKET).eq(bucketEntityAdapter.recordIdentity(bucket))
                        .and(AssetEntityAdapter.P_COMPONENT).eq(componentEntityAdapter.recordIdentity(component))
                        .and(getAttributePropertyName(AssetEntityAdapter.P_ASSET_KIND)).eq(AssetKind.REF.name())
                        .and(AssetEntityAdapter.P_NAME).eq(name).build(),
                null).iterator();
        return refAssets.hasNext() ? refAssets.next() : null;
    }

    public Ref getRef(Bucket bucket, Component component, Asset asset) {
        switch (asset.formatAttributes().require(P_REF_TYPE, String.class)) {
            case I_REF_TYPE_OBJECT_ID_UNPEELED:
                return new ObjectIdRef.Unpeeled(Ref.Storage.LOOSE, asset.name(), ObjectId.fromString(getTarget(asset)));
            case I_REF_TYPE_OBJECT_ID_PEELED_TAG:
                return new ObjectIdRef.PeeledTag(Ref.Storage.LOOSE, asset.name(), ObjectId.fromString(getTarget(asset)),
                        getPeeledObjectId(asset));
            case I_REF_TYPE_OBJECT_ID_PEELED_NONTAG:
                return new ObjectIdRef.PeeledNonTag(Ref.Storage.LOOSE, asset.name(),
                        ObjectId.fromString(getTarget(asset)));
            case I_REF_TYPE_SYMBOLIC:
                Asset targetAsset = findRefAssetWithName(bucket, component, getTarget(asset));
                if (targetAsset == null)
                    return null;

                return new SymbolicRef(asset.name(), getRef(bucket, component, targetAsset));
        }

        return null;
    }

    @TransactionalStoreMetadata
    public void setRef(Asset asset, Ref ref) {
        StorageTx tx = UnitOfWork.currentTx();

        if (ref.isSymbolic()) {
            asset.formatAttributes().set(P_REF_TYPE, I_REF_TYPE_SYMBOLIC);
            asset.formatAttributes().set(P_REF_TARGET, ref.getTarget().getName());
        }
        else {
            asset.formatAttributes().set(P_REF_TARGET, ref.getObjectId().getName());

            if (!ref.isPeeled()) {
                asset.formatAttributes().set(P_REF_TYPE, I_REF_TYPE_OBJECT_ID_UNPEELED);
            }
            else {
                ObjectId peeledObjectId = ref.getPeeledObjectId();
                if (peeledObjectId != null) {
                    asset.formatAttributes().set(P_REF_TYPE, I_REF_TYPE_OBJECT_ID_PEELED_TAG);
                    asset.formatAttributes().set(P_REF_PEELED_OBJECT_ID, peeledObjectId.getName());
                }
                else {
                    asset.formatAttributes().set(P_REF_TYPE, I_REF_TYPE_OBJECT_ID_PEELED_NONTAG);
                }
            }
        }

        tx.saveAsset(asset);
    }

    private String getTarget(Asset asset) {
        return asset.formatAttributes().require(P_REF_TARGET, String.class);
    }

    private ObjectId getPeeledObjectId(Asset asset) {
        String obj_id = asset.formatAttributes().require(P_REF_PEELED_OBJECT_ID, String.class);
        return ObjectId.fromString(obj_id);
    }
}
