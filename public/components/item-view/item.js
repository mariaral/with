define(['knockout', 'text!./item.html', 'app'], function(ko, template, app) {

  function ItemViewModel(params) {
	  var self = this;

	  self.route = params.route;
      var thumb="";
      self.recordId=ko.observable("");
	  self.title = ko.observable(false);
	  self.description=ko.observable(false);
	  self.thumb = ko.observable("");
	  self.fullres=ko.observable("");
	  self.view_url=ko.observable("");
	  self.creator=ko.observable("");
	  self.provider=ko.observable("");
	  self.apisource=ko.observable(true);
	  
    itemShow = function(record) {
    	self.itemload(record);
    	$('#modal-1').css('display', 'block');

    	$('#modal-1').addClass('md-show');
    	
    	$('#modal-1').css('overflow-y', 'auto');
    }

    self.itemload = function(e) {
    	data=ko.toJS(e);
    	console.log(data);
    	if(data.title==undefined){
			self.title("No title");
		}else{self.title(data.title);}

		self.thumb(data.thumb);
		thumb=data.thumb;
		if(data.fullres!==undefined && data.fullres!=null && data.fullres[0].length>0 && data.fullres!="null"){
		  self.fullres(data.fullres[0]);}
		else{
			self.fullres(data.thumb);

		  }

		if(data.description==undefined){
			self.description(data.title);
		}
		else{
		self.description(data.description);}
		if(data.creator!==undefined){
			self.creator(data.creator);
		}
		if(data.provider!==undefined){
			self.provider(data.provider);
		}

		self.apisource(data.source);

		self.view_url(data.view_url);
		
		self.recordId = data.id;
	};

    self.close= function(){
    	self.fullres('');
    	$("#modal-1").find("div[id^='modal-']").removeClass('md-show').css('display', 'none');
    	$('#modal-1').removeClass('md-show');
    	$('#modal-1').css('display', 'none');
    	$("#myModal").modal('hide'); 


    }

    self.changeSource=function(item){
    	if(item.fullres!=item.thumb){
    		 $("#fullresim").attr('src',thumb);
    	}
    	else{
    		 $("#fullresim").attr('src',thumb);
    	}

    }

    self.sourceImage = ko.pureComputed(function() {
		if(self.apisource() =="DPLA") return "images/logos/dpla.png";
		else if(self.apisource() == "Europeana") return "images/logos/europeana.jpeg";
		else if(self.apisource() == "NLA") return "images/logos/nla_logo.png";
		else if(self.apisource() == "DigitalNZ") return "images/logos/digitalnz.png";
		else if(self.apisource() == "DigitalNZ") return "images/logos/digitalnz.png";
		else if(self.apisource() == "EFashion") return "images/logos/eufashion.png";

		else return "";
	});

    self.collect = function(item){
		if (!isLogged()) {
			showLoginPopup(item);
		}
		else {
			collectionShow(item);
		}
    }

    self.recordSelect= function (e){
		console.log(e);
		itemShow(e);
		
	}
    
  }

  return { viewModel: ItemViewModel, template: template };
});
