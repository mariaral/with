define(['jquery','knockout', './router', 'knockout-mapping', 'bootstrap', 'knockout-projections', 'knockout-amd-helpers', 'header'], function ($, ko, router, kom) {

	// Knockout AMD Helpers Initialization
	ko.amdTemplateEngine.defaultPath                  = 'templates';
	ko.amdTemplateEngine.defaultSuffix                = '.tpl.html';
	ko.amdTemplateEngine.defaultRequireTextPluginName = 'text';

	ko.mapping = kom;

	// Components can be packaged as AMD modules, such as the following:
	ko.components.register('nav-bar', { require: 'components/nav-bar/nav-bar' });
	ko.components.register('top-bar', { require: 'components/top-bar/top-bar'});
	ko.components.register('home-page', { require: 'components/home-page/home' });
	ko.components.register('main-content', { require: 'components/main-content/main-content' });
	ko.components.register('search-page', { require: 'components/search-page/search' });
	ko.components.register('item-view', { require: 'components/item-view/item' });
	ko.components.register('collection', { require: 'components/collection/collection' });
	ko.components.register('login-page', { require: 'components/login-register-page/login-register' });
	ko.components.register('register-page', { require: 'components/login-register-page/login-register' });
	ko.components.register('myexhibitions', { require: 'components/myexhibitions/myexhibitions' });
	ko.components.register('mycollections', { require: 'components/mycollections/mycollections' });
	ko.components.register('myfavorites', { require: 'components/myfavorites/myfavorites' });
	ko.components.register('collection-view', { require: 'components/collection-view/collection-view' });
	ko.components.register('facets', { require: 'components/facets/facets' });
	ko.components.register('testsearch', { require: 'components/testsearch/testsearch' });

	ko.components.register('popup-login', {
		viewModel: { require: 'components/login-register-page/login-register' },
		template: { require: 'text!components/login-register-page/popup-login.html' }
	});
	ko.components.register('edit-profile', {
		viewModel: { require: 'components/profile-page/profile' },
		template: { require: 'text!components/profile-page/edit-profile.html' }
	});
	ko.components.register('edit-collection', {
		viewModel: { instance: 'components/mycollections/mycollections' },
		template: { require: 'text!components/mycollections/edit-collection.html' }
	});
	ko.components.register('share-collection', {
		viewModel: { instance: 'components/mycollections/mycollections' },
		template: { require: 'text!components/mycollections/share-collection.html' }
	});
	ko.components.register('reset-password', { require: 'components/login-register-page/reset-password' });
	ko.components.register('new-password', {
		viewModel: { require: 'components/login-register-page/reset-password' },
		template: { require: 'text!components/login-register-page/login-register.html' }
	});
	ko.components.register('image-upload', {
		viewModel: { require: 'components/media-uploader/media-uploader' },
		template: { require: 'text!components/media-uploader/image-upload.html' }
	});
	// ... or for template-only components, you can just point to a .html file directly:
	ko.components.register('empty', { template: '&nbsp;' });

	// [Scaffolded component registrations will be inserted here. To retain this feature, don't remove this comment.]
	popupName = ko.observable('empty');


	// Start the application
	ko.applyBindings({ route: router.currentRoute, popupName: popupName });
});
