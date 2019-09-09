## REST cases runner

CLI tool for REST tests.

Download [latest standalone release](https://github.com/Aidbox/stresty/releases/latest).

Create file `test.yaml`:
```yaml
desc: Create & Read Patient

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
      name: [{given: ['Petr'], family: 'Pupkin'}]
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
      name: [{family: 'Pupkin'}]
```

Run script

```sh
$ AIDBOX_URL=http://<box_name>.aidbox.app java -jar stresty-1.0.0-standalone.jar test.yaml 

```

# todo

Auth is not supported.
