# REST cases runner

CLI tool for REST tests.

Stresty walks through the steps which are REST calls with matcher (response template). It checks only fields you described in the `match` part. If the response body contains other fields, they will be ignored.

## Ussage

Download [latest standalone release](https://github.com/Aidbox/stresty/releases/latest).

Create file `test.yaml`:
```yaml
desc: Create & Read Patient

steps:
- id: clean
  desc: Clear all patients
  POST: /$sql
  body: 'TRUNCATE patient'
  match:
    status: 200
- id: create
  desc: Create test patient
  POST: /Patient
  body:
    id: pt-1
    name: [{given: ['Ivan'], family: 'Pupkin'}]
  match:
    status: 201
    body:
      id: pt-1
      name: [{given: ['Ivan']}] # Checks only given name
- id: read
  desc: Read our patient
  GET: /Patient/pt-1
  match:
    status: 200
    body:
      id: pt-1
      name: [{given: ['Ivan'], family: 'Pupkin'}]
- id: search-by-id
  GET: /Patient?_id=pt-1
  match:
    status: 200
    body:
      entry:
      - resource: {resourceType: 'Patient', id: 'pt-1'}
- id: update
  desc: Update our patient
  PUT: /Patient/pt-1
  body:
    name: [{given: ['Petr'], family: 'Pupkin'}]
  match:
    status: 200
    body:
      name: [{given: ['Petr'], family: 'Pupkin'}]
```

Run script

```sh
$ AIDBOX_URL=http://<box_name>.aidbox.app AIDBOX_BASIC_AUTH=<username:pass encoded with base64> java -jar stresty-1.0.0-standalone.jar test.yaml 

Args: (test/another.yaml)
Read  test/another.yaml
test/another.yaml
Clear all patients

POST /$sql
body:  TRUNCATE patient

status:  200
body:  
  message:  No results where returned by statement
 OK!

---------------------------------

Create test patient         

POST /Patient
body:
  id: pt-1
  name:
  - given:  
    - Ivan
    family:  Pupkin

status:  201
body:  
  name:  
  - given:  
    - Ivan
    family:  Pupkin
  id:  pt-1
  resourceType:  Patient
  meta:  
    lastUpdated:  2019-09-09T12:31:32.179Z
    versionId:  60
 OK!

---------------------------------

Read our patient         

GET /Patient/pt-1

status:  200
body:  
  name:  
  - given:  
    - Ivan
    family:  Pupkin
  id:  pt-1
  resourceType:  Patient
  meta:  
    lastUpdated:  2019-09-09T12:31:32.179Z
    versionId:  60
 OK!

---------------------------------

GET /Patient?_id=pt-1

status:  200
body:  
  resourceType:  Bundle
  type:  searchset
  entry:  
  - resource:  
      name:  
      - given:  
        - Ivan
        family:  Pupkin
      id:  pt-1
      resourceType:  Patient
      meta:  
        lastUpdated:  2019-09-09T12:31:32.179Z
        versionId:  60
    fullUrl:  https://main.aidbox.app/Patient/pt-1
  total:  1
  link:  
  - relation:  first
    url:  /Patient?_id=pt-1&page=1
  - relation:  self
    url:  /Patient?_id=pt-1&page=1
  query-sql:  
  - SELECT "patient".* FROM "patient" WHERE ("patient".id in (?)) LIMIT ? OFFSET ? 
  - pt-1
  - 100
  - 0
  query-time:  1
 OK!

---------------------------------

Update our patient         

PUT /Patient/pt-1
body:
  name:
  - given:
    - Petr
    family:  Pupkin

status:  200
body:  
  name:  
  - given:  
    - Petr
    family:  Pupkin
  id:  pt-1
  resourceType:  Patient
  meta:  
    lastUpdated:  2019-09-09T12:31:32.759Z
    versionId:  61
 OK!

---------------------------------
```

You can run several scripts:
```bash
java -jar stresty.jar test-1.yaml test-2.yaml

# or use wildcards

java -jar stresty.jar *.yaml
```


### Step fields

| Field name    | Description                   |
|---------------|-------------------------------|
| id            | Step identifier               |
| desc          | Step description              |
| <HTTP_METHOD> | GET, POST, PUT, etc. + url    |
| body          | request body                  |
| match         | template of expected response |
| match.status  | expected status code          |
| match.body    | expected body                 |

## Configuration properties

| VAR_NAME   | Description |
|------------|-------------|
| AIDBOX_URL | URL (ex. http://box.aidbox.app) |
| AIDBOX_BASIC_AUTH | Base64 encoded auth token (ex. `dXNlcjpwYXNz` for `user:pass`). See more in [aidbox.doc](https://docs.aidbox.app/auth-betta/basic-auth) |

# Matcho

Stresty uses [Matcho](https://github.com/niquola/matcho) under the hood.

## Predicates

String values ended with question mark (?) is considered as predicate. For now supported only functions from [clojure.core ns](https://clojuredocs.org/clojure.core) like `string?`, `number?`, etc.

Example:
```yaml
- id: create
  desc: Create test patient
  POST: /Patient
  body:
    id: pt-1
    name: [{given: ['Vladislav']}]
  match:
    status: 201
    body:
      id: pt-1
      name: [{given: ['string?']}]
```

## Regex

String values started with number sign (#) is considered as [regex pattern](https://clojure.org/reference/other_functions#regex).

```yaml
- id: create
  desc: Create test patient
  POST: /Patient
  body:
    id: pt-1
    name: [{given: ['Vladislav']}]
  match:
    status: 201
    body:
      id: pt-1
      name: [{given: ['#\w+']}]

```
# todo

1. json schema validation for incoming files.
2. print the conf before run tests
3. print summary (num of passed, failed tests)
5. fail fast
1. Doc for built-in-functions
1. Generate `stresty.jar` (not `stresty-1.0.0-standalone.jar`).
