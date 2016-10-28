<#compress>
<!doctype html>
<html ng-app="dirApp" lang="en">
<head>
    <title>Dirlist</title>

    <link rel="stylesheet"
          href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
    <script>var contextPath = "${contextPath}"</script>
    <script src="FileSaver.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.8/angular.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.8/angular-resource.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.5.8/angular-route.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
    <script src="dirApp.js"></script>
    <script src="dirService.js"></script>
    <script src="dirController.js"></script>
    <meta charset="utf-8">
</head>

<body ng-app="dirApp" class="ng-cloak">
<div class="container-fluid dir-list-container" ng-controller="dirController as ctrl">
    <ul ng-repeat="u in ctrl.files | filter : {'type' : 'DIRECTORY'}">
        <li class="glyphicon glyphicon-folder-close"><a href="{{u.path}}"
                                                        target="_self">{{u.name}}</a></li>
    </ul>
    <ul ng-repeat="u in ctrl.files | filter : {'type' : 'FILE'}">
        <li class="glyphicon glyphicon-file"><a href="{{u.path}}" target="_self">{{u.name}}</a>
            <button class="btn btn-primary btn-xs" type="button" ng-click="extract(u.path)"
                    ng-show="{{u.archiveType}}">Extract
            </button>
        </li>
    </ul>
</div>
</body>
</html>
</#compress>