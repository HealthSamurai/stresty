.EXPORT_ALL_VARIABLES:

repl:
	clj -A:test:nrepl -e "(-main)" -r

# export GRAALVM_HOME=$HOME/graalvm/Contents/Home
# clojure -A:native-image --graalvm-opt 'H:ReflectionConfigurationFiles=reflection.json'
build-native:
	clojure -A:native-image

build:
	clojure -A:build
