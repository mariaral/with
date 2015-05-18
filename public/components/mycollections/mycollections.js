define(['knockout', 'text!./mycollections.html', 'knockout-else', 'app'], function(ko, template, KnockoutElse, app) {
	
	function Entry(entryData) {
		var entry = ko.mapping.fromJS(entryData);
		ko.mapping.fromJS(entryData, entry);
		return entry;
	}

	function MyCollectionsModel(params) {
		KnockoutElse.init([spec={}]);
		var self = this;
		//self.route = params.route;
		var collections = [];
		self.index = ko.observable(0);
		var mapping = {
			'dbId': {
				key: function(data) {
		            return ko.utils.unwrapObservable(data.dbId);
		        }
			},
		    'firstEntries': {
		        key: function(data) {
		            return ko.utils.unwrapObservable(data.dbId);
		        }
		    }
		};
		self.myCollections = ko.mapping.fromJS([], mapping);
		var promise = app.getUserCollections();
		self.titleToEdit = ko.observable("");
        self.descriptionToEdit = ko.observable("");
        self.isPublicToEdit = ko.observable(true);
        self.index = ko.observable(0);
		$.when(promise).done(function() {
			if (sessionStorage.getItem('UserCollections') !== null) 
			  collections = JSON.parse(sessionStorage.getItem("UserCollections"));
			if (localStorage.getItem('UserCollections') !== null) 
			  collections = JSON.parse(localStorage.getItem("UserCollections"));
			/*self.myCollections(ko.utils.arrayMap(collections, function(collectionData) {
			    return new MyCollection(collectionData);
			}));*/
			ko.mapping.fromJS(collections, self.myCollections);
		});
		
		self.deleteMyCollection = function(collection) {
			collectionId = collection.dbId();
			collectionTitle = collection.title();
			showDelCollPopup(collectionTitle, collectionId);
		};
		
		self.createCollection = function() {
			createNewCollection();
		};
		
		showDelCollPopup = function(collectionTitle, collectionId) {
			$("#myModal").find("h4").html("Do you want to delete this collection?");
			var body = $("#myModal").find("div.modal-body");
			body.html("All records in that collection will be deleted.");
	        var confirmBtn = $('<button> Confirm </button>').appendTo(body);
	        confirmBtn.click(function() {
	        	deleteCollection(collectionId);
	        	$("#myModal").modal('hide');
	        });
	        var cancelBtn = $('<button> Cancel </button>').appendTo(body);
	        cancelBtn.click(function() {
	        	$("#myModal").modal('hide');
	        });
			$("#myModal").modal('show');
		};
		
	    //Storage needs to be updated, because collection.js gets user collections from there
		saveCollectionsToStorage = function(collections) {
			if (sessionStorage.getItem('User') !== null) {
				sessionStorage.setItem('UserCollections', JSON.stringify(collections));
			}
			else if (localStorage.getItem('User') !== null) {
				localStorage.setItem('UserCollections', JSON.stringify(collections));
			}
		};
		
		deleteCollection = function(collectionId) {
			$.ajax({
				"url": "/collection/"+collectionId,
				"method": "DELETE",
				//"contentType": "application/json",
				//"data": {id: collectionId}),
				success: function(result){
					self.myCollections.remove(function (item) {
                        return item.dbId() == collectionId;
                    });
					saveCollectionsToStorage(self.myCollections());
				}
			});
		};
		
		
		self.openEditCollectionPopup = function(collection, event) {
	        var context = ko.contextFor(event.target);
	        var collIndex = context.$index();
			self.index(collIndex);
	        self.titleToEdit(self.myCollections()[collIndex].title());
	        self.descriptionToEdit(self.myCollections()[collIndex].description());
	        self.isPublicToEdit(self.myCollections()[collIndex].isPublic());
			app.showPopup("edit-collection");
		}
		
		self.closeEditPopup = function() {
			app.closePopup();
		}
		
		self.editCollection = function () {
			var collIndex = self.index();
			$.ajax({
				"url": "/collection/"+self.myCollections()[collIndex].dbId(),
				"method": "POST",
				"contentType": "application/json",
				"data": JSON.stringify({title: self.titleToEdit(),
						description: self.descriptionToEdit(),
						isPublic: self.isPublicToEdit()
					}),
				success: function(result){
					self.myCollections()[collIndex].title(self.titleToEdit());
					self.myCollections()[collIndex].description(self.descriptionToEdit());
					self.myCollections()[collIndex].isPublic(self.isPublicToEdit());
					saveCollectionsToStorage(self.myCollections());
				},
				error: function(error) {
					var r = JSON.parse(error.responseText);
					$("#myModal").find("h4").html("An error occured");
					$("#myModal").find("div.modal-body").html(r.message);
					$("#myModal").modal('show');
				}
			});
			self.closeEditPopup();
		};
		
		self.privateToggle=function(e,arg){
			/*$(arg.currentTarget).parent().find('.btn').toggleClass('active');
		    if ($(arg.currentTarget).parent().find('.btn-primary').size()>0) {
		    	$(arg.currentTarget).parent().find('.btn').toggleClass('btn-primary');
		    }
		    $(arg.currentTarget).parent().find('.btn').toggleClass('btn-default');*/
		    if (self.isPublicToEdit())
		    	self.isPublicToEdit(false);
		    else
		    	self.isPublicToEdit(true);
		}
		
		self.reloadRecord = function(dbId, recordDataString) {
			/*$.ajax({
				"url": "/collection/"+dbId,
				"method": "GET",
				success: function(data){
					//TODO: Confirm that 1) myCollections array is updated (recursively) 2) as well as firstEntries
					//self.myCollections()[index](self.load());
					//alert(JSON.stringify(self.editableCollection()));
					ko.mapping.fromJS(data, self.myCollections);
					var collIndex = arrayFirstIndexOf(viewModel.items(), function(item) {
						   return item.dbId === dbId;    
					}));
					self.myCollections()[collIndex].remove((data);
				}
			});*/
			var collIndex = arrayFirstIndexOf(self.myCollections(), function(item) {
				   return item.dbId() === dbId;    
			});
			var recordData = JSON.parse(recordDataString);
			var recordObservable = ko.mapping.fromJS(recordData);
			ko.mapping.fromJS(recordData, recordObservable);
			var newItemCount = self.myCollections()[collIndex].itemCount() + 1;
			self.myCollections()[collIndex].itemCount(newItemCount);
			self.myCollections()[collIndex].firstEntries.push(recordObservable);
			saveCollectionsToStorage(self.myCollections());
		};
		
		self.reloadCollection = function(data) {
			var newCollection = ko.mapping.fromJS(data);
			ko.mapping.fromJS(data, newCollection);
			self.myCollections.push(newCollection);
			saveCollectionsToStorage(ko.mapping.toJS(self.myCollections));
		}
		
	    arrayFirstIndexOf=function(array, predicate, predicateOwner) {
		    for (var i = 0, j = array.length; i < j; i++) {
		        if (predicate.call(predicateOwner, array[i])) {
		            return i;
		        }
		    }
		    return -1;
		}
	    
	}
	
	return {viewModel: MyCollectionsModel, template: template};
});
