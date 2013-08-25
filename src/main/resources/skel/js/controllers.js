var qma = angular.module('qma', []);

function ModelDetailsCtrl($scope, $routeParams) {
	
	$scope.fieldsOrderBy = 'name';
	
	$scope.model = $scope.plugins[$routeParams.pluginIdentifier].models[$routeParams.modelName];
	
	function fieldShouldBeMarked(field) {
		return (field.name === $routeParams.fieldName);
	}
	
	$scope.getMarkedClass = function (field) {
		if (fieldShouldBeMarked(field)) {
			return 'warning';
		}
	};
	
	$scope.shouldScroll = function (field) {
		return fieldShouldBeMarked(field);
	};
	
	$scope.getClassPkgAndName = function (qualifiedClassName) {
		var classNameArr = qualifiedClassName.split('.'),
			className = classNameArr.pop(),
			classPackage = classNameArr.join('.');
		return {
			classPackage : classPackage,
			className : className
		};
	};
	
	$scope.isDefined = function(value) {
		return angular.isDefined(value);
	}
	
}

function PluginDetailsCtrl($scope, $routeParams) {
	$scope.plugin = $scope.plugins[$routeParams.pluginIdentifier];
}

qma.directive('scrollIf', function () {
	return function (scope, element, attributes) {
		setTimeout(function () {
			if (scope.$eval(attributes.scrollIf)) {
				window.scrollTo(0, element[0].offsetTop - 100)
			}
		});
	};
});

qma.config(['$routeProvider', function ($routeProvider) {
	$routeProvider
	.when('/welcome', {
		templateUrl : 'welcome.html'
	})
	.when('/plugin/:pluginIdentifier', {
		templateUrl : 'pluginDetails.html',
		controller : PluginDetailsCtrl
	})
	.when('/model/:pluginIdentifier-:modelName', {
		templateUrl : 'modelDetails.html',
		controller : ModelDetailsCtrl
	})
	.when('/model/:pluginIdentifier-:modelName/field/:fieldName', {
		templateUrl : 'modelDetails.html',
		controller : ModelDetailsCtrl
	})
	.otherwise({
		redirectTo : '/welcome'
	});
}]);

qma.filter('extractName', function () {
	return function (item, text) {
		var res = [];
		angular.forEach(item, function (model) {
			if (model.hasOwnProperty('name')) {
				res.push(model.name);
			}
		});
		return res;
	};
});

qma.controller('TestController', function ($scope) {
	
	function setActiveMenuItem(pluginName, modelName) {
		$scope.activeMenuItem = {
				pluginName : pluginName,
				modelName : modelName
			};
	}
	
	setActiveMenuItem();

	$scope.isMenuGroupVisible = function (pluginIdentifier) {
		return angular.isDefined($scope.activeMenuItem) && $scope.activeMenuItem.pluginName === pluginIdentifier;
	};
	
	$scope.toggleMenuGroupVisible = function (pluginIdentifier) {
		setActiveMenuItem(pluginIdentifier);
	};
	
	$scope.isActive = function (pluginName, modelName) {
		if (!$scope.isMenuGroupVisible(pluginName)) {
			return null;
		} else if ($scope.activeMenuItem.modelName === modelName) {
			return 'active';
		}
	};
	
	$scope.menuStruct = [];
	angular.forEach(QMDT.data, function (pluginData, pluginIdentifier) {
		var plugin = {
			name : pluginIdentifier,
			visible : false,
			models : []
		};
		
		angular.forEach(pluginData.models, function (modelContents, modelName) {
			this.push({name : modelName});
		}, plugin.models);
		
		this.push(plugin);
	}, $scope.menuStruct);
	
	
	$scope.plugins = QMDT.data;
	
});
