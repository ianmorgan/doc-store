# Standards 

Common behaviours expected across services.


## Use of HTTP status codes 

The general style is more RPC over JSON/HTTP than pure REST, thought it doesn't follow the 
[JSON-RPC](http://www.jsonrpc.org/) spec.

Services should restrict status codes to just 200 (Success) and 500 (Error/Exception).

 Note *Error/Exception* refers to technical / protocol problems (i.e. a problem making the call, 
 or a problem with the server) and not genuine application errors, for example trying to create an
 account that already exists, which should be returned as part of the data (see below). In programming 
  terms this can be thought of as an API where any method may through exceptions but any substantial 
  business rule that might require decisions by a client layer (as opposed to dropping into a generic 'something is wrong, please try again' style message)
  should be coded to as part of the return data. 
  
 Servers may still of course return other status codes before they even reaching the application logic, 
 e.g. 404, 401, 503.
 
## Use of HTTP verbs 

Only GET, POST and DELETE are used. 

POST is for any operation that will modify data (apart from DELETE) and non-modifying operations that require 
JSON data (generally either because of the size or complexity of the required parameters).

## Returning data 

### 200 response

All data is returned as a JSON Object. For success responses data is under the <code>"data"</code> key  (this follows the 
convention for [JSON-RPC](http://www.jsonrpc.org/) and [GraphQL](http://facebook.github.io/graphql/October2016/#sec-Response-Format)). 

Some APIs may chose to return partial responses (this is typically for APIs that 
 aggregate across multiple backend services) in which case there may be errors even for a 
 success response (this is part of the GraphQL spec). Use of this feature is an API decision, and 
 it gives clients the flexibility to accept or reject partial responses.
 
### 500 response 

This should return a JSON object with one or more errors under the "errors" key. Use the conventions documented in the 
[GraphQL](http://facebook.github.io/graphql/October2016/#sec-Response-Format) spec, with the following additions 

* "fatal" : true 

If this key is set and has the value true the error is considered fatal and there is no point in retrying. If not present 
we assume a value if false. 

## Client processing 

The following are recommended as default rules:

* if code indicates an authorisation problem process as appropriate (for example redirect to login page)
* if not 500 or 200 fail with any appropriate logging 
* if 500 fail, logging errors collections 
* if 200 with errors fail, logging errors collections (so default is to aggressively reject any partial response)
* process results in 'data'. If data contains errors or problems associated to the application business rules 
process as appropriate 

## Server processing 

The following are recommended as default rules 

### Request style operations 

Whether coded as simple GET or POST with JSON body 

* bad url returns 404 (normally handled by web server / framework)
* any problem with formatting of parameters or JSON returns a 500 with a message indicating the problem and "fatal" set 
to true (there is no point in retrying with the same request)
* any internal problems (such as a database error) return a 500 with a message indicating the problem.
* success returns with results under the "data" key and no "errors" collection 

### Modification operations 

POST or DELETE

* bad url returns 404 (normally handled by web server / framework)
* any problem with formatting of parameters or JSON returns a 500 with a message indicating the problem and "fatal" set 
to true (there is no point in retrying with the same request)
* any internal problems (such as a database error) return a 500 with a message indicating the problem.
* success returns with an empty "{}" json object  
 
 
 
 
