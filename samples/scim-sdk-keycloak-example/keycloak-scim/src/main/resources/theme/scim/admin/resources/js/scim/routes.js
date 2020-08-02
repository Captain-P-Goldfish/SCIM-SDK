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
        });
}]);
