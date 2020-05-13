# DaD

DaD is a [Docs as Data][docs-as-data] toolkit — a [CLI][cli] tool *and* a [Clojure][clojure]
library.

Right now it’s focused on:

* Authoring and maintaining a database that is reified as a set of [YAML][yaml] files
* Rendering textual documents/pages from that database and a set of [templates][templates]


## Status

This tool is in a very early stage of development, and is probably not suitable for anyone but its
developers to use.

**If** you’re familiar with Clojure, [Markdown][markdown], **and** templating systems such as
[Django][django], [Jinja][jinja], or [Liquid][liquid], **and** you’re comfortable reading the
source code thoroughly before usage, **and** you’re comfortable running the tool from a Clojure
[REPL][repl] — then it *might* not be a terrible idea for you to try using this tool.


## Documentation

* [Contributing][contributing]


[cli]: https://en.wikipedia.org/wiki/Command-line_interface
[clojure]: https://clojure.org/
[contributing]: doc/contributing.md
[django]: https://www.djangoproject.com/
[docs-as-data]: doc/docs-as-data.md
[jinja]: https://jinja.palletsprojects.com/en/2.11.x/
[liquid]: https://shopify.github.io/liquid/
[markdown]: https://en.wikipedia.org/wiki/Markdown
[repl]: https://en.wikipedia.org/wiki/REPL
[templates]: doc/templates.md
[yaml]: https://yaml.org/
