// require.js looks for the following global when initializing
var require = {
	baseUrl: ".",
	paths: {
		"jquery"                    : "bower_modules/jquery/dist/jquery.min",
		"bridget"                   : "bower_modules/jquery-bridget/jquery.bridget",
		"bootstrap"                 : "bower_modules/components-bootstrap/js/bootstrap.min",
		"crossroads"                : "bower_modules/crossroads/dist/crossroads.min",
		"hasher"                    : "bower_modules/hasher/dist/js/hasher.min",
		"knockout"                  : "bower_modules/knockout/dist/knockout",
		"knockout-projections"      : "bower_modules/knockout-projections/dist/knockout-projections",
		"app"                       : "js/app/app",
		"signals"                   : "bower_modules/js-signals/dist/signals.min",
		"text"                      : "bower_modules/requirejs-text/text",
		"holder"                    : "bower_modules/hasherjs/dist/js/hasher.min",
		"eventie"                   : "bower_modules/eventie/eventie",
		"eventEmitter"              : "bower_modules/eventEmitter/EventEmitter",
		"doc-ready"                 : "bower_modules/doc-ready/doc-ready",
		"get-style-property"        : "bower_modules/get-style-property/get-style-property",
		"get-size"                  : "bower_modules/get-size/get-size",
		"matches-selector"          : "bower-modules/matches-selector/matches-selector",
		"outlayer"                  : "bower-modules/outlayer/outlayer",
		"imagesloaded"              : "bower_modules/imagesloaded/imagesloaded.pkgd.min",
		"masonry"                   : "bower_modules/masonry/dist/masonry.pkgd",
		"facebook"                  : "//connect.facebook.net/en_US/all",
		"google"                    : "https://apis.google.com/js/client:platform",
		"knockout-validation"       : "bower_modules/knockout-validation/dist/knockout.validation.min",
		"knockout-amd-helpers"      : "bower_modules/knockout-amd-helpers/build/knockout-amd-helpers.min",
		"selectize"                 : "bower_modules/selectize/dist/js/standalone/selectize.min",
		"flip"                      : "bower_modules/flip/dist/jquery.flip.min",
		"autocomplete"              : "bower_modules/devbridge-autocomplete/dist/jquery.autocomplete.min",
		"jquery.ui.widget"          : "bower_modules/jquery-ui/ui/minified/widget.min",
		"load-image"                : "bower_modules/blueimp-load-image/js/load-image",
		"canvas-to-blob"            : "bower_modules/blueimp-canvas-to-blob/js/canvas-to-blob.min",
		"jquery.fileupload"         : "bower_modules/jquery-file-upload/js/jquery.fileupload",
		"knockout-else"             : "bower_modules/knockout-else/dist/knockout-else",
		"knockout-mapping"          : "bower_modules/knockout-mapping/build/output/knockout.mapping-latest"
	},
	shim: {
		"knockout":  { exports: 'ko' },
		"bootstrap": { deps: ["jquery"] },
		"facebook":  { exports: "FB" },
		/*"komapping": {
            deps: ['knockout'],
            exports: 'komapping'
        }*/
	}
};
