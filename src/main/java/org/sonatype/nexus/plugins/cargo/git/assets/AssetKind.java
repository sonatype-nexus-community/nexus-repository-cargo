
package org.sonatype.nexus.plugins.cargo.git.assets;

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.cache.CacheControllerHolder.CacheType;

public enum AssetKind {
    OBJECT(CacheControllerHolder.CONTENT), CONFIG(CacheControllerHolder.METADATA), REF(
            CacheControllerHolder.METADATA);

    private final CacheType cache_type;

    AssetKind(final CacheType cacheType) {
        this.cache_type = cacheType;
    }

    @Nonnull
    public CacheType getCacheType() {
        return cache_type;
    }
}
