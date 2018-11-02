
package org.sonatype.nexus.plugins.cargo.registry.assets;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.cache.CacheControllerHolder.CacheType;

public enum AssetKind {
    TARBALL(CacheControllerHolder.CONTENT), METADATA(CacheControllerHolder.METADATA);

    private final CacheType cacheType;

    AssetKind(final CacheType cacheType) {
        this.cacheType = cacheType;
    }

    @Nonnull
    public CacheType getCacheType() {
        return cacheType;
    }
}
