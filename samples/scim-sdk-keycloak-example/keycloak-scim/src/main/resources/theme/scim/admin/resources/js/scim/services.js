/*
 * inspired by beercloak: https://github.com/dteleguin/beercloak
 */
module.factory('ServiceProvider', function ($resource) {
    return $resource(authUrl + '/realms/:realm/scim/v2/ServiceProviderConfig',
        {},
        {
            update: {
                method: 'PUT',
                url: authUrl + '/realms/:realm/scim/admin/serviceProviderConfig'
            }
        });
});

module.factory('ServiceProviderLoader', function (Loader, ServiceProvider, $route, $q) {
    return Loader.get(ServiceProvider, function () {
        return {
            realm: $route.current.params.realm
        };
    });
});

/* ***************************************************************************************************** */

module.factory('ResourceType', function ($resource) {
    return $resource(authUrl + '/realms/:realm/scim/v2/ResourceTypes?sortBy=name',
        {},
        {});
});

module.factory('ResourceTypeLoader', function (Loader, ResourceType, $route, $q) {
    return Loader.get(ResourceType, function () {
        return {
            realm: $route.current.params.realm
        };
    });
});
