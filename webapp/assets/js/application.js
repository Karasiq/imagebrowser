function initImageBrowser() {
    "use strict";
    var App = angular.module('imageBrowser', ['angularUtils.directives.dirPagination']);

    App.factory('galleryService', function () {
        var images = [];
        return {
            getImages: function () {
                return images;
            },
            setImages: function (data) {
                images = data;
            }
        };
    });

    App.controller('dirCtrl', ['$scope', '$log', '$location', '$http', 'galleryService', '$rootScope', function ($scope, $log, $location, $http, galleryService, $rootScope) {
        $scope.directories = [];

        $scope.currentDirectory = "";

        $scope.renderDirectoryLink = function (dir) {
            function strStartsWith(str, prefix) { // String.startsWith
                return str.indexOf(prefix) === 0;
            }

            if (!$scope.isAtRoot() && strStartsWith(dir, $scope.currentDirectory)) {
                return dir.slice($scope.currentDirectory.length + 1);
            } else {
                return dir;
            }
        };

        $scope.isAtRoot = function () {
            return $scope.currentDirectory === "";
        };

        $scope.addRootDirectory = function () {
            var path = $scope.newRootDirectory.path;
            var readMetadata = $scope.newRootDirectory.readMetadata;
            $http({
                method: 'PUT',
                url: 'directory',
                params: {
                    path: path,
                    readMetadata: readMetadata
                }
            }).success(function () {
                $log.debug("New root directory added: " + path);
                if ($scope.directories.indexOf(path) === -1) {
                    $scope.directories.push(path);
                }
                $scope.newRootDirectory = undefined;
            }).error($log.error);
        };

        $scope.deleteRootDirectory = function (dir) {
            $http({
                method: 'DELETE',
                url: 'directory',
                params: {path: dir}
            }).success(function () {
                $log.debug("Root directory deleted: " + dir);
                $scope.directories.splice($scope.directories.indexOf(dir), 1);
            }).error($log.error);
        };

        $scope.loadDirectory = function (dir) {
            function showDirectory(data) {
                $log.debug("Directory loaded: " + data.path + " (" + data.subDirs.length + "/" + data.images.length + " entries)");
                $scope.currentDirectory = data.path;
                $scope.directories = data.subDirs;
                galleryService.setImages(data.images);
            }

            function showRoot(data) {
                $log.debug("Root loaded (" + data.length + " entries)");
                $scope.currentDirectory = "";
                $scope.directories = data;
                galleryService.setImages([]);
            }

            if (dir) {
                $http({
                    url: 'directory',
                    method: 'GET',
                    params: {
                        path: dir
                    }
                }).success(showDirectory)
                    .error(function (error) {
                        $log.error(error);
                        $scope.loadRootLevel();
                    });
            } else {
                $http.get('directories')
                    .success(showRoot)
                    .error($log.error);
            }
        };

        $scope.setCurrentDirectory = function (dir) {
            $location.search("path", dir);
        };

        $scope.loadRootLevel = function () {
            $scope.setCurrentDirectory("");
        };

        $scope.loadUpperLevel = function () {
            if (!$scope.isAtRoot()) {
                $scope.setCurrentDirectory($scope.currentDirectory + "/..");
            } else {
                $scope.loadRootLevel();
            }
        };

        $scope.updateLocation = function () {
            $scope.loadDirectory($location.search());
        };

        $scope.loadDirectory($location.search().path);

        $scope.updateLocation();
        $rootScope.$on('$locationChangeSuccess', function() {
            $scope.updateLocation();
        });
    }]);

    App.controller('galleryCtrl', ['$scope', 'galleryService', function ($scope, galleryService) {
        $scope.images = function () {
            return galleryService.getImages();
        };

        $scope.imageTitle = function (image) {
            var title = "Last modified: " + image.lastModified;
            if (image.iptc) {
                title += "\nIPTC: " + JSON.stringify(image.iptc);
            }
            return title;
        };

        $scope.imageFull = function (image) {
            return "/image?path=" + encodeURIComponent(image);
        };

        $scope.imageThumbnail = function (image) {
            return "/thumbnail?path=" + encodeURIComponent(image);
        };
    }]);
}

// Initialize application
initImageBrowser();