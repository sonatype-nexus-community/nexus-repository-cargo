
package org.sonatype.nexus.plugins.cargo.git.assets;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.plugins.cargo.git.Constants;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.ContentTypes;
import org.sonatype.nexus.transaction.UnitOfWork;

@Named
public class AssetKindConfigAttributes {
    private static String getAttributePropertyName(String format_attribute) {
        return MetadataNodeEntityAdapter.P_ATTRIBUTES + "." + format_attribute;
    }

    @TransactionalStoreMetadata
    public Asset createConfigAsset(Bucket bucket, Component component) {
        StorageTx tx = UnitOfWork.currentTx();
        Asset configAsset = tx.createAsset(bucket, component);
        configAsset.attributes().set(AssetEntityAdapter.P_ASSET_KIND,
                AssetKind.CONFIG.name());
        tx.saveAsset(configAsset);
        return configAsset;
    }

    @TransactionalTouchMetadata
    public Asset findConfigAsset(Bucket bucket, Component component) {
        // Find the repository's stored config asset. If there isn't one, create
        // one.
        StorageTx tx = UnitOfWork.currentTx();
        return tx.findAssetWithProperty(
                getAttributePropertyName(AssetEntityAdapter.P_ASSET_KIND),
                AssetKind.CONFIG.name(), component);
    }

    @TransactionalTouchBlob
    public InputStream getContents(Asset config_asset) {
        StorageTx tx = UnitOfWork.currentTx();
        Blob contents = tx.getBlob(config_asset.blobRef());
        return contents.getInputStream();
    }

    @TransactionalStoreBlob
    public void setContents(Asset config_asset, InputStream stream) throws IOException {
        // Attach config in text format to the above Asset as a blob.
        StorageTx tx = UnitOfWork.currentTx();
        tx.setBlob(config_asset, "config",
                () -> stream,
                Constants.OBJECT_HASHES, null, ContentTypes.TEXT_PLAIN, true);
        tx.saveAsset(config_asset);
    }
}
