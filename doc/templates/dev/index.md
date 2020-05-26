# Developing and Testing DaD


## Dependencies

{% for name, props in dependencies %}🆇
* [{{ name }}]({{ props.url }}) for {{ props.for }}
{% endfor %}


## Testing

Coming soon!


## Building

We have a few useful scripts:

* bin/build-uberjar
* bin/build-native-image

The latter will build an executable that can run on the same OS and architecture as the machine on
which it’s run.
