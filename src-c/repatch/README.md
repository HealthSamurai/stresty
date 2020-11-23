# sem(antic)p(atc)h

semantic diff/merge library


1. Keep old value in a diff for safe patch

```clj
{:a 1} => {:a 2}
{:a [::change 1 2]}

{:a 1} => {}
{:a [::delete 1]}

{:b {:a 1} :rest 2} => {:b {:a 2} :rest 2}
{:b {:a [::change 1 2]}}

``

2. Use schema to get semantic info


```clj
{:a {:type ::set, :schema {...}}}
{:a [1 2]} => {:a [1 3]}
{:a [::set [::conj 3] 
           [::disj 2]]}


{:a {:type ::key :key :system, :schema {...}}}
{:a [{:system "phone" :value 1} {:system "fax" :value 2}]} => {:a [{:system "fax" :value 2}]}
{:a [::batch [::match {:system "fax"} [::patch {:value [::change 1 2]}]] 
             [::match {:system "phone"} [::delete {:system "phone" :value 1}]]]}
```


```clj
(diff schema old new) => diff
(patch old diff {::safe? boolean})
```

Pluggable custom conflict resolution and differs

## Basic operations

### patch

Apply patch recursively
```clj
[::patch <patch-expression>]

```

### change

```clj 
[::change old-value new-value]
```

While patch in ::safe mode check old-value

### delete

```clj
[::delete old-value]
```

While patch in ::safe mode check old-value

### set (conj, disj)

If schema :type ::set produce set semantic operatiions

```clj
[::set [::conj 1 2] [::disj 3 4]]
```

### key

If schema :type = ::key inerpret collection as map by key
In safe mode verfy assumption for only one key per collectio

```clj
[::key
  [::match {:key "value1"} [::patch ...]]
  [::match {:key "value2"} [::delete ...]]]
```
