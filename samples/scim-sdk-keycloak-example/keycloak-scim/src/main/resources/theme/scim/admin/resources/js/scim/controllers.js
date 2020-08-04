/*
 * inspired by beercloak: https://github.com/dteleguin/beercloak
 */
module.controller('ServiceProviderController', function ($modal, $scope, realm, ServiceProvider, serviceProvider,
                                                         $location, $route, Dialog, Notifications) {

    $scope.realm = realm;
    $scope.serviceProvider = serviceProvider;
    $scope.copy = angular.copy(serviceProvider);

    $scope.changed = false;

    $scope.$watch('serviceProvider', function (serviceProvider) {
        if (!angular.equals($scope.copy, serviceProvider)) {
            $scope.changed = true;
        } else {
            $scope.changed = false;
        }
    }, true);

    $scope.save = function () {
        ServiceProvider.update(
            {
                realm: realm.realm
            },
            $scope.serviceProvider,
            function (response) {
                $scope.changed = false;
                $scope.serviceProvider = response;
                $scope.copy = angular.copy($scope.serviceProvider);
                Notifications.success("Your changes have been saved to ServiceProvider.");
            }
        );
    };

    $scope.reset = function () {
        $scope.serviceProvider = angular.copy($scope.copy);
        $scope.changed = false;
    };

});

module.controller('ResourceTypeController', function ($scope, realm, ResourceType, listResponse) {

    $scope.realm = realm;
    $scope.resourceTypeList = listResponse.Resources;
    $scope.featureKey = 'urn:gold:params:scim:schemas:extension:url:2.0:ResourceTypeFeatures';

});
