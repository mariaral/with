define(['bridget','knockout', 'text!./main-content.html','isotope','imagesloaded','app','jquery.sticky'], function(bridget,ko, template,Isotope,imagesLoaded,app,sticky) {
	
	
	$.bridget('isotope', Isotope);
	
	//self.loading=ko.observable(false);
		
	ko.bindingHandlers.homeisotope = {
					init: app.initOrUpdate('init'),
					update: app.initOrUpdate('update')
				};
	
	
	// method to initialize filter stick plugin
	// dependency sticker
	// moved to here from plugin.js, no need to track scroll on the rest of the pages
	var initFilterStick = function(){

		// log
		//logger( 'info','plugins.js / initSticky' );
 
		if ( $( '.filter' ).length !== 0 ){

			// init sticky
			$( '.filter' ).sticky({
				topSpacing: 74
			});

			// on scroll
			$( window ).on( 'scroll touchmove', function(){

				// var
				if( $( "body" ).attr( 'data-page' )=== 'home' ) {
				var offset = $( '.partners' ).offset();
				if(offset){
					topPos = offset.top - 74;
	
					// set class
					if ( $( document ).scrollTop() >= topPos ){
	
						$( '.filter' ).addClass('unstick');
	
					} else {
	
						$( '.filter' ).removeClass('unstick');
	
					}
				}}
			});
		
	}};
	
	
	
	function Collection(data) {
		var self=this;
		
		var mapping = {
				create: function(options) {
			    	var self=this;
			        // use extend instead of map to avoid observables
			    	
			    	self=$.extend(self, options.data);
			    	
			    	self.title=findByLang(self.descriptiveData.label);
			    	self.thumbnail = ko.computed(function() {
			          if(self.media && self.media[0] && self.media[0].Thumbnail){
			        	var data=self.media[0].Thumbnail.url;
			        	 if(data){
			 				return data;}
			 			  else{
			 				   return "img/content/thumb-empty.png";
			 			   }
			        	}
			        	return "img/content/thumb-empty.png";
			        });

			        self.type=ko.computed(function() {
			        	if(self.administrative){
			        		if (self.administrative.collectionType.indexOf("Collection")!=-1)
			        		  return "COLLECTION";
			        		else if (self.administrative.collectionType.indexOf("Space")!=-1)
			        			return "SPACE";
			        	    else return "EXHIBITION";
			        	}else return "";
			        });
			        
			        self.css=ko.computed(function() {
			        	if(self.administrative){
			        		if (self.administrative.collectionType.indexOf("Collection")!=-1)
			        		  return "item collection";
			        		else if (self.administrative.collectionType.indexOf("Space")!=-1)
			        			return "item space";
			        		else return "item exhibition";
			        	}
			        	else{return "item collection";}
			        });
			        
			        self.url=ko.computed(function() {
			        	if(self.administrative){
			        		if (self.administrative.collectionType.indexOf("Collection")>-1)
				    		  return 'index.html#collectionview/'+ self.dbId;
			        		else if (self.administrative.collectionType.indexOf("Space")>-1){
			        			return self.administrative.isShownAt;
			        		}
				    		else{
				    			return 'index.html#exhibitionview/'+ self.dbId;
				    		}
			        	}else return "";
			        });
			        self.owner=ko.computed(function(){
			        	if(self.withCreatorInfo){
			        		return self.withCreatorInfo.username;
			        	}
			        });
			        
			        return self;
			     }
			  
		};
		
		
		var recmapping={
				'dbId': {
					key: function(data) {
			            return ko.utils.unwrapObservable(data.dbId);
			        }
				 }};
		self.isLoaded = ko.observable(false);
		self.records=ko.mapping.fromJS([], recmapping);
		
		
		self.data = ko.mapping.fromJS({"dbID":"","administrative":"","descriptiveData":""}, mapping);
		
		self.load = function(data) {
			self.data=ko.mapping.fromJS(data, mapping);
			
		};

		self.loadRecords= function(offset,count){
			//loading(true);
			var promise=self.getCollectionRecords(0,30);
			 $.when(promise).done(function(responseRecords) {
				 ko.mapping.fromJS(responseRecords.records,recmapping,self.records);
				 //loading(false);
				 
			 });
		}
		
		self.more= function(){
			var offset=self.records().length;
			self.loadRecords(offset,30);
		}
		
		self.getCollectionRecords = function (offset,count) {
			//call should be replaced with space collections+exhibitions
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/"+self.data.dbId+"/list",
				processData: false,
				data: "start="+offset+"&count="+count,
			}).success (function(){
			});
		};
		
		if(data != undefined){ 
			self.load(data);
			
		}
	}
	
	
  function MainContentModel(params) {
	  this.route = params.route;
	  var self = this;
	  
	  $("div[role='main']").toggleClass( "homepage", true );
	  self.featuredExhibition=ko.observable(null);	
	  self.featuredCollection=ko.observable(null);
	  self.homecollections=ko.observableArray();
	  self.totalCollections=ko.observable(0);
	  self.totalExhibitions=ko.observable(0);
	  self.spaces=ko.observableArray();
	  self.collections=ko.observableArray();
	  self.exhibitions=ko.observableArray();
	  self.all=ko.observableArray();
	  self.morespaces=ko.observable(true);
	  self.fetchitemnum=5;
	  self.nocollections=ko.observable(false);
	  self.noexhibitions=ko.observable(false);
	  self.nospaces=ko.observable(false);
	  self.loadingex=ko.observable(false);
	  self.loadingcoll=ko.observable(false);
	  self.loadingspaces=ko.observable(false);
	  
	  
	  
	
	  self.buttontext=ko.computed(function() {
		  if(self.homecollections().length>0 && self.noexhibitions()==true && self.nocollections()==true && self.nospaces()==true){
			  $(".loadmore").text("no more results");
		  }
		  
	  });
	  
	  self.revealItems = function (data) {
		  if((data.length==0 || data.length<self.fetchitemnum)){ self.nocollections(true);}
			
			for (var i in data) {
				var c=new Collection(
							data[i]				
				);
				
				
				self.homecollections().push(c);
				self.collections.push(c);
			}
			
			self.homecollections.valueHasMutated();self.loadingcoll(false);
		};
		
		
		self.revealExItems = function (data) {
			  if((data.length==0 || data.length<self.fetchitemnum)){self.noexhibitions(true);}
				
				for (var i in data) {
					var c=new Collection(
								data[i]				
					);
					
					
					self.homecollections().push(c);
					self.exhibitions.push(c);
				}
				self.homecollections.valueHasMutated();self.loadingex(false);
			};
	  
		self.revealSpaceItems = function (data) {
			if(data.length==0 || data.length<self.fetchitemnum){self.morespaces(false);self.nospaces(true);}
				for (var i in data) {
					var page=data[i].page;
					var thumb=null;
					var url="";
					
					if(page && page.cover){
						thumb=page.cover.Thumbnail;
						url=page.url;
					}
					
					var spacetocollection={administrative:{collectionType:'Space',isShownAt:url}, descriptiveData:{label:{default:[data[i].friendlyName]}},media:[{Thumbnail:{url: thumb}}]};
					var c=new Collection(
								spacetocollection				
					);
					
					
					self.homecollections().push(c);
					self.spaces.push(c);
				}
				self.homecollections.valueHasMutated();self.loadingspaces(false);
			};	
		
	  self.loadAll = function () {
		  //this should replaced with get space collections + exhibitions
		  
		  self.loadingcoll(true);
		  self.loadingex(true);
		  self.loadingspaces(true);
		  var promiseCollections = self.getSpaceCollections();
		  var promiseExhibitions=self.getSpaceExhibitions();
		  var promiseSpaces=self.getSpaces();
		  WITHApp.initTooltip();
		 
		  $.when(promiseCollections).done(function(responseCollections) {
			        //self.totalCollections(responseCollections.totalCollections);
			        //self.totalExhibitions(responseCollections.totalExhibitions);
				    self.revealItems(responseCollections['collectionsOrExhibitions']);
				    initFilterStick();
				    WITHApp.initIsotope();
				    var selector=$("ul.nav").find("li.active").attr('data-filter');
				    $( settings.mSelector ).isotope({ filter: selector });
				   
					
			});
		 
		  $.when(promiseExhibitions).done(function(response) {
		        //self.totalCollections(response.totalCollections);
		        //self.totalExhibitions(response.totalExhibitions);
			    self.revealExItems(response['collectionsOrExhibitions']);
			    initFilterStick();
			    WITHApp.initIsotope();
			    var selector=$("ul.nav").find("li.active").attr('data-filter');
			    $( settings.mSelector ).isotope({ filter: selector });
			   
				
		});
		  $.when(promiseSpaces).done(function(response) {
		        //self.totalSpaces(response);
			    self.revealSpaceItems(response);
			    var selector=$("ul.nav").find("li.active").attr('data-filter');
			    $( settings.mSelector ).isotope({ filter: selector });
			    
				
		  });
		  
		  
		  var promise2 = self.getFeatured("56cd993275fe2461e089a8a5");
          $.when(promise2).done(function (data) {
        	  
        	 
        	  self.featuredExhibition(new Collection(data));
        	  $("div.featured-box.exhibition").find("div.featured-hero > img").attr("src",self.featuredExhibition().data.thumbnail());    
        	 
        	  
          });
          var promise3 = self.getFeatured("56cd85e375fe2461e0868723");
          $.when(promise3).done(function (data) {
        	  
        	 
        	  self.featuredCollection(new Collection(data));
        	 
        	  WITHApp.initCharacterLimiter();
          });
          

		 
		};
		
		self.getSpaces = function () {
			//call should be replaced with space collections+exhibitions
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/group/list",
				processData: false,
				data: "offset=0&count="+self.fetchitemnum,
			}).success (function(){
			});
		};
		
		self.getSpaceCollections = function () {
			//call should be replaced with space collections+exhibitions
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/list",
				processData: false,
				data: "offset=0&count="+self.fetchitemnum+"&collectionHits=true&isPublic=true&isExhibition=false",
			}).success (function(){
			});
		};
		
		
		self.getSpaceExhibitions = function () {
			//call should be replaced with space collections+exhibitions
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/list",
				processData: false,
				data: "offset=0&count="+self.fetchitemnum+"&collectionHits=true&isPublic=true&isExhibition=true",
			}).success (function(){
			});
		};
		
		
         self.getFeatured=function(id) {
			
			/*call must change to get featured exhibition for space*/
	        return $.ajax({
	            type: "GET",
	            url: "/collection/"+id,
	            success: function () {

	            }
	        });
	    };
		
      
		
	    
		self.loadNext = function () {
		  if (!self.loadingcoll() && !self.loadingex() && !self.loadingspaces()) {
			self.loadingcoll(true);
			self.loadingex(true);
			self.loadingspaces(true);
			var promise1=self.moreSpaces();
			var promise2=self.moreCollections();
			var promise3=self.moreExhibitions();
			$.when(promise1,promise2,promise3).done(function(data1,data2,data3){
				self.revealSpaceItems(data1[0]);
				
				self.revealItems(data2[0]['collectionsOrExhibitions']);
				self.revealExItems(data3[0]['collectionsOrExhibitions']);
				var selector=$("ul.nav").find("li.active").attr('data-filter');
				$( settings.mSelector ).isotope({ filter: '*' });
			    $( settings.mSelector ).isotope({ filter: selector });
				
			})
			
			
			}
		};
		
		self.moreSpaces = function(){
			
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/group/list",
				processData: false,
				data: "count="+self.fetchitemnum+"&offset=" + self.spaces().length,
			}).success (function(){
			});
				
			
		}

		self.moreCollections = function () {
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/list",
				processData: false,
				data: "isPublic=true&count="+self.fetchitemnum+"&offset=" + self.collections().length+"&isExhibition=false",
			}).success (function(){
			});
			
		};

		self.moreExhibitions = function () {
			return $.ajax({
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				url: "/collection/list",
				processData: false,
				data: "isPublic=true&count="+self.fetchitemnum+"&offset=" + self.exhibitions().length+"&isExhibition=true",
			}).success (function(){
			});
			
		};
		
	  self.loadCollectionOrExhibition = function(item) {
		  if (item.isExhibition) {
			  window.location = 'index.html#exhibitionview/'+ item.id;
			  
		  }
		  else {
			  window.location = 'index.html#collectionview/' + item.id;
		  }
		  return false;
	  };
		
      self.loadAll();	  

		
	  
	  self.filter=function(data, event) {
		  			  var selector = event.currentTarget.attributes.getNamedItem("data-filter").value;
					  $(event.currentTarget).siblings().removeClass("active");
					  $(event.currentTarget).addClass("active");
					  $( settings.mSelector ).isotope({ filter: selector });
					  return false;
				}
					  
	 

	  
	
  }
  
 
 
  return { viewModel: MainContentModel, template: template };
});