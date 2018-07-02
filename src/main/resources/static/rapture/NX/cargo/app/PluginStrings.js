/**
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
