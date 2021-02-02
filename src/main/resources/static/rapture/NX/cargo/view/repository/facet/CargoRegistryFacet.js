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
 * Configuration specific to cargo repositories.
 */
Ext.define('NX.cargo.view.repository.facet.CargoRegistryFacet', {
    extend: 'Ext.form.FieldContainer',
    alias: 'widget.nx-cargo-repository-cargo-registry-facet',
    requires: [
        'NX.I18n'
    ],
    /**
     * @override
     */
    initComponent: function () {
        var me = this;

        me.items = [
            {
                xtype: 'fieldset',
                cls: 'nx-form-section',
                title: NX.I18n.get('Repository_Facet_CargoRegistryFacet_Title'),
                items: [
                    {
                        xtype: 'nx-url',
                        name: 'attributes.cargo.allowedRegistries',
                        itemId: "allowedRegistries",
                        fieldLabel: NX.I18n.get('Repository_Facet_CargoRegistryFacet_AllowedRegistries_FieldLabel'),
                        helpText: NX.I18n.get('Repository_Facet_CargoRegistryFacet_AllowedRegistries_HelpText'),
                        emptyText: NX.I18n.get('Repository_Facet_CargoRegistryFacet_AllowedRegistries_EmptyText'),
                        value: 'https://github.com/rust-lang/crates.io-index'
                    }
                ]
            }
        ];

        me.callParent();
    }

});
