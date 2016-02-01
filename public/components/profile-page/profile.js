define(['knockout', 'text!./profile.html', 'app', 'knockout-validation', 'jquery.fileupload'], function (ko, template, app) {

	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block',
		decorateInputElement: true
	});

	function ProfileViewModel(params) {
		var self = this;
		self.firstName = ko.observable().extend({
			required: true
		});
		self.lastName = ko.observable().extend({
			required: true
		});
		self.about = ko.observable();
		self.facebookId = ko.observable();
		self.googleId = ko.observable();
		self.avatar = {
			Original: ko.observable(),
			Tiny: ko.observable(),
			Square: ko.observable(),
			Thumbnail: ko.observable(),
			Medium: ko.observable()
		};
		self.hasGoogle = ko.computed(function () {
			return self.googleId() ? true : false;
		});
		self.hasFacebook = ko.computed(function () {
			return self.facebookId() ? true : false;
		});

		self.validationModel = ko.validatedObservable({
			firstName: self.firstName,
			lastName: self.lastName,
		});

		self.checkLogged = function () {
			if (isLogged() === false) {

				window.location = '#login';
				return;
			}
		};

		self.checkLogged();

		// Load the User information from the database and initialize the template
		$.ajax({
			type: "get",
			url: "/user/" + app.currentUser._id(),
			success: function (data) {
				self.firstName(data.firstName);
				self.lastName(data.lastName);
				self.about(data.about);
				if (data.avatar) {
					self.avatar.Original(data.avatar.Original);
					self.avatar.Tiny(data.avatar.Tiny);
					self.avatar.Square(data.avatar.Square);
					self.avatar.Thumbnail(data.avatar.Thumbnail);
					self.avatar.Medium(data.avatar.Medium);
				}
				self.facebookId(data.facebookId);
				self.googleId(data.googleId);
			},
			error: function (request, status, error) {
				console.log(request);
			}
		});

		$('#imageupload').fileupload({
			type: "POST",
			url: '/media/create',
			acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
			maxFileSize: 500000,
			done: function (e, data) {
				self.avatar.Original(data.result.original);
				self.avatar.Tiny(data.result.tiny);
				self.avatar.Square(data.result.square);
				self.avatar.Thumbnail(data.result.thumbnail);
				self.avatar.Medium(data.result.medium);
			},
			error: function (e, data) {
				$.smkAlert({
					text: 'Error uploading the file',
					type: 'danger',
					time: 10
				});
			}
		});

		// Call the global closePopup function to dispose the component without saving the changes
		self.closeWindow = function () {
			app.closePopup();
		};

		// Save the changes and dispose the component
		self.updateProfile = function () {
			if (self.validationModel.isValid()) {
				var data = {
					firstName: self.firstName,
					lastName: self.lastName,
					about: self.about,
					avatar: self.avatar
				};
				var json = ko.toJSON(data);
				$.ajax({
					type: "put",
					contentType: 'application/json',
					dataType: 'json',
					processData: false,
					url: "/user/" + app.currentUser._id(),
					data: json,
					success: function (data) {
						app.loadUser(data, true, false);
					},
					error: function (request, status, error) {
						console.log(error);
					}
				});
				app.closePopup();
			}
		};

		self.loadFromFacebook = function () {
			FB.api(
				"/" + self.facebookId() + "/picture?type=normal",
				function (response) {
					if (response && !response.error) {
						self.createMedia(response.data.url);
					}
				}
			);
		};

		self.loadFromGoogle = function () {
			$.ajax({
				type: "get",
				url: "https://www.googleapis.com/plus/v1/people/" + self.googleId() + "?key=AIzaSyA2X9H_PW8pfGnaHBc6VhQMjDPp7Yxaj5k",
				success: function (data) {
					var url = data.image.url;
					url = url.replace('sz=50', 'sz=500');
					self.createMedia(url);
				}
			});
		};

		self.createMedia = function (remoteurl) {
			$.ajax({
				type: "POST",
				url: '/media/create',
				contentType: 'application/json',
				dataType: 'json',
				processData: false,
				data: JSON.stringify({
					'url': remoteurl
				}),
				success: function (data) {
					self.avatar.Original(data.original);
					self.avatar.Tiny(data.tiny);
					self.avatar.Square(data.square);
					self.avatar.Thumbnail(data.thumbnail);
					self.avatar.Medium(data.medium);
				},
				error: function (e, data) {
					$.smkAlert({
						text: 'Error retrieving the photo!',
						type: 'danger',
						time: 10
					});
				}
			});
		};
	}

	return {
		viewModel: ProfileViewModel,
		template: template
	};
});