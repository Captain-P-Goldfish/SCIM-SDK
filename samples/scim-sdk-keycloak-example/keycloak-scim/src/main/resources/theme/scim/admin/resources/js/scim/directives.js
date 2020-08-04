module.directive('kcTabsScimList', function () {
    return {
        scope: true,
        restrict: 'E',
        replace: true,
        templateUrl: resourceUrl + '/templates/kc-tabs-scim-list.html'
    }
});