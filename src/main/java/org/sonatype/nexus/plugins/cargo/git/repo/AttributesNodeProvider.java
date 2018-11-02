
package org.sonatype.nexus.plugins.cargo.git.repo;

import java.io.IOException;

import org.eclipse.jgit.attributes.AttributesNode;

public class AttributesNodeProvider implements org.eclipse.jgit.attributes.AttributesNodeProvider {
    private final AttributesNode global_node;
    private final AttributesNode info_node;

    AttributesNodeProvider() {
        this.global_node = new AttributesNode();
        this.info_node = new AttributesNode();
    }

    @Override
    public AttributesNode getGlobalAttributesNode() throws IOException {
        return this.global_node;
    }

    @Override
    public AttributesNode getInfoAttributesNode() throws IOException {
        return this.info_node;
    }
}
