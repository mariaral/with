define(['knockout', 'text!./login-register.html',  'facebook', 'app', 'knockout-validation', 'google'], function(ko, template, FB, app) {

	FB.init({
		appId   : '1584816805087190',
		status  : true,
		version : 'v2.2'
	});

	ko.validation.init({
		errorElementClass: 'has-error',
		errorMessageClass: 'help-block',
		decorateInputElement: true
	});

	function LoginRegisterViewModel(params) {
		var self = this;

		// Template variables
		self.title        = ko.observable('Join with your email address');
		self.description  = ko.observable('');
		if (typeof params.title !== 'undefined') {	// To avoid problems with template-less components
			self.templateName = ko.observable(params.title.toLowerCase());
		}

		// Registration Parameters
		self.acceptTerms  = ko.observable(false);
		self.genders      = ko.observableArray(['Female', 'Male', 'Unspecified']);
		self.facebookid   = ko.observable('');
		self.googleid     = ko.observable('');
		self.firstName    = ko.observable('').extend({ required: true });
		self.lastName     = ko.observable('').extend({ required: true });
		self.email        = ko.observable('').extend({ required: true, email: true });
		self.username     = ko.observable('').extend({ required: true, minLength: 4, maxLength: 32 });
		self.password     = ko.observable('').extend({
			required  : { onlyIf : function() { return (self.facebookid() == "" && self.googleid() == "") } }
		});
		self.password2    = ko.observable('').extend({ equal: self.password });
		self.gender       = ko.observable();

		self.validationModel = ko.validatedObservable({
			firstName : self.firstName,
			lastName  : self.lastName,
			email     : self.email,
			username  : self.username,
			password  : self.password,
			password2 : self.password2
		});

		// Email Login
		self.emailUser       = ko.observable('').extend({ required: true });
		self.emailPass       = ko.observable('').extend({ required: true });
		self.stayLogged      = ko.observable(false);
		self.loginValidation = ko.validatedObservable({ email: self.emailUser, password: self.emailPass });

		// Control variables
		self.usingEmail  = ko.observable(true);

		// Functionality
		self.fbRegistration       = function() {
			FB.login(function(response) {
				if (response.status === 'connected') {
					FB.api('/me', function(response) {
						self.title('You are almost ready...');
						self.description('We loaded your account with your Facebook details. Help us with just a few more questions.' +
							' You can always edit this or any other info in settings after joining.');
						self.facebookid(response.id);
						self.email(response.email);
						self.firstName(response.first_name);
						self.lastName(response.last_name);
						self.username(response.first_name.toLowerCase() + '.' + response.last_name.toLowerCase());
						self.gender(response.gender === 'male' ? 'Male' : (response.gender === 'female' ? 'Female' : 'Unspecified'));
						self.usingEmail(false);
						self.templateName('email');
					});
				}
				else if (response.status === 'not_athorized') {
					// User didn't authorize the application
				}
				else {
					// User is not logged
				}
			}, {scope: 'public_profile, email'});
		}
		self.googleRegistration   = function() {
			gapi.auth.signIn({
				'clientid'     : '712515719334-u6ofvnotfug9ktv0e9kou7ms2cq9lb85.apps.googleusercontent.com',
				'cookiepolicy' : 'single_host_origin',
				'scope'        : 'profile email',
				'callback'     : function(authResult) {
					if (authResult['status']['signed_in']) {
						gapi.client.load('plus','v1', function() {
							var request = gapi.client.plus.people.get({ 'userId': 'me' });
							request.execute(function(response) {
								self.title('You are almost ready...');
								self.description('We loaded your account with your Google details. Help us with just a few more questions.' +
									' You can always edit this or any other info in settings after joining.');
								self.googleid(response['id']);
								self.email(response['emails'][0]['value']);
								self.firstName(response['name']['givenName']);
								self.lastName(response['name']['familyName']);
								self.username(response['name']['givenName'].toLowerCase() + '.' + response['name']['familyName'].toLowerCase());
								self.gender(response['gender'] === 'male' ? 'Male' : (response['gender'] === 'female' ? 'Female' : 'Unspecified'));
								self.usingEmail(false);
								self.templateName('email');
							});
						});
					}
				}
			});
		}
		self.emailRegistration    = function() {
			self.usingEmail(true);
			self.templateName('email');
		}

		self.submitRegistration   = function() {
			if (self.validationModel.isValid()) {
				var data = {
					firstName  : self.firstName,
					lastName   : self.lastName,
					username   : self.username,
					email      : self.email,
					password   : self.password,
					gender     : self.gender,
					googleId   : self.googleid,
					facebookId : self.facebookid
				};

				var json = ko.toJSON(data);
				$.ajax({
					type        : "post",
					contentType : 'application/json',
					dataType    : 'json',
					processData : false,
					url         : "/user/register",
					data        : json,
					success     : function(data, text) {
						// console.log(app.currentUser());
						app.loadUser(data);
						self.templateName('postregister');
					},
					error       : function(request, status, error) {
						var err = JSON.parse(request.responseText);
						if (err.error.email !== undefined) {
							self.email.setError(err.error.email);
							self.email.isModified(true);
						}
						if (err.error.username !== undefined) {
							self.username.setError(err.error.username + " (Suggestions: " + err.proposal[0] + ", " + err.proposal[1] +")");
							self.username.isModified(true);
						}
						if (err.error.password !== undefined) {
							self.password.setError(err.error.password);
							self.password.isModified(true);
						}
						self.validationModel.errors.showAllMessages();
					}
				});
			}
			else {
				self.validationModel.errors.showAllMessages();
			}
		}

		self.emailLogin           = function(popup, callback) {
			if (self.loginValidation.isValid()) {
				var json = ko.toJSON(self.loginValidation);

				$.ajax({
					type        : "post",
					contentType : 'application/json',
					dataType    : 'json',
					processData : false,
					url         : "/user/login",
					data        : json,
					success     : function (data, text) {
						app.loadUser(data, self.stayLogged());

						if (typeof popup !== 'undefined') {
							self.emailUser(null);
							self.emailPass(null);
							if (popup) { self.closeLoginPopup(); }

							if (typeof callback !== 'undefined') {
								callback();
							}
						}
						else {
							window.location.href = "#";
						}
					},
					error   : function (request, status, error) {
						console.log(request);
						console.log(error);
						// TODO: Show error messages
					}
				});
			}
			else {
				self.loginValidation.errors.showAllMessages();
			}
		}
		self.googleLogin          = function(popup, callback) {
			gapi.auth.signIn({
				'clientid'     : '712515719334-u6ofvnotfug9ktv0e9kou7ms2cq9lb85.apps.googleusercontent.com',
				'cookiepolicy' : 'single_host_origin',
				'scope'        : 'profile email',
				'callback'     : function(authResult) {
					if (authResult['status']['signed_in'] && authResult['status']['method'] === 'PROMPT') {
						gapi.client.load('plus','v1', function() {
							var request = gapi.client.plus.people.get({ 'userId': 'me' });
							request.execute(function(response) {
								self.emailUser(response['emails'][0]['value']);
								self.googleid(response['id']);

								var json = ko.toJSON(self.loginValidation);

								$.ajax({
									type        : "get",
									// contentType : 'application/json',
									// dataType    : 'json',
									// processData : false,
									url         : "/user/googleLogin",
									data        : { accessToken: authResult['access_token'] },
									success     : function(data, text) {
										app.loadUser(data, true);
										if (typeof popup !== 'undefined') {
											if (popup) { self.closeLoginPopup(); }

											if (typeof callback !== 'undefined') {
												callback(params.item);
											}
										}
										else {
											window.location.href = "#";
										}
									},
									error       : function(request, status, error) {
										console.log(request);
										console.log(status);
										console.log(error);
									}
								});
							});
						});
					}
				}
			});
		}
		self.fbLogin              = function(popup, callback) {
			FB.login(function(response) {
				if (response.status === 'connected') {
					FB.api('/me', function(response) {
						self.facebookid(response.id);
						self.emailUser(response.email);

						var json = ko.toJSON(self.loginValidation);
						console.log(json);

						console.log(response);

						$.ajax({
							type        : "get",
							// contentType : 'application/json',
							// dataType    : 'json',
							// processData : false,
							url         : "/user/facebookLogin",
							data        : { accessToken: FB.getAuthResponse()['accessToken'] },
							success     : function(data, text) {
								app.loadUser(data, true);
								if (typeof popup !== 'undefined') {
									if (popup) { self.closeLoginPopup(); }

									if (typeof callback !== 'undefined') {
										callback(params.item);
									}
								}
								else {
									window.location.href = "#";
								}
							},
							error       : function(request, status, error) {
								console.log(request);
								console.log(status);
								console.log(error);
							}
						});
						// TODO: Send to server to sign in
						// TODO: Add the user to the global app: app.currentUser('finik');
						app.currentUser('finik'); // TODO: REMOVE
						if (typeof popup !== 'undefined') {
							if (popup) { self.closeLoginPopup(); }

							if (typeof callback !== 'undefined') {
								callback(params.item);
							}
						}
						else {
							window.location.href = "#";
						}
					});
				}
			}, {scope: 'email'});
		}

		showLoginPopup            = function(record) {
			$('#loginPopup').addClass('open');
		}

		self.scrollEmail          = function() {
			$('.externalLogin').slideUp();
		}

		self.closeLoginPopup      = function() {
			$('#loginPopup').removeClass('open');
			$('.externalLogin').slideDown();	// Reset dialog state
		}

		self.completeRegistration = function() {
			// TODO: Get values, send to server and redirect to the landing page
		}

		self.route = params.route;
	}

	return { viewModel: LoginRegisterViewModel, template: template };
});
