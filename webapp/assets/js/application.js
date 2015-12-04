(function() {
    "use strict";
    var App = angular.module('imageBrowser', ['angularUtils.directives.dirPagination']);

    App.controller('imageBrowserCtrl', ['$scope', '$log', '$location', '$http', '$rootScope', function ($scope, $log, $location, $http, $rootScope) {
        // Model
        $scope.directories = [];

        $scope.images = [];

        $scope.currentDirectory = "";

        $scope.pagination = {
            directories: 1,
            images: 1
        };


        /**
         * Returns [tree (string array), path separator (string)] or undefined
         * @param dir Directory full path
         * @returns {Array, undefined}
         */
        function parsePathTree(dir) {
            var tree, separator;
            if (dir.indexOf("\\") > -1) { // Windows
                tree = dir.split("\\");
                separator = "\\";
            } else if (dir.indexOf("/") > -1) { // Linux
                tree = dir.split("/");
                separator = "/";
            }

            if (tree && separator) {
                return [tree, separator];
            } else {
                return undefined;
            }
        }

        function strStartsWith(str, prefix) { // String.startsWith
            return str.indexOf(prefix) === 0;
        }

        function getPageTitle(directory) {
            var baseTitle = "ImageBrowser";
            var tree = parsePathTree(directory);
            if (tree) {
                var dirTitle;
                var dirTitleSize = 3;
                if (tree[0].length > dirTitleSize) {
                    // "/1/2/3/4/5" -> ".../3/4/5"
                    dirTitle = "..." + tree[1] + tree[0].slice(Math.max(tree[0].length - dirTitleSize, 0)).reduce(function (result, node) {
                            return result + tree[1] + node;
                        });
                } else {
                    dirTitle = directory; // As is
                }

                return baseTitle + " - " + dirTitle;
            } else {
                return baseTitle;
            }
        }

        // Public functions
        $scope.renderDirectoryLink = function (dir) {
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
                params: {
                    path: dir
                }
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
                $scope.images = data.images;
                document.title = getPageTitle(data.path);
                if (data.images.length === 0) {
                    $('#folders-tab-control').tab('show');
                } else if (data.subDirs.length === 0) {
                    $('#images-tab-control').tab('show');
                }
            }

            function showRoot(data) {
                $log.debug("Root loaded (" + data.length + " entries)");
                $scope.currentDirectory = "";
                $scope.directories = data;
                $scope.images = [];
                document.title = getPageTitle("");
            }

            if (dir !== undefined && dir.length > 0) {
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
            if ($location.search().path !== dir) {
                if (dir === undefined || dir.length === 0) {
                    $location.search("path", undefined);
                } else {
                    $location.search("path", dir);
                }
                $scope.pagination.directories = 1;
                $scope.pagination.images = 1;
            }
        };

        $scope.loadRootLevel = function () {
            $scope.setCurrentDirectory("");
        };

        $scope.getDirectoryName = function (dir) {
            var tree = parsePathTree(dir);
            if (tree) {
                return tree[0].pop(); // Last element
            }
        };

        $scope.getUpperLevel = function (dir) {
            var tree = parsePathTree(dir);
            if (tree) {
                tree[0].pop(); // Remove last element
                return tree[0].reduce(function (result, node) {
                    return result + tree[1] + node;
                });
            } else {
                return "";
            }
        };

        $scope.loadUpperLevel = function () {
            if (!$scope.isAtRoot()) {
                var upper = $scope.getUpperLevel($scope.currentDirectory);
                $scope.setCurrentDirectory(upper);
            } else {
                $scope.loadRootLevel();
            }
        };

        $scope.updateLocation = function () {
            var params = $location.search();

            var dir = params.path;
            if (dir !== $scope.currentDirectory) {
                $log.debug("Changing directory to " + dir);
                $('#folders-tab-control').tab('show'); // Switch to folders tab
                $scope.loadDirectory(dir);
            }

            function readPage(param) {
                var int = parseInt(param);
                return isNaN(int) ? 1 : int;
            }
            $scope.pagination.directories = readPage(params.dirs_page);
            $scope.pagination.images = readPage(params.images_page);
        };

        function getFileName(image) {
            return image.slice($scope.currentDirectory.length + 1);
        }

        // Gallery functions
        $scope.imageTitle = function (image) {
            var title = getFileName(image.path) + "\nLast modified: " + image.lastModified;
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

        $scope.showImage = function (image) {
            function extOf(image) {
              var p = image.split(/\./);
              if (p.length > 0) {
                return p[p.length - 1].toLowerCase();
              } else {
                return "";
              }
            }
          
            function screenSize() {
                var w, h;
                w = (window.innerWidth
                    ? window.innerWidth
                    : (document.documentElement.clientWidth
                    ? document.documentElement.clientWidth
                    : document.body.offsetWidth));
                h = (window.innerHeight
                    ? window.innerHeight
                    : (document.documentElement.clientHeight
                    ? document.documentElement.clientHeight
                    : document.body.offsetHeight));
                return {width: w, height: h};
            }

            function showBig(img) {
                var width = screenSize().width;
                if (img.width > width) {
                    img.width = width - 60;
                }

                var popup = document.createElement('div');
                popup.style.marginTop = '50px';
                popup.style.position = 'fixed';
                popup.style.height = '100%';
                popup.style.width = '100%';
                popup.style.left = '0px';
                popup.style.top = '0px';
                popup.style.zIndex = '999';
                popup.style.overflow = 'auto';
                popup.style.textAlign = 'center';
                popup.style.backgroundColor = 'rgba(0,0,0,0.3)';
                popup.style.cursor = 'pointer';
                document.body.style.overflow = 'hidden';

                popup.onmousedown = function () {
                    return false;
                };
                popup.onclick = function () {
                    document.body.style.overflow = 'auto';
                    this.parentNode.removeChild(this);
                };

                popup.appendChild(img);

                document.body.appendChild(popup);
            }

            switch (extOf(image)) {
              case "jpg":
              case "jpeg":
              case "bmp":
              case "gif":
              case "png":
                // Show in current page
                var im = document.createElement('img');
                im.src = $scope.imageFull(image);
                im.onload = function () {
                    showBig(this);
                };
                break;
                
              default:
                // Open in new tab
                window.open($scope.imageFull(image), '_blank');
            }
        };

        // Events
        $scope.updateLocation();
        $rootScope.$on('$locationChangeSuccess', function () {
            $scope.updateLocation();
        });

        $scope.$watch('pagination.directories', function(page) {
            $location.search("dirs_page", page > 1 ? page.toString() : undefined);
        });

        $scope.$watch('pagination.images', function(page) {
            $location.search("images_page", page > 1 ? page.toString() : undefined);
        });
    }]);
})();