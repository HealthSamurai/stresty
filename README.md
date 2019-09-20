# REST Cases Runner

CLI Tool for REST Tests. [![Build Status](https://travis-ci.org/Aidbox/stresty.svg?branch=master)](https://travis-ci.org/Aidbox/stresty)

Stresty walks through the steps which are REST calls with a matcher (response template). It checks only fields that you described in the `match` part. If the response body contains other fields, they will be ignored.

## Configuration Properties

| VAR_NAME             | Description                                                                                                                             |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| AIDBOX_URL           | URL (ex. http://box.aidbox.app)                                                                                                         |
| AIDBOX_AUTH_TYPE     | Auth type. `Basic` is supported for now. If absent than stresty doesn't use auth.                                                       |
| AIDBOX_CLIENT_ID     | client id is used in case auth                                                                                                          |
| AIDBOX_CLIENT_SECRET | client secret                                                                                                                           |

## Usage

Download stresty.jar: 

```
$ curl -L -o stresty.jar https://github.com/Aidbox/stresty/releases/latest/download/stresty.jar
$ # export your configuration properties here
$ java -jar stresty.jar <test-file>.yaml
```

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

Run the script (we expect that you have box with configured basic auth):

```bash
$ export AIDBOX_URL=http://<box_name>.aidbox.app 
$ export AIDBOX_AUTH_TYPE=Basic
$ export AIDBOX_CLIENT_ID=<client-id>
$ export AIDBOX_CLIENT_SECRET=<client-secret>
$ java -jar stresty.jar test.yaml 

Args: [test.yaml]
Configuration:
{:verbosity 0,
 :interactive false,
 :base-url "http://main.aidbox.app",
 :client-id "wow",
 :client-secret "pass",
 :authorization-type "Basic"}


run test case test.yaml
passed test.yaml

Test results: 5 passed,
              0 failed,
              0 skipped.
```

You can run several scripts:
```bash
java -jar stresty.jar test-1.yaml test-2.yaml

# or use wildcards

java -jar stresty.jar *.yaml
```


### Step Fields

| Field name    | Description                   |
|---------------|-------------------------------|
| id            | Step identifier               |
| desc          | Step description              |
| <HTTP_METHOD> | GET, POST, PUT, etc. + url    |
| body          | request body                  |
| match         | template of expected response |
| match.status  | expected status code          |
| match.body    | expected body                 |

# Matcho

Stresty uses [Matcho](https://github.com/niquola/matcho) under the hood.

## Predicates

String values ending with the question mark (?) are considered as predicates. For now, only functions from [clojure.core ns](https://clojuredocs.org/clojure.core) like `string?`, `number?`, etc. are supported.

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

### Predicates from clojure.core

- [boolean?](https://clojuredocs.org/clojure.core/boolean_q)
- [bound?](https://clojuredocs.org/clojure.core/bound_q)
- [contains?](https://clojuredocs.org/clojure.core/contains_q) -- not supported
- [decimal?](https://clojuredocs.org/clojure.core/decimal_q)
- [distinct?](https://clojuredocs.org/clojure.core/distinct_q)
- [double?](https://clojuredocs.org/clojure.core/double_q)
- [empty?](https://clojuredocs.org/clojure.core/empty_q)
- [even?](https://clojuredocs.org/clojure.core/even_q)
- [false?](https://clojuredocs.org/clojure.core/false_q)
- [float?](https://clojuredocs.org/clojure.core/float_q)
- [int?](https://clojuredocs.org/clojure.core/int_q)
- [integer?](https://clojuredocs.org/clojure.core/integer_q)
- [list?](https://clojuredocs.org/clojure.core/list_q)
- [map?](https://clojuredocs.org/clojure.core/map_q)
- [nat-int?](https://clojuredocs.org/clojure.core/nat-int_q)
- [neg-int?](https://clojuredocs.org/clojure.core/neg-int_q)
- [nil?](https://clojuredocs.org/clojure.core/nil_q)
- [number?](https://clojuredocs.org/clojure.core/number_q)
- [odd?](https://clojuredocs.org/clojure.core/odd_q)
- [pos?](https://clojuredocs.org/clojure.core/pos_q)
- [seq?](https://clojuredocs.org/clojure.core/seq_q)
- [sequential?](https://clojuredocs.org/clojure.core/sequential_q)
- [string?](https://clojuredocs.org/clojure.core/string_q)
- [true?](https://clojuredocs.org/clojure.core/true_q)
- [uri?](https://clojuredocs.org/clojure.core/uri_q)
- [uuid?](https://clojuredocs.org/clojure.core/uuid_q)
- [vector?](https://clojuredocs.org/clojure.core/vector_q)
- [zero?](https://clojuredocs.org/clojure.core/zero_q)


### Custom Predicates

- `ok?` -- `(and (>= % 200) (< % 300))`
- `4xx?`
- `5xx?`

## Regex

String values starting with the number sign (#) are considered as [regex patterns](https://clojure.org/reference/other_functions#regex).

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
1. If there is no expected field in the response, the error is not obvious. 
1. add possibility to clean server after test case run
1. Request body is printed as a string
1. Conf validation. Unobviuos error if AIDBOX_URL is undefined
1. ok? is not ok. `404 != #object[matcho$fn__1973 0x113eed88 "matcho$fn__1973@113eed88"]`
1. readme for skip/only keywords

