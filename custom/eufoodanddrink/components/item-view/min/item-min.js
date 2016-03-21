define(["knockout","text!./item.html","app","smoke"],function(e,r,i){function t(r){var a=this;a.recordId="-1",a.title="",a.description="",a.thumb="",a.fullres=e.observable(""),a.view_url="",a.source="",a.creator="",a.provider="",a.dataProvider="",a.dataProvider_uri="",a.rights="",a.url="",a.externalId="",a.likes=0,a.collected=0,a.data=e.observable(""),a.collectedIn=[],a.isLike=e.observable(!1),a.related=e.observableArray([]),a.similar=e.observableArray([]),a.facebook="",a.twitter="",a.mail="",a.forrelated=e.observable("").extend({uppercase:!0}),a.relatedlabel="",a.loc=e.observable(""),a.similarsearch=!1,a.relatedsearch=!1,a.loading=e.observable(!1),a.pinterest=function(){var e=encodeURIComponent(a.loc()),r=encodeURIComponent(a.fullres()),i=encodeURIComponent(a.title+" on "+window.location.host);return window.open("//www.pinterest.com/pin/create/button/?url="+e+"&media="+r+"&description="+i,"","height=500,width=750"),!1},a.isLiked=e.pureComputed(function(){return i.isLiked(a.externalId)}),a.isLoaded=e.observable(!1),a.load=function(e){void 0==e.title?a.title="No title":a.title=e.title,a.view_url=e.view_url,a.thumb=e.thumb,e.fullres&&e.fullres.length>0?a.fullres(e.fullres):a.fullres(a.calcThumbnail()),a.description=e.description,a.source=e.source,a.creator=e.creator,a.provider=e.provider,a.dataProvider=e.dataProvider,a.dataProvider_uri=e.dataProvider_uri,a.rights=e.rights,e.dbId&&(a.recordId=e.dbId,a.loc(location.href.replace(location.hash,"")+"#item/"+a.recordId)),a.externalId=e.externalId,a.likes=e.likes,a.collected=e.collected,a.collectedIn=e.collectedIn,a.data(e.data),a.facebook="https://www.facebook.com/sharer/sharer.php?u="+encodeURIComponent(a.loc()),a.twitter="https://twitter.com/share?url="+encodeURIComponent(a.loc())+"&text="+encodeURIComponent(a.title+" on "+window.location.host)+'"',a.mail="mailto:?subject="+a.title+"&body="+encodeURIComponent(a.loc());var r=i.isLiked(a.externalId);a.isLike(r),a.loading(!1)},a.findsimilar=function(){0==a.related().length&&0==a.relatedsearch&&(a.relatedsearch=!0,a.creator.length>0?a.forrelated(a.creator.toUpperCase()):a.forrelated(a.provider.toUpperCase()),a.relatedlabel=a.creator.length>0?"CREATOR":"PROVIDER",a.forrelated().length>0&&(a.loading(!0),$.ajax({type:"post",url:"/api/advancedsearch",contentType:"application/json",data:JSON.stringify({searchTerm:a.forrelated(),page:1,pageSize:10,source:[a.source],filters:[]}),success:function(e){r=void 0!=e.responses[0]&&void 0!=e.responses[0].items.culturalCHO?e.responses[0].items.culturalCHO:null;var i=[];if(null!=r){for(var n in r){var e=r[n];if(null!=e){var l=e.administrative,o=e.descriptiveData,d=e.media,u=e.provenance,s=e.usage,c=null;d&&(d[0].Original?c=findResOrLit(d[0].Original.originalRights):d[0].Thumbnail&&(c=findResOrLit(d[0].Thumbnail.originalRights)));var p=findProvenanceValues(u,"source");"Rijksmuseum"==p&&d&&(d[0].Thumbnail=d[0].Original);var m=new t({thumb:null!=d&&null!=d[0]&&null!=d[0].Thumbnail&&"null"!=d[0].Thumbnail.url?d[0].Thumbnail.url:"img/content/thumb-empty.png",fullres:null!=d&&null!=d[0]&&null!=d[0].Original&&"null"!=d[0].Original.url?d[0].Original.url:"",title:findByLang(o.label),description:findByLang(o.description),view_url:findProvenanceValues(u,"source_uri"),creator:findByLang(o.dccreator),dataProvider:findProvenanceValues(u,"dataProvider"),dataProvider_uri:findProvenanceValues(u,"dataProvider_uri"),provider:findProvenanceValues(u,"provider"),rights:c,externalId:l.externalId,source:p,likes:s.likes,collected:s.collected,collectedIn:e.collectedIn,data:e});m.thumb&&m.thumb.length>0&&m.externalId!=a.externalId&&i.push(m)}if(i.length>3)break}a.related().push.apply(a.related(),i),a.related.valueHasMutated()}a.loading(!1)},error:function(e,r,i){a.loading(!1)}}))),0==a.similar().length&&0==a.similarsearch&&(a.similarsearch=!0,a.loading(!0),$.ajax({type:"post",url:"/api/advancedsearch",contentType:"application/json",data:JSON.stringify({searchTerm:a.title,page:1,pageSize:10,source:[a.source],filters:[]}),success:function(e){r=void 0!=e.responses[0]&&void 0!=e.responses[0].items.culturalCHO?e.responses[0].items.culturalCHO:null;var i=[];if(null!=r){for(var n in r){var e=r[n];if(null!=e){var l=e.administrative,o=e.descriptiveData,d=e.media,u=e.provenance,s=e.usage,c=null;d&&(d[0].Original?c=findResOrLit(d[0].Original.originalRights):d[0].Thumbnail&&(c=findResOrLit(d[0].Thumbnail.originalRights)));var p=findProvenanceValues(u,"source");"Rijksmuseum"==p&&d&&(d[0].Thumbnail=d[0].Original);var m=new t({thumb:null!=d&&null!=d[0]&&null!=d[0].Thumbnail&&"null"!=d[0].Thumbnail.url?d[0].Thumbnail.url:"img/content/thumb-empty.png",fullres:null!=d&&null!=d[0]&&null!=d[0].Original&&"null"!=d[0].Original.url?d[0].Original.url:"",title:findByLang(o.label),description:findByLang(o.description),view_url:findProvenanceValues(u,"source_uri"),creator:findByLang(o.dccreator),dataProvider:findProvenanceValues(u,"dataProvider"),dataProvider_uri:findProvenanceValues(u,"dataProvider_uri"),provider:findProvenanceValues(u,"provider"),rights:c,externalId:l.externalId,source:p,likes:s.likes,collected:s.collected,collectedIn:e.collectedIn,data:e});m.thumb&&m.thumb.length>0&&m.externalId!=a.externalId&&i.push(m)}if(i.length>3)break}a.similar().push.apply(a.similar(),i),a.similar.valueHasMutated()}a.loading(!1)},error:function(e,r,i){a.loading(!1)}}))},a.doLike=function(){a.isLike(!0)},a.calcThumbnail=e.pureComputed(function(){return a.thumb?a.thumb:"img/content/thumb-empty.png"}),a.sourceCredits=e.pureComputed(function(){switch(a.source){case"DPLA":return"dp.la";case"Europeana":return"europeana.eu";case"NLA":return"nla.gov.au";case"DigitalNZ":return"digitalnz.org";case"EFashion":return"europeanafashion.eu";case"YouTube":return"youtube.com";case"The British Library":return"www.bl.uk";case"Mint":return"mint";case"Rijksmuseum":return"www.rijksmuseum.nl";case"DDB":return"deutsche-digitale-bibliothek.de";default:return""}}),a.displayTitle=e.pureComputed(function(){var e="";return e=a.title,a.creator&&a.creator.length>0&&(e+=", by "+a.creator),a.dataProvider&&a.dataProvider.length>0&&a.dataProvider!=a.creator&&(e+=", "+a.dataProvider),e}),void 0!=r&&a.load(r)}function a(r){function a(){var e=$(window).height(),r=$(window).width(),i=e-70;r>=1200&&$(".itemopen .itemview").css({height:i+"px"})}var n=this;document.body.setAttribute("data-page","item"),setTimeout(function(){WITHApp.init()},300),n.route=r.route,n.from=window.location.href;var l="";n.loggedUser=e.pureComputed(function(){return i.isLogged()?!0:!1}),n.record=e.observable(new t),n.id=e.observable(r.id),itemShow=function(r){data=e.toJS(r),$('.nav-tabs a[href="#information"]').tab("show"),$(".mediathumb > img").attr("src",""),n.open(),n.record(new t(data))},n.open=function(){window.location.href.indexOf("#item")>0&&document.body.setAttribute("data-page","media"),document.body.setAttribute("data-page","item"),$(".itemview").fadeIn(),$("body").css("overflow","hidden"),a()},n.close=function(){$("body").css("overflow","visible"),$(".itemview").fadeOut()},n.changeSource=function(e){e.record().fullres(e.record().calcThumbnail())},n.collect=function(e){isLogged()?collectionShow(n.record()):showLoginPopup(n.record())},n.recordSelect=function(e,r){itemShow(e,r)},n.likeRecord=function(e,r){r.preventDefault();var t=$(r.target.parentNode).parent();i.likeItem(e,function(r){r?(t.addClass("active"),($('[id="'+e.externalId+'"]')||$('[id="'+e.recordId+'"]'))&&($('[id="'+e.externalId+'"]').find("span.star").addClass("active"),$('[id="'+e.recordId+'"]').find("span.star").addClass("active"))):(t.removeClass("active"),($('[id="'+e.externalId+'"]')||$('[id="'+e.recordId+'"]'))&&($('[id="'+e.externalId+'"]').find("span.star").removeClass("active"),$('[id="'+e.recordId+'"]').find("span.star").removeClass("active")))})},n.collect=function(e,r){r.preventDefault(),collectionShow(e)},n.loadItem=function(){$.ajax({url:"/record/"+n.id(),method:"get",contentType:"application/json",success:function(e){var r=e.administrative,i=e.descriptiveData,a=e.media,l=e.provenance,o=e.usage;a&&(a[0].Original?rights=findResOrLit(a[0].Original.originalRights):a[0].Thumbnail&&(rights=findResOrLit(a[0].Thumbnail.originalRights)));var d=findProvenanceValues(l,"source");"Rijksmuseum"==d&&a&&(a[0].Thumbnail=a[0].Original);var u=new t({thumb:null!=a&&null!=a[0]&&null!=a[0].Thumbnail&&"null"!=a[0].Thumbnail.url?a[0].Thumbnail.url:"img/content/thumb-empty.png",fullres:null!=a&&null!=a[0]&&null!=a[0].Original&&"null"!=a[0].Original.url?a[0].Original.url:"",title:findByLang(i.label),description:findByLang(i.description),view_url:findProvenanceValues(l,"source_uri"),creator:findByLang(i.dccreator),dataProvider:findProvenanceValues(l,"dataProvider"),dataProvider_uri:findProvenanceValues(l,"dataProvider_uri"),provider:findProvenanceValues(l,"provider"),rights:rights,externalId:r.externalId,source:d,dbId:e.dbId,likes:o.likes,collected:o.collected,collectedIn:e.collectedIn,data:e});n.record(u),$('.nav-tabs a[href="#information"]').tab("show"),n.open(),$(".itemview").fadeIn()},error:function(e,r,i){n.open(),$.smkAlert({text:"An error has occured",type:"danger",permanent:!0})}})},void 0!=n.id()&&n.loadItem(),n.annotate=function(){window.open("http://euspndwidget.netseven.it/index.php?id="+n.record().externalId,n.record().externalId,"top=10, left=10, width=900, height=600, status=no, menubar=no, toolbar=no scrollbars=no")}}return{viewModel:a,template:r}});