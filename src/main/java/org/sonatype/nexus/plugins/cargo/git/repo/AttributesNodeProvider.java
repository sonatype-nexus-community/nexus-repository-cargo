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

package org.sonatype.nexus.plugins.cargo.git.repo;

import java.io.IOException;

import org.eclipse.jgit.attributes.AttributesNode;

public class AttributesNodeProvider
        implements org.eclipse.jgit.attributes.AttributesNodeProvider
{
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
