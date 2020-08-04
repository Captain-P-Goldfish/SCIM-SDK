/*
 * inspired by beercloak: https://github.com/dteleguin/beercloak
 */
module.config(['$routeProvider', function ($routeProvider) {
    $routeProvider
        .when('/realms/:realm/scim/service-provider', {
            templateUrl: resourceUrl + '/partials/service-provider.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                serviceProvider: function (ServiceProviderLoader) {
                    return ServiceProviderLoader();
                }
            },
            controller: 'ServiceProviderController'
        })
        .when('/realms/:realm/scim/resource-type-list', {
            templateUrl: resourceUrl + '/partials/resource-type-list.html',
            resolve: {
                realm: function (RealmLoader) {
                    return RealmLoader();
                },
                listResponse: function (ResourceTypeLoader) {
                    return ResourceTypeLoader();
                }
            },
            controller: 'ResourceTypeController'
        });
}]);
