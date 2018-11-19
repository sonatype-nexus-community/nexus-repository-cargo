
package org.sonatype.nexus.plugins.cargo.git.repo;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectStream;
import org.sonatype.nexus.plugins.cargo.git.assets.AssetKindObjectAttributes;
import org.sonatype.nexus.repository.storage.Asset;

public class ObjectLoader extends org.eclipse.jgit.lib.ObjectLoader {
    private final AssetKindObjectAttributes asset_attributes;

    private final AnyObjectId obj_id;
    private final Asset obj_asset;

    ObjectLoader(Repository db, AnyObjectId obj_id,
            Asset obj_asset) {
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
