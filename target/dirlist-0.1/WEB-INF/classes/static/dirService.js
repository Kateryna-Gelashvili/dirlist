'use strict';

dirApp.factory('dirService', ['$resource', '$location', function ($resource, $location) {
    return $resource(contextPath + '/api' + $location.url().substring(contextPath.length), {}, {
        query: {
            method: 'GET',
            responseType: 'json',
            isArray: true,
            transformResponse: function (response) {
                response.forEach(function (obj) {
                    var normalizedPath = obj.path.charAt(obj.path.length - 1) === '/' ?
                        obj.path.substring(0, obj.path.length - 1) : obj.path;

                    obj.name = normalizedPath.replace(/^.*[\\\/]/, '');
                    obj.path = contextPath + '/' + obj.path;
                });
                if (contextPath.replace(/\/$/, "") !== $location.url().replace(/\/$/, "")) {
                    response.unshift({
                        name: '..',
                        path: '../',
                        type: 'DIRECTORY'
                    });
                }

                return response;
            }
        }
    });
}]);