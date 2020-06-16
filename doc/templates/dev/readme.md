# Developing and Testing DaD


## Dependencies

{% for name, props in dependencies %}🆇
* [{{ name }}]({{ props.url }}) for {{ props.for }}
{% endfor %}


## Scripts

We have a few useful scripts:

<dl>
{% for name, props in scripts %}🆇
  <dt><code>{{ name }}</code></dt>
  <dd>{{ props.summary }}</dd>
{% endfor %}
</dl>


## Testing

This project uses [Kaocha][kaocha] as its test runner.

Each subdirectory under `<project-root>/test` is a test suite, e.g. `examples`, `integration`,
`property`, etc.


### Running the tests

#### From a shell

```shell
# Run all suites
bin/kaocha

# Run a single suite
bin/kaocha examples
```

#### From a REPL

```clojure
(use 'kaocha.repl)

; Run all suites
(run-all)

; Run a single suite
(run :examples)

; Run a single namespace
(run 'dad.rendering.tags-test)

; Run a single test var
(run 'dad.rendering.tags-tags/test-exec)
```


[kaocha]: https://github.com/lambdaisland/kaocha
