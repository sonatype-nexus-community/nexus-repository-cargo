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
 * Cargo plugin strings.
 */
Ext.define('NX.cargo.app.PluginStrings', {
    '@aggregate_priority': 90,

    singleton: true,
    requires: [
        'NX.I18n'
    ],

    keys: {
        Repository_Facet_CargoFacet_Title: 'Cargo Settings',
        SearchCargo_Group: 'Cargo Repositories',
        SearchCargo_License_FieldLabel: 'License',
        SearchCargo_Text: 'Cargo',
        SearchCargo_Description: 'Search for components in Cargo repositories',
    }
}, function (self) {
    NX.I18n.register(self);
});
