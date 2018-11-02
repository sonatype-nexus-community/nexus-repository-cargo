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

/*
 * Repository settings form for a Cargo repository
 */
Ext.define('NX.cargo.view.repository.recipe.CargoHosted', {
    extend: 'NX.coreui.view.repository.RepositorySettingsForm',
    alias: 'widget.nx-coreui-repository-cargo-hosted',
    requires: [
        'NX.cargo.view.repository.facet.CargoRegistryFacet',
        'NX.coreui.view.repository.facet.StorageFacet',
        'NX.coreui.view.repository.facet.StorageFacetHosted'
    ],

    /**
     * @override
     */
    initComponent: function () {
        var me = this;

        me.items = [
            { xtype: 'nx-coreui-repository-storage-facet' },
            { xtype: 'nx-coreui-repository-storage-hosted-facet' },
            { xtype: 'nx-cargo-repository-cargo-registry-facet' }
        ];

        me.callParent();
    }
});
