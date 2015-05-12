define("app", ['knockout', 'facebook'], function(ko, FB) {

	var self         = this;
	self.currentUser = {
		"_id"              : ko.observable(),
		"email"            : ko.observable(),
		"username"         : ko.observable(),
		"firstName"        : ko.observable(),
		"lastName"         : ko.observable(),
		"gender"           : ko.observable(),
		"facebookId"       : ko.observable(),
		"googleId"         : ko.observable(),
		"image"            : ko.observable(),
		"recordLimit"      : ko.observable(),
		"collectedRecords" : ko.observable(),
		"storageLimit"     : ko.observable(),
	};
	isLogged         = ko.observable(false);

	loadUser         = function(data, remember, loadCollections) {
		self.currentUser._id(data._id.$oid);
		self.currentUser.email(data.email);
		self.currentUser.username(data.username);
		self.currentUser.firstName(data.firstName);
		self.currentUser.lastName(data.lastName);
		self.currentUser.gender(data.gender);
		self.currentUser.facebookId(data.facebookId);
		self.currentUser.googleId(data.googleId);
		self.currentUser.recordLimit(data.recordLimit);
		self.currentUser.collectedRecords(data.collectedRecords);
		self.currentUser.storageLimit(data.storageLimit);
		self.currentUser.image(data.photo);

		// Save to session
		if (typeof(Storage) !== 'undefined') {
			if (remember) {
				localStorage.setItem("User", JSON.stringify(data));
			}
			else {
				sessionStorage.setItem("User", JSON.stringify(data));
			}
		}

		isLogged(true);

		if (typeof(loadCollections) === 'undefined' || loadCollections === true) {
			return getUserCollections();
		}
	};

	getUserCollections = function() {
		return $.ajax({
			type        : "GET",
			contentType : "application/json",
			dataType    : "json",
			url         : "/collection/list",
			processData : false,
			data        : "username=" + self.currentUser.username()+"&ownerId=" + self.currentUser._id() + "&email=" + self.currentUser.email() + "&offset=0" + "&count=20"}).done(

			function(data, text) {
				// console.log("User collections " + JSON.stringify(data));
				if (sessionStorage.getItem('User') !== null) {
					sessionStorage.setItem('UserCollections', JSON.stringify(data));
				}
				else if (localStorage.getItem('User') !== null) {
					localStorage.setItem('UserCollections', JSON.stringify(data));
				}

			}).fail(function(request, status, error) {

				//var err = JSON.parse(request.responseText);
			}
		);
	};

	logout           = function() {
		$.ajax({
			type        : "GET",
			url         : "/user/logout",
			success     : function() {
				sessionStorage.removeItem('User');
				localStorage.removeItem('User');
				sessionStorage.removeItem('UserCollections');
				localStorage.removeItem('UserCollections');
				isLogged(false);
				window.location.href="/assets/index.html";
			}
		});
	};

	showPopup        = function(name) {
		popupName(name);
		$('#popup').modal('show');
	};

	// Closing modal dialog and setting back to empty to dispose the component
	closePopup       = function() {
		$('#popup').modal('hide');
		popupName("empty");
	};

	// Check if user information already exist in session
	if (sessionStorage.getItem('User') !== null) {
		var sessionData = JSON.parse(sessionStorage.getItem('User'));
		loadUser(sessionData, false);
	}
	else if (localStorage.getItem('User') !== null) {
		var storageData = JSON.parse(localStorage.getItem('User'));
		loadUser(storageData, true);
	}

	return { currentUser: currentUser, loadUser: loadUser, showPopup: showPopup, closePopup: closePopup, logout: logout, getUserCollections: getUserCollections };
});
