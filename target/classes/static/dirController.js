'use strict';

dirApp.controller('dirController',
    ['$q', '$http', '$scope', '$location', '$window', 'dirService',
        function ($q, $http, $scope, $location, $window, dirService) {
            var self = this;
            $scope.fetchAllFiles = function () {
                self.files = dirService.query();
            };

            $scope.fetchAllFiles();
        }]);