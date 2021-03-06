/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package db;

import java.util.List;

import model.ApiKey;

import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;

import play.Logger;

public class ApiKeyDAO extends DAO<ApiKey> {
	static private final Logger.ALogger log = Logger.of(ApiKeyDAO.class);

	public ApiKeyDAO() {
		super( ApiKey.class );
	}

	public List<ApiKey> getAll() {
		return find().asList();
	}
//	
//	public List<ApiKey> getByIpPattern( String ipPattern ) {
//		Query<ApiKey> q = createQuery()
//					.field( "ipPattern")
//					.equal( ipPattern );
//		return find( q ).asList();
//	}

	public ApiKey getByName( String name ) {
		Query<ApiKey> q = createQuery()
					.field( "name")
					.equal( name );
		return find( q ).get();
	}
	
	public ApiKey getByEmail( String email ) {
		Query<ApiKey> q = createQuery()
					.field( "email")
					.equal( email );
		return find( q ).get();
	}
	
	public ApiKey getByUserId( ObjectId userID ) {
		Query<ApiKey> q = createQuery()
					.field( "proxyUserId")
					.equal( userID );
		return find( q ).get();
	}
	
	

	
}
