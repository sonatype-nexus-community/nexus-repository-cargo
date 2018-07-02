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
