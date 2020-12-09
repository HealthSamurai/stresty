.EXPORT_ALL_VARIABLES:

clean:
	rm pom.xml && rm -rf .cpcache

repl:
	mkdir -p target/shadow/dev && mkdir -p target/stylo/dev && clj -M:nrepl:ui:build -m nrepl.cmdline --middleware "[cider.nrepl/cider-middleware refactor-nrepl.middleware/wrap-refactor]"

npm:
	npm install && npx webpack --config webpack.config.js --mode=production

resources/VERSION:
	cp VERSION resources/VERSION

# export GRAALVM_HOME=$HOME/graalvm/Contents/Home
# clojure -A:native-image --graalvm-opt 'H:ReflectionConfigurationFiles=reflection.json'
build-native: resources/VERSION
	clojure -M:native-image

build: resources/VERSION
	clojure -A:run-test && clojure -M:ui:build -m build  && cp target/uberjar/stresty-*-standalone.jar target/stresty.jar
	rm resources/VERSION

jar:
	clj -M:ui:build -m build
