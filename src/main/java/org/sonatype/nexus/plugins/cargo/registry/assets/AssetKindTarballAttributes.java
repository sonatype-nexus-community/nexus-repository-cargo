
package org.sonatype.nexus.plugins.cargo.registry.assets;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.storage.Asset;
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
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

@Named
public class AssetKindTarballAttributes {
    public static final String CONTENT_TYPE_TARBALL = "application/x-tar";
    public static final List<HashAlgorithm> HASH_ALGORITHMS = Lists
            .newArrayList(HashAlgorithm.SHA1);

    protected final BucketEntityAdapter bucketEntityAdapter;
    protected final ComponentEntityAdapter componentEntityAdapter;
    protected final ComponentKindCrateAttributes crateAttributes;

    @Inject
    protected AssetKindTarballAttributes(BucketEntityAdapter bucketEntityAdapter,
            ComponentEntityAdapter componentEntityAdapter,
            ComponentKindCrateAttributes crateAttributes) {
        this.bucketEntityAdapter = bucketEntityAdapter;
        this.componentEntityAdapter = componentEntityAdapter;
        this.crateAttributes = crateAttributes;
    }

    private static String getAttributePropertyName(String format_attribute) {
        return MetadataNodeEntityAdapter.P_ATTRIBUTES + "." + format_attribute;
    }

    /*
     * Creates an asset to hold a specific crate version's tarball. Caller is expected to attach a
     * blob containing tarball payload.
     */
    @TransactionalTouchMetadata
    @TransactionalTouchBlob
    public Asset createAsset(Bucket bucket, Component component, InputStream tarball)
            throws IOException {
        StorageTx tx = UnitOfWork.currentTx();

        // Create an Asset to hold the new version.
        Asset asset = tx.createAsset(bucket, component);
        String tarballName = crateAttributes.getCoordinates(component).getFileBasename() + ".crate";
        asset.name(tarballName);
        asset.attributes().set(AssetEntityAdapter.P_ASSET_KIND, AssetKind.TARBALL.name());
        tx.setBlob(asset, tarballName, Suppliers.ofInstance(tarball), HASH_ALGORITHMS, null,
                CONTENT_TYPE_TARBALL, true);
        tx.saveAsset(asset);

        return asset;
    }

    @TransactionalTouchMetadata
    public Asset findAsset(Bucket bucket, Component component) {
        StorageTx tx = UnitOfWork.currentTx();
        Iterable<Asset> objAssets = tx.findAssets(
                Query.builder()
                        .where(AssetEntityAdapter.P_BUCKET)
                        .eq(bucketEntityAdapter.recordIdentity(bucket))
                        .and(AssetEntityAdapter.P_COMPONENT)
                        .eq(componentEntityAdapter.recordIdentity(component))
                        .and(getAttributePropertyName(AssetEntityAdapter.P_ASSET_KIND))
                        .eq(AssetKind.TARBALL.name())
                        .build(),
                null);
        return Iterables.getOnlyElement(objAssets, null);
    }

    @TransactionalTouchBlob
    public Content getAssetContent(Bucket bucket, Component component) {
        StorageTx tx = UnitOfWork.currentTx();

        final Asset asset = findAsset(bucket, component);
        if (asset == null)
            return null;

        BlobRef blobref = asset.blobRef();
        if (blobref == null)
            return null;

        final Blob blob = tx.requireBlob(blobref);
        if (blob == null)
            return null;

        if (asset.markAsDownloaded()) {
            tx.saveAsset(asset);
        }

        final String contentType = asset.contentType();
        final Content content = new Content(new BlobPayload(blob, contentType));
        Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
        return content;
    }
}
