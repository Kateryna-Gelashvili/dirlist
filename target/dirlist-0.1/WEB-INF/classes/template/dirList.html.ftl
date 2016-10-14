<#compress>
<!doctype html>
<html ng-app="dirApp">
<head>
    <title>Dirlist</title>

    <script>var contextPath = "${contextPath}"</script>
    <script src="FileSaver.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.8/angular.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.8/angular-resource.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.8/angular-route.js"></script>
    <script src="dirApp.js"></script>
    <script src="dirService.js"></script>
    <script src="dirController.js"></script>
</head>

<body ng-app="dirApp" class="ng-cloak">
<div class="generic-container" ng-controller="dirController as ctrl">
    <ul ng-repeat="u in ctrl.files">
        <li><a href="{{u.path}}" target="_self">{{u.name}}</a></li>
        <li>{{u.type}}</li>
    </ul>
</div>
</body>
</html>
</#compress>