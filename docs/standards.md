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
  business rule that might require decisions by a client layer, as opposed to dropping into a generic "_something is wrong, 
  please try again_" style message), should be coded to as part of the return data. 
  
 Servers may still of course return other status codes before they even reaching the application logic, 
 e.g. 401, 404, 503.
 
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

This should return a JSON object with one or more errors under the <code>"errors"</code> key. Use the conventions documented in the 
[GraphQL](http://facebook.github.io/graphql/October2016/#sec-Response-Format) spec, with the following optional additions 

* "fatal" : true 

If present and has the value true the error is considered fatal and there is no point in retrying. If not present assume a value of false. 

* "code" : "ERR123"

This is the convention for including an agreed error code with the message.

* "stackTrace" : "<<dump of internal call stack as a single multi-line string>>"

Convention for returning the internal stack trace if useful. 


## Client processing 

The following are recommended as default rules:

* if the status code indicates an authorisation problem process as appropriate (for example redirect to login page)
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
* any problem with formatting of parameters or JSON returns a 500 with a message indicating the problem and <code>"fatal"</code> set 
to true (there is no point in retrying with the same request)
```json
{
    "errors" : [{"message":"Missing name search param", "fatal" : true}]
}
```   
* any internal problems (such as a database error) return a 500 with a message indicating the problem. Including the stack trace if possible is recommended
```json
{
    "errors" : [{"message":"Couldn't connect to database",
                 "stackTrace" : "<<dump of internal stack trace>>"}]
}
```  
* success returns with results under the <code>"data"</code> key and no <code>"errors"</code> collection 
```json
{ 
  "data": { "searchResults": ["R2-D2", "C-3PIO", "Luke Sykewalker"] }
}
```
* if the service returns a partial result, it would also include the errors
```json
{ 
  "data": { "searchResults": ["R2-D2", "C-3PIO"] },
  "errors": [ { "message" : "Failed to include search of 'Humans' in the results"}]
}
```
* if the service returns a partial result and clients may want to make decision over which 
errors are acceptable include a <code>code</code> with the error
```json
{ 
  "data": { "searchResults": ["R2-D2", "C-3PIO"] },
  "errors": [ { "message" : "Failed to include search of 'Humans' in the results",
                "code" : "ERR123"}]
}
```


### Modification operations 

POST or DELETE

* bad url returns 404 (normally handled by web server / framework)
* any problem with formatting of parameters or JSON returns a 500 with a message indicating the problem and <code>"fatal"</code> set 
```json
{
    "errors" : [{"message":"name is missing", "fatal" : true}]
}
```   
to true (there is no point in retrying with the same request)
* any internal problems (such as a database error) return a 500 with a message indicating the problem.
```json
{
    "errors" : [{"message":"Couldn't connect to database",
                 "stackTrace" : "<<dump of internal stack trace>>"}]
}
```  
* success returns an empty json object by default unless there is a need to return information such as an id (_though 
generally we aim to minimise this style as it makes idempotent behaviour harder_)
```json
{}
```
or 
```json
{
    "data": { "id" : "123456"}
}
```
 
### Operations returning business rule problems

A typical evolution especially in agile development is to start with the simple business rules and then add complexity. 
As business rules expand, the use of the <code>errors</code> collection to hold detailed information is likely to get 
complex and its not really the design intention to use it for this purpose. Instead the information should be returned 
as part of the <code>data</code> and by convention under <code>problems</code> key if possible (_there may be other API standards 
imposed by tools and frameworks that make this impractical_).

Situations that might require this include:
* collecting multiple problems and returning them all to the client rather than simply failing on the first (a common 
pattern when validating form data for example).
* providing more detailed information for decision making or constructing user friendly messages.

As a rule of thumb, this approach should be considered when:
* custom error handling flows are needed in the client layer.
* the backend business rules require custom values returned from calls rather than relying upon exceptions. 

As not all clients will necessarily have the logic to examining custom fields in the return data its recommend that an 
<code>"errors"</code> entry is also added with the code of <code>"problems"</code>. Clients that wish to examine the 
more detailed information simply need to ignore this particular error. 

An example response is:

```json
{ 
    "data" : { 
        "problems" : ["episode 'Star Trek: The Next Generation' is not a Star Wars film",
                      "character 'Spock' is not 100% human"]
    },
    "errors" :[
        { "message" : "Validation problems - see 'problems' key under 'data' for details",
          "code" : "problems"}
     ]
}
```
  

 