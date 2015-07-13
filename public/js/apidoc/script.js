
var swaggerUi = new SwaggerUi({
  url:"http://petstore.swagger.io/v2/swagger.json",
	spec: {
	    "swagger": "2.0",
	    "info": {
	        "version": "v1",
	        "title": "WITH API",
	        "description": "Test WITH API documentation!\n"
	    },
	    "paths": {
	        "/api/search": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/api/advancedsearch": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/api/testsearch": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            },
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/api/autocompleteExt": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/assets/headers.js": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/assets/*file": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/list": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/listShared": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/create": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/listByUser": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/{id}/addRecord": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/{id}/removeRecord": {
	            "delete": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/{id}/list": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/{id}/edit": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/favorites": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/liked": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/unliked/{recId}": {
	            "delete": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/favoriteCollection": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/cache/byUrl": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/exhibition/create": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/exhibition/list": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/{id}/listUsers": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collectiion/{id}/download": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/collection/{id}": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            },
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            },
	            "delete": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/record/findInColletions": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/record/{id}": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            },
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            },
	            "delete": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/group/create": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/group/{id}": {
	            "delete": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/rights/list": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/rights/{colId}/{right}": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/media/create": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/media/{id}": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            },
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            },
	            "delete": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/register": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/login": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/logout": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/listNames": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/emailAvailable": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/token": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/loginWithToken": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/findByUsernameOrEmail": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/{id}/photo": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/{id}": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            },
	            "put": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            },
	            "delete": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/addToGroup/{id}": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/resetPassword/{emailOrUserName}": {
	            "get": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/changePassword": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        },
	        "/user/apikey/{email}": {
	            "post": {
	                "responses": {
	                    "200": {
	                        "description": "OK"
	                    }
	                }
	            }
	        }
	    }
	},
  dom_id:"swagger-ui-container"
});

swaggerUi.load();

