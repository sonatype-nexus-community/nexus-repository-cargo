
package org.sonatype.nexus.plugins.cargo.security;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.SecurityFacetSupport;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;

/**
 * Cargo format security facet.
 */
@Named
public class CargoSecurityFacet
        extends SecurityFacetSupport {
    @Inject
    public CargoSecurityFacet(final CargoFormatSecurityContributor securityResource,
            @Named("simple") final VariableResolverAdapter variableResolverAdapter,
            final ContentPermissionChecker contentPermissionChecker) {
        super(securityResource, variableResolverAdapter, contentPermissionChecker);
    }

    @Override
    protected void doValidate(final Configuration configuration) throws Exception {
        super.doValidate(configuration);
    }
}
