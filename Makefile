.PHONY: clean repl npm resource/VERSION build-native build docker oub jar deploy all
.EXPORT_ALL_VARIABLES:

VERSION=$(shell cat VERSION)
DATE = $(shell date)

IMAGE_NAME=aidbox/stresty
IMG=${IMAGE_NAME}:${VERSION}

clean:
	rm pom.xml && rm -rf .cpcache

repl:
	mkdir -p target/shadow/dev && mkdir -p target/stylo/dev && clj -M:nrepl:ui:build -m nrepl.cmdline --middleware "[cider.nrepl/cider-middleware refactor-nrepl.middleware/wrap-refactor]"

npm:
	npm install

resources/VERSION:
	cp VERSION resources/VERSION

# export GRAALVM_HOME=$HOME/graalvm/Contents/Home
# clojure -A:native-image --graalvm-opt 'H:ReflectionConfigurationFiles=reflection.json'
build-native: resources/VERSION
	clojure -M:native-image

build: resources/VERSION
	clojure -M:run-test && clojure -M:ui:build -m build  && cp target/uberjar/stresty-*-standalone.jar target/stresty.jar
	cp -R resources/public/* dist
	cp -R target/stylo/release/public/* dist
	cp -R target/shadow/release/public/* dist
	rm resources/VERSION

docker:
	docker build -t ${IMG} .

pub:
	docker push ${IMG}

jar:
	clj -M:ui:build -m build && cp target/uberjar/stresty-*-standalone.jar target/stresty.jar

deploy:
	cd deploy && envsubst < kustomization.template.yaml > kustomization.yaml && kubectl apply -k .
	kubectl rollout restart deployment stresty-app -n stresty
	kubectl get pod -n stresty

all: jar docker pub deploy
	echo "Done"

