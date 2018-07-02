/*
 * Copyright 2019, Imperva, Inc. All rights reserved.
 *
 * Imperva, the Imperva logo, SecureSphere, Incapsula, CounterBreach,
 * ThreatRadar, Camouflage, Attack Analytics, Prevoty and design are trademarks
 * of Imperva, Inc. and its subsidiaries. All other brand or product names are
 * trademarks or registered trademarks of their respective holders.
 */

Ext.define('NX.cargo.util.CargoRepositoryUrls', {
    '@aggregate_priority': 90,

    singleton: true,
    requires: [
        'NX.coreui.util.RepositoryUrls',
        'NX.util.Url'
    ]
}, function (self) {
    NX.coreui.util.RepositoryUrls.addRepositoryUrlStrategy('cargo', function (assetModel) {
        var repositoryName = assetModel.get('repositoryName'), assetName = assetModel.get('name');
        return NX.util.Url.asLink(NX.util.Url.baseUrl + '/repository/' + repositoryName + '/' + assetName, assetName);
    });
});
