'use strict';

var dirApp = angular.module('dirApp', ['ngResource', 'ngRoute'], function ($locationProvider) {
    $locationProvider.html5Mode({
        enabled: true,
        requireBase: false
    });
});

dirApp.filter('encodeURIComponent', function () {
    return window.encodeURIComponent;
});