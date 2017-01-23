/*global cordova, module*/

module.exports = {
	init: function (name, successCallback, errorCallback) {
	    cordova.exec(successCallback, errorCallback, "MFS100Sample", "init", [name]);
	},
    uninit: function (name, successCallback, errorCallback) {
	    cordova.exec(successCallback, errorCallback, "MFS100Sample", "uninit", [name]);
	},
    startcapture: function (name, successCallback, errorCallback) {
	    cordova.exec(successCallback, errorCallback, "MFS100Sample", "startcapture", [name]);
	},
	stopcapture: function (name, successCallback, errorCallback) {
	    cordova.exec(successCallback, errorCallback, "MFS100Sample", "stopcapture", [name]);
	},
	autocapture: function (name, successCallback, errorCallback) {
	    cordova.exec(successCallback, errorCallback, "MFS100Sample", "autocapture", [name]);
	},
	matchiso: function (name, successCallback, errorCallback) {
	    cordova.exec(successCallback, errorCallback, "MFS100Sample", "matchiso", [name]);
	},
	openPdf: function (name, successCallback, errorCallback) {
	    cordova.exec(successCallback, errorCallback, "MFS100Sample", "openPdf", [name]);
	}
};
