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

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.cache.CacheControllerHolder.CacheType;

public enum AssetKind
{
    OBJECT(CacheControllerHolder.CONTENT), CONFIG(CacheControllerHolder.METADATA), REF(CacheControllerHolder.METADATA);

    private final CacheType cache_type;

    AssetKind(final CacheType cacheType) {
        this.cache_type = cacheType;
    }

    @Nonnull
    public CacheType getCacheType() {
        return cache_type;
    }
}
